package com.example.fitness.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.util.UUID

/**
 * Firebase 體態相簿 Repository
 * 
 * 提供雲端體態照片的存儲和管理
 * 使用 Firebase Storage 存儲圖片，Firestore 存儲元數據
 */
object FirebaseBodyPhotoRepository {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val photosCollection = firestore.collection("body_photos")
    private val storageRef = storage.reference.child("body_photos")
    
    private val _photos = MutableStateFlow<List<BodyPhoto>>(emptyList())
    val photos = _photos.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    
    init {
        // 監聽當前用戶的體態照片變化
        startListening()
    }
    
    /**
     * 開始監聽用戶的體態照片
     */
    private fun startListening() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.w("FirebaseBodyPhotoRepo", "No authenticated user found")
            return
        }
        
        photosCollection
            .whereEqualTo("userId", currentUser.uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirebaseBodyPhotoRepo", "Listen failed: ${error.message}")
                    _error.value = "數據監聽失敗: ${error.message}"
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val photos = snapshot.documents.mapNotNull { doc ->
                        try {
                            val data = doc.data ?: return@mapNotNull null
                            BodyPhoto(
                                id = doc.id,
                                userId = data["userId"] as? String ?: "",
                                fileName = data["fileName"] as? String ?: "",
                                storagePath = data["storagePath"] as? String ?: "",
                                downloadUrl = data["downloadUrl"] as? String ?: "",
                                timestamp = Instant.ofEpochMilli(
                                    data["timestampEpochMillis"] as? Long ?: 0L
                                ),
                                fileSizeBytes = (data["fileSizeBytes"] as? Number)?.toLong() ?: 0L,
                                mimeType = data["mimeType"] as? String ?: "image/jpeg",
                                description = data["description"] as? String,
                                tags = (data["tags"] as? List<String>) ?: emptyList()
                            )
                        } catch (e: Exception) {
                            Log.e("FirebaseBodyPhotoRepo", "Error parsing document: ${e.message}")
                            null
                        }
                    }
                    _photos.value = photos
                    Log.d("FirebaseBodyPhotoRepo", "Updated ${photos.size} body photos")
                }
            }
    }
    
    /**
     * 上傳體態照片
     */
    suspend fun uploadPhoto(
        context: Context,
        imageUri: Uri,
        description: String? = null,
        tags: List<String> = emptyList()
    ): Result<BodyPhoto> = try {
        _isLoading.value = true
        _error.value = null
        
        val currentUser = auth.currentUser
        if (currentUser == null) {
            throw Exception("用戶未登入")
        }
        
        // 生成唯一檔名
        val fileName = "${UUID.randomUUID()}.jpg"
        val storagePath = "${currentUser.uid}/${fileName}"
        val photoRef = storageRef.child(storagePath)
        
        // 上傳圖片到 Firebase Storage
        val uploadTask = photoRef.putFile(imageUri).await()
        val downloadUrl = photoRef.downloadUrl.await().toString()
        
        // 獲取檔案大小
        val fileSizeBytes = uploadTask.bytesTransferred
        
        // 創建元數據
        val photoMetadata = BodyPhoto(
            id = "",
            userId = currentUser.uid,
            fileName = fileName,
            storagePath = storagePath,
            downloadUrl = downloadUrl,
            timestamp = Instant.now(),
            fileSizeBytes = fileSizeBytes,
            mimeType = "image/jpeg",
            description = description,
            tags = tags
        )
        
        // 保存元數據到 Firestore
        val metadataMap = mapOf(
            "userId" to currentUser.uid,
            "fileName" to fileName,
            "storagePath" to storagePath,
            "downloadUrl" to downloadUrl,
            "timestampEpochMillis" to photoMetadata.timestamp.toEpochMilli(),
            "fileSizeBytes" to fileSizeBytes,
            "mimeType" to "image/jpeg",
            "description" to description,
            "tags" to tags,
            "createdAt" to System.currentTimeMillis(),
            "updatedAt" to System.currentTimeMillis()
        )
        
        val documentRef = photosCollection.add(metadataMap).await()
        val finalPhoto = photoMetadata.copy(id = documentRef.id)
        
        Log.d("FirebaseBodyPhotoRepo", "Uploaded body photo with ID: ${documentRef.id}")
        Result.success(finalPhoto)
    } catch (e: Exception) {
        Log.e("FirebaseBodyPhotoRepo", "Error uploading photo: ${e.message}")
        _error.value = "上傳照片失敗: ${e.message}"
        Result.failure(e)
    } finally {
        _isLoading.value = false
    }
    
    /**
     * 刪除體態照片
     */
    suspend fun deletePhoto(photoId: String): Result<Unit> = try {
        _isLoading.value = true
        _error.value = null
        
        val currentUser = auth.currentUser
        if (currentUser == null) {
            throw Exception("用戶未登入")
        }
        
        // 獲取照片元數據
        val doc = photosCollection.document(photoId).get().await()
        if (!doc.exists()) {
            throw Exception("照片不存在")
        }
        
        val userId = doc.getString("userId")
        if (userId != currentUser.uid) {
            throw Exception("無權限刪除此照片")
        }
        
        val storagePath = doc.getString("storagePath")
        
        // 刪除 Firestore 中的元數據
        photosCollection.document(photoId).delete().await()
        
        // 刪除 Firebase Storage 中的圖片
        if (storagePath != null) {
            storageRef.child(storagePath).delete().await()
        }
        
        Log.d("FirebaseBodyPhotoRepo", "Deleted body photo: $photoId")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("FirebaseBodyPhotoRepo", "Error deleting photo: ${e.message}")
        _error.value = "刪除照片失敗: ${e.message}"
        Result.failure(e)
    } finally {
        _isLoading.value = false
    }
    
    /**
     * 更新照片元數據
     */
    suspend fun updatePhotoMetadata(
        photoId: String,
        description: String? = null,
        tags: List<String> = emptyList()
    ): Result<Unit> = try {
        _isLoading.value = true
        _error.value = null
        
        val currentUser = auth.currentUser
        if (currentUser == null) {
            throw Exception("用戶未登入")
        }
        
        // 檢查照片是否屬於當前用戶
        val doc = photosCollection.document(photoId).get().await()
        if (!doc.exists()) {
            throw Exception("照片不存在")
        }
        
        val userId = doc.getString("userId")
        if (userId != currentUser.uid) {
            throw Exception("無權限修改此照片")
        }
        
        // 更新元數據
        val updateData = mutableMapOf<String, Any>(
            "updatedAt" to System.currentTimeMillis()
        )
        
        description?.let { updateData["description"] = it }
        if (tags.isNotEmpty()) {
            updateData["tags"] = tags
        }
        
        photosCollection.document(photoId).update(updateData).await()
        Log.d("FirebaseBodyPhotoRepo", "Updated photo metadata: $photoId")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("FirebaseBodyPhotoRepo", "Error updating photo metadata: ${e.message}")
        _error.value = "更新照片元數據失敗: ${e.message}"
        Result.failure(e)
    } finally {
        _isLoading.value = false
    }
    
    /**
     * 清除用戶的所有體態照片
     */
    suspend fun clearAllPhotos(): Result<Unit> = try {
        _isLoading.value = true
        _error.value = null
        
        val currentUser = auth.currentUser
        if (currentUser == null) {
            throw Exception("用戶未登入")
        }
        
        val userPhotos = photosCollection
            .whereEqualTo("userId", currentUser.uid)
            .get()
            .await()
        
        // 批量刪除 Firestore 元數據
        val batch = firestore.batch()
        userPhotos.documents.forEach { doc ->
            batch.delete(doc.reference)
        }
        batch.commit().await()
        
        // 批量刪除 Storage 檔案
        userPhotos.documents.forEach { doc ->
            val storagePath = doc.getString("storagePath")
            if (storagePath != null) {
                storageRef.child(storagePath).delete()
            }
        }
        
        Log.d("FirebaseBodyPhotoRepo", "Cleared all body photos for user: ${currentUser.uid}")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("FirebaseBodyPhotoRepo", "Error clearing photos: ${e.message}")
        _error.value = "清除照片失敗: ${e.message}"
        Result.failure(e)
    } finally {
        _isLoading.value = false
    }
    
    /**
     * 獲取指定日期範圍的照片
     */
    suspend fun getPhotosByDateRange(
        startDate: Instant,
        endDate: Instant
    ): Result<List<BodyPhoto>> = try {
        _isLoading.value = true
        _error.value = null
        
        val currentUser = auth.currentUser
        if (currentUser == null) {
            throw Exception("用戶未登入")
        }
        
        val photos = photosCollection
            .whereEqualTo("userId", currentUser.uid)
            .whereGreaterThanOrEqualTo("timestampEpochMillis", startDate.toEpochMilli())
            .whereLessThanOrEqualTo("timestampEpochMillis", endDate.toEpochMilli())
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .await()
            .documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    BodyPhoto(
                        id = doc.id,
                        userId = data["userId"] as? String ?: "",
                        fileName = data["fileName"] as? String ?: "",
                        storagePath = data["storagePath"] as? String ?: "",
                        downloadUrl = data["downloadUrl"] as? String ?: "",
                        timestamp = Instant.ofEpochMilli(
                            data["timestampEpochMillis"] as? Long ?: 0L
                        ),
                        fileSizeBytes = (data["fileSizeBytes"] as? Number)?.toLong() ?: 0L,
                        mimeType = data["mimeType"] as? String ?: "image/jpeg",
                        description = data["description"] as? String,
                        tags = (data["tags"] as? List<String>) ?: emptyList()
                    )
                } catch (e: Exception) {
                    Log.e("FirebaseBodyPhotoRepo", "Error parsing document: ${e.message}")
                    null
                }
            }
        
        Log.d("FirebaseBodyPhotoRepo", "Retrieved ${photos.size} photos for date range")
        Result.success(photos)
    } catch (e: Exception) {
        Log.e("FirebaseBodyPhotoRepo", "Error getting photos by date range: ${e.message}")
        _error.value = "獲取照片失敗: ${e.message}"
        Result.failure(e)
    } finally {
        _isLoading.value = false
    }
    
    /**
     * 下載照片到本地
     */
    suspend fun downloadPhotoToLocal(
        context: Context,
        photo: BodyPhoto,
        localFileName: String? = null
    ): Result<File> = try {
        _isLoading.value = true
        _error.value = null
        
        val fileName = localFileName ?: "body_photo_${photo.timestamp.toEpochMilli()}.jpg"
        val localFile = File(context.cacheDir, fileName)
        
        // 從 Firebase Storage 下載
        val photoRef = storage.getReferenceFromUrl(photo.downloadUrl)
        photoRef.getFile(localFile).await()
        
        Log.d("FirebaseBodyPhotoRepo", "Downloaded photo to: ${localFile.absolutePath}")
        Result.success(localFile)
    } catch (e: Exception) {
        Log.e("FirebaseBodyPhotoRepo", "Error downloading photo: ${e.message}")
        _error.value = "下載照片失敗: ${e.message}"
        Result.failure(e)
    } finally {
        _isLoading.value = false
    }
    
    /**
     * 獲取用戶照片統計
     */
    suspend fun getPhotoStats(): Result<BodyPhotoStats> = try {
        _isLoading.value = true
        _error.value = null
        
        val currentUser = auth.currentUser
        if (currentUser == null) {
            throw Exception("用戶未登入")
        }
        
        val photos = photosCollection
            .whereEqualTo("userId", currentUser.uid)
            .get()
            .await()
            .documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    BodyPhoto(
                        id = doc.id,
                        userId = data["userId"] as? String ?: "",
                        fileName = data["fileName"] as? String ?: "",
                        storagePath = data["storagePath"] as? String ?: "",
                        downloadUrl = data["downloadUrl"] as? String ?: "",
                        timestamp = Instant.ofEpochMilli(
                            data["timestampEpochMillis"] as? Long ?: 0L
                        ),
                        fileSizeBytes = (data["fileSizeBytes"] as? Number)?.toLong() ?: 0L,
                        mimeType = data["mimeType"] as? String ?: "image/jpeg",
                        description = data["description"] as? String,
                        tags = (data["tags"] as? List<String>) ?: emptyList()
                    )
                } catch (e: Exception) {
                    Log.e("FirebaseBodyPhotoRepo", "Error parsing document: ${e.message}")
                    null
                }
            }
        
        val stats = calculatePhotoStats(photos)
        Log.d("FirebaseBodyPhotoRepo", "Calculated photo stats")
        Result.success(stats)
    } catch (e: Exception) {
        Log.e("FirebaseBodyPhotoRepo", "Error calculating stats: ${e.message}")
        _error.value = "計算統計失敗: ${e.message}"
        Result.failure(e)
    } finally {
        _isLoading.value = false
    }
    
    /**
     * 計算照片統計數據
     */
    private fun calculatePhotoStats(photos: List<BodyPhoto>): BodyPhotoStats {
        if (photos.isEmpty()) {
            return BodyPhotoStats()
        }
        
        val totalSizeBytes = photos.sumOf { it.fileSizeBytes }
        val totalSizeMB = totalSizeBytes / (1024.0 * 1024.0)
        
        val latestPhoto = photos.first()
        val oldestPhoto = photos.last()
        
        val allTags = photos.flatMap { it.tags }.distinct()
        val tagCounts = photos.flatMap { it.tags }.groupBy { it }.mapValues { it.value.size }
        
        return BodyPhotoStats(
            totalPhotos = photos.size,
            totalSizeBytes = totalSizeBytes,
            totalSizeMB = totalSizeMB,
            latestPhotoTimestamp = latestPhoto.timestamp,
            oldestPhotoTimestamp = oldestPhoto.timestamp,
            allTags = allTags,
            tagCounts = tagCounts,
            averageFileSizeBytes = totalSizeBytes / photos.size
        )
    }
}

/**
 * 體態照片數據模型
 */
data class BodyPhoto(
    val id: String,
    val userId: String,
    val fileName: String,
    val storagePath: String,
    val downloadUrl: String,
    val timestamp: Instant,
    val fileSizeBytes: Long,
    val mimeType: String,
    val description: String? = null,
    val tags: List<String> = emptyList()
)

/**
 * 體態照片統計數據
 */
data class BodyPhotoStats(
    val totalPhotos: Int = 0,
    val totalSizeBytes: Long = 0L,
    val totalSizeMB: Double = 0.0,
    val latestPhotoTimestamp: Instant = Instant.now(),
    val oldestPhotoTimestamp: Instant = Instant.now(),
    val allTags: List<String> = emptyList(),
    val tagCounts: Map<String, Int> = emptyMap(),
    val averageFileSizeBytes: Long = 0L
)
