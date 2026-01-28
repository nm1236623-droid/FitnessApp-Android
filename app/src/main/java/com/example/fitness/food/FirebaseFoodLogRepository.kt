package com.example.fitness.food

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.util.UUID

/**
 * 飲食紀錄的資料模型
 * 對應 Firestore 中的文件結構
 */
data class FoodLogEntry(
    val id: String = UUID.randomUUID().toString(), // 唯一識別碼
    val name: String,                              // 食物名稱
    val quantity: Float,                           // 份量數值
    val unit: String,                              // 單位 (如: 份, 克)
    val calories: Double?,                         // 熱量 (kcal)
    val nutrients: Map<String, Double>,            // 營養素 Map (protein, fat, carbs)
    val timestamp: Instant,                        // 記錄時間
    val userId: String? = null                     // 綁定的使用者 ID (Firebase UID)
)

/**
 * 負責與 Firebase Firestore 溝通的儲存庫 (Singleton)
 */
object FirebaseFoodLogRepository {
    private const val TAG = "FirebaseFoodLog"
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Firestore 集合名稱
    private val collection = firestore.collection("food_logs")

    // 使用 StateFlow 讓 UI 可以即時觀察資料變化
    private val _items = MutableStateFlow<List<FoodLogEntry>>(emptyList())
    val items = _items.asStateFlow()

    init {
        // 初始化時，開始監聽當前登入使用者的資料變更
        listenForUpdates()
    }

