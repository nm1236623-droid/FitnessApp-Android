package com.example.fitness.coach

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "coach_display_names")

/**
 * Stores user-customizable labels for showing ids in UI.
 *
 * Note: this does NOT change FirebaseAuth uid / Firestore ids; it's UI-only.
 */
class CoachDisplayNameRepository(private val context: Context) {

    private val KEY_COACH_NAME = stringPreferencesKey("display_name_coach")
    private val KEY_TRAINEE_NAME = stringPreferencesKey("display_name_trainee")

    val coachDisplayName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_COACH_NAME] ?: ""
    }

    val traineeDisplayName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_TRAINEE_NAME] ?: ""
    }

    suspend fun setCoachDisplayName(name: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_COACH_NAME] = name.trim()
        }
    }

    suspend fun setTraineeDisplayName(name: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TRAINEE_NAME] = name.trim()
        }
    }
}

