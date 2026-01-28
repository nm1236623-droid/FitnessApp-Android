package com.example.fitness.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate

import java.time.ZoneId
import java.util.UUID


class FirebaseTrainingRecordRepository : TrainingRecordRepository() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val collection = firestore.collection("training_records")
    
    override val _records = MutableStateFlow<List<TrainingRecord>>(emptyList())
    override val records = _records.asStateFlow()

    init {
        // Listen to real-time updates for current user
        listenForUpdates()
    }

    private fun listenForUpdates() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            collection
                .whereEqualTo("userId", currentUser.uid)
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("FirebaseTrainingRepo", "Listen failed: ${error.message}")
                        return@addSnapshotListener
                    }
                    
                    val trainingRecords = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            val data = doc.data
                            TrainingRecord(
                                id = doc.id,
                                planId = data?.get("planId") as? String ?: "",
                                planName = data?.get("planName") as? String ?: "",
                                date = LocalDate.parse(data?.get("date") as? String ?: ""),
                                durationInSeconds = (data?.get("durationInSeconds") as? Number)?.toInt() ?: 0,
                                caloriesBurned = (data?.get("caloriesBurned") as? Number)?.toDouble() ?: 0.0,
                                exercises = (data?.get("exercises") as? List<Map<String, Any>>)?.map { ex ->
                                    ExerciseRecord(
                                        name = ex["name"] as? String ?: "",
                                        sets = (ex["sets"] as? Number)?.toInt() ?: 0,
                                        reps = (ex["reps"] as? Number)?.toInt() ?: 0,
                                        weight = (ex["weight"] as? Number)?.toDouble()
                                    )
                                } ?: emptyList(),
                                notes = data?.get("notes") as? String,
                                userId = data?.get("userId") as? String
                            )
                        } catch (e: Exception) {
                            Log.e("FirebaseTrainingRepo", "Error parsing document ${doc.id}: ${e.message}")
                            null
                        }
                    } ?: emptyList()
                    
                    _records.value = trainingRecords
                    Log.d("FirebaseTrainingRepo", "Loaded ${trainingRecords.size} training records from Firestore")
                }
        }
    }

    override fun addRecord(record: TrainingRecord) {
    _records.value = _records.value + record
}

