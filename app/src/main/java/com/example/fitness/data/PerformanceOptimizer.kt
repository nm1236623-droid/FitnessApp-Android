package com.example.fitness.data

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.tasks.await

/**
 * 性能優化工具
 * 
 * 提供數據緩存和批量操作功能
 */
class PerformanceOptimizer(private val context: Context) {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val cache = mutableMapOf<String, Any>()
    private val cacheTimestamps = mutableMapOf<String, Instant>()
    
    // 性能指標
    private val _queryCount = MutableStateFlow(0)
    val queryCount: Flow<Int> = _queryCount.asStateFlow()
    
    private val _cacheHitCount = MutableStateFlow(0)
    val cacheHitCount: Flow<Int> = _cacheHitCount.asStateFlow()
    
    private val _batchOperationCount = MutableStateFlow(0)
    val batchOperationCount: Flow<Int> = _batchOperationCount.asStateFlow()
    
    private val _averageQueryTime = MutableStateFlow(0L)
    val averageQueryTime: Flow<Long> = _averageQueryTime.asStateFlow()
    
    private val queryTimes = mutableListOf<Long>()
    
    /**
     * 緩存數據
     */
    fun cacheData(key: String, data: Any) {
        cache[key] = data
        cacheTimestamps[key] = Instant.now()
        Log.d("PerformanceOptimizer", "緩存數據: $key")
    }
    
    /**
     * 獲取緩存數據
     */
    fun getCachedData(key: String): Any? {
        val data = cache[key]
        if (data != null) {
            _cacheHitCount.value += 1
            Log.d("PerformanceOptimizer", "緩存命中: $key")
        }
        return data
    }
    
    /**
     * 清除緩存
     */
    fun clearCache() {
        cache.clear()
        cacheTimestamps.clear()
        Log.d("PerformanceOptimizer", "緩存已清除")
    }
    
    /**
     * 批量寫入操作
     */
    suspend fun batchWrite(collection: String, documents: List<Map<String, Any>>): Boolean {
        return try {
            val batch = firestore.batch()
            val collectionRef = firestore.collection(collection)
            
            documents.forEach { document ->
                val docRef = collectionRef.document()
                batch.set(docRef, document)
            }
            
            val startTime = System.currentTimeMillis()
            batch.commit().await()
            val endTime = System.currentTimeMillis()
            
            _batchOperationCount.value += 1
            recordQueryTime(endTime - startTime)
            
            Log.d("PerformanceOptimizer", "批量寫入完成: ${documents.size} 條記錄")
            true
        } catch (e: Exception) {
            Log.e("PerformanceOptimizer", "批量寫入失敗", e)
            false
        }
    }
    
    /**
     * 批量更新操作
     */
    suspend fun batchUpdate(collection: String, updates: List<Pair<String, Map<String, Any>>>): Boolean {
        return try {
            val batch = firestore.batch()
            val collectionRef = firestore.collection(collection)
            
            updates.forEach { (documentId, updateData) ->
                val docRef = collectionRef.document(documentId)
                batch.update(docRef, updateData)
            }
            
            val startTime = System.currentTimeMillis()
            batch.commit().await()
            val endTime = System.currentTimeMillis()
            
            _batchOperationCount.value += 1
            recordQueryTime(endTime - startTime)
            
            Log.d("PerformanceOptimizer", "批量更新完成: ${updates.size} 條記錄")
            true
        } catch (e: Exception) {
            Log.e("PerformanceOptimizer", "批量更新失敗", e)
            false
        }
    }
    
    /**
     * 批量刪除操作
     */
    suspend fun batchDelete(collection: String, documentIds: List<String>): Boolean {
        return try {
            val batch = firestore.batch()
            val collectionRef = firestore.collection(collection)
            
            documentIds.forEach { documentId ->
                val docRef = collectionRef.document(documentId)
                batch.delete(docRef)
            }
            
            val startTime = System.currentTimeMillis()
            batch.commit().await()
            val endTime = System.currentTimeMillis()
            
            _batchOperationCount.value += 1
            recordQueryTime(endTime - startTime)
            
            Log.d("PerformanceOptimizer", "批量刪除完成: ${documentIds.size} 條記錄")
            true
        } catch (e: Exception) {
            Log.e("PerformanceOptimizer", "批量刪除失敗", e)
            false
        }
    }
    
