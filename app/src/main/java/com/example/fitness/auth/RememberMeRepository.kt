package com.example.fitness.auth

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.rememberMeDataStore by preferencesDataStore(name = "remember_me")

/**
 * Stores the user's opt-in choice for staying signed in.
 *
 * Note: We intentionally do NOT store passwords. FirebaseAuth already persists the auth session
 * on-device; this flag only controls whether the app should auto-enter the main screen.
 */
class RememberMeRepository(private val context: Context) {
    private val KEY_REMEMBER_ME = booleanPreferencesKey("remember_me_enabled")

    val rememberMeEnabledFlow: Flow<Boolean> = context.rememberMeDataStore.data
        .map { prefs -> prefs[KEY_REMEMBER_ME] ?: true }

    suspend fun isRememberMeEnabled(): Boolean {
        return context.rememberMeDataStore.data.first()[KEY_REMEMBER_ME] ?: true
    }

    suspend fun setRememberMeEnabled(enabled: Boolean) {
        context.rememberMeDataStore.edit { prefs ->
            prefs[KEY_REMEMBER_ME] = enabled
        }
    }
}

