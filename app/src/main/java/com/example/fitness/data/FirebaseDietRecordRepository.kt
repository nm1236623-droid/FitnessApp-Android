package com.example.fitness.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.util.*

object FirebaseDietRecordRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val collection = firestore.collection("diet_records")
    
    private val _records = MutableStateFlow<List<DietRecord>>(emptyList())
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
                        Log.e("FirebaseDietRecordRepo", "Listen failed: ${error.message}")
                        return@addSnapshotListener
                    }
                    
                    val dietRecords = snapshot?.documents?.mapNotNull { doc ->
                        parseDietRecord(doc)
                    } ?: emptyList()
                    
                    _records.value = dietRecords
                    Log.d("FirebaseDietRecordRepo", "Loaded ${dietRecords.size} diet records from Firestore")
                }
        }
    }

    private fun parseDietRecord(doc: com.google.firebase.firestore.DocumentSnapshot): DietRecord? {
        return try {
            val data = doc.data
            val dateString = data?.get("dateIso") as? String
            val defaultDate = LocalDate.now().toString()
            
            DietRecord(
                id = doc.id,
                foodName = data?.get("foodName") as? String ?: "",
                calories = (data?.get("calories") as? Number)?.toInt() ?: 0,
                date = LocalDate.parse(dateString ?: defaultDate),
                mealType = data?.get("mealType") as? String ?: "",
                proteinG = data?.get("proteinG") as? Double,
                carbsG = data?.get("carbsG") as? Double,
                fatG = data?.get("fatG") as? Double,
                timestamp = (data?.get("timestamp") as? com.google.firebase.Timestamp)?.toInstant() ?: Instant.now(),
                userId = data?.get("userId") as? String
            )
        } catch (e: Exception) {
            Log.e("FirebaseDietRecordRepo", "Error parsing document ${doc.id}: ${e.message}")
            null
        }
    }

    suspend fun addRecord(record: DietRecord): Result<String> = try {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Result.failure(Exception("User not authenticated"))
        } else {
            val recordWithUser = record.copy(userId = currentUser.uid)
            val data = mapOf(
                "foodName" to recordWithUser.foodName,
                "calories" to recordWithUser.calories,
                "dateIso" to recordWithUser.date.toString(),
                "mealType" to recordWithUser.mealType,
                "proteinG" to recordWithUser.proteinG,
                "carbsG" to recordWithUser.carbsG,
                "fatG" to recordWithUser.fatG,
                "timestamp" to recordWithUser.timestamp,
                "userId" to recordWithUser.userId
            )
            
            val documentRef = collection.add(data).await()
            Log.d("FirebaseDietRecordRepo", "Successfully added diet record with ID: ${documentRef.id}")
            Result.success(documentRef.id)
        }
    } catch (e: Exception) {
        Log.e("FirebaseDietRecordRepo", "Failed to add diet record: ${e.message}", e)
        Result.failure(e)
    }

    suspend fun updateRecord(record: DietRecord): Result<Unit> = try {
        val data = mapOf(
            "foodName" to record.foodName,
            "calories" to record.calories,
            "dateIso" to record.date.toString(),
            "mealType" to record.mealType,
            "proteinG" to record.proteinG,
            "carbsG" to record.carbsG,
            "fatG" to record.fatG,
            "timestamp" to record.timestamp,
            "userId" to record.userId
        )
        
        collection.document(record.id).set(data).await()
        Log.d("FirebaseDietRecordRepo", "Successfully updated diet record with ID: ${record.id}")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("FirebaseDietRecordRepo", "Failed to update diet record: ${e.message}", e)
        Result.failure(e)
    }

    suspend fun deleteRecord(id: String): Result<Unit> = try {
        collection.document(id).delete().await()
        Log.d("FirebaseDietRecordRepo", "Successfully deleted diet record with ID: $id")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("FirebaseDietRecordRepo", "Failed to delete diet record: ${e.message}", e)
        Result.failure(e)
    }

    suspend fun getRecordsForDateRange(startDate: LocalDate, endDate: LocalDate): Result<List<DietRecord>> = try {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Result.failure(Exception("User not authenticated"))
        } else {
            val docs = collection
                .whereEqualTo("userId", currentUser.uid)
                .whereGreaterThanOrEqualTo("dateIso", startDate.toString())
                .whereLessThanOrEqualTo("dateIso", endDate.toString())
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val dietRecords = docs.documents.mapNotNull { doc ->
                parseDietRecord(doc)
            }
            
            Result.success(dietRecords)
        }
    } catch (e: Exception) {
        Log.e("FirebaseDietRecordRepo", "Failed to get records for date range: ${e.message}", e)
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
            Log.d("FirebaseDietRecordRepo", "Successfully cleared all diet records for user")
            Result.success(Unit)
        }
    } catch (e: Exception) {
        Log.e("FirebaseDietRecordRepo", "Failed to clear diet records: ${e.message}", e)
        Result.failure(e)
    }
}