    /**
     * 預加載數據
     */
    suspend fun preloadData(collection: String, limit: Int = 50): Boolean {
        return try {
            val startTime = System.currentTimeMillis()
            
            val documents = firestore
                .collection(collection)
                .limit(limit.toLong())
                .get()
                .await()
            
            val endTime = System.currentTimeMillis()
            recordQueryTime(endTime - startTime)
            _queryCount.value += 1
            
            // 緩存預加載的數據
            documents.forEach { document ->
                cacheData("${collection}_${document.id}", document.data)
            }
            
            Log.d("PerformanceOptimizer", "預加載完成: ${documents.size()} 條記錄")
            true
        } catch (e: Exception) {
            Log.e("PerformanceOptimizer", "預加載失敗", e)
            false
        }
    }
    
    /**
     * 優化查詢 - 帶緩存
     */
    suspend fun optimizedQuery(collection: String, cacheKey: String? = null): List<Map<String, Any>> {
        // 嘗試從緩存獲取
        if (cacheKey != null) {
            val cached = getCachedData(cacheKey)
            if (cached is List<*>) {
                @Suppress("UNCHECKED_CAST")
                return cached as List<Map<String, Any>>
            }
        }
        
        // 從 Firebase 獲取
        return try {
            val startTime = System.currentTimeMillis()
            
            val documents = firestore
                .collection(collection)
                .get()
                .await()
            
            val endTime = System.currentTimeMillis()
            recordQueryTime(endTime - startTime)
            _queryCount.value += 1
            
            val result = documents.map { it.data }
            
            // 緩存結果
            if (cacheKey != null) {
                cacheData(cacheKey, result)
            }
            
            Log.d("PerformanceOptimizer", "查詢完成: ${result.size} 條記錄")
            result
        } catch (e: Exception) {
            Log.e("PerformanceOptimizer", "查詢失敗", e)
            emptyList()
        }
    }
    
    /**
     * 獲取性能統計
     */
    fun getPerformanceStats(): PerformanceStats {
        val cacheHitRate = if (_queryCount.value > 0) {
            (_cacheHitCount.value.toFloat() / _queryCount.value) * 100
        } else {
            0f
        }
        
        val avgQueryTime = if (queryTimes.isNotEmpty()) {
            queryTimes.average().toLong()
        } else {
            0L
        }
        
        return PerformanceStats(
            totalQueries = _queryCount.value,
            cacheHits = _cacheHitCount.value,
            cacheHitRate = cacheHitRate,
            batchOperations = _batchOperationCount.value,
            averageQueryTime = avgQueryTime,
            cacheSize = cache.size
        )
    }
    
    /**
     * 重置性能統計
     */
    fun resetStats() {
        _queryCount.value = 0
        _cacheHitCount.value = 0
        _batchOperationCount.value = 0
        _averageQueryTime.value = 0
        queryTimes.clear()
        Log.d("PerformanceOptimizer", "性能統計已重置")
    }
    
    /**
     * 清理過期緩存
     */
    fun cleanupExpiredCache(maxAgeMinutes: Int = 30) {
        val cutoffTime = Instant.now().minusSeconds(maxAgeMinutes * 60L)
        val expiredKeys = cacheTimestamps.filter { it.value < cutoffTime }.keys
        
        expiredKeys.forEach { key ->
            cache.remove(key)
            cacheTimestamps.remove(key)
        }
        
        if (expiredKeys.isNotEmpty()) {
            Log.d("PerformanceOptimizer", "清理過期緩存: ${expiredKeys.size} 條記錄")
        }
    }
    
