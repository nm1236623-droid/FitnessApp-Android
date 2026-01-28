package com.example.fitness.social

import java.time.Instant

/**
 * 好友關係
 */
data class Friendship(
    val id: String,
    val userId: String,
    val friendId: String,
    val friendName: String,
    val friendAvatar: String? = null,
    val status: FriendshipStatus = FriendshipStatus.PENDING,
    val createdAt: Instant = Instant.now()
)

enum class FriendshipStatus {
    PENDING,    // 待確認
    ACCEPTED,   // 已接受
    BLOCKED     // 已封鎖
}

/**
 * 挑戰賽
 */
data class Challenge(
    val id: String,
    val title: String,
    val description: String,
    val type: ChallengeType,
    val goal: ChallengeGoal,
    val startDate: Instant,
    val endDate: Instant,
    val participants: List<String> = emptyList(), // User IDs
    val createdBy: String,
    val reward: String? = null,
    val iconUrl: String? = null
)

enum class ChallengeType {
    WEEKLY,     // 每週挑戰
    MONTHLY,    // 每月挑戰
    CUSTOM      // 自訂挑戰
}

data class ChallengeGoal(
    val metric: ChallengeMetric,
    val target: Double,
    val unit: String
)

enum class ChallengeMetric {
    TOTAL_CALORIES_BURNED,  // 總消耗卡路里
    TOTAL_WORKOUTS,         // 總訓練次數
    TOTAL_DISTANCE,         // 總距離（公里）
    CONSECUTIVE_DAYS,       // 連續訓練天數
    TOTAL_WEIGHT_LIFTED,    // 總重量（公斤）
    PROTEIN_INTAKE          // 蛋白質攝取
}

/**
 * 挑戰參與記錄
 */
data class ChallengeParticipation(
    val challengeId: String,
    val userId: String,
    val userName: String,
    val userAvatar: String? = null,
    val currentProgress: Double = 0.0,
    val lastUpdated: Instant = Instant.now(),
    val completed: Boolean = false
)

/**
 * 排行榜條目
 */
data class LeaderboardEntry(
    val rank: Int,
    val userId: String,
    val userName: String,
    val userAvatar: String? = null,
    val score: Double,
    val metric: String,
    val isCurrentUser: Boolean = false
)

/**
 * 成就系統
 */
data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val iconName: String,
    val category: AchievementCategory,
    val requirement: AchievementRequirement,
    val reward: String? = null
)

enum class AchievementCategory {
    WORKOUT,        // 訓練相關
    NUTRITION,      // 營養相關
    SOCIAL,         // 社交相關
    CONSISTENCY,    // 堅持相關
    MILESTONE       // 里程碑
}

data class AchievementRequirement(
    val metric: String,
    val target: Double
)

/**
 * 用戶成就解鎖記錄
 */
data class UserAchievement(
    val achievementId: String,
    val userId: String,
    val unlockedAt: Instant = Instant.now(),
    val progress: Double = 100.0
)
