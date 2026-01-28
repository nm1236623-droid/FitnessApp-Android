package com.example.fitness.inbody

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Firebase InBody 體態數據 Repository
 * 
 * 提供雲端 InBody 數據的 CRUD 操作
 * 支援即時數據同步和用戶隔離
 */
object FirebaseInBodyRepository {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val collection = firestore.collection("inbody_records")
    
    private val _records = MutableStateFlow<List<InBodyRecord>>(emptyList())
    val records = _records.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    
    init {
        // 監聽當前用戶的 InBody 數據變化
        startListening()
    }
    
    /**
     * 開始監聽用戶的 InBody 數據
     */
    private fun startListening() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.w("FirebaseInBodyRepo", "No authenticated user found")
            return
        }
        
        collection
            .whereEqualTo("userId", currentUser.uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirebaseInBodyRepo", "Listen failed: ${error.message}")
                    _error.value = "數據監聽失敗: ${error.message}"
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val records = snapshot.documents.mapNotNull { doc ->
                        try {
                            val data = doc.data ?: return@mapNotNull null
                            InBodyRecord(
                                id = doc.id,
                                timestamp = Instant.ofEpochMilli(
                                    data["timestampEpochMillis"] as? Long ?: 0L
                                ),
                                weightKg = (data["weightKg"] as? Number)?.toFloat() ?: 0f,
                                bodyFatPercent = (data["bodyFatPercent"] as? Number)?.toFloat(),
                                muscleMassKg = (data["muscleMassKg"] as? Number)?.toFloat()
                            )
                        } catch (e: Exception) {
                            Log.e("FirebaseInBodyRepo", "Error parsing document: ${e.message}")
                            null
                        }
                    }
                    _records.value = records
                    Log.d("FirebaseInBodyRepo", "Updated ${records.size} InBody records")
                }
            }
    }
    
    /**
     * 添加新的 InBody 記錄
     */
    suspend fun addRecord(record: InBodyRecord): Result<String> = try {
        _isLoading.value = true
        _error.value = null
        
        val currentUser = auth.currentUser
        if (currentUser == null) {
            throw Exception("用戶未登入")
        }
        
        val recordData = mapOf(
            "userId" to currentUser.uid,
            "timestampEpochMillis" to record.timestamp.toEpochMilli(),
            "weightKg" to record.weightKg,
            "bodyFatPercent" to record.bodyFatPercent,
            "muscleMassKg" to record.muscleMassKg,
            "createdAt" to System.currentTimeMillis(),
            "updatedAt" to System.currentTimeMillis()
        )
        
        val documentRef = collection.add(recordData).await()
        Log.d("FirebaseInBodyRepo", "Added InBody record with ID: ${documentRef.id}")
        Result.success(documentRef.id)
    } catch (e: Exception) {
        Log.e("FirebaseInBodyRepo", "Error adding record: ${e.message}")
        _error.value = "添加記錄失敗: ${e.message}"
        Result.failure(e)
    } finally {
        _isLoading.value = false
    }
    
    /**
     * 更新 InBody 記錄
     */
    suspend fun updateRecord(record: InBodyRecord): Result<Unit> = try {
        _isLoading.value = true
        _error.value = null
        
        val currentUser = auth.currentUser
        if (currentUser == null) {
            throw Exception("用戶未登入")
        }
        
        // 檢查記錄是否屬於當前用戶
        val doc = collection.document(record.id).get().await()
        if (!doc.exists()) {
            throw Exception("記錄不存在")
        }
        
        val userId = doc.getString("userId")
        if (userId != currentUser.uid) {
            throw Exception("無權限修改此記錄")
        }
        
        val updateData = mapOf(
            "timestampEpochMillis" to record.timestamp.toEpochMilli(),
            "weightKg" to record.weightKg,
            "bodyFatPercent" to record.bodyFatPercent,
            "muscleMassKg" to record.muscleMassKg,
            "updatedAt" to System.currentTimeMillis()
        )
        
        collection.document(record.id).update(updateData).await()
        Log.d("FirebaseInBodyRepo", "Updated InBody record: ${record.id}")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("FirebaseInBodyRepo", "Error updating record: ${e.message}")
        _error.value = "更新記錄失敗: ${e.message}"
        Result.failure(e)
    } finally {
        _isLoading.value = false
    }
    
    /**
     * 刪除 InBody 記錄
     */
    suspend fun deleteRecord(recordId: String): Result<Unit> = try {
        _isLoading.value = true
        _error.value = null
        
        val currentUser = auth.currentUser
        if (currentUser == null) {
            throw Exception("用戶未登入")
        }
        
        // 檢查記錄是否屬於當前用戶
        val doc = collection.document(recordId).get().await()
        if (!doc.exists()) {
            throw Exception("記錄不存在")
        }
        
        val userId = doc.getString("userId")
        if (userId != currentUser.uid) {
            throw Exception("無權限刪除此記錄")
        }
        
        collection.document(recordId).delete().await()
        Log.d("FirebaseInBodyRepo", "Deleted InBody record: $recordId")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("FirebaseInBodyRepo", "Error deleting record: ${e.message}")
        _error.value = "刪除記錄失敗: ${e.message}"
        Result.failure(e)
    } finally {
        _isLoading.value = false
    }
    
    /**
     * 清除用戶的所有 InBody 記錄
     */
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
        Log.d("FirebaseInBodyRepo", "Cleared all InBody records for user: ${currentUser.uid}")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("FirebaseInBodyRepo", "Error clearing records: ${e.message}")
        _error.value = "清除記錄失敗: ${e.message}"
        Result.failure(e)
    } finally {
        _isLoading.value = false
    }
    
    /**
     * 獲取指定日期範圍的 InBody 記錄
     */
    suspend fun getRecordsByDateRange(
        startDate: Instant,
        endDate: Instant
    ): Result<List<InBodyRecord>> = try {
        _isLoading.value = true
        _error.value = null
        
        val currentUser = auth.currentUser
        if (currentUser == null) {
            throw Exception("用戶未登入")
        }
        
        val records = collection
            .whereEqualTo("userId", currentUser.uid)
            .whereGreaterThanOrEqualTo("timestampEpochMillis", startDate.toEpochMilli())
            .whereLessThanOrEqualTo("timestampEpochMillis", endDate.toEpochMilli())
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .await()
            .documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    InBodyRecord(
                        id = doc.id,
                        timestamp = Instant.ofEpochMilli(
                            data["timestampEpochMillis"] as? Long ?: 0L
                        ),
                        weightKg = (data["weightKg"] as? Number)?.toFloat() ?: 0f,
                        bodyFatPercent = (data["bodyFatPercent"] as? Number)?.toFloat(),
                        muscleMassKg = (data["muscleMassKg"] as? Number)?.toFloat()
                    )
                } catch (e: Exception) {
                    Log.e("FirebaseInBodyRepo", "Error parsing document: ${e.message}")
                    null
                }
            }
        
        Log.d("FirebaseInBodyRepo", "Retrieved ${records.size} records for date range")
        Result.success(records)
    } catch (e: Exception) {
        Log.e("FirebaseInBodyRepo", "Error getting records by date range: ${e.message}")
        _error.value = "獲取記錄失敗: ${e.message}"
        Result.failure(e)
    } finally {
        _isLoading.value = false
    }
    
    /**
     * 獲取最新的 InBody 記錄
     */
    suspend fun getLatestRecord(): Result<InBodyRecord?> = try {
        _isLoading.value = true
        _error.value = null
        
        val currentUser = auth.currentUser
        if (currentUser == null) {
            throw Exception("用戶未登入")
        }
        
        val doc = collection
            .whereEqualTo("userId", currentUser.uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
        
        val record = if (doc != null) {
            val data = doc.data ?: return@getLatestRecord Result.success(null)
            InBodyRecord(
                id = doc.id,
                timestamp = Instant.ofEpochMilli(
                    data["timestampEpochMillis"] as? Long ?: 0L
                ),
                weightKg = (data["weightKg"] as? Number)?.toFloat() ?: 0f,
                bodyFatPercent = (data["bodyFatPercent"] as? Number)?.toFloat(),
                muscleMassKg = (data["muscleMassKg"] as? Number)?.toFloat()
            )
        } else null
        
        Log.d("FirebaseInBodyRepo", "Retrieved latest InBody record: ${record?.id}")
        Result.success(record)
    } catch (e: Exception) {
        Log.e("FirebaseInBodyRepo", "Error getting latest record: ${e.message}")
        _error.value = "獲取最新記錄失敗: ${e.message}"
        Result.failure(e)
    } finally {
        _isLoading.value = false
    }
    
    /**
     * 計算體態變化統計
     */
    suspend fun getBodyCompositionStats(): Result<BodyCompositionStats> = try {
        _isLoading.value = true
        _error.value = null
        
        val currentUser = auth.currentUser
        if (currentUser == null) {
            throw Exception("用戶未登入")
        }
        
        val records = collection
            .whereEqualTo("userId", currentUser.uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .await()
            .documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    InBodyRecord(
                        id = doc.id,
                        timestamp = Instant.ofEpochMilli(
                            data["timestampEpochMillis"] as? Long ?: 0L
                        ),
                        weightKg = (data["weightKg"] as? Number)?.toFloat() ?: 0f,
                        bodyFatPercent = (data["bodyFatPercent"] as? Number)?.toFloat(),
                        muscleMassKg = (data["muscleMassKg"] as? Number)?.toFloat()
                    )
                } catch (e: Exception) {
                    Log.e("FirebaseInBodyRepo", "Error parsing document: ${e.message}")
                    null
                }
            }
        
        val stats = calculateStats(records)
        Log.d("FirebaseInBodyRepo", "Calculated body composition stats")
        Result.success(stats)
    } catch (e: Exception) {
        Log.e("FirebaseInBodyRepo", "Error calculating stats: ${e.message}")
        _error.value = "計算統計失敗: ${e.message}"
        Result.failure(e)
    } finally {
        _isLoading.value = false
    }
    
    /**
     * 計算體態統計數據
     */
    private fun calculateStats(records: List<InBodyRecord>): BodyCompositionStats {
        if (records.isEmpty()) {
            return BodyCompositionStats()
        }
        
        val latest = records.first()
        val previous = if (records.size >= 2) records[1] else null
        
        // 計算變化
        val weightChange = previous?.let { latest.weightKg - it.weightKg } ?: 0f
        val bodyFatChange = previous?.let { 
            latest.bodyFatPercent?.minus(it.bodyFatPercent ?: 0f) 
        } ?: 0f
        val muscleChange = previous?.let { 
            latest.muscleMassKg?.minus(it.muscleMassKg ?: 0f) 
        } ?: 0f
        
        // 計算平均值
        val avgWeight = records.map { it.weightKg }.average().toFloat()
        val avgBodyFat = records.mapNotNull { it.bodyFatPercent }.average().toFloat()
        val avgMuscle = records.mapNotNull { it.muscleMassKg }.average().toFloat()
        
        return BodyCompositionStats(
            latestWeight = latest.weightKg,
            latestBodyFatPercent = latest.bodyFatPercent,
            latestMuscleMassKg = latest.muscleMassKg,
            weightChange = weightChange,
            bodyFatChange = bodyFatChange,
            muscleChange = muscleChange,
            averageWeight = avgWeight,
            averageBodyFatPercent = avgBodyFat,
            averageMuscleMassKg = avgMuscle,
            totalRecords = records.size,
            lastUpdated = latest.timestamp
        )
    }
}
