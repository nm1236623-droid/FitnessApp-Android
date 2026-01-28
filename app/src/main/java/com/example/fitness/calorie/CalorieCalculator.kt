package com.example.fitness.calorie

object CalorieCalculator {
    // Simple MET-based calories burned estimate: calories = met * weightKg * hours
    fun estimateCalories(met: Double, weightKg: Double, durationMinutes: Double): Double {
        val hours = durationMinutes / 60.0
        return met * weightKg * hours
    }

    // BMR (Mifflin-St Jeor) - male/female flag
    fun estimateBMR(weightKg: Double, heightCm: Double, ageYears: Int, male: Boolean): Double {
        return if (male) {
            10 * weightKg + 6.25 * heightCm - 5 * ageYears + 5
        } else {
            10 * weightKg + 6.25 * heightCm - 5 * ageYears - 161
        }
    }
}

