package com.example.fitness.coach.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.fitness.data.ExerciseEntry
import com.example.fitness.data.TrainingPlan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.util.UUID

private val Context.dataStore by preferencesDataStore(name = "coach_local_plans")

/**
 * Persistent store for coach-only local plans.
 * These are NOT the user's personal plans, and they should not go into TrainingPlanRepository.
 */
class CoachLocalPlanRepository(private val context: Context) {

    // Expose app context for UI wiring (auth / cloud sync). Avoid leaking Activity.
    val appContext: Context get() = context.applicationContext

    private val KEY_JSON = stringPreferencesKey("coach_plans_json")

    val plans: Flow<List<TrainingPlan>> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_JSON].orEmpty()
        if (raw.isBlank()) emptyList() else decode(raw)
    }

    suspend fun add(plan: TrainingPlan) {
        context.dataStore.edit { prefs ->
            val list = decode(prefs[KEY_JSON].orEmpty()).toMutableList()
            // prepend
            list.add(0, plan)
            prefs[KEY_JSON] = encode(list)
        }
    }

    suspend fun remove(planId: String) {
        context.dataStore.edit { prefs ->
            val list = decode(prefs[KEY_JSON].orEmpty()).filterNot { it.id == planId }
            prefs[KEY_JSON] = encode(list)
        }
    }

    suspend fun update(plan: TrainingPlan) {
        context.dataStore.edit { prefs ->
            val list = decode(prefs[KEY_JSON].orEmpty()).map { if (it.id == plan.id) plan.copy(updatedAt = Instant.now()) else it }
            prefs[KEY_JSON] = encode(list)
        }
    }

    /**
     * Returns a de-duplicated list of all exercises that exist across all coach-local plans.
     * Useful when Create Plan wants to "sync" / inherit the full action list.
     */
    suspend fun getAllExercisesSnapshot(): List<ExerciseEntry> {
        val raw = context.dataStore.data.map { it[KEY_JSON].orEmpty() }
        // Avoid collecting flow indefinitely: read once
        val json = raw.first()
        if (json.isBlank()) return emptyList()
        val plans = decode(json)
        val seen = LinkedHashMap<String, ExerciseEntry>()
        plans.forEach { p ->
            p.exercises.forEach { e ->
                // de-dupe by name
                val key = e.name.trim()
                if (key.isNotBlank() && !seen.containsKey(key)) {
                    seen[key] = e
                }
            }
        }
        return seen.values.toList()
    }

    private fun encode(list: List<TrainingPlan>): String {
        val arr = JSONArray()
        list.forEach { p ->
            val o = JSONObject()
            o.put("id", p.id.ifBlank { UUID.randomUUID().toString() })
            o.put("name", p.name)
            o.put("createdAt", p.createdAt.toString())
            o.put("updatedAt", p.updatedAt.toString())
            o.put("publishedAt", p.publishedAt?.toString())
            o.put("favorite", p.favorite)
            val exArr = JSONArray()
            p.exercises.forEach { e ->
                val eo = JSONObject()
                eo.put("name", e.name)
                eo.put("reps", e.reps)
                eo.put("sets", e.sets)
                eo.put("weight", e.weight)
                exArr.put(eo)
            }
            o.put("exercises", exArr)
            arr.put(o)
        }
        return arr.toString()
    }

    private fun decode(json: String): List<TrainingPlan> {
        return try {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val id = o.optString("id", UUID.randomUUID().toString())
                    val name = o.optString("name", "").trim()
                    if (name.isBlank()) continue
                    val createdAt = try { Instant.parse(o.optString("createdAt")) } catch (_: Exception) { Instant.now() }
                    val updatedAt = try { Instant.parse(o.optString("updatedAt")) } catch (_: Exception) { Instant.now() }
                    val publishedAt = try {
                        val raw = o.optString("publishedAt", "")
                        if (raw.isNullOrBlank() || raw == "null") null else Instant.parse(raw)
                    } catch (_: Exception) {
                        null
                    }
                    val favorite = o.optBoolean("favorite", false)
                    val exArr = o.optJSONArray("exercises")
                    val exercises = mutableListOf<ExerciseEntry>()
                    if (exArr != null) {
                        for (j in 0 until exArr.length()) {
                            val eo = exArr.optJSONObject(j) ?: continue
                            val en = eo.optString("name", "").trim()
                            if (en.isBlank()) continue
                            val reps = if (eo.has("reps") && !eo.isNull("reps")) eo.optInt("reps") else null
                            val sets = if (eo.has("sets") && !eo.isNull("sets")) eo.optInt("sets") else null
                            val weight = if (eo.has("weight") && !eo.isNull("weight")) eo.optDouble("weight").toFloat() else null
                            exercises.add(ExerciseEntry(name = en, reps = reps, sets = sets, weight = weight))
                        }
                    }
                    add(
                        TrainingPlan(
                            id = id,
                            name = name,
                            exercises = exercises,
                            createdAt = createdAt,
                            updatedAt = updatedAt,
                            publishedAt = publishedAt,
                            favorite = favorite
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
