package com.example.fitness.coach.messaging

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.util.UUID

/**
 * 訊息類型
 */
enum class MessageType {
    TEXT,           // 文字訊息
    IMAGE,          // 圖片
    VIDEO,          // 影片
    VOICE,          // 語音
    WORKOUT_PLAN,   // 訓練計劃
    FEEDBACK        // 動作回饋
}

/**
 * 訊息模型
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val chatRoomId: String,
    val senderId: String,
    val senderName: String,
    val senderAvatar: String? = null,
    val type: MessageType = MessageType.TEXT,
    val content: String,
    val mediaUrl: String? = null,
    val thumbnailUrl: String? = null,
    val metadata: Map<String, String>? = null,
    val timestamp: Instant = Instant.now(),
    val read: Boolean = false
)

/**
 * 聊天室模型
 */
data class ChatRoom(
    val id: String,
    val coachId: String,
    val coachName: String,
    val traineeId: String,
    val traineeName: String,
    val lastMessage: String? = null,
    val lastMessageTime: Instant? = null,
    val unreadCount: Int = 0,
    val createdAt: Instant = Instant.now()
)

class CoachMessagingRepository {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // ==================== 聊天室管理 ====================

    /**
     * 創建或獲取聊天室
     */
    suspend fun getOrCreateChatRoom(
        coachId: String,
        coachName: String,
        traineeId: String,
        traineeName: String
    ): Result<ChatRoom> {
        return try {
            val chatRoomId = generateChatRoomId(coachId, traineeId)

            val doc = db.collection("chat_rooms")
                .document(chatRoomId)
                .get()
                .await()

            if (doc.exists()) {
                val chatRoom = doc.toObject(ChatRoom::class.java)
                Result.success(chatRoom!!)
            } else {
                val newChatRoom = ChatRoom(
                    id = chatRoomId,
                    coachId = coachId,
                    coachName = coachName,
                    traineeId = traineeId,
                    traineeName = traineeName
                )

                db.collection("chat_rooms")
                    .document(chatRoomId)
                    .set(newChatRoom)
                    .await()

                Result.success(newChatRoom)
            }
        } catch (e: Exception) {
            Log.e("CoachMessaging", "Error getting/creating chat room", e)
            Result.failure(e)
        }
    }

    /**
     * 獲取用戶的所有聊天室
     */
    fun getChatRooms(userId: String, isCoach: Boolean): Flow<List<ChatRoom>> = callbackFlow {
        val field = if (isCoach) "coachId" else "traineeId"

        val listener = db.collection("chat_rooms")
            .whereEqualTo(field, userId)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("CoachMessaging", "Error getting chat rooms", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val chatRooms = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ChatRoom::class.java)
                } ?: emptyList()

                trySend(chatRooms)
            }

        awaitClose { listener.remove() }
    }

    // ==================== 訊息管理 ====================

    /**
     * 發送文字訊息
     */
    suspend fun sendTextMessage(
        chatRoomId: String,
        senderId: String,
        senderName: String,
        content: String
    ): Result<Unit> {
        return try {
            val message = ChatMessage(
                chatRoomId = chatRoomId,
                senderId = senderId,
                senderName = senderName,
                type = MessageType.TEXT,
                content = content
            )

            // 保存訊息
            db.collection("messages")
                .document(message.id)
                .set(message)
                .await()

            // 更新聊天室最後訊息
            db.collection("chat_rooms")
                .document(chatRoomId)
                .update(
                    mapOf(
                        "lastMessage" to content,
                        "lastMessageTime" to Instant.now()
                    )
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CoachMessaging", "Error sending message", e)
            Result.failure(e)
        }
    }

    /**
     * 發送圖片訊息
     */
    suspend fun sendImageMessage(
        chatRoomId: String,
        senderId: String,
        senderName: String,
        imageUri: android.net.Uri
    ): Result<Unit> {
        return try {
            // 上傳圖片到 Storage
            val fileName = "chat_images/${UUID.randomUUID()}.jpg"
            val storageRef = storage.reference.child(fileName)

            storageRef.putFile(imageUri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()

            val message = ChatMessage(
                chatRoomId = chatRoomId,
                senderId = senderId,
                senderName = senderName,
                type = MessageType.IMAGE,
                content = "[圖片]",
                mediaUrl = downloadUrl
            )

            db.collection("messages")
                .document(message.id)
                .set(message)
                .await()

            db.collection("chat_rooms")
                .document(chatRoomId)
                .update(
                    mapOf(
                        "lastMessage" to "[圖片]",
                        "lastMessageTime" to Instant.now()
                    )
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CoachMessaging", "Error sending image", e)
            Result.failure(e)
        }
    }

    /**
     * 發送語音訊息
     */
    suspend fun sendVoiceMessage(
        chatRoomId: String,
        senderId: String,
        senderName: String,
        audioUri: android.net.Uri,
        duration: Int
    ): Result<Unit> {
        return try {
            val fileName = "chat_voice/${UUID.randomUUID()}.m4a"
            val storageRef = storage.reference.child(fileName)

            storageRef.putFile(audioUri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()

            val message = ChatMessage(
                chatRoomId = chatRoomId,
                senderId = senderId,
                senderName = senderName,
                type = MessageType.VOICE,
                content = "[語音訊息]",
                mediaUrl = downloadUrl,
                metadata = mapOf("duration" to duration.toString())
            )

            db.collection("messages")
                .document(message.id)
                .set(message)
                .await()

            db.collection("chat_rooms")
                .document(chatRoomId)
                .update(
                    mapOf(
                        "lastMessage" to "[語音訊息 ${duration}s]",
                        "lastMessageTime" to Instant.now()
                    )
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CoachMessaging", "Error sending voice", e)
            Result.failure(e)
        }
    }

    /**
     * 獲取聊天訊息
     */
    fun getMessages(chatRoomId: String): Flow<List<ChatMessage>> = callbackFlow {
        val listener = db.collection("messages")
            .whereEqualTo("chatRoomId", chatRoomId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("CoachMessaging", "Error getting messages", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ChatMessage::class.java)
                } ?: emptyList()

                trySend(messages)
            }

        awaitClose { listener.remove() }
    }

    /**
     * 標記訊息為已讀
     */
    suspend fun markAsRead(messageIds: List<String>): Result<Unit> {
        return try {
            val batch = db.batch()

            messageIds.forEach { id ->
                val ref = db.collection("messages").document(id)
                batch.update(ref, "read", true)
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CoachMessaging", "Error marking as read", e)
            Result.failure(e)
        }
    }

    // ==================== 工具方法 ====================

    private fun generateChatRoomId(coachId: String, traineeId: String): String {
        // 確保 ID 一致性（不論誰先誰後）
        return listOf(coachId, traineeId).sorted().joinToString("_")
    }
}
