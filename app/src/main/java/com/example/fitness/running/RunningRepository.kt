package com.example.fitness.running

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.time.Instant
import java.util.UUID

// New names

data class CardioRecord(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Instant,
    val durationSeconds: Int,
    val averageSpeedKph: Float?,
    val inclinePercent: Float?,
    val calories: Double? = null,
    val cardioType: CardioType = CardioType.WALK_OR_JOG,
    val userId: String? = null // 添加 userId 字段支援 Firebase
)

class CardioRepository(private val useFirebase: Boolean = false) {
    private val _records = MutableStateFlow<List<CardioRecord>>(emptyList())
    val records = _records.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        if (useFirebase) {
            // Use Firebase data source
            scope.launch {
                FirebaseRunningRepository.records.collect { list ->
                    _records.value = list
                }
            }
        }
    }

    fun add(record: CardioRecord) {
        // Optimistically update UI
        if (_records.value.none { it.id == record.id }) {
            _records.value = listOf(record) + _records.value
        }

        scope.launch {
            if (useFirebase) {
                FirebaseRunningRepository.addRecord(record)
            }
            // Local storage is handled by the optimistic update above
        }
    }

    fun remove(id: String) {
        _records.value = _records.value.filterNot { it.id == id }

        scope.launch {
            if (useFirebase) {
                FirebaseRunningRepository.deleteRecord(id)
            }
            // Local storage is handled by the optimistic update above
        }
    }

    fun clear() {
        _records.value = emptyList()

        scope.launch {
            if (useFirebase) {
                FirebaseRunningRepository.clear()
            }
            // Local storage is handled by the optimistic update above
        }
    }

    suspend fun update(record: CardioRecord) {
        _records.value = _records.value.map { if (it.id == record.id) record else it }

        if (useFirebase) {
            FirebaseRunningRepository.updateRecord(record)
        }
        // Local storage is handled by the optimistic update above
    }
}

// Backward-compatible aliases (so existing imports keep compiling)

typealias RunningRecord = CardioRecord

typealias RunningRepository = CardioRepository
