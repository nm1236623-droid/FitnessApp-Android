package com.example.fitness.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ★★★ 新增：定義 PartWeights 資料模型 ★★★
data class PartWeights(
    val id: String = "",
    val date: String = "",
    val partWeights: Map<String, Float> = emptyMap(),
    val exerciseWeights: Map<String, Float> = emptyMap()
)

// ★★★ 新增：定義 PartProgressStats 資料模型 ★★★
data class PartProgressStats(
    val partName: String = "",
    val startWeight: Float = 0f,
    val endWeight: Float = 0f,
    val weightChange: Float = 0f,
    val percentChange: Double = 0.0,
    val averageWeight: Float = 0f,
    val maxWeight: Float = 0f,
    val minWeight: Float = 0f,
    val dataPoints: Int = 0,
    val trend: String = "stable",
    val lastUpdated: String = ""
)

/**
 * Firebase 部位分析 Repository
 */
object FirebasePartAnalysisRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val collection = firestore.collection("part_analysis")

    private val _partWeights = MutableStateFlow<List<PartWeights>>(emptyList())
    val partWeights = _partWeights.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    init {
        startListening()
    }

    private fun startListening() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.w("FirebasePartAnalysisRepo", "No authenticated user found")
            return
        }

        collection
            .whereEqualTo("userId", currentUser.uid)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirebasePartAnalysisRepo", "Listen failed: ${error.message}")
                    _error.value = "數據監聽失敗: ${error.message}"
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val partWeights = snapshot.documents.mapNotNull { doc ->
                        try {
                            val data = doc.data ?: return@mapNotNull null
                            PartWeights(
                                id = doc.id,
                                date = data["date"] as? String ?: "",
                                partWeights = (data["partWeights"] as? Map<String, Any>)?.mapValues {
                                    (it.value as? Number)?.toFloat() ?: 0f
                                } ?: emptyMap(),
                                exerciseWeights = (data["exerciseWeights"] as? Map<String, Any>)?.mapValues {
                                    (it.value as? Number)?.toFloat() ?: 0f
                                } ?: emptyMap()
                            )
                        } catch (e: Exception) {
                            Log.e("FirebasePartAnalysisRepo", "Error parsing document: ${e.message}")
                            null
                        }
                    }
                    _partWeights.value = partWeights
                    Log.d("FirebasePartAnalysisRepo", "Updated ${partWeights.size} part weight records")
                }
            }
    }

    suspend fun addPartWeights(partWeights: PartWeights): Result<String> = try {
        _isLoading.value = true
        _error.value = null

        val currentUser = auth.currentUser
        if (currentUser == null) {
            throw Exception("用戶未登入")
        }

        val recordData = mapOf(
            "userId" to currentUser.uid,
            "date" to partWeights.date,
            "partWeights" to partWeights.partWeights,
            "exerciseWeights" to partWeights.exerciseWeights,
            "createdAt" to System.currentTimeMillis(),
            "updatedAt" to System.currentTimeMillis()
        )

        val documentRef = collection.add(recordData).await()
        Log.d("FirebasePartAnalysisRepo", "Added part weights record with ID: ${documentRef.id}")
        Result.success(documentRef.id)
    } catch (e: Exception) {
        Log.e("FirebasePartAnalysisRepo", "Error adding part weights: ${e.message}")
        _error.value = "添加部位重量失敗: ${e.message}"
        Result.failure(e)
    } finally {
        _isLoading.value = false
    }

    suspend fun updatePartWeights(partWeights: PartWeights): Result<Unit> = try {
        _isLoading.value = true
        _error.value = null

        val currentUser = auth.currentUser
        if (currentUser == null) {
            throw Exception("用戶未登入")
        }

        val doc = collection.document(partWeights.id).get().await()
        if (!doc.exists()) {
            throw Exception("記錄不存在")
        }

        val userId = doc.getString("userId")
        if (userId != currentUser.uid) {
            throw Exception("無權限修改此記錄")
        }

        val updateData = mapOf(
            "date" to partWeights.date,
            "partWeights" to partWeights.partWeights,
            "exerciseWeights" to partWeights.exerciseWeights,
            "updatedAt" to System.currentTimeMillis()
        )

        collection.document(partWeights.id).update(updateData).await()
        Log.d("FirebasePartAnalysisRepo", "Updated part weights record: ${partWeights.id}")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("FirebasePartAnalysisRepo", "Error updating part weights: ${e.message}")
        _error.value = "更新部位重量失敗: ${e.message}"
        Result.failure(e)
    } finally {
        _isLoading.value = false
    }

    suspend fun deletePartWeights(recordId: String): Result<Unit> = try {
        _isLoading.value = true
        _error.value = null

        val currentUser = auth.currentUser
        if (currentUser == null) {
            throw Exception("用戶未登入")
        }

        val doc = collection.document(recordId).get().await()
        if (!doc.exists()) {
            throw Exception("記錄不存在")
        }

        val userId = doc.getString("userId")
        if (userId != currentUser.uid) {
            throw Exception("無權限刪除此記錄")
        }

        collection.document(recordId).delete().await()
        Log.d("FirebasePartAnalysisRepo", "Deleted part weights record: $recordId")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("FirebasePartAnalysisRepo", "Error deleting part weights: ${e.message}")
        _error.value = "刪除部位重量失敗: ${e.message}"
        Result.failure(e)
    } finally {
        _isLoading.value = false
    }

    suspend fun clear(): Result<Unit> = try {
        _isLoading.value = true
        _error.value = null

        val currentUser = auth.currentUser
        if (currentUser == null) {
            throw Exception("用戶未登入")
        }

        val userRecords = collection
            .whereEqualTo("userId", currentUser.uid)
            .get()
            .await()

        val batch = firestore.batch()
        userRecords.documents.forEach { doc ->
            batch.delete(doc.reference)
        }

        batch.commit().await()
        Log.d("FirebasePartAnalysisRepo", "Cleared all part weights for user: ${currentUser.uid}")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("FirebasePartAnalysisRepo", "Error clearing part weights: ${e.message}")
        _error.value = "清除部位重量失敗: ${e.message}"
        Result.failure(e)
    } finally {
        _isLoading.value = false
    }

    suspend fun getLatestPartWeights(): Result<PartWeights?> = try {
        _isLoading.value = true
        _error.value = null

        val currentUser = auth.currentUser
        if (currentUser == null) {
            throw Exception("用戶未登入")
        }

        val doc = collection
            .whereEqualTo("userId", currentUser.uid)
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()

        val record = if (doc != null) {
            val data = doc.data ?: return@getLatestPartWeights Result.success(null)
            PartWeights(
                id = doc.id,
                date = data["date"] as? String ?: "",
                partWeights = (data["partWeights"] as? Map<String, Any>)?.mapValues {
                    (it.value as? Number)?.toFloat() ?: 0f
                } ?: emptyMap(),
                exerciseWeights = (data["exerciseWeights"] as? Map<String, Any>)?.mapValues {
                    (it.value as? Number)?.toFloat() ?: 0f
                } ?: emptyMap()
            )
        } else null

        Log.d("FirebasePartAnalysisRepo", "Retrieved latest part weights: ${record?.id}")
        Result.success(record)
    } catch (e: Exception) {
        Log.e("FirebasePartAnalysisRepo", "Error getting latest part weights: ${e.message}")
        _error.value = "獲取最新部位重量失敗: ${e.message}"
        Result.failure(e)
    } finally {
        _isLoading.value = false
    }

    suspend fun getPartProgressStats(
        partName: String,
        days: Int = 30
    ): Result<PartProgressStats> = try {
        _isLoading.value = true
        _error.value = null

        val currentUser = auth.currentUser
        if (currentUser == null) {
            throw Exception("用戶未登入")
        }

        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(days.toLong())

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val startStr = startDate.format(formatter)
        val endStr = endDate.format(formatter)

        val records = collection
            .whereEqualTo("userId", currentUser.uid)
            .whereGreaterThanOrEqualTo("date", startStr)
            .whereLessThanOrEqualTo("date", endStr)
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .await()
            .documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    PartWeights(
                        id = doc.id,
                        date = data["date"] as? String ?: "",
                        partWeights = (data["partWeights"] as? Map<String, Any>)?.mapValues {
                            (it.value as? Number)?.toFloat() ?: 0f
                        } ?: emptyMap(),
                        exerciseWeights = (data["exerciseWeights"] as? Map<String, Any>)?.mapValues {
                            (it.value as? Number)?.toFloat() ?: 0f
                        } ?: emptyMap()
                    )
                } catch (e: Exception) {
                    Log.e("FirebasePartAnalysisRepo", "Error parsing document: ${e.message}")
                    null
                }
            }

        val stats = calculatePartProgressStats(partName, records)
        Log.d("FirebasePartAnalysisRepo", "Calculated progress stats for part: $partName")
        Result.success(stats)
    } catch (e: Exception) {
        Log.e("FirebasePartAnalysisRepo", "Error calculating part progress stats: ${e.message}")
        _error.value = "計算部位進度失敗: ${e.message}"
        Result.failure(e)
    } finally {
        _isLoading.value = false
    }

    private fun calculatePartProgressStats(
        partName: String,
        records: List<PartWeights>
    ): PartProgressStats {
        val partData = records.mapNotNull { record ->
            record.partWeights[partName]?.let { weight ->
                Pair(record.date, weight)
            }
        }.sortedBy { it.first }

        if (partData.isEmpty()) {
            return PartProgressStats(partName = partName)
        }

        val weights = partData.map { it.second }
        val firstWeight = weights.first()
        val lastWeight = weights.last()
        val weightChange = lastWeight - firstWeight
        val percentChange = if (firstWeight > 0) (weightChange / firstWeight * 100) else 0.0
        val avgWeight = weights.map { it.toDouble() }.average().toFloat()
        val maxWeight = weights.maxOrNull() ?: 0f
        val minWeight = weights.minOrNull() ?: 0f

        val trend = if (partData.size >= 2) {
            val recentWeights = partData.takeLast(3).map { it.second }
            val earlierWeights = partData.take(3).map { it.second }
            val recentAvg = recentWeights.map { it.toDouble() }.average()
            val earlierAvg = earlierWeights.map { it.toDouble() }.average()
            when {
                recentAvg > earlierAvg * 1.05 -> "increasing"
                recentAvg < earlierAvg * 0.95 -> "decreasing"
                else -> "stable"
            }
        } else "insufficient_data"

        return PartProgressStats(
            partName = partName,
            startWeight = firstWeight,
            endWeight = lastWeight,
            weightChange = weightChange,
            percentChange = percentChange.toDouble(),
            averageWeight = avgWeight,
            maxWeight = maxWeight,
            minWeight = minWeight,
            dataPoints = partData.size,
            trend = trend,
            lastUpdated = partData.last().first
        )
    }
}