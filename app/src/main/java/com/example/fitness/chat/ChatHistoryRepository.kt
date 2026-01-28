package com.example.fitness.chat

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.chatDataStore by preferencesDataStore(name = "chat_history")

class ChatHistoryRepository(private val context: Context) {
    private val KEY_HISTORY = stringPreferencesKey("history_packed")

    /** A packed String representing the chat history. */
    val historyPackedFlow: Flow<String> = context.chatDataStore.data
        .map { prefs -> prefs[KEY_HISTORY] ?: "" }

    suspend fun setHistoryPacked(value: String) {
        context.chatDataStore.edit { prefs ->
            prefs[KEY_HISTORY] = value
        }
    }

    suspend fun clear() {
        context.chatDataStore.edit { prefs ->
            prefs.remove(KEY_HISTORY)
        }
    }
}

