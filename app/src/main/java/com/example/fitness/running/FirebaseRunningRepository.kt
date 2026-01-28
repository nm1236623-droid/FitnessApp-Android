package com.example.fitness.running

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.util.*

object FirebaseRunningRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val collection = firestore.collection("cardio_records")
    
    private val _records = MutableStateFlow<List<CardioRecord>>(emptyList())
    val records = _records.asStateFlow()

    init {
        listenForUpdates()
    }

    private fun listenForUpdates() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            collection
                .whereEqualTo("userId", currentUser.uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("FirebaseRunningRepo", "Listen failed: ${error.message}")
                        return@addSnapshotListener
                    }
                    
                    val cardioRecords = snapshot?.documents?.mapNotNull { doc ->
                        parseCardioRecord(doc)
                    } ?: emptyList()
                    
                    _records.value = cardioRecords
                    Log.d("FirebaseRunningRepo", "Loaded ${cardioRecords.size} cardio records from Firestore")
                }
        }
    }

    private fun parseCardioRecord(doc: com.google.firebase.firestore.DocumentSnapshot): CardioRecord? {
        return try {
            val data = doc.data
            val cardioTypeName = data?.get("cardioType") as? String
            val defaultCardioType = CardioType.WALK_OR_JOG.name
            
            CardioRecord(
                id = doc.id,
                timestamp = (data?.get("timestamp") as? com.google.firebase.Timestamp)?.toInstant() ?: Instant.now(),
                durationSeconds = (data?.get("durationSeconds") as? Number)?.toInt() ?: 0,
                averageSpeedKph = data?.get("averageSpeedKph") as? Float,
                inclinePercent = data?.get("inclinePercent") as? Float,
                calories = data?.get("calories") as? Double,
                cardioType = CardioType.valueOf(cardioTypeName ?: defaultCardioType),
                userId = data?.get("userId") as? String
            )
        } catch (e: Exception) {
            Log.e("FirebaseRunningRepo", "Error parsing document ${doc.id}: ${e.message}")
            null
        }
    }

    suspend fun addRecord(record: CardioRecord): Result<String> = try {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Result.failure(Exception("User not authenticated"))
        } else {
            val recordWithUser = record.copy(userId = currentUser.uid)
            val data = mapOf(
                "timestamp" to recordWithUser.timestamp,
                "durationSeconds" to recordWithUser.durationSeconds,
                "averageSpeedKph" to recordWithUser.averageSpeedKph,
                "inclinePercent" to recordWithUser.inclinePercent,
                "calories" to recordWithUser.calories,
                "cardioType" to recordWithUser.cardioType.name,
                "userId" to recordWithUser.userId
            )
            
            val documentRef = collection.add(data).await()
            Log.d("FirebaseRunningRepo", "Successfully added cardio record with ID: ${documentRef.id}")
            Result.success(documentRef.id)
        }
    } catch (e: Exception) {
        Log.e("FirebaseRunningRepo", "Failed to add cardio record: ${e.message}", e)
        Result.failure(e)
    }

    suspend fun updateRecord(record: CardioRecord): Result<Unit> = try {
        val data = mapOf(
            "timestamp" to record.timestamp,
            "durationSeconds" to record.durationSeconds,
            "averageSpeedKph" to record.averageSpeedKph,
            "inclinePercent" to record.inclinePercent,
            "calories" to record.calories,
            "cardioType" to record.cardioType.name,
            "userId" to record.userId
        )
        
        collection.document(record.id).set(data).await()
        Log.d("FirebaseRunningRepo", "Successfully updated cardio record with ID: ${record.id}")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("FirebaseRunningRepo", "Failed to update cardio record: ${e.message}", e)
        Result.failure(e)
    }

    suspend fun deleteRecord(id: String): Result<Unit> = try {
        collection.document(id).delete().await()
        Log.d("FirebaseRunningRepo", "Successfully deleted cardio record with ID: $id")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("FirebaseRunningRepo", "Failed to delete cardio record: ${e.message}", e)
        Result.failure(e)
    }

    suspend fun getRecordsForDateRange(startDate: Instant, endDate: Instant): Result<List<CardioRecord>> = try {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Result.failure(Exception("User not authenticated"))
        } else {
            val docs = collection
                .whereEqualTo("userId", currentUser.uid)
                .whereGreaterThanOrEqualTo("timestamp", startDate)
                .whereLessThanOrEqualTo("timestamp", endDate)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val cardioRecords = docs.documents.mapNotNull { doc ->
                parseCardioRecord(doc)
            }
            
            Result.success(cardioRecords)
        }
    } catch (e: Exception) {
        Log.e("FirebaseRunningRepo", "Failed to get records for date range: ${e.message}", e)
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
            Log.d("FirebaseRunningRepo", "Successfully cleared all cardio records for user")
            Result.success(Unit)
        }
    } catch (e: Exception) {
        Log.e("FirebaseRunningRepo", "Failed to clear cardio records: ${e.message}", e)
        Result.failure(e)
    }
}
