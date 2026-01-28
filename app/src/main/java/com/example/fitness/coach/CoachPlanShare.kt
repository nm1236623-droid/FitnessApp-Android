package com.example.fitness.coach

import com.example.fitness.data.ExerciseEntry

/**
 * V1 share payload. Kept minimal and versioned so we can evolve later.
 *
 * This is meant to be encoded as JSON then Base64, with a prefix.
 */
data class CoachPlanShare(
    val version: Int = 1,
    val shareId: String,
    val coachId: String,
    val coachName: String? = null,
    val planName: String,
    val exercises: List<ExerciseEntry>,
    val createdAtEpochMs: Long,
)

