package com.example.fitness.coach

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore by preferencesDataStore(name = "coach_auth")

class CoachAuthRepository(private val context: Context) {

    private val KEY_USER_ID = stringPreferencesKey("coach_user_id")
    private val KEY_ROLE = stringPreferencesKey("coach_user_role")
    private val KEY_NAME = stringPreferencesKey("coach_display_name")

    val userId: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_ID] ?: ""
    }

    val role: Flow<UserRole> = context.dataStore.data.map { prefs ->
        when (prefs[KEY_ROLE]) {
            UserRole.COACH.name -> UserRole.COACH
            UserRole.TRAINEE.name -> UserRole.TRAINEE
            else -> UserRole.TRAINEE
        }
    }

    val displayName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_NAME] ?: ""
    }

    suspend fun ensureUserId(): String {
        var id: String? = null
        context.dataStore.edit { prefs ->
            val existing = prefs[KEY_USER_ID]
            if (existing.isNullOrBlank()) {
                val newId = UUID.randomUUID().toString()
                prefs[KEY_USER_ID] = newId
                id = newId
            } else {
                id = existing
            }
        }
        return id ?: UUID.randomUUID().toString()
    }

    suspend fun setRole(role: UserRole) {
        context.dataStore.edit { it[KEY_ROLE] = role.name }
    }

    suspend fun setDisplayName(name: String) {
        context.dataStore.edit { it[KEY_NAME] = name }
    }

    suspend fun resetUser() {
        context.dataStore.edit { prefs ->
            prefs[KEY_USER_ID] = UUID.randomUUID().toString()
        }
    }
}
