package com.example.fitness.ai

import com.example.fitness.data.ExerciseEntry
import com.example.fitness.data.TrainingPlan
import org.json.JSONArray
import org.json.JSONObject

/**
 * Parses Gemini recommended plan response.
 * We ask Gemini for strict JSON, then parse into existing TrainingPlan/ExerciseEntry.
 */
object GeminiTrainingPlanParser {

    data class DayPlan(
        val day: Int,
        val focus: String? = null,
        val exercises: List<ExerciseEntry>
    )

    data class ParsedPlan(
        val name: String,
        val exercises: List<ExerciseEntry>,
        /** Optional multi-day breakdown; when present UI can show day-by-day. */
        val days: List<DayPlan>? = null
    )

    // In-memory bridge so we can show day-by-day preview without changing persistent TrainingPlan schema.
    private val lastParsedDaysByPlanId = mutableMapOf<String, List<DayPlan>>()

    fun parseJson(json: String): ParsedPlan {
        // Gemini sometimes wraps JSON in code fences.
        val cleaned = json.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val root = JSONObject(cleaned)
        val name = root.optString("name").ifBlank { "AI 推薦計畫" }

        // New schema: { name, days: [ { day, focus, exercises: [...] } ] }
        val daysArr = root.optJSONArray("days")
        if (daysArr != null && daysArr.length() > 0) {
            val days = parseDays(daysArr)
            val flat = days.flatMap { it.exercises }
            if (flat.isEmpty()) throw IllegalArgumentException("Gemini plan JSON contains no exercises")
            return ParsedPlan(name = name, exercises = flat, days = days)
        }

        // Legacy schema: { name, exercises: [...] }
        val exercisesArr: JSONArray = root.optJSONArray("exercises") ?: JSONArray()
        val exercises = parseExercises(exercisesArr)

        if (exercises.isEmpty()) {
            throw IllegalArgumentException("Gemini plan JSON contains no exercises")
        }

        return ParsedPlan(name = name, exercises = exercises)
    }

    private fun parseDays(daysArr: JSONArray): List<DayPlan> = buildList {
        for (i in 0 until daysArr.length()) {
            val d = daysArr.optJSONObject(i) ?: continue
            val dayNum = d.optInt("day", i + 1).takeIf { it > 0 } ?: (i + 1)
            val focus = d.optString("focus").trim().ifBlank { null }
            val exercisesArr = d.optJSONArray("exercises") ?: JSONArray()
            val exercises = parseExercises(exercisesArr)
            if (exercises.isNotEmpty()) add(DayPlan(day = dayNum, focus = focus, exercises = exercises))
        }
    }

    private fun parseExercises(exercisesArr: JSONArray): List<ExerciseEntry> = buildList {
        for (i in 0 until exercisesArr.length()) {
            val o = exercisesArr.optJSONObject(i) ?: continue
            val exName = o.optString("name").trim()
            if (exName.isBlank()) continue
            val sets = o.optInt("sets", 0).takeIf { it > 0 }
            val reps = o.optInt("reps", 0).takeIf { it > 0 }
            val weight = o.optDouble("weightKg", Double.NaN)
                .takeIf { !it.isNaN() && it >= 0.0 }
                ?.toFloat()

            add(ExerciseEntry(name = exName, sets = sets, reps = reps, weight = weight))
        }
    }

    fun toTrainingPlan(parsed: ParsedPlan): TrainingPlan {
        val plan = TrainingPlan(name = parsed.name, exercises = parsed.exercises)
        parsed.days?.let { days ->
            if (days.isNotEmpty()) lastParsedDaysByPlanId[plan.id] = days
        }
        return plan
    }

    /**
     * If this TrainingPlan was created from a "days" response in the current session,
     * this returns the day-by-day breakdown for UI preview.
     */
    fun extractDaysOrNull(plan: TrainingPlan): List<DayPlan>? = lastParsedDaysByPlanId[plan.id]
}
