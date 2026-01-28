package com.example.fitness.social

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.Instant

class SocialRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    // ==================== 好友系統 ====================

    /**
     * 發送好友邀請
     */
    suspend fun sendFriendRequest(friendId: String, friendName: String): Result<Unit> {
        return try {
            val userId = currentUserId ?: return Result.failure(Exception("未登入"))

            val friendship = Friendship(
                id = "${userId}_${friendId}",
                userId = userId,
                friendId = friendId,
                friendName = friendName,
                status = FriendshipStatus.PENDING
            )

            db.collection("friendships")
                .document(friendship.id)
                .set(friendship)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SocialRepository", "Error sending friend request", e)
            Result.failure(e)
        }
    }

    /**
     * 接受好友邀請
     */
    suspend fun acceptFriendRequest(friendshipId: String): Result<Unit> {
        return try {
            db.collection("friendships")
                .document(friendshipId)
                .update("status", FriendshipStatus.ACCEPTED.name)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SocialRepository", "Error accepting friend request", e)
            Result.failure(e)
        }
    }

    /**
     * 獲取好友列表
     */
    fun getFriends(): Flow<List<Friendship>> = callbackFlow {
        val userId = currentUserId
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("friendships")
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", FriendshipStatus.ACCEPTED.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("SocialRepository", "Error getting friends", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val friends = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Friendship::class.java)
                } ?: emptyList()

                trySend(friends)
            }

        awaitClose { listener.remove() }
    }

    /**
     * 搜尋用戶
     */
    suspend fun searchUsers(query: String): Result<List<UserProfile>> {
        return try {
            val snapshot = db.collection("users")
                .orderBy("displayName")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .limit(20)
                .get()
                .await()

            val users = snapshot.documents.mapNotNull { doc ->
                UserProfile(
                    id = doc.id,
                    displayName = doc.getString("displayName") ?: "",
                    avatarUrl = doc.getString("avatarUrl"),
                    bio = doc.getString("bio")
                )
            }

            Result.success(users)
        } catch (e: Exception) {
            Log.e("SocialRepository", "Error searching users", e)
            Result.failure(e)
        }
    }

    // ==================== 挑戰系統 ====================

    /**
     * 創建挑戰
     */
    suspend fun createChallenge(challenge: Challenge): Result<String> {
        return try {
            val docRef = db.collection("challenges").document()
            val newChallenge = challenge.copy(id = docRef.id)

            docRef.set(newChallenge).await()

            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("SocialRepository", "Error creating challenge", e)
            Result.failure(e)
        }
    }

    /**
     * 加入挑戰
     */
    suspend fun joinChallenge(challengeId: String): Result<Unit> {
        return try {
            val userId = currentUserId ?: return Result.failure(Exception("未登入"))
            val userName = auth.currentUser?.displayName ?: "Unknown"

            val participation = ChallengeParticipation(
                challengeId = challengeId,
                userId = userId,
                userName = userName
            )

            db.collection("challenge_participations")
                .document("${challengeId}_${userId}")
                .set(participation)
                .await()

            // 更新挑戰的參與者列表
            db.collection("challenges")
                .document(challengeId)
                .update("participants", com.google.firebase.firestore.FieldValue.arrayUnion(userId))
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SocialRepository", "Error joining challenge", e)
            Result.failure(e)
        }
    }

    /**
     * 更新挑戰進度
     */
    suspend fun updateChallengeProgress(challengeId: String, progress: Double): Result<Unit> {
        return try {
            val userId = currentUserId ?: return Result.failure(Exception("未登入"))

            db.collection("challenge_participations")
                .document("${challengeId}_${userId}")
                .update(
                    mapOf(
                        "currentProgress" to progress,
                        "lastUpdated" to Instant.now()
                    )
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SocialRepository", "Error updating challenge progress", e)
            Result.failure(e)
        }
    }

    /**
     * 獲取活躍挑戰列表
     */
    fun getActiveChallenges(): Flow<List<Challenge>> = callbackFlow {
        val now = Instant.now()

        val listener = db.collection("challenges")
            .whereGreaterThan("endDate", now)
            .orderBy("endDate", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("SocialRepository", "Error getting challenges", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val challenges = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Challenge::class.java)
                } ?: emptyList()

                trySend(challenges)
            }

        awaitClose { listener.remove() }
    }

    /**
     * 獲取挑戰排行榜
     */
    suspend fun getChallengeLeaderboard(challengeId: String): Result<List<ChallengeParticipation>> {
        return try {
            val snapshot = db.collection("challenge_participations")
                .whereEqualTo("challengeId", challengeId)
                .orderBy("currentProgress", Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .await()

            val leaderboard = snapshot.documents.mapNotNull { doc ->
                doc.toObject(ChallengeParticipation::class.java)
            }

            Result.success(leaderboard)
        } catch (e: Exception) {
            Log.e("SocialRepository", "Error getting leaderboard", e)
            Result.failure(e)
        }
    }

    // ==================== 排行榜系統 ====================

    /**
     * 獲取全局排行榜
     */
    suspend fun getGlobalLeaderboard(
        metric: ChallengeMetric,
        limit: Int = 50
    ): Result<List<LeaderboardEntry>> {
        return try {
            val userId = currentUserId

            // 這裡需要根據實際的統計數據來源來實作
            // 暫時返回模擬數據
            val entries = listOf(
                LeaderboardEntry(1, "user1", "健身達人", null, 15000.0, metric.name, false),
                LeaderboardEntry(2, "user2", "運動狂熱者", null, 12000.0, metric.name, false),
                LeaderboardEntry(3, "user3", "堅持不懈", null, 10000.0, metric.name, false)
            )

            Result.success(entries)
        } catch (e: Exception) {
            Log.e("SocialRepository", "Error getting leaderboard", e)
            Result.failure(e)
        }
    }
}

/**
 * 用戶資料（簡化版）
 */
data class UserProfile(
    val id: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val bio: String? = null
)
