package com.example.fitness.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate



open class TrainingRecordRepository {
    open val _records = MutableStateFlow<List<TrainingRecord>>(emptyList())
    open val records: Flow<List<TrainingRecord>> = _records.asStateFlow()

    open fun addRecord(record: TrainingRecord) {
        _records.value = _records.value + record
    }

    open fun getRecordsForDate(date: LocalDate): List<TrainingRecord> {
        return _records.value.filter { it.date == date }
    }

    open fun getRecordsForPlan(planId: String): List<TrainingRecord> {
        return _records.value.filter { it.planId == planId }
    }
}
