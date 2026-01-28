package com.example.fitness.template

import android.content.Context
import android.util.Log
import com.example.fitness.data.ExerciseEntry
import com.example.fitness.data.TrainingPlan
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.gson.Gson
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.Instant

/**
 * 訓練模板類別
 */
enum class TemplateCategory {
    BEGINNER,       // 新手
    INTERMEDIATE,   // 中階
    ADVANCED,       // 進階
    STRENGTH,       // 力量
    HYPERTROPHY,    // 肌肥大
    ENDURANCE,      // 耐力
    WEIGHT_LOSS,    // 減脂
    FULL_BODY,      // 全身
    UPPER_BODY,     // 上半身
    LOWER_BODY,     // 下半身
    CUSTOM          // 自訂
}

/**
 * 訓練模板
 */
data class WorkoutTemplate(
    val id: String,
    val name: String,
    val description: String,
    val category: TemplateCategory,
    val difficulty: Int,               // 1-5
    val estimatedDuration: Int,        // 分鐘
    val exercises: List<ExerciseEntry>,
    val createdBy: String,
    val creatorName: String,
    val isPublic: Boolean = false,
    val likes: Int = 0,
    val usageCount: Int = 0,
    val tags: List<String> = emptyList(),
    val imageUrl: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

class WorkoutTemplateRepository(private val context: Context) {
    private val db = FirebaseFirestore.getInstance()
    private val gson = Gson()
    private val fileName = "workout_templates.json"

    // ==================== 本地模板管理 ====================

    /**
     * 保存模板到本地
     */
    suspend fun saveTemplateLocally(template: WorkoutTemplate): Result<Unit> {
        return try {
            val templates = loadLocalTemplates().toMutableList()
            val existingIndex = templates.indexOfFirst { it.id == template.id }

            if (existingIndex >= 0) {
                templates[existingIndex] = template
            } else {
                templates.add(template)
            }

            saveLocalTemplates(templates)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("TemplateRepo", "Error saving template locally", e)
            Result.failure(e)
        }
    }

    /**
     * 獲取本地模板
     */
    suspend fun getLocalTemplates(): List<WorkoutTemplate> {
        return loadLocalTemplates()
    }

    /**
     * 刪除本地模板
     */
    suspend fun deleteLocalTemplate(templateId: String): Result<Unit> {
        return try {
            val templates = loadLocalTemplates().toMutableList()
            templates.removeAll { it.id == templateId }
            saveLocalTemplates(templates)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== 雲端模板管理 ====================

    /**
     * 發布模板到社群
     */
    suspend fun publishTemplate(template: WorkoutTemplate): Result<String> {
        return try {
            val publicTemplate = template.copy(
                isPublic = true,
                createdAt = Instant.now()
            )

            val docRef = db.collection("workout_templates").document()
            val newTemplate = publicTemplate.copy(id = docRef.id)

            docRef.set(newTemplate).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("TemplateRepo", "Error publishing template", e)
            Result.failure(e)
        }
    }

    /**
     * 獲取公開模板列表
     */
    fun getPublicTemplates(category: TemplateCategory? = null): Flow<List<WorkoutTemplate>> = callbackFlow {
        var query = db.collection("workout_templates")
            .whereEqualTo("isPublic", true)
            .orderBy("usageCount", Query.Direction.DESCENDING)

        category?.let {
            query = query.whereEqualTo("category", it.name)
        }

        val listener = query.limit(50).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("TemplateRepo", "Error getting public templates", error)
                trySend(emptyList())
                return@addSnapshotListener
            }

            val templates = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(WorkoutTemplate::class.java)
            } ?: emptyList()

            trySend(templates)
        }

        awaitClose { listener.remove() }
    }

    /**
     * 使用模板創建訓練計劃
     */
    fun createPlanFromTemplate(template: WorkoutTemplate): TrainingPlan {
        return TrainingPlan(
            id = java.util.UUID.randomUUID().toString(),
            name = template.name,
            exercises = template.exercises,
            createdAt = Instant.now()
        )
    }

    /**
     * 增加模板使用次數
     */
    suspend fun incrementUsageCount(templateId: String): Result<Unit> {
        return try {
            db.collection("workout_templates")
                .document(templateId)
                .update("usageCount", com.google.firebase.firestore.FieldValue.increment(1))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 點讚模板
     */
    suspend fun likeTemplate(templateId: String): Result<Unit> {
        return try {
            db.collection("workout_templates")
                .document(templateId)
                .update("likes", com.google.firebase.firestore.FieldValue.increment(1))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== 本地存儲輔助方法 ====================

    private fun loadLocalTemplates(): List<WorkoutTemplate> {
        return try {
            val file = context.getFileStreamPath(fileName)
            if (!file.exists()) return emptyList()

            val content = context.openFileInput(fileName).bufferedReader().use { it.readText() }
            val type = object : com.google.gson.reflect.TypeToken<List<WorkoutTemplate>>() {}.type
            gson.fromJson(content, type) ?: emptyList()
        } catch (e: Exception) {
            Log.w("TemplateRepo", "Failed to load local templates", e)
            emptyList()
        }
    }

    private fun saveLocalTemplates(templates: List<WorkoutTemplate>) {
        try {
            val content = gson.toJson(templates)
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use {
                it.write(content.toByteArray())
            }
        } catch (e: Exception) {
            Log.e("TemplateRepo", "Failed to save local templates", e)
        }
    }
}
