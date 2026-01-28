package com.example.fitness.util

import java.util.Locale

/**
 * Single source of truth for mapping an exercise name -> a stable part key.
 *
 * Keys are intended to be stable identifiers (not localized strings):
 * - chest, back, legs, shoulders, arms, abs, other
 *
 * Rules requested:
 * - fly: ONLY "後三角/反向飛鳥/reverse fly/rear delt fly" count as shoulder;
 *   otherwise generic fly is chest.
 * - press: ONLY shoulder press / overhead / 推舉 / 肩推 are shoulders;
 *   bench / chest press are chest.
 */
object ExercisePartInfer {
    const val CHEST = "chest"
    const val BACK = "back"
    const val LEGS = "legs"
    const val SHOULDERS = "shoulders"
    const val ARMS = "arms"
    const val ABS = "abs"
    const val OTHER = "other"

    fun inferPartKey(exerciseName: String?): String {
        if (exerciseName.isNullOrBlank()) return OTHER
        val s = exerciseName.lowercase(Locale.getDefault()).trim()

        // --- abs ---
        if (s.contains("腹") || s.contains("abs") || s.contains("crunch") || s.contains("plank") || s.contains("leg raise")) {
            return ABS
        }

        // --- fly rules ---
        // Only rear-delt / reverse fly should be shoulders.
        if (
            s.contains("後三角") ||
            s.contains("反向飛鳥") ||
            s.contains("reverse fly") ||
            s.contains("rear delt fly") ||
            s.contains("rear-delt fly")
        ) {
            return SHOULDERS
        }
        // Generic fly defaults to chest (including dumbbell fly)
        if (s.contains("飛鳥") || s.contains(" fly")) {
            return CHEST
        }

        // --- press rules ---
        // Shoulder press / overhead / 推舉 / 肩推 => shoulders
        val isShoulderPress =
            s.contains("肩推") ||
                s.contains("推舉") ||
                s.contains("overhead") ||
                s.contains("shoulder press") ||
                s.contains("military press")

        // Bench/chest press => chest
        val isChestPress =
            s.contains("bench") ||
                s.contains("chest press") ||
                s.contains("臥推") ||
                s.contains("胸推")

        if (isShoulderPress && !isChestPress) return SHOULDERS

        // --- remaining parts (basic heuristics) ---
        return when {
            s.contains("胸") || isChestPress -> CHEST
            s.contains("背") || s.contains("row") || s.contains("lat") || s.contains("pull") -> BACK
            s.contains("腿") || s.contains("squat") || s.contains("leg") || s.contains("deadlift") -> LEGS
            s.contains("肩") || s.contains("shoulder") || s.contains("delt") -> SHOULDERS
            s.contains("腕") || s.contains("臂") || s.contains("arm") || s.contains("bicep") || s.contains("tricep") -> ARMS
            else -> OTHER
        }
    }
}

