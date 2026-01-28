package com.example.fitness.coach.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "coach_plan_selection")

/**
 * Stores which local TrainingPlan ids are considered "coach plans" (eligible to show in Coach Center).
 *
 * This separates:
 *  - Personal plans (Workout -> choose part -> create)
 *  - Coach plans (explicitly added to Coach Center)
 */
class CoachPlanSelectionRepository(private val context: Context) {

    private fun key(planId: String) = booleanPreferencesKey("coach_plan_$planId")

    val selectedPlanIds: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs.asMap()
            .filter { (k, v) -> k.name.startsWith("coach_plan_") && v == true }
            .map { (k, _) -> k.name.removePrefix("coach_plan_") }
            .toSet()
    }

    suspend fun setSelected(planId: String, selected: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[key(planId)] = selected
        }
    }

    suspend fun remove(planId: String) {
        context.dataStore.edit { prefs ->
            prefs.remove(key(planId))
        }
    }
}

