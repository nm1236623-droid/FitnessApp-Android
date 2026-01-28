package com.example.fitness.ui

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Firebase 數據分析 Repository
 * 
 * 提供雲端訓練數據分析和統計功能
 * 支援跨設備數據同步和高級分析功能
 */
object FirebaseAnalysisRepository {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val analysisCollection = firestore.collection("training_analysis")
    
    private val _analysisResults = MutableStateFlow<List<TrainingAnalysis>>(emptyList())
    val analysisResults = _analysisResults.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    
    init {
        // 監聽用戶的分析結果
        startListening()
    }
    
    /**
     * 開始監聽用戶的分析數據
     */
    private fun startListening() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.w("FirebaseAnalysisRepo", "No authenticated user found")
            return
        }
        
        analysisCollection
            .whereEqualTo("userId", currentUser.uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirebaseAnalysisRepo", "Listen failed: ${error.message}")
                    _error.value = "數據監聽失敗: ${error.message}"
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val analyses = snapshot.documents.mapNotNull { doc ->
                        try {
                            val data = doc.data ?: return@mapNotNull null
                            TrainingAnalysis(
                                id = doc.id,
                                userId = data["userId"] as? String ?: "",
                                analysisType = data["analysisType"] as? String ?: "",
                                analysisData = data["analysisData"] as? Map<String, Any> ?: emptyMap(),
                                dateRangeStart = Instant.ofEpochMilli(
                                    data["dateRangeStartEpochMillis"] as? Long ?: 0L
                                ),
                                dateRangeEnd = Instant.ofEpochMilli(
                                    data["dateRangeEndEpochMillis"] as? Long ?: 0L
                                ),
                                createdAt = Instant.ofEpochMilli(
                                    data["createdAtEpochMillis"] as? Long ?: 0L
                                )
                            )
                        } catch (e: Exception) {
                            Log.e("FirebaseAnalysisRepo", "Error parsing document: ${e.message}")
                            null
                        }
                    }
                    _analysisResults.value = analyses
                    Log.d("FirebaseAnalysisRepo", "Updated ${analyses.size} analysis results")
                }
            }
    }
    
    /**
     * 創建訓練進度分析
     */
    suspend fun createProgressAnalysis(
        dateRangeStart: Instant,
        dateRangeEnd: Instant,
        exerciseData: List<ExerciseProgressData>
    ): Result<TrainingAnalysis> = try {
        _isLoading.value = true
        _error.value = null
        
        val currentUser = auth.currentUser
        if (currentUser == null) {
            throw Exception("用戶未登入")
        }
        
        // 計算進度分析
        val analysisData = calculateProgressAnalysis(exerciseData)
        
        val analysis = TrainingAnalysis(
            id = "",
            userId = currentUser.uid,
            analysisType = "progress",
            analysisData = analysisData,
            dateRangeStart = dateRangeStart,
            dateRangeEnd = dateRangeEnd,
            createdAt = Instant.now()
        )
        
        val analysisMap = mapOf(
            "userId" to currentUser.uid,
            "analysisType" to "progress",
            "analysisData" to analysisData,
            "dateRangeStartEpochMillis" to dateRangeStart.toEpochMilli(),
            "dateRangeEndEpochMillis" to dateRangeEnd.toEpochMilli(),
            "createdAtEpochMillis" to analysis.createdAt.toEpochMilli()
        )
        
        val documentRef = analysisCollection.add(analysisMap).await()
        val finalAnalysis = analysis.copy(id = documentRef.id)
        
        Log.d("FirebaseAnalysisRepo", "Created progress analysis: ${documentRef.id}")
        Result.success(finalAnalysis)
    } catch (e: Exception) {
        Log.e("FirebaseAnalysisRepo", "Error creating progress analysis: ${e.message}")
        _error.value = "創建進度分析失敗: ${e.message}"
        Result.failure(e)
    } finally {
        _isLoading.value = false
    }
    
    /**
     * 創建體重變化分析
     */
    suspend fun createWeightAnalysis(
        dateRangeStart: Instant,
        dateRangeEnd: Instant,
        weightData: List<WeightDataPoint>
    ): Result<TrainingAnalysis> = try {
        _isLoading.value = true
        _error.value = null
        
        val currentUser = auth.currentUser
        if (currentUser == null) {
            throw Exception("用戶未登入")
        }
        
        // 計算體重分析
        val analysisData = calculateWeightAnalysis(weightData)
        
        val analysis = TrainingAnalysis(
            id = "",
            userId = currentUser.uid,
            analysisType = "weight",
            analysisData = analysisData,
            dateRangeStart = dateRangeStart,
            dateRangeEnd = dateRangeEnd,
            createdAt = Instant.now()
        )
        
        val analysisMap = mapOf(
            "userId" to currentUser.uid,
            "analysisType" to "weight",
            "analysisData" to analysisData,
            "dateRangeStartEpochMillis" to dateRangeStart.toEpochMilli(),
            "dateRangeEndEpochMillis" to dateRangeEnd.toEpochMilli(),
            "createdAtEpochMillis" to analysis.createdAt.toEpochMilli()
        )
        
        val documentRef = analysisCollection.add(analysisMap).await()
        val finalAnalysis = analysis.copy(id = documentRef.id)
        
        Log.d("FirebaseAnalysisRepo", "Created weight analysis: ${documentRef.id}")
        Result.success(finalAnalysis)
    } catch (e: Exception) {
        Log.e("FirebaseAnalysisRepo", "Error creating weight analysis: ${e.message}")
        _error.value = "創建體重分析失敗: ${e.message}"
        Result.failure(e)
    } finally {
        _isLoading.value = false
    }
    
    /**
     * 創建綜合訓練報告
     */
    suspend fun createComprehensiveReport(
        dateRangeStart: Instant,
        dateRangeEnd: Instant,
        trainingData: TrainingSummaryData
    ): Result<TrainingAnalysis> = try {
        _isLoading.value = true
        _error.value = null
        
        val currentUser = auth.currentUser
        if (currentUser == null) {
            throw Exception("用戶未登入")
        }
        
        // 計算綜合分析
        val analysisData = calculateComprehensiveAnalysis(trainingData)
        
        val analysis = TrainingAnalysis(
            id = "",
            userId = currentUser.uid,
            analysisType = "comprehensive",
            analysisData = analysisData,
            dateRangeStart = dateRangeStart,
            dateRangeEnd = dateRangeEnd,
            createdAt = Instant.now()
        )
        
        val analysisMap = mapOf(
            "userId" to currentUser.uid,
            "analysisType" to "comprehensive",
            "analysisData" to analysisData,
            "dateRangeStartEpochMillis" to dateRangeStart.toEpochMilli(),
            "dateRangeEndEpochMillis" to dateRangeEnd.toEpochMilli(),
            "createdAtEpochMillis" to analysis.createdAt.toEpochMilli()
        )
        
        val documentRef = analysisCollection.add(analysisMap).await()
        val finalAnalysis = analysis.copy(id = documentRef.id)
        
        Log.d("FirebaseAnalysisRepo", "Created comprehensive report: ${documentRef.id}")
        Result.success(finalAnalysis)
    } catch (e: Exception) {
        Log.e("FirebaseAnalysisRepo", "Error creating comprehensive report: ${e.message}")
        _error.value = "創建綜合報告失敗: ${e.message}"
        Result.failure(e)
    } finally {
        _isLoading.value = false
    }
    
    /**
     * 獲取指定類型的分析結果
     */
    suspend fun getAnalysisByType(
        analysisType: String,
        limit: Int = 10
    ): Result<List<TrainingAnalysis>> = try {
        _isLoading.value = true
        _error.value = null
        
        val currentUser = auth.currentUser
        if (currentUser == null) {
            throw Exception("用戶未登入")
        }
        
        val analyses = analysisCollection
            .whereEqualTo("userId", currentUser.uid)
            .whereEqualTo("analysisType", analysisType)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .await()
            .documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    TrainingAnalysis(
                        id = doc.id,
                        userId = data["userId"] as? String ?: "",
                        analysisType = data["analysisType"] as? String ?: "",
                        analysisData = data["analysisData"] as? Map<String, Any> ?: emptyMap(),
                        dateRangeStart = Instant.ofEpochMilli(
                            data["dateRangeStartEpochMillis"] as? Long ?: 0L
                        ),
                        dateRangeEnd = Instant.ofEpochMilli(
                            data["dateRangeEndEpochMillis"] as? Long ?: 0L
                        ),
                        createdAt = Instant.ofEpochMilli(
                            data["createdAtEpochMillis"] as? Long ?: 0L
                        )
                    )
                } catch (e: Exception) {
                    Log.e("FirebaseAnalysisRepo", "Error parsing document: ${e.message}")
                    null
                }
            }
        
        Log.d("FirebaseAnalysisRepo", "Retrieved ${analyses.size} analyses of type: $analysisType")
        Result.success(analyses)
    } catch (e: Exception) {
        Log.e("FirebaseAnalysisRepo", "Error getting analyses by type: ${e.message}")
        _error.value = "獲取分析結果失敗: ${e.message}"
        Result.failure(e)
    } finally {
        _isLoading.value = false
    }
    
    /**
     * 刪除分析結果
     */
    suspend fun deleteAnalysis(analysisId: String): Result<Unit> = try {
        _isLoading.value = true
        _error.value = null
        
        val currentUser = auth.currentUser
        if (currentUser == null) {
            throw Exception("用戶未登入")
        }
        
        // 檢查分析是否屬於當前用戶
        val doc = analysisCollection.document(analysisId).get().await()
        if (!doc.exists()) {
            throw Exception("分析不存在")
        }
        
        val userId = doc.getString("userId")
        if (userId != currentUser.uid) {
            throw Exception("無權限刪除此分析")
        }
        
        analysisCollection.document(analysisId).delete().await()
        Log.d("FirebaseAnalysisRepo", "Deleted analysis: $analysisId")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("FirebaseAnalysisRepo", "Error deleting analysis: ${e.message}")
        _error.value = "刪除分析失敗: ${e.message}"
        Result.failure(e)
    } finally {
        _isLoading.value = false
    }
    
    /**
     * 清除用戶的所有分析結果
     */
    suspend fun clearAllAnalyses(): Result<Unit> = try {
        _isLoading.value = true
        _error.value = null
        
        val currentUser = auth.currentUser
        if (currentUser == null) {
            throw Exception("用戶未登入")
        }
        
        val userAnalyses = analysisCollection
            .whereEqualTo("userId", currentUser.uid)
            .get()
            .await()
        
        val batch = firestore.batch()
        userAnalyses.documents.forEach { doc ->
            batch.delete(doc.reference)
        }
        
        batch.commit().await()
        Log.d("FirebaseAnalysisRepo", "Cleared all analyses for user: ${currentUser.uid}")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("FirebaseAnalysisRepo", "Error clearing analyses: ${e.message}")
        _error.value = "清除分析失敗: ${e.message}"
        Result.failure(e)
    } finally {
        _isLoading.value = false
    }
    
    /**
     * 計算進度分析
     */
    private fun calculateProgressAnalysis(exerciseData: List<ExerciseProgressData>): Map<String, Any> {
        if (exerciseData.isEmpty()) {
            return mapOf("status" to "no_data")
        }
        
        // 按運動名稱分組
        val groupedData = exerciseData.groupBy { it.exerciseName }
        
        val progressResults = mutableMapOf<String, Map<String, Any>>()
        
        groupedData.forEach { (exerciseName, dataPoints) ->
            val sortedData = dataPoints.sortedBy { it.date }
            
            if (sortedData.size >= 2) {
                val firstWeight = sortedData.first().weight ?: 0.0
                val lastWeight = sortedData.last().weight ?: 0.0
                val weightChange = lastWeight - firstWeight
                val percentChange = if (firstWeight > 0) (weightChange / firstWeight * 100) else 0.0
                
                val totalVolume = sortedData.sumOf { (it.weight ?: 0.0) * it.sets * it.reps }
                val avgVolume = totalVolume / sortedData.size
                
                progressResults[exerciseName] = mapOf(
                    "weightChange" to weightChange,
                    "percentChange" to percentChange,
                    "totalVolume" to totalVolume,
                    "averageVolume" to avgVolume,
                    "dataPoints" to sortedData.size,
                    "trend" to if (weightChange > 0) "increasing" else if (weightChange < 0) "decreasing" else "stable"
                )
            }
        }
        
        return mapOf(
            "exerciseProgress" to progressResults,
            "totalExercises" to progressResults.size,
            "analysisDate" to Instant.now().toEpochMilli()
        )
    }
    
    /**
     * 計算體重分析
     */
    private fun calculateWeightAnalysis(weightData: List<WeightDataPoint>): Map<String, Any> {
        if (weightData.isEmpty()) {
            return mapOf("status" to "no_data")
        }
        
        val sortedData = weightData.sortedBy { it.date }
        val weights = sortedData.map { it.weight }
        
        val firstWeight = weights.first()
        val lastWeight = weights.last()
        val weightChange = lastWeight - firstWeight
        val avgWeight = weights.average()
        val maxWeight = weights.maxOrNull() ?: 0.0
        val minWeight = weights.minOrNull() ?: 0.0
        
        // 計算趨勢（簡單線性回歸）
        val trend = calculateTrend(sortedData)
        
        return mapOf(
            "startWeight" to firstWeight,
            "endWeight" to lastWeight,
            "weightChange" to weightChange,
            "averageWeight" to avgWeight,
            "maxWeight" to maxWeight,
            "minWeight" to minWeight,
            "trend" to trend,
            "dataPoints" to sortedData.size,
            "analysisDate" to Instant.now().toEpochMilli()
        )
    }
    
    /**
     * 計算綜合分析
     */
    private fun calculateComprehensiveAnalysis(trainingData: TrainingSummaryData): Map<String, Any> {
        return mapOf(
            "totalWorkouts" to trainingData.totalWorkouts,
            "totalDuration" to trainingData.totalDurationMinutes,
            "totalVolume" to trainingData.totalVolume,
            "averageWorkoutDuration" to (trainingData.totalDurationMinutes / trainingData.totalWorkouts.toDouble()),
            "mostTrainedDay" to trainingData.mostTrainedDay,
            "exerciseFrequency" to trainingData.exerciseFrequency,
            "weeklyProgress" to trainingData.weeklyProgress,
            "analysisDate" to Instant.now().toEpochMilli()
        )
    }
    
    /**
     * 計算趨勢（簡單線性回歸）
     */
    private fun calculateTrend(data: List<WeightDataPoint>): String {
        if (data.size < 2) return "insufficient_data"
        
        val n = data.size.toDouble()
        val x = data.indices.map { it.toDouble() }
        val y = data.map { it.weight }
        
        val sumX = x.sum()
        val sumY = y.sum()
        val sumXY = x.zip(y) { xi, yi -> xi * yi }.sum()
        val sumX2 = x.map { it * it }.sum()
        
        val slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX)
        
        return when {
            slope > 0.1 -> "increasing"
            slope < -0.1 -> "decreasing"
            else -> "stable"
        }
    }
}

/**
 * 訓練分析數據模型
 */
data class TrainingAnalysis(
    val id: String,
    val userId: String,
    val analysisType: String, // progress, weight, comprehensive
    val analysisData: Map<String, Any>,
    val dateRangeStart: Instant,
    val dateRangeEnd: Instant,
    val createdAt: Instant
)

/**
 * 運動進度數據點
 */
data class ExerciseProgressData(
    val exerciseName: String,
    val date: Instant,
    val weight: Double?,
    val sets: Int,
    val reps: Int
)

/**
 * 體重數據點
 */
data class WeightDataPoint(
    val date: Instant,
    val weight: Double
)

/**
 * 訓練摘要數據
 */
data class TrainingSummaryData(
    val totalWorkouts: Int,
    val totalDurationMinutes: Int,
    val totalVolume: Double,
    val mostTrainedDay: String,
    val exerciseFrequency: Map<String, Int>,
    val weeklyProgress: Map<String, Double>
)
