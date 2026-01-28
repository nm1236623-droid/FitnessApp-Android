package com.example.fitness.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import java.time.Instant
import java.util.UUID
import java.time.LocalDate

// Exercise entry with optional reps and sets
data class ExerciseEntry(
    val name: String,
    val reps: Int? = null,
    val sets: Int? = null,
    val weight: Float? = null
)

// Simple immutable data model for a training plan
data class TrainingPlan(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val exercises: List<ExerciseEntry> = emptyList(),
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    /**
     * Coach-center only:
     * - null: not published yet
     * - non-null: already published to cloud at this time
     */
    val publishedAt: Instant? = null,
    val favorite: Boolean = false
)

class TrainingPlanRepository {
    private val _plans = MutableStateFlow<List<TrainingPlan>>(emptyList())
    val plans: Flow<List<TrainingPlan>> = _plans.asStateFlow()

    // Simple in-memory schedule mapping dates to training plan ids
    private val _schedules = MutableStateFlow<Map<LocalDate, List<String>>>(emptyMap())
    val schedules: Flow<Map<LocalDate, List<String>>> = _schedules.asStateFlow()

    // Add a plan to a date
    fun addScheduledPlan(date: LocalDate, planId: String) {
        val m = _schedules.value.toMutableMap()
        val list = m[date]?.toMutableList() ?: mutableListOf()
        if (!list.contains(planId)) list.add(planId)
        m[date] = list
        _schedules.value = m
    }

    // Remove a scheduled plan from a date
    fun removeScheduledPlan(date: LocalDate, planId: String) {
        val m = _schedules.value.toMutableMap()
        val list = m[date]?.toMutableList() ?: mutableListOf()
        if (list.remove(planId)) {
            if (list.isEmpty()) m.remove(date) else m[date] = list
            _schedules.value = m
        }
    }

    // Get plan objects scheduled for a date (snapshot)
    fun getPlansForDate(date: LocalDate): List<TrainingPlan> {
        val ids = _schedules.value[date] ?: return emptyList()
        return ids.mapNotNull { id -> _plans.value.firstOrNull { it.id == id } }
    }

    // Observe plans scheduled for a date
    fun getPlansForDateFlow(date: LocalDate): Flow<List<TrainingPlan>> = _schedules.map { map ->
        (map[date] ?: emptyList()).mapNotNull { id -> _plans.value.firstOrNull { it.id == id } }
    }

    // Get single plan (snapshot)
    fun getPlan(id: String): TrainingPlan? = _plans.value.firstOrNull { it.id == id }

    // Observe a single plan as Flow
    fun getPlanFlow(id: String): Flow<TrainingPlan?> = _plans.map { list -> list.firstOrNull { it.id == id } }

    // Create
    fun addPlan(plan: TrainingPlan) {
        // Prepend the new plan so that newly created plans appear at the top
        _plans.value = listOf(plan) + _plans.value
    }

    // Delete
    fun removePlan(id: String) {
        _plans.value = _plans.value.filterNot { it.id == id }
    }

    // Replace/Update
    fun updatePlan(updated: TrainingPlan) {
        _plans.value = _plans.value.map { if (it.id == updated.id) updated.copy(updatedAt = Instant.now()) else it }
    }

    fun updatePlanName(id: String, newName: String) {
        _plans.value = _plans.value.map {
            if (it.id == id) it.copy(name = newName, updatedAt = Instant.now()) else it
        }
    }

    fun updateExercises(id: String, newExercises: List<ExerciseEntry>) {
        _plans.value = _plans.value.map {
            if (it.id == id) it.copy(exercises = newExercises, updatedAt = Instant.now()) else it
        }
    }

    fun reorderExercises(id: String, fromIndex: Int, toIndex: Int) {
        val plan = getPlan(id) ?: return
        val list = plan.exercises.toMutableList()
        if (fromIndex !in list.indices || toIndex !in 0..list.size) return
        val item = list.removeAt(fromIndex)
        val insertIndex = if (toIndex > list.size) list.size else toIndex
        list.add(insertIndex, item)
        updateExercises(id, list)
    }

    fun duplicatePlan(id: String): TrainingPlan? {
        val original = getPlan(id) ?: return null
        val copy = original.copy(
            id = UUID.randomUUID().toString(),
            name = original.name + " (複製)",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        addPlan(copy)
        return copy
    }

    fun findByName(query: String): List<TrainingPlan> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        return _plans.value.filter { it.name.lowercase().contains(q) }
    }

    fun clearPlans() {
        _plans.value = emptyList()
    }

    fun replaceAll(newPlans: List<TrainingPlan>) {
        _plans.value = newPlans
    }

    fun exportToJson(): String {
        val root = JSONArray()
        _plans.value.forEach { p ->
            val obj = org.json.JSONObject()
            obj.put("id", p.id)
            obj.put("name", p.name)
            val exercisesArr = JSONArray()
            p.exercises.forEach { e ->
                val eo = org.json.JSONObject()
                eo.put("name", e.name)
                eo.put("reps", e.reps)
                eo.put("sets", e.sets)
                eo.put("weight", e.weight)
                exercisesArr.put(eo)
            }
            obj.put("exercises", exercisesArr)
            obj.put("createdAt", p.createdAt.toString())
            obj.put("updatedAt", p.updatedAt.toString())
            obj.put("favorite", p.favorite)
            root.put(obj)
        }
        return root.toString()
    }

