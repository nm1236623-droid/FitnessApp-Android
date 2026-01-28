package com.example.fitness.coach.cloud

import com.example.fitness.data.TrainingPlan

/**
 * A received plan with metadata about where it came from.
 *
 * We keep this separate from [TrainingPlan] to avoid breaking local plan usage.
 */
data class RemotePlanItem(
    val plan: TrainingPlan,
    /** Coach uid if known (from inbox plans or coach plans). */
    val sourceCoachId: String? = null,
    /** Optional coach display name if available. */
    val sourceCoachDisplayName: String? = null,
    /** True if this plan came from trainee inbox (single-target publish). */
    val isInbox: Boolean = false,
)

