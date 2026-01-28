package com.example.fitness.user

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 這是唯一的 UserProfile 資料模型定義
 * 包含：基本資料 (設定頁用) + 身體數值 (健身計算用)
 */
data class UserProfile(
    val nickname: String = "",
    val age: Int = 0,
    val avatarUri: String? = null,
    val weightKg: Float = 70f,
    val heightCm: Float = 170f,
    val tdee: Int = 2000,
    val proteinGoalGrams: Float = 120f
)

/**
 * Firebase Repository 單例
 */
object FirebaseUserProfileRepository {
    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile = _profile.asStateFlow()

    suspend fun saveProfile(profile: UserProfile) {
        _profile.value = profile
        // 注意：如果您有真實的 Firestore 寫入邏輯，請寫在這裡
        // 例如: FirebaseFirestore.getInstance().collection("users")...
    }
}