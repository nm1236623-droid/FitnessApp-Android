package com.example.fitness.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// import coil.compose.rememberAsyncImagePainter // 暫時註釋，可能沒有添加依賴
import com.example.fitness.ui.theme.TechColors
import com.example.fitness.ui.theme.glassEffect
import com.example.fitness.ui.theme.neonGlowBorder
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Firebase 版本的體態相簿畫面
 * 
 * 支援雲端照片存儲和管理
 */
@Composable
fun BodyPhotoScreenFirebase(
    onDone: () -> Unit,
    useFirebase: Boolean = true
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Firebase 照片數據
    val photos by FirebaseBodyPhotoRepository.photos.collectAsState(initial = emptyList())
    val isLoading by FirebaseBodyPhotoRepository.isLoading.collectAsState()
    val error by FirebaseBodyPhotoRepository.error.collectAsState()
    
    // UI 狀態
    var selectedPhoto by remember { mutableStateOf<BodyPhoto?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDescriptionDialog by remember { mutableStateOf(false) }
    var photoDescription by remember { mutableStateOf("") }
    
    // 相機和相簿啟動器
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            // 處理拍照結果
            scope.launch {
                // 這裡需要實現拍照後的處理邏輯
            }
        }
    }
    
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                FirebaseBodyPhotoRepository.uploadPhoto(
                    context = context,
                    imageUri = it,
                    description = null,
                    tags = emptyList()
                ).onSuccess { photo ->
                    // 上傳成功
                }.onFailure { error ->
                    // 處理錯誤
                }
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(TechColors.DarkBlue, TechColors.DeepPurple)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // 頂部導航欄
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDone,
                    modifier = Modifier
                        .glassEffect(cornerRadius = 12.dp)
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White
                    )
                }
                
                Text(
                    text = "體態相簿",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                
                if (photos.isNotEmpty()) {
                    Text(
                        text = "${photos.size} 張",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
            
            // 操作按鈕
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { 
                        // 啟動相機 - 暫時跳過實現
                        scope.launch {
                            // TODO: 實現相機拍照功能
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .neonGlowBorder(cornerRadius = 12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TechColors.NeonBlue.copy(alpha = 0.2f),
                        contentColor = TechColors.NeonBlue
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "拍照",
                        modifier = Modifier.size(20.dp),
                        tint = TechColors.NeonBlue
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("📸 拍照", color = Color.White)
                }
                
                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .neonGlowBorder(cornerRadius = 12.dp, borderWidth = 1.5.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        Brush.linearGradient(
                            listOf(
                                TechColors.NeonBlue.copy(alpha = 0.8f),
                                TechColors.NeonBlue.copy(alpha = 0.4f)
                            )
                        )
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TechColors.NeonBlue
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "相簿",
                        modifier = Modifier.size(20.dp),
                        tint = TechColors.NeonBlue
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("相簿", color = Color.White)
                }
            }
            
            // 錯誤訊息
            error?.let {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color(0xFFFF6B6B).copy(alpha = 0.2f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = "⚠️ $it",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
            }
            
            // 照片列表
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            color = TechColors.NeonBlue,
                            strokeWidth = 3.dp
                        )
                        Text(
                            "載入中...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }
                }
            } else if (photos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "📸",
                            style = MaterialTheme.typography.displayMedium,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        Text(
                            "還沒有體態照片",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            "點擊上方按鈕開始拍照或從相簿選擇",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(photos) { photo ->
                        BodyPhotoItem(
                            photo = photo,
                            onClick = { selectedPhoto = photo },
                            onDelete = { 
                                selectedPhoto = photo
                                showDeleteDialog = true 
                            },
                            onEditDescription = {
                                selectedPhoto = photo
                                photoDescription = photo.description ?: ""
                                showDescriptionDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
    
    // 刪除確認對話框
    if (showDeleteDialog && selectedPhoto != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteDialog = false
                selectedPhoto = null
            },
            title = {
                Text(
                    "刪除照片",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "確定要刪除這張體態照片嗎？此操作無法復原。",
                    color = Color.White.copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            FirebaseBodyPhotoRepository.deletePhoto(selectedPhoto!!.id)
                                .onSuccess {
                                    showDeleteDialog = false
                                    selectedPhoto = null
                                }
                                .onFailure { error ->
                                    // 處理錯誤
                                }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6B6B),
                        contentColor = Color.White
                    )
                ) {
                    Text("刪除")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { 
                        showDeleteDialog = false
                        selectedPhoto = null
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        TechColors.NeonBlue.copy(alpha = 0.5f)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TechColors.NeonBlue
                    )
                ) {
                    Text("取消")
                }
            },
            containerColor = Color(0xFF1A1A2E),
            titleContentColor = Color.White
        )
    }
    
    // 編輯描述對話框
    if (showDescriptionDialog && selectedPhoto != null) {
        AlertDialog(
            onDismissRequest = { 
                showDescriptionDialog = false
                selectedPhoto = null
                photoDescription = ""
            },
            title = {
                Text(
                    "編輯照片描述",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "為這張體態照片添加描述：",
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    OutlinedTextField(
                        value = photoDescription,
                        onValueChange = { photoDescription = it },
                        placeholder = {
                            Text(
                                "輸入描述...",
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TechColors.NeonBlue,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            FirebaseBodyPhotoRepository.updatePhotoMetadata(
                                photoId = selectedPhoto!!.id,
                                description = photoDescription.ifBlank { null }
                            ).onSuccess {
                                showDescriptionDialog = false
                                selectedPhoto = null
                                photoDescription = ""
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TechColors.NeonBlue,
                        contentColor = Color.White
                    )
                ) {
                    Text("儲存")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { 
                        showDescriptionDialog = false
                        selectedPhoto = null
                        photoDescription = ""
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        TechColors.NeonBlue.copy(alpha = 0.5f)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TechColors.NeonBlue
                    )
                ) {
                    Text("取消")
                }
            },
            containerColor = Color(0xFF1A1A2E),
            titleContentColor = Color.White
        )
    }
}

/**
 * 體態照片項目組件
 */
@Composable
private fun BodyPhotoItem(
    photo: BodyPhoto,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onEditDescription: () -> Unit
) {
    val formatter = remember { 
        DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
            .withZone(ZoneId.systemDefault())
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassEffect(cornerRadius = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // 照片預覽 - 暫時使用簡單的佔位符
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        TechColors.NeonBlue.copy(alpha = 0.1f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "📸",
                    style = MaterialTheme.typography.displayMedium,
                    color = TechColors.NeonBlue.copy(alpha = 0.6f)
                )
                Text(
                    text = "照片載入中...",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.BottomCenter)
                        .padding(8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 照片資訊
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formatter.format(photo.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    
                    photo.description?.let { desc ->
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                    
                    Text(
                        text = "${(photo.fileSizeBytes / 1024.0).toInt()} KB",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
                
                // 操作按鈕
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onEditDescription,
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                TechColors.NeonBlue.copy(alpha = 0.2f),
                                RoundedCornerShape(8.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "編輯描述",
                            tint = TechColors.NeonBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                Color(0xFFFF6B6B).copy(alpha = 0.2f),
                                RoundedCornerShape(8.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "刪除",
                            tint = Color(0xFFFF6B6B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