    /**
     * 記錄查詢時間
     */
    private fun recordQueryTime(timeMs: Long) {
        queryTimes.add(timeMs)
        // 保持最近 100 次查詢的記錄
        if (queryTimes.size > 100) {
            queryTimes.removeAt(0)
        }
        
        // 更新平均查詢時間
        _averageQueryTime.value = if (queryTimes.isNotEmpty()) {
            queryTimes.average().toLong()
        } else {
            0L
        }
    }
    
    /**
     * 性能統計數據類
     */
    data class PerformanceStats(
        val totalQueries: Int,
        val cacheHits: Int,
        val cacheHitRate: Float,
        val batchOperations: Int,
        val averageQueryTime: Long,
        val cacheSize: Int
    )
    
    /**
     * 批量操作結果
     */
    data class BatchResult(
        val success: Boolean,
        val processedCount: Int,
        val failedCount: Int,
        val executionTimeMs: Long,
        val errorMessage: String? = null
    )
    
    /**
     * 智能批量操作 - 自動分批處理大量數據
     */
    suspend fun smartBatchOperation(
        collection: String,
        operations: List<BatchOperation>,
        batchSize: Int = 500
    ): BatchResult {
        val startTime = System.currentTimeMillis()
        var totalProcessed = 0
        var totalFailed = 0
        
        try {
            // 分批處理
            operations.chunked(batchSize).forEach { batch ->
                val batchResult = processBatch(collection, batch)
                totalProcessed += batchResult.processedCount
                totalFailed += batchResult.failedCount
                
                // 添加延遲避免過載
                delay(100)
            }
            
            val endTime = System.currentTimeMillis()
            
            return BatchResult(
                success = totalFailed == 0,
                processedCount = totalProcessed,
                failedCount = totalFailed,
                executionTimeMs = endTime - startTime
            )
        } catch (e: Exception) {
            val endTime = System.currentTimeMillis()
            Log.e("PerformanceOptimizer", "智能批量操作失敗", e)
            
            return BatchResult(
                success = false,
                processedCount = totalProcessed,
                failedCount = totalFailed,
                executionTimeMs = endTime - startTime,
                errorMessage = e.message
            )
        }
    }
    
    /**
     * 處理單個批次
     */
    private suspend fun processBatch(collection: String, operations: List<BatchOperation>): BatchResult {
        val batch = firestore.batch()
        val collectionRef = firestore.collection(collection)
        var processedCount = 0
        var failedCount = 0
        
        try {
            operations.forEach { operation ->
                try {
                    when (operation.type) {
                        BatchOperationType.CREATE -> {
                            val docRef = collectionRef.document()
                            batch.set(docRef, operation.data)
                        }
                        BatchOperationType.UPDATE -> {
                            val docRef = collectionRef.document(operation.documentId!!)
                            batch.update(docRef, operation.data)
                        }
                        BatchOperationType.DELETE -> {
                            val docRef = collectionRef.document(operation.documentId!!)
                            batch.delete(docRef)
                        }
                    }
                    processedCount++
                } catch (e: Exception) {
                    Log.w("PerformanceOptimizer", "批量操作中的單個操作失敗", e)
                    failedCount++
                }
            }
            
            batch.commit().await()
            _batchOperationCount.value += 1
            
        } catch (e: Exception) {
            Log.e("PerformanceOptimizer", "批次提交失敗", e)
            failedCount = operations.size
        }
        
        return BatchResult(
            success = failedCount == 0,
            processedCount = processedCount,
            failedCount = failedCount,
            executionTimeMs = 0
        )
    }
    
    /**
     * 批量操作類型
     */
    enum class BatchOperationType {
        CREATE, UPDATE, DELETE
    }
    
    /**
     * 批量操作數據類
     */
    data class BatchOperation(
        val type: BatchOperationType,
        val data: Map<String, Any>,
        val documentId: String? = null
    )
}
