package com.example.fitness.activity

import android.content.Context
import android.util.Log
import com.example.fitness.util.ExercisePartInfer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

// Include planId so we can determine which training plan a record is for
data class ActivityRecord(
    val id: String,
    val planId: String?,
    val type: String,
    val start: Instant,
    val end: Instant?,
    val calories: Double?,
    // new optional nutrient fields (grams)
    val proteinGrams: Double? = null,
    val carbsGrams: Double? = null,
    val fatGrams: Double? = null,
    // list of exercises performed during this activity (optional)
    val exercises: List<com.example.fitness.data.ExerciseEntry>? = null,
    val userId: String? = null // 添加 userId 字段支援 Firebase
)

/**
 * Repository with Firebase support - Room removed
 *
 * IMPORTANT: Public API is kept stable so ViewModels/UI do not change.
 */
class ActivityLogRepository(
    private val context: Context? = null,
    private val dataSource: ActivityLogDataSource? = null,
    private val useFirebase: Boolean = false
) {
    private val _logs = MutableStateFlow<List<ActivityRecord>>(emptyList())
    val logs = _logs.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        if (useFirebase) {
            // Use Firebase data source
            scope.launch {
                FirebaseActivityLogRepository.records.collect { list ->
                    _logs.value = list
                }
            }
        } else {
            // Use local data source
            dataSource?.let { source ->
                scope.launch {
                    source.observeAll().collect { list ->
                        _logs.value = list
                    }
                }
            }
        }
    }

    fun add(log: ActivityRecord) {
        // Optimistically update UI
        if (_logs.value.none { it.id == log.id }) {
            _logs.value = listOf(log) + _logs.value
        }

        scope.launch {
            if (useFirebase) {
                FirebaseActivityLogRepository.addRecord(log)
            } else {
                dataSource?.upsert(log)
            }
        }
    }

    fun remove(id: String) {
        _logs.value = _logs.value.filterNot { it.id == id }

        scope.launch {
            if (useFirebase) {
                FirebaseActivityLogRepository.deleteRecord(id)
            } else {
                dataSource?.deleteById(id)
            }
        }
    }

    fun clear() {
        _logs.value = emptyList()

        scope.launch {
            if (useFirebase) {
                FirebaseActivityLogRepository.clear()
            } else {
                dataSource?.clear()
            }
        }
    }

    suspend fun update(log: ActivityRecord) {
        _logs.value = _logs.value.map { if (it.id == log.id) log else it }

        if (useFirebase) {
            FirebaseActivityLogRepository.updateRecord(log)
        } else {
            dataSource?.upsert(log)
        }
    }

    fun getLogsForDate(date: java.time.LocalDate): List<ActivityRecord> {
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant()
        return _logs.value.filter { it.start >= start && it.start < end }
    }

    fun getLogsForDateRange(startDate: java.time.LocalDate, endDate: java.time.LocalDate): List<ActivityRecord> {
        val zone = ZoneId.systemDefault()
        val start = startDate.atStartOfDay(zone).toInstant()
        val end = endDate.plusDays(1).atStartOfDay(zone).toInstant()
        return _logs.value.filter { it.start >= start && it.start < end }
    }
}

interface ActivityLogDataSource {
    fun observeAll(): Flow<List<ActivityRecord>>
    suspend fun upsert(record: ActivityRecord)
    suspend fun deleteById(id: String)
    suspend fun clear()
}

// File-based data source for local storage
class FileActivityLogDataSource(private val context: Context) : ActivityLogDataSource {
    private val gson = com.google.gson.Gson()
    private val fileName = "activity_logs.json"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun observeAll(): Flow<List<ActivityRecord>> = kotlinx.coroutines.flow.flow {
        emit(loadFromFile())
    }

    override suspend fun upsert(record: ActivityRecord) {
        val logs = loadFromFile().toMutableList()
        val existingIndex = logs.indexOfFirst { it.id == record.id }
        if (existingIndex >= 0) {
            logs[existingIndex] = record
        } else {
            logs.add(0, record)
        }
        saveToFile(logs)
    }

    override suspend fun deleteById(id: String) {
        val logs = loadFromFile().toMutableList()
        logs.removeAll { it.id == id }
        saveToFile(logs)
    }

    override suspend fun clear() {
        saveToFile(emptyList())
    }

    private suspend fun loadFromFile(): List<ActivityRecord> {
        return try {
            val file = context.getFileStreamPath(fileName)
            if (!file.exists()) return emptyList()
            
            val content = context.openFileInput(fileName).bufferedReader().use { it.readText() }
            val type = object : com.google.gson.reflect.TypeToken<List<ActivityRecord>>() {}.type
            gson.fromJson(content, type) ?: emptyList()
        } catch (e: Exception) {
            Log.w("FileActivityLogDataSource", "Failed to load logs: ${e.message}")
            emptyList()
        }
    }

    private suspend fun saveToFile(logs: List<ActivityRecord>) {
        try {
            val content = gson.toJson(logs)
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use { it.write(content.toByteArray()) }
        } catch (e: Exception) {
            Log.e("FileActivityLogDataSource", "Failed to save logs: ${e.message}")
        }
    }
}