suspend fun addRecordToFirebase(record: TrainingRecord): Result<String> = try {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Result.failure(Exception("User not authenticated"))
        } else {
            val recordWithUser = record.copy(userId = currentUser.uid)
            val data = mapOf(
                "planId" to recordWithUser.planId,
                "planName" to recordWithUser.planName,
                "date" to recordWithUser.date.toString(),
                "durationInSeconds" to recordWithUser.durationInSeconds,
                "caloriesBurned" to recordWithUser.caloriesBurned,
                "exercises" to recordWithUser.exercises.map { ex ->
                    mapOf(
                        "name" to ex.name,
                        "sets" to ex.sets,
                        "reps" to ex.reps,
                        "weight" to ex.weight
                    )
                },
                "notes" to recordWithUser.notes,
                "userId" to recordWithUser.userId
            )
            
            val documentRef = collection.add(data).await()
            Log.d("FirebaseTrainingRepo", "Successfully added training record with ID: ${documentRef.id}")
            Result.success(documentRef.id)
        }
    } catch (e: Exception) {
        Log.e("FirebaseTrainingRepo", "Failed to add training record: ${e.message}", e)
        Result.failure(e)
    }

    suspend fun updateRecord(record: TrainingRecord): Result<Unit> = try {
        val data = mapOf(
            "planId" to record.planId,
            "planName" to record.planName,
            "date" to record.date.toString(),
            "durationInSeconds" to record.durationInSeconds,
            "caloriesBurned" to record.caloriesBurned,
            "exercises" to record.exercises.map { ex ->
                mapOf(
                    "name" to ex.name,
                    "sets" to ex.sets,
                    "reps" to ex.reps,
                    "weight" to ex.weight
                )
            },
            "notes" to record.notes,
            "userId" to record.userId
        )
        
        collection.document(record.id).set(data).await()
        Log.d("FirebaseTrainingRepo", "Successfully updated training record with ID: ${record.id}")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("FirebaseTrainingRepo", "Failed to update training record: ${e.message}", e)
        Result.failure(e)
    }

    suspend fun deleteRecord(id: String): Result<Unit> = try {
        collection.document(id).delete().await()
        Log.d("FirebaseTrainingRepo", "Successfully deleted training record with ID: $id")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("FirebaseTrainingRepo", "Failed to delete training record: ${e.message}", e)
        Result.failure(e)
    }

    suspend fun clear(): Result<Unit> = try {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Result.failure(Exception("User not authenticated"))
        } else {
            val docs = collection
                .whereEqualTo("userId", currentUser.uid)
                .get()
                .await()
            
            val batch = firestore.batch()
            docs.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            
            batch.commit().await()
            Log.d("FirebaseTrainingRepo", "Successfully cleared all training records for user")
            Result.success(Unit)
        }
    } catch (e: Exception) {
        Log.e("FirebaseTrainingRepo", "Failed to clear training records: ${e.message}", e)
        Result.failure(e)
    }

    // Query methods
    override fun getRecordsForDate(date: LocalDate): List<TrainingRecord> {
        return _records.value.filter { it.date == date }
    }

    override fun getRecordsForPlan(planId: String): List<TrainingRecord> {
        return _records.value.filter { it.planId == planId }
    }

    suspend fun getRecordsForDateRange(startDate: LocalDate, endDate: LocalDate): Result<List<TrainingRecord>> = try {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Result.failure(Exception("User not authenticated"))
        } else {
            val docs = collection
                .whereEqualTo("userId", currentUser.uid)
                .whereGreaterThanOrEqualTo("date", startDate.toString())
                .whereLessThanOrEqualTo("date", endDate.toString())
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val trainingRecords = docs.documents.mapNotNull { doc ->
                try {
                    val data = doc.data
                    TrainingRecord(
                        id = doc.id,
                        planId = data?.get("planId") as? String ?: "",
                        planName = data?.get("planName") as? String ?: "",
                        date = LocalDate.parse(data?.get("date") as? String ?: ""),
                        durationInSeconds = (data?.get("durationInSeconds") as? Number)?.toInt() ?: 0,
                        caloriesBurned = (data?.get("caloriesBurned") as? Number)?.toDouble() ?: 0.0,
                        exercises = (data?.get("exercises") as? List<Map<String, Any>>)?.map { ex ->
                            ExerciseRecord(
                                name = ex["name"] as? String ?: "",
                                sets = (ex["sets"] as? Number)?.toInt() ?: 0,
                                reps = (ex["reps"] as? Number)?.toInt() ?: 0,
                                weight = (ex["weight"] as? Number)?.toDouble()
                            )
                        } ?: emptyList(),
                        notes = data?.get("notes") as? String,
                        userId = data?.get("userId") as? String
                    )
                } catch (e: Exception) {
                    Log.e("FirebaseTrainingRepo", "Error parsing document ${doc.id}: ${e.message}")
                    null
                }
            }
            
            Result.success(trainingRecords)
        }
    } catch (e: Exception) {
        Log.e("FirebaseTrainingRepo", "Failed to get records for date range: ${e.message}", e)
        Result.failure(e)
    }

    // Statistics methods
    fun getTotalCaloriesBurned(date: LocalDate): Double {
        return _records.value
            .filter { it.date == date }
            .sumOf { it.caloriesBurned }
    }

    fun getTotalDuration(date: LocalDate): Int {
        return _records.value
            .filter { it.date == date }
            .sumOf { it.durationInSeconds }
    }

    fun getWeeklyStats(weekStart: LocalDate): Map<LocalDate, Double> {
        val weekEnd = weekStart.plusDays(6)
        return _records.value
            .filter { it.date in weekStart..weekEnd }
            .groupBy { it.date }
            .mapValues { (_, records) -> records.sumOf { it.caloriesBurned } }
    }
}