    /**
     * 啟動 Firestore 的即時監聽器 (Snapshot Listener)
     * 這會自動將雲端的資料變更同步到本地的 _items StateFlow
     */
    fun listenForUpdates() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            collection
                .whereEqualTo("userId", currentUser.uid) // 嚴格過濾：只抓取當前使用者的資料
                .orderBy("timestamp", Query.Direction.DESCENDING) // 按時間倒序排列 (最新的在上面)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "監聽失敗: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val foodLogs = snapshot.documents.mapNotNull { doc ->
                            try {
                                val data = doc.data
                                // 安全地解析 Firestore 資料並轉換為 FoodLogEntry
                                FoodLogEntry(
                                    id = doc.id,
                                    name = data?.get("name") as? String ?: "未命名食物",
                                    quantity = (data?.get("quantity") as? Number)?.toFloat() ?: 1f,
                                    unit = data?.get("unit") as? String ?: "份",
                                    calories = (data?.get("calories") as? Number)?.toDouble(),
                                    // 處理 Map<String, Any> 轉 Map<String, Double> 的型別轉換
                                    nutrients = (data?.get("nutrients") as? Map<String, Any>)?.mapValues {
                                        (it.value as? Number)?.toDouble() ?: 0.0
                                    } ?: emptyMap(),
                                    // 將 Firestore 的時間戳 (Long/Number) 轉回 Java Instant
                                    timestamp = Instant.ofEpochMilli((data?.get("timestamp") as? Number)?.toLong() ?: System.currentTimeMillis()),
                                    userId = data?.get("userId") as? String
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "解析文件錯誤 ${doc.id}: ${e.message}")
                                null
                            }
                        }

                        // 更新 StateFlow，通知所有訂閱的 UI
                        _items.value = foodLogs
                        Log.d(TAG, "已從 Firestore 載入 ${foodLogs.size} 筆紀錄")
                    }
                }
        } else {
            // 如果未登入，清空列表
            _items.value = emptyList()
            Log.d(TAG, "使用者未登入，不監聽資料")
        }
    }

    /**
     * 新增一筆飲食紀錄到 Firestore
     */
    suspend fun add(entry: FoodLogEntry): Result<String> = try {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Result.failure(Exception("使用者未登入"))
        } else {
            // 強制綁定當前使用者的 UID
            val entryWithUser = entry.copy(userId = currentUser.uid)

            // 轉換為 Map 格式以符合 Firestore 寫入要求
            val data = mapOf(
                "name" to entryWithUser.name,
                "quantity" to entryWithUser.quantity,
                "unit" to entryWithUser.unit,
                "calories" to entryWithUser.calories,
                "nutrients" to entryWithUser.nutrients,
                "timestamp" to entryWithUser.timestamp.toEpochMilli(), // 存成毫秒
                "userId" to entryWithUser.userId
            )

            // 使用 await() 等待寫入完成
            val documentRef = collection.add(data).await()
            Log.d(TAG, "成功新增飲食紀錄，ID: ${documentRef.id}")
            Result.success(documentRef.id)
        }
    } catch (e: Exception) {
        Log.e(TAG, "新增失敗: ${e.message}", e)
        Result.failure(e)
    }

    /**
     * 刪除指定 ID 的紀錄
     */
    suspend fun remove(id: String): Result<Unit> = try {
        collection.document(id).delete().await()
        Log.d(TAG, "成功刪除紀錄: $id")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "刪除失敗: ${e.message}", e)
        Result.failure(e)
    }

    /**
     * 更新現有的紀錄
     */
    suspend fun update(entry: FoodLogEntry): Result<Unit> = try {
        val data = mapOf(
            "name" to entry.name,
            "quantity" to entry.quantity,
            "unit" to entry.unit,
            "calories" to entry.calories,
            "nutrients" to entry.nutrients,
            "timestamp" to entry.timestamp.toEpochMilli(),
            "userId" to entry.userId
        )

        collection.document(entry.id).set(data).await()
        Log.d(TAG, "成功更新紀錄: ${entry.id}")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "更新失敗: ${e.message}", e)
        Result.failure(e)
    }

    /**
     * 清除當前使用者的所有飲食紀錄 (危險操作，通常用於重置帳號)
     */
    suspend fun clear(): Result<Unit> = try {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Result.failure(Exception("使用者未登入"))
        } else {
            // 先查詢出該使用者的所有文件
            val docs = collection
                .whereEqualTo("userId", currentUser.uid)
                .get()
                .await()

            // 使用 Batch 批次刪除，提高效能
            val batch = firestore.batch()
            docs.documents.forEach { doc ->
                batch.delete(doc.reference)
            }

            batch.commit().await()
            Log.d(TAG, "已清除使用者所有紀錄")
            Result.success(Unit)
        }
    } catch (e: Exception) {
        Log.e(TAG, "清除失敗: ${e.message}", e)
        Result.failure(e)
    }

    /**
     * 取得「今日」的所有飲食紀錄
     * (用於首頁計算今日攝取卡路里總和)
     */
    suspend fun getTodayFoodLogs(): Result<List<FoodLogEntry>> = try {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Result.failure(Exception("使用者未登入"))
        } else {
            // 計算「今天凌晨 00:00」的毫秒數
            val todayStart = Instant.now().atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

            // 查詢 Timestamp >= 今天 00:00 的紀錄
            val docs = collection
                .whereEqualTo("userId", currentUser.uid)
                .whereGreaterThanOrEqualTo("timestamp", todayStart)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val foodLogs = docs.documents.mapNotNull { doc ->
                try {
                    val data = doc.data
                    FoodLogEntry(
                        id = doc.id,
                        name = data?.get("name") as? String ?: "",
                        quantity = (data?.get("quantity") as? Number)?.toFloat() ?: 0f,
                        unit = data?.get("unit") as? String ?: "",
                        calories = (data?.get("calories") as? Number)?.toDouble(),
                        nutrients = (data?.get("nutrients") as? Map<String, Any>)?.mapValues {
                            (it.value as? Number)?.toDouble() ?: 0.0
                        } ?: emptyMap(),
                        timestamp = Instant.ofEpochMilli((data?.get("timestamp") as? Number)?.toLong() ?: 0L),
                        userId = data?.get("userId") as? String
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "解析文件錯誤 ${doc.id}: ${e.message}")
                    null
                }
            }

            Result.success(foodLogs)
        }
    } catch (e: Exception) {
        Log.e(TAG, "取得今日紀錄失敗: ${e.message}", e)
        Result.failure(e)
    }
}