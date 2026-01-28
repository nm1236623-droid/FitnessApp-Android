package com.example.fitness.user

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// DataStore delegate (確保全域唯一)
private val Context.dataStore by preferencesDataStore(name = "user_profile")

class UserProfileRepository(private val context: Context, private val useFirebase: Boolean = false) {

    // --- Keys Definition ---
    private val KEY_NICKNAME = stringPreferencesKey("nickname")
    private val KEY_AGE = intPreferencesKey("age")
    private val KEY_WEIGHT = stringPreferencesKey("weight_kg")
    private val KEY_HEIGHT = stringPreferencesKey("height_cm")
    private val KEY_TDEE = stringPreferencesKey("tdee")
    private val KEY_PROTEIN_GOAL = stringPreferencesKey("protein_goal_grams")

    // Firebase profile flow
    private val firebaseProfile = FirebaseUserProfileRepository.profile

    // ==========================================
    // Section 1: Basic Info (New)
    // ==========================================

    val nickname: Flow<String> = if (useFirebase) {
        firebaseProfile.map { it?.nickname ?: "" }
    } else {
        context.dataStore.data.map { prefs -> prefs[KEY_NICKNAME] ?: "" }
    }

    val age: Flow<Int> = if (useFirebase) {
        firebaseProfile.map { it?.age ?: 0 }
    } else {
        context.dataStore.data.map { prefs -> prefs[KEY_AGE] ?: 0 }
    }

    // ==========================================
    // Section 2: Fitness Stats
    // ==========================================

    val weightKg: Flow<Float> = if (useFirebase) {
        firebaseProfile.map { it?.weightKg ?: 70f }
    } else {
        context.dataStore.data.map { prefs -> prefs[KEY_WEIGHT]?.toFloatOrNull() ?: 70f }
    }

    val heightCm: Flow<Float> = if (useFirebase) {
        firebaseProfile.map { it?.heightCm ?: 170f }
    } else {
        context.dataStore.data.map { prefs -> prefs[KEY_HEIGHT]?.toFloatOrNull() ?: 170f }
    }

    val tdee: Flow<Float> = if (useFirebase) {
        firebaseProfile.map { it?.tdee?.toFloat() ?: 2000f }
    } else {
        context.dataStore.data.map { prefs -> prefs[KEY_TDEE]?.toFloatOrNull() ?: 2000f }
    }

    val proteinGoalGrams: Flow<Float> = if (useFirebase) {
        firebaseProfile.map { it?.proteinGoalGrams ?: 120f }
    } else {
        context.dataStore.data.map { prefs -> prefs[KEY_PROTEIN_GOAL]?.toFloatOrNull() ?: 120f }
    }

    // ==========================================
    // Section 3: Setters
    // ==========================================

    suspend fun setNickname(value: String) {
        if (useFirebase) updateFirebaseProfile { it.copy(nickname = value) }
        else context.dataStore.edit { it[KEY_NICKNAME] = value }
    }

    suspend fun setAge(value: Int) {
        if (useFirebase) updateFirebaseProfile { it.copy(age = value) }
        else context.dataStore.edit { it[KEY_AGE] = value }
    }

    suspend fun setWeightKg(value: Float) {
        if (useFirebase) updateFirebaseProfile { it.copy(weightKg = value) }
        else context.dataStore.edit { it[KEY_WEIGHT] = value.toString() }
    }

    suspend fun setHeightCm(value: Float) {
        if (useFirebase) updateFirebaseProfile { it.copy(heightCm = value) }
        else context.dataStore.edit { it[KEY_HEIGHT] = value.toString() }
    }

    suspend fun setTdee(value: Float) {
        if (useFirebase) updateFirebaseProfile { it.copy(tdee = value.toInt()) }
        else context.dataStore.edit { it[KEY_TDEE] = value.toString() }
    }

    suspend fun setProteinGoalGrams(value: Float) {
        if (useFirebase) updateFirebaseProfile { it.copy(proteinGoalGrams = value) }
        else context.dataStore.edit { it[KEY_PROTEIN_GOAL] = value.toString() }
    }

    // ==========================================
    // Helpers
    // ==========================================

    private suspend fun updateFirebaseProfile(transform: (UserProfile) -> UserProfile) {
        try {
            val currentProfile = firebaseProfile.first() ?: UserProfile()
            val updatedProfile = transform(currentProfile)
            FirebaseUserProfileRepository.saveProfile(updatedProfile)
        } catch (e: Exception) {
            android.util.Log.e("UserProfileRepo", "Failed to update Firebase profile: ${e.message}")
        }
    }

    suspend fun getFullProfile(): UserProfile? {
        return if (useFirebase) {
            firebaseProfile.first()
        } else {
            val prefs = context.dataStore.data.first()
            UserProfile(
                nickname = prefs[KEY_NICKNAME] ?: "",
                age = prefs[KEY_AGE] ?: 0,
                weightKg = prefs[KEY_WEIGHT]?.toFloatOrNull() ?: 70f,
                heightCm = prefs[KEY_HEIGHT]?.toFloatOrNull() ?: 170f,
                tdee = prefs[KEY_TDEE]?.toIntOrNull() ?: 2000,
                proteinGoalGrams = prefs[KEY_PROTEIN_GOAL]?.toFloatOrNull() ?: 120f
            )
        }
    }
}