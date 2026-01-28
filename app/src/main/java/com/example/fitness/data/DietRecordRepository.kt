package com.example.fitness.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.util.*

data class DietRecord(
    val id: String = UUID.randomUUID().toString(),
    val foodName: String,
    val calories: Int,
    val date: LocalDate,
    val mealType: String, // e.g., Breakfast, Lunch, Dinner, Snack
    // estimated macros (grams)
    val proteinG: Double? = null,
    val carbsG: Double? = null,
    val fatG: Double? = null,
    // store exact time for sorting / charting
    val timestamp: Instant = Instant.now(),
    val userId: String? = null // 添加 userId 字段支援 Firebase
)

class DietRecordRepository(
    private val context: Context? = null
) {
    private val _records = MutableStateFlow<List<DietRecord>>(emptyList())
    val records: Flow<List<DietRecord>> = _records.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()
    private val storageFileName = "diet_records.json"

    init {
        if (context != null) {
            scope.launch {
                SyncStrategy.getUseFirebase(context).collect { useFirebase ->
                    if (useFirebase) {
                        // Use Firebase data source
                        FirebaseDietRecordRepository.records.collect { list ->
                            _records.value = list
                        }
                    } else {
                        // Load from local file
                        loadFromFile()
                    }
                }
            }
        }
    }

    private data class DietRecordDto(
        val id: String,
        val foodName: String,
        val calories: Int,
        val dateIso: String,
        val mealType: String,
        val proteinG: Double? = null,
        val carbsG: Double? = null,
        val fatG: Double? = null,
        val timestampEpochMillis: Long? = null,
    )

    fun addRecord(record: DietRecord) {
        // Optimistically update UI
        if (_records.value.none { it.id == record.id }) {
            _records.value = listOf(record) + _records.value
        }

        scope.launch {
            if (context != null) {
                val useFirebase = SyncStrategy.getUseFirebase(context).first()
                if (useFirebase) {
                    FirebaseDietRecordRepository.addRecord(record)
                } else {
                    persistToFile()
                }
            }
        }
    }

    fun remove(id: String) {
        _records.value = _records.value.filterNot { it.id == id }

        scope.launch {
            if (context != null) {
                val useFirebase = SyncStrategy.getUseFirebase(context).first()
                if (useFirebase) {
                    FirebaseDietRecordRepository.deleteRecord(id)
                } else {
                    persistToFile()
                }
            }
        }
    }

    fun clear() {
        _records.value = emptyList()

        scope.launch {
            if (context != null) {
                val useFirebase = SyncStrategy.getUseFirebase(context).first()
                if (useFirebase) {
                    FirebaseDietRecordRepository.clear()
                } else {
                    persistToFile()
                }
            }
        }
    }

    fun getRecordsForDate(date: LocalDate): List<DietRecord> {
        return _records.value.filter { it.date == date }
    }

    suspend fun updateRecord(record: DietRecord) {
        _records.value = _records.value.map { if (it.id == record.id) record else it }

        if (context != null) {
            val useFirebase = SyncStrategy.getUseFirebase(context).first()
            if (useFirebase) {
                FirebaseDietRecordRepository.updateRecord(record)
            } else {
                persistToFile()
            }
        }
    }

    private suspend fun persistToFile() {
        if (context != null) {
            val useFirebase = SyncStrategy.getUseFirebase(context).first()
            if (useFirebase) return // Don't persist to file when using Firebase
            
            val ctx = context ?: return
            val list = _records.value
            scope.launch {
                try {
                    val file = File(ctx.filesDir, storageFileName)
                    val dtos = list.map {
                        DietRecordDto(
                            id = it.id,
                            foodName = it.foodName,
                            calories = it.calories,
                            dateIso = it.date.toString(),
                            mealType = it.mealType,
                            proteinG = it.proteinG,
                            carbsG = it.carbsG,
                            fatG = it.fatG,
                            timestampEpochMillis = it.timestamp.toEpochMilli(),
                        )
                    }
                    file.writeText(gson.toJson(dtos))
                } catch (e: Exception) {
                    Log.w("DietRecordRepo", "Failed to persist diet records: ${e.message}")
                }
            }
        }
    }

    private fun loadFromFile() {
        val ctx = context ?: return
        try {
            val file = File(ctx.filesDir, storageFileName)
            if (!file.exists()) return
            
            val content = file.readText()
            val dtos = gson.fromJson(content, Array<DietRecordDto>::class.java) ?: return
            
            val records = dtos.map { dto ->
                DietRecord(
                    id = dto.id,
                    foodName = dto.foodName,
                    calories = dto.calories,
                    date = LocalDate.parse(dto.dateIso),
                    mealType = dto.mealType,
                    proteinG = dto.proteinG,
                    carbsG = dto.carbsG,
                    fatG = dto.fatG,
                    timestamp = dto.timestampEpochMillis?.let { Instant.ofEpochMilli(it) } ?: Instant.now(),
                    userId = null
                )
            }
            
            _records.value = records
        } catch (e: Exception) {
            Log.w("DietRecordRepo", "Failed to load diet records: ${e.message}")
        }
    }
}
