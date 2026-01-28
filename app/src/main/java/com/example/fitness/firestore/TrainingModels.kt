package com.example.fitness.firestore

import java.time.LocalDate

/**
 * Firestore user training record.
 *
 * Collection layout (suggested):
 * users/{userId}/trainingRecords/{recordId}
 */
data class TrainingRecord(
    val id: String = "",
    val date: LocalDate = LocalDate.now(),
    val type: String = "", // e.g., chest/back/legs
    val durationMinutes: Int = 0,
    val exercises: List<ExerciseEntry> = emptyList()
)

data class ExerciseEntry(
    val name: String = "",
    val sets: Int = 0,
    val reps: Int = 0,
    val weight: Float = 0f
)
