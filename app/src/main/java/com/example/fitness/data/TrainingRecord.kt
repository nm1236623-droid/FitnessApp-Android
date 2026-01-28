package com.example.fitness.data

import java.time.LocalDate

data class TrainingRecord(
    val id: String = java.util.UUID.randomUUID().toString(),
    val planId: String,
    val planName: String,
    val date: LocalDate,
    val durationInSeconds: Int,
    val caloriesBurned: Double,
    val exercises: List<ExerciseRecord> = emptyList(),
    val notes: String? = null,
    val userId: String? = null
)

data class ExerciseRecord(
    val name: String,
    val sets: Int,
    val reps: Int,
    val weight: Double? = null
)
