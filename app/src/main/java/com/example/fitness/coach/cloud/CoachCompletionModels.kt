package com.example.fitness.coach.cloud

import java.time.Instant

/** A completion event reported by a trainee for a coach plan. */
data class CoachPlanCompletion(
    val id: String,
    val coachId: String,
    val traineeId: String,
    val planId: String,
    val planName: String,
    val completedAt: Instant,
)

