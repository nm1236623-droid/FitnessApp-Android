package com.example.fitness.inbody

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.util.UUID

data class InBodyRecord(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Instant,
    val weightKg: Float,
    val bodyFatPercent: Float?,
    val muscleMassKg: Float?
)

/**
 * 體態組成統計數據
 */
data class BodyCompositionStats(
    val latestWeight: Float = 0f,
    val latestBodyFatPercent: Float? = null,
    val latestMuscleMassKg: Float? = null,
    val weightChange: Float = 0f,
    val bodyFatChange: Float = 0f,
    val muscleChange: Float = 0f,
    val averageWeight: Float = 0f,
    val averageBodyFatPercent: Float = 0f,
    val averageMuscleMassKg: Float = 0f,
    val totalRecords: Int = 0,
    val lastUpdated: Instant = Instant.now()
)

class InBodyRepository(
    private val context: Context? = null,
    private val useFirebase: Boolean = false
) {
    private val _records = MutableStateFlow<List<InBodyRecord>>(emptyList())
    val records = _records.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()
    private val storageFileName = "inbody_records.json"

    private data class InBodyRecordDto(
        val id: String,
        val timestampEpochMillis: Long,
        val weightKg: Float,
        val bodyFatPercent: Float?,
        val muscleMassKg: Float?,
    )

    init {
        if (useFirebase) {
            // 使用 Firebase 數據源
            scope.launch {
                FirebaseInBodyRepository.records.collect { list ->
                    _records.value = list
                }
            }
        } else if (context != null) {
            // 使用本地檔案數據源
            scope.launch {
                loadFromFile()
            }
        }
    }

    fun add(record: InBodyRecord) {
        if (useFirebase) {
            scope.launch {
                FirebaseInBodyRepository.addRecord(record)
                    .onSuccess { 
                        Log.d("InBodyRepo", "Successfully added record to Firebase")
                    }
                    .onFailure { error ->
                        Log.e("InBodyRepo", "Failed to add record to Firebase: ${error.message}")
                    }
            }
        } else {
            _records.value = _records.value + record
            persistToFile()
        }
    }

    fun remove(id: String) {
        if (useFirebase) {
            scope.launch {
                FirebaseInBodyRepository.deleteRecord(id)
                    .onSuccess { 
                        Log.d("InBodyRepo", "Successfully deleted record from Firebase")
                    }
                    .onFailure { error ->
                        Log.e("InBodyRepo", "Failed to delete record from Firebase: ${error.message}")
                    }
            }
        } else {
            _records.value = _records.value.filterNot { it.id == id }
            persistToFile()
        }
    }

    fun clear() {
        if (useFirebase) {
            scope.launch {
                FirebaseInBodyRepository.clear()
                    .onSuccess { 
                        Log.d("InBodyRepo", "Successfully cleared all records from Firebase")
                    }
                    .onFailure { error ->
                        Log.e("InBodyRepo", "Failed to clear records from Firebase: ${error.message}")
                    }
            }
        } else {
            _records.value = emptyList()
            persistToFile()
        }
    }

    suspend fun update(record: InBodyRecord) {
        if (useFirebase) {
            FirebaseInBodyRepository.updateRecord(record)
                .onSuccess { 
                    Log.d("InBodyRepo", "Successfully updated record in Firebase")
                }
                .onFailure { error ->
                    Log.e("InBodyRepo", "Failed to update record in Firebase: ${error.message}")
                }
        } else {
            _records.value = _records.value.map { if (it.id == record.id) record else it }
            persistToFile()
        }
    }

    suspend fun getLatestRecord(): InBodyRecord? {
        return if (useFirebase) {
            FirebaseInBodyRepository.getLatestRecord()
                .getOrNull()
        } else {
            _records.value.firstOrNull()
        }
    }

    suspend fun getRecordsByDateRange(
        startDate: Instant,
        endDate: Instant
    ): List<InBodyRecord> {
        return if (useFirebase) {
            FirebaseInBodyRepository.getRecordsByDateRange(startDate, endDate)
                .getOrNull() ?: emptyList()
        } else {
            _records.value.filter { 
                it.timestamp >= startDate && it.timestamp <= endDate 
            }.sortedByDescending { it.timestamp }
        }
    }

    suspend fun getBodyCompositionStats(): BodyCompositionStats? {
        return if (useFirebase) {
            FirebaseInBodyRepository.getBodyCompositionStats()
                .getOrNull()
        } else {
            calculateLocalStats()
        }
    }

    private fun calculateLocalStats(): BodyCompositionStats? {
        val records = _records.value
        if (records.isEmpty()) return null
        
        val latest = records.first()
        val previous = if (records.size >= 2) records[1] else null
        
        // 計算變化
        val weightChange = previous?.let { latest.weightKg - it.weightKg } ?: 0f
        val bodyFatChange = previous?.let { 
            latest.bodyFatPercent?.minus(it.bodyFatPercent ?: 0f) 
        } ?: 0f
        val muscleChange = previous?.let { 
            latest.muscleMassKg?.minus(it.muscleMassKg ?: 0f) 
        } ?: 0f
        
        // 計算平均值
        val avgWeight = records.map { it.weightKg }.average().toFloat()
        val avgBodyFat = records.mapNotNull { it.bodyFatPercent }.average().toFloat()
        val avgMuscle = records.mapNotNull { it.muscleMassKg }.average().toFloat()
        
        return BodyCompositionStats(
            latestWeight = latest.weightKg,
            latestBodyFatPercent = latest.bodyFatPercent,
            latestMuscleMassKg = latest.muscleMassKg,
            weightChange = weightChange,
            bodyFatChange = bodyFatChange,
            muscleChange = muscleChange,
            averageWeight = avgWeight,
            averageBodyFatPercent = avgBodyFat,
            averageMuscleMassKg = avgMuscle,
            totalRecords = records.size,
            lastUpdated = latest.timestamp
        )
    }

    private fun persistToFile() {
        val ctx = context ?: return
        val list = _records.value
        scope.launch {
            try {
                val file = File(ctx.filesDir, storageFileName)
                val dtos = list.map {
                    InBodyRecordDto(
                        id = it.id,
                        timestampEpochMillis = it.timestamp.toEpochMilli(),
                        weightKg = it.weightKg,
                        bodyFatPercent = it.bodyFatPercent,
                        muscleMassKg = it.muscleMassKg,
                    )
                }
                file.writeText(gson.toJson(dtos))
            } catch (e: Exception) {
                Log.w("InBodyRepo", "Failed to persist InBody records: ${e.message}")
            }
        }
    }

    private fun loadFromFile() {
        val ctx = context ?: return
        try {
            val file = File(ctx.filesDir, storageFileName)
            if (!file.exists()) return

            val json = file.readText()
            val type = object : TypeToken<List<InBodyRecordDto>>() {}.type
            val dtos: List<InBodyRecordDto> = gson.fromJson(json, type) ?: emptyList()

            _records.value = dtos.map {
                InBodyRecord(
                    id = it.id,
                    timestamp = Instant.ofEpochMilli(it.timestampEpochMillis),
                    weightKg = it.weightKg,
                    bodyFatPercent = it.bodyFatPercent,
                    muscleMassKg = it.muscleMassKg,
                )
            }

            Log.d("InBodyRepo", "Loaded ${_records.value.size} InBody records")
        } catch (e: Exception) {
            Log.w("InBodyRepo", "Failed to load InBody records: ${e.message}")
        }
    }
}
