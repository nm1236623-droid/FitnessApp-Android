package com.example.fitness.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "auth_session")

class SessionRepository(private val context: Context) {
    private val KEY_LAST_ACTIVE_MS = longPreferencesKey("auth_last_active_ms")
    private val KEY_UID = stringPreferencesKey("auth_uid")

    val lastActiveEpochMillis: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_ACTIVE_MS]
    }

    suspend fun getLastActiveEpochMillis(): Long? {
        return context.dataStore.data.first()[KEY_LAST_ACTIVE_MS]
    }

    suspend fun setLastActive(epochMillis: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_ACTIVE_MS] = epochMillis
        }
    }

    suspend fun setLastActiveNow() {
        setLastActive(System.currentTimeMillis())
    }

    suspend fun setUid(uid: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_UID] = uid
        }
    }

    suspend fun getUid(): String? {
        return context.dataStore.data.first()[KEY_UID]
    }

    suspend fun clear() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_LAST_ACTIVE_MS)
            prefs.remove(KEY_UID)
        }
    }
}