    fun importFromJson(json: String) {
        try {
            val arr = org.json.JSONArray(json)
            val parsed = mutableListOf<TrainingPlan>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.optString("id", UUID.randomUUID().toString())
                val name = if (obj.has("name")) obj.optString("name") else null
                if (name.isNullOrBlank()) continue
                val exercisesJson = obj.optJSONArray("exercises")
                val exercises = mutableListOf<ExerciseEntry>()
                if (exercisesJson != null) {
                    for (j in 0 until exercisesJson.length()) {
                        val eo = exercisesJson.optJSONObject(j) ?: continue
                        val ename = eo.optString("name", "")
                        val repsVal = if (eo.has("reps")) {
                            val r = eo.optInt("reps")
                            if (r == 0 && eo.isNull("reps")) null else r
                        } else null
                        val setsVal = if (eo.has("sets")) {
                            val s = eo.optInt("sets")
                            if (s == 0 && eo.isNull("sets")) null else s
                        } else null
                        val weightVal = if (eo.has("weight")) {
                            val w = eo.optDouble("weight")
                            if (w == 0.0 && eo.isNull("weight")) null else w.toFloat()
                        } else null
                        if (ename.isNotBlank()) exercises.add(ExerciseEntry(name = ename, reps = repsVal, sets = setsVal, weight = weightVal))
                    }
                }
                val createdAt = try { Instant.parse(obj.optString("createdAt")) } catch (_: Exception) { Instant.now() }
                val updatedAt = try { Instant.parse(obj.optString("updatedAt")) } catch (_: Exception) { Instant.now() }
                val favorite = obj.optBoolean("favorite", false)
                parsed.add(
                    TrainingPlan(
                        id = id,
                        name = name,
                        exercises = exercises,
                        createdAt = createdAt,
                        updatedAt = updatedAt,
                        favorite = favorite
                    )
                )
            }
            replaceAll(parsed)
        } catch (_: Exception) {
            // ignore
        }
    }

    // Seed default abdominal plans (upper & lower) with default sets = 8 if they don't already exist
    fun seedAbdominalPlansIfMissing() {
        val existingNames = _plans.value.map { it.name }

        if ("腹部 - 上腹訓練 (上半部)" !in existingNames) {
            val upperExercises = listOf(
                // 入門
                ExerciseEntry("仰臥起坐 Sit-Up", reps = null, sets = 8),
                ExerciseEntry("卷腹 Crunch", reps = null, sets = 8),
                ExerciseEntry("十字卷腹 Cross Crunch", reps = null, sets = 8),
                ExerciseEntry("膝蓋碰手卷腹 Knee-to-Hand Crunch", reps = null, sets = 8),
                ExerciseEntry("上腹彈震 Crunch Pulse", reps = null, sets = 8),
                // 進階
                ExerciseEntry("負重卷腹 Weighted Crunch", reps = null, sets = 8),
                ExerciseEntry("Cable Crunch（拉繩捲腹）", reps = null, sets = 8),
                ExerciseEntry("Decline Sit-Up（下斜仰臥起坐）", reps = null, sets = 8)
            )
            addPlan(TrainingPlan(name = "腹部 - 上腹訓練 (上半部)", exercises = upperExercises))
        }

        if ("腹部 - 下腹訓練 (下半部)" !in existingNames) {
            val lowerExercises = listOf(
                // 入門
                ExerciseEntry("抬腿 Leg Raise", reps = null, sets = 8),
                ExerciseEntry("仰臥屈膝抬腿 Knee Raise", reps = null, sets = 8),
                ExerciseEntry("反向捲腹 Reverse Crunch", reps = null, sets = 8),
                ExerciseEntry("仰臥交替腳踢 Flutter Kicks", reps = null, sets = 8),
                // 進階
                ExerciseEntry("懸垂舉腿 Hanging Leg Raise", reps = null, sets = 8),
                ExerciseEntry("懸垂屈膝 Knee Raise", reps = null, sets = 8),
                ExerciseEntry("Cable Leg Raise", reps = null, sets = 8)
            )
            addPlan(TrainingPlan(name = "腹部 - 下腹訓練 (下半部)", exercises = lowerExercises))
        }
    }

    // Create a TrainingPlan from selected exercise group ids (e.g., "abs_upper", "abs_lower")
    fun createPlanFromGroups(name: String, groupIds: List<String>): TrainingPlan {
        val entries = mutableListOf<ExerciseEntry>()
        for (gid in groupIds) {
            val g = com.example.fitness.data.ExerciseLibrary.getGroupById(gid) ?: continue
            entries.addAll(g.exercises.map { com.example.fitness.data.ExerciseLibrary.toEntry(it) })
        }
        val plan = TrainingPlan(name = name, exercises = entries)
        addPlan(plan)
        return plan
    }
}
