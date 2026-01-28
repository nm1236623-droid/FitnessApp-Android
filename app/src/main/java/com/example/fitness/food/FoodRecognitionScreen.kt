package com.example.fitness.food

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.fitness.activity.ActivityLogRepository
import com.example.fitness.activity.ActivityRecord
import com.example.fitness.data.DietRecord
import com.example.fitness.data.DietRecordRepository
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executors
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.ui.Alignment
import kotlin.math.roundToInt
import org.json.JSONObject
import com.example.fitness.ui.theme.glassEffect
import com.example.fitness.ui.theme.neonGlowBorder
import com.example.fitness.ui.theme.TechColors
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.concurrent.TimeUnit
import android.content.Context
import java.net.URLEncoder
import android.view.Surface

// CameraX imports
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import com.example.fitness.ui.SecurePrefs

// Data model
data class FoodAnalysis(
    val name: String,
    val calories: Double?,
    val nutrients: Map<String, Double>
)

@Composable
fun FoodRecognitionScreenOptimized(activityRepository: ActivityLogRepository, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val dietRepository = remember { DietRecordRepository(context) }

    // ★ 修改處：畫面啟動時，自動執行一次模型檢查
    LaunchedEffect(Unit) {
        initGlobalContext(context)
        FoodRecognitionService.debugListModels(context)
    }

    var previewBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var bitmapAvailable by remember { mutableStateOf(false) }

    var analyzing by remember { mutableStateOf(false) }
    var analysisResult by remember { mutableStateOf<FoodAnalysis?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var savedToLog by remember { mutableStateOf(false) }

    // portion controls
    var portionQuantity by remember { mutableStateOf(1f) }
    var portionUnit by remember { mutableStateOf("serving") }

    // time and meal type controls
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedTime by remember { mutableStateOf(LocalTime.now()) }
    var mealType by remember { mutableStateOf("午餐") }
    val mealTypes = listOf("早餐", "午餐", "晚餐", "點心")

    // permission
    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
    }

    // CameraX imageCapture holder
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    // gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            analysisResult = null
            errorMsg = null
            savedToLog = false
            try {
                context.contentResolver.openInputStream(it)?.use { ins ->
                    val bmp = BitmapFactory.decodeStream(ins)
                    previewBitmap = bmp
                    bitmapAvailable = bmp != null
                }
                // analyze in coroutine
                coroutineScope.launch {
                    analyzing = true
                    try {
                        context.contentResolver.openInputStream(it)?.use { ins ->
                            var res = FoodRecognitionService.analyzeImageWithGemini(ins, context, SecurePrefs.readGeminiKey(context))
                            if (res != null) {
                                // if calories missing, try Edamam
                                if (res.calories == null) {
                                    val kcal = FoodRecognitionService.fetchCaloriesFromEdamam(res.name, context)
                                    if (kcal != null) res = res.copy(calories = kcal)
                                }
                                analysisResult = res
                                showSaveDialog = true
                            } else errorMsg = "辨識失敗，請檢查網路連線或稍後重試"
                        } ?: run { errorMsg = "無法讀取圖片" }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        errorMsg = "辨識錯誤：${e.message}"
                    }
                    finally { analyzing = false }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                errorMsg = "讀取圖片失敗"
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
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 🎯 縮小的頂部食物辨識框
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .glassEffect(cornerRadius = 16.dp)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "🍽食物辨識",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "AI 營養分析",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    if (savedToLog) {
                        Box(
                            modifier = Modifier
                                .background(
                                    TechColors.NeonGreen.copy(alpha = 0.2f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "✓ 已儲存",
                                style = MaterialTheme.typography.labelSmall,
                                color = TechColors.NeonGreen
                            )
                        }
                    }
                }
            }

            if (!hasCameraPermission) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .glassEffect(cornerRadius = 16.dp)
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "需要相機權限",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFFF6B6B),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "為了辨識食物，需要存取您的相機",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Button(
                            onClick = { permissionLauncher.launch(android.Manifest.permission.CAMERA) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .neonGlowBorder(cornerRadius = 12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TechColors.NeonBlue.copy(alpha = 0.2f),
                                contentColor = TechColors.NeonBlue
                            )
                        ) {
                            Text("授予相機權限", color = Color.White)
                        }
                    }
                }
                return@Column
            }

            // 📸 相機預覽 - 佔據主要中間空間
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .glassEffect(cornerRadius = 20.dp)
                    .clip(RoundedCornerShape(20.dp))
            ) {
                val previewView = remember { PreviewView(context) }

                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier.fillMaxSize()
                    )

                    // 相機中央指引框
                    if (!analyzing && analysisResult == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .drawBehind {
                                        val strokeWidth = 2.dp.toPx()
                                        drawRoundRect(
                                            color = TechColors.NeonBlue,
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth),
                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx())
                                        )
                                    }
                            )

                            // 中央提示文字
                            Box(
                                modifier = Modifier
                                    .offset(y = 80.dp)
                                    .background(
                                        Color.Black.copy(alpha = 0.7f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    "將食物置於框內",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // 分析中指示器
                    if (analyzing) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f)),
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
                                    "AI 分析中...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                DisposableEffect(previewView, lifecycleOwner) {
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(previewView.context)
                    val listener = Runnable {
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val previewUseCase = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val ic = ImageCapture.Builder()
                                .setTargetRotation(previewView.display?.rotation ?: Surface.ROTATION_0)
                                .build()
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                previewUseCase,
                                ic
                            )
                            imageCapture = ic
                        } catch (exc: Exception) {
                            exc.printStackTrace()
                            errorMsg = "啟動相機失敗: ${exc.message}"
                        }
                    }
                    cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(previewView.context))
                    onDispose {
                        try {
                            if (cameraProviderFuture.isDone) {
                                val cp = cameraProviderFuture.get()
                                cp?.unbindAll()
                            }
                        } catch (_: Exception) {}
                    }
                }
            }

            // 📸 拍照按鈕
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .neonGlowBorder(cornerRadius = 12.dp, borderWidth = 1.5.dp),
                    enabled = !analyzing,
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
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = TechColors.NeonBlue
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("相簿", color = Color.White)
                }

                Button(
                    onClick = {
                        val ic = imageCapture
                        if (ic == null) {
                            errorMsg = "相機尚未就緒"
                            return@Button
                        }
                        savedToLog = false
                        analysisResult = null
                        errorMsg = null

                        val file = File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg")
                        val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
                        val executor = ContextCompat.getMainExecutor(context)

                        ic.takePicture(outputOptions, executor, object: ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                try {
                                    file.inputStream().use { ins ->
                                        val bmp = BitmapFactory.decodeStream(ins)
                                        previewBitmap = bmp
                                        bitmapAvailable = bmp != null
                                    }

                                    coroutineScope.launch {
                                        analyzing = true
                                        try {
                                            file.inputStream().use { ins ->
                                                var res = FoodRecognitionService.analyzeImageWithGemini(
                                                    ins,
                                                    context,
                                                    SecurePrefs.readGeminiKey(context)
                                                )
                                                if (res != null) {
                                                    if (res.calories == null) {
                                                        val kcal = FoodRecognitionService.fetchCaloriesFromEdamam(
                                                            res.name,
                                                            context
                                                        )
                                                        if (kcal != null) res = res.copy(calories = kcal)
                                                    }
                                                    analysisResult = res
                                                    showSaveDialog = true
                                                } else {
                                                    errorMsg = "辨識失敗，請重試"
                                                }
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            errorMsg = "辨識錯誤：${e.message}"
                                        } finally {
                                            analyzing = false
                                        }
                                    }
                                } catch (ex: Exception) {
                                    ex.printStackTrace()
                                    errorMsg = "讀取拍攝圖片失敗"
                                }
                            }

                            override fun onError(exception: ImageCaptureException) {
                                exception.printStackTrace()
                                errorMsg = "拍照失敗: ${exception.message}"
                            }
                        })
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .neonGlowBorder(cornerRadius = 12.dp),
                    enabled = !analyzing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TechColors.NeonBlue.copy(alpha = 0.2f),
                        contentColor = TechColors.NeonBlue
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Camera,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = TechColors.NeonBlue
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(" 拍照", color = Color.White)
                }
            }

            // 🖼️ 拍照圖片顯示區域
            if (bitmapAvailable && previewBitmap != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .glassEffect(cornerRadius = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Image(
                        bitmap = previewBitmap!!.asImageBitmap(),
                        contentDescription = "preview",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // 📊 分析結果顯示 - 高對比背景
            analysisResult?.let { r ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(16.dp),
                            ambientColor = TechColors.NeonBlue.copy(alpha = 0.3f)
                        )
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black,
                                    Color(0xFF1A1A2E)
                                )
                            ),
                            RoundedCornerShape(16.dp)
                        )
                        .drawBehind {
                            val strokeWidth = 2.dp.toPx()
                            drawRoundRect(
                                color = TechColors.NeonBlue,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx())
                            )
                        }
                        .padding(16.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 食物名稱 - 高對比
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "🍽️ 辨識結果",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TechColors.NeonBlue.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = r.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // 營養資訊卡片 - 高對比設計
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 熱量卡片
                            NutritionCardHighContrast(
                                icon = "🔥",
                                label = "熱量",
                                value = r.calories?.let { String.format("%.0f", it * portionQuantity) } ?: "?",
                                unit = "kcal",
                                modifier = Modifier.weight(1f),
                                bgColor = Color(0xFFFF6B35).copy(alpha = 0.2f),
                                borderColor = Color(0xFFFF6B35)
                            )

                            // 蛋白質卡片
                            val protein = r.nutrients["protein"]
                            NutritionCardHighContrast(
                                icon = "💪",
                                label = "蛋白質",
                                value = protein?.let { String.format("%.1f", it * portionQuantity) } ?: "?",
                                unit = "g",
                                modifier = Modifier.weight(1f),
                                bgColor = Color(0xFF4ECDC4).copy(alpha = 0.2f),
                                borderColor = Color(0xFF4ECDC4)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 脂肪卡片
                            val fat = r.nutrients["fat"]
                            NutritionCardHighContrast(
                                icon = "🥑",
                                label = "脂肪",
                                value = fat?.let { String.format("%.1f", it * portionQuantity) } ?: "?",
                                unit = "g",
                                modifier = Modifier.weight(1f),
                                bgColor = Color(0xFF95E77E).copy(alpha = 0.2f),
                                borderColor = Color(0xFF95E77E)
                            )

                            // 碳水卡片
                            val carbs = r.nutrients["carbs"]
                            NutritionCardHighContrast(
                                icon = "🌾",
                                label = "碳水",
                                value = carbs?.let { String.format("%.1f", it * portionQuantity) } ?: "?",
                                unit = "g",
                                modifier = Modifier.weight(1f),
                                bgColor = Color(0xFFA78BFA).copy(alpha = 0.2f),
                                borderColor = Color(0xFFA78BFA)
                            )
                        }

                        // 份量調整 - 高對比
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Color.White.copy(alpha = 0.1f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "📏 份量調整",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        "${(portionQuantity * 100).roundToInt()}%",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TechColors.NeonBlue,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Slider(
                                    value = portionQuantity,
                                    onValueChange = { portionQuantity = it },
                                    valueRange = 0.1f..3f,
                                    steps = 29,
                                    colors = SliderDefaults.colors(
                                        thumbColor = TechColors.NeonBlue,
                                        activeTrackColor = TechColors.NeonBlue,
                                        inactiveTrackColor = TechColors.NeonBlue.copy(alpha = 0.3f)
                                    )
                                )
                            }
                        }

                        // 操作按鈕
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!savedToLog) {
                                Button(
                                    onClick = { showSaveDialog = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(45.dp)
                                        .neonGlowBorder(cornerRadius = 12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = TechColors.NeonBlue.copy(alpha = 0.3f),
                                        contentColor = TechColors.NeonBlue
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Save,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("儲存", color = Color.White)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        analysisResult = null
                                        savedToLog = false
                                        previewBitmap = null
                                        bitmapAvailable = false
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(45.dp)
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
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("再拍", color = Color.White)
                                }
                            }

                            OutlinedButton(
                                onClick = { /* TODO: Share */ },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(45.dp)
                                    .neonGlowBorder(cornerRadius = 12.dp, borderWidth = 1.5.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.5.dp,
                                    Brush.linearGradient(
                                        listOf(
                                            TechColors.NeonPurple.copy(alpha = 0.8f),
                                            TechColors.NeonPurple.copy(alpha = 0.4f)
                                        )
                                    )
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = TechColors.NeonPurple
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("分享", color = Color.White)
                            }
                        }
                    }
                }
            }

            // 錯誤訊息
            errorMsg?.let {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color(0xFFFF6B6B).copy(alpha = 0.2f),
                            RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = Color(0xFFFF6B6B).copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
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
        }
    }

    // 儲存對話框
    if (showSaveDialog && analysisResult != null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            AlertDialog(
                onDismissRequest = { showSaveDialog = false },
                title = {
                    Text(
                        "儲存到日誌",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "食物：${analysisResult!!.name}",
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Medium
                    )

                    analysisResult!!.calories?.let { calories ->
                        Text(
                            "熱量：${String.format("%.0f", calories * portionQuantity)} kcal",
                            color = Color(0xFFFF6B35),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // 顯示主要營養成分
                    val protein = analysisResult!!.nutrients["protein"]
                    val fat = analysisResult!!.nutrients["fat"]
                    val carbs = analysisResult!!.nutrients["carbs"]

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        protein?.let {
                            Text(
                                "蛋白質：${String.format("%.1f", it * portionQuantity)}g",
                                color = Color(0xFF4ECDC4),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        fat?.let {
                            Text(
                                "脂肪：${String.format("%.1f", it * portionQuantity)}g",
                                color = Color(0xFF95E77E),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        carbs?.let {
                            Text(
                                "碳水：${String.format("%.1f", it * portionQuantity)}g",
                                color = Color(0xFFA78BFA),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    if (portionQuantity != 1f) {
                        Text(
                            "份量：${String.format("%.1f", portionQuantity)} $portionUnit",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 時間和餐食類型選擇
                    Text(
                        "記錄時間設定",
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Medium
                    )

                    // 餐食類型選擇
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        mealTypes.forEach { type ->
                            FilterChip(
                                onClick = { mealType = type },
                                label = { Text(type, fontSize = 12.sp) },
                                selected = mealType == type,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TechColors.NeonBlue.copy(alpha = 0.3f),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 日期和時間顯示
                    Text(
                        "日期：${selectedDate}  時間：${selectedTime}",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        "確定要將以上營養資訊儲存到今日日誌嗎？",
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                // 計算正確的時間戳
                                val recordDateTime = selectedDate.atTime(selectedTime).atZone(ZoneId.systemDefault()).toInstant()

                                val dietRecord = DietRecord(
                                    id = UUID.randomUUID().toString(),
                                    foodName = analysisResult!!.name,
                                    calories = (analysisResult!!.calories?.times(portionQuantity))?.toInt() ?: 0,
                                    date = selectedDate,
                                    mealType = mealType,
                                    proteinG = analysisResult!!.nutrients["protein"]?.times(portionQuantity),
                                    carbsG = analysisResult!!.nutrients["carbs"]?.times(portionQuantity),
                                    fatG = analysisResult!!.nutrients["fat"]?.times(portionQuantity),
                                    timestamp = recordDateTime,
                                    userId = null
                                )
                                dietRepository.addRecord(dietRecord)
                                savedToLog = true
                                showSaveDialog = false
                            } catch (e: Exception) {
                                errorMsg = "儲存失敗: ${e.message}"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TechColors.NeonBlue,
                        contentColor = Color.White
                    )
                ) {
                    Text("確定儲存")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showSaveDialog = false },
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
}

@Composable
private fun NutritionCardHighContrast(
    icon: String,
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    bgColor: Color,
    borderColor: Color
) {
    Box(
        modifier = modifier
            .background(
                bgColor,
                RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.5.dp,
                color = borderColor.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f)
            )
            Text(
                text = "$value $unit",
                style = MaterialTheme.typography.titleSmall,
                color = borderColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// 從原檔案複製必要的函數和類別
object FoodRecognitionService {
    private const val GEMINI_API_KEY = ""
    private const val MODEL = "gemini-2.5-flash"

    suspend fun analyzeImageWithGemini(input: InputStream, context: Context, apiKeyOverride: String? = null): FoodAnalysis? = withContext(Dispatchers.IO) {
        try {
            val originalBitmap = BitmapFactory.decodeStream(input)
            val outputStream = ByteArrayOutputStream()
            originalBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, outputStream)
            val imgBytes = outputStream.toByteArray()
            val base64 = Base64.encodeToString(imgBytes, Base64.NO_WRAP)

            val apiKey = when {
                !apiKeyOverride.isNullOrBlank() -> apiKeyOverride
                else -> SecurePrefs.readGeminiKey(context).takeIf { it.isNotBlank() }
                    ?: readBuildConfigGeminiKey().takeIf { it.isNotBlank() }
                    ?: GEMINI_API_KEY
            }

            val payloadJson = JSONObject()
            val contentsArray = org.json.JSONArray()
            val contentObject = JSONObject()
            val partsArray = org.json.JSONArray()

            val textPart = JSONObject()
            textPart.put("text", "分析這張照片中的食物，請嚴格只回傳純JSON格式，不要有Markdown標記。必須包含以下字段：\n1. name: 食物名稱（中文）\n2. calories: 熱量（數字，單位kcal）\n3. nutrients: 營養成分對象，包含：\n   - protein: 蛋白質（克）\n   - fat: 脂肪（克）\n   - carbs: 碳水化合物（克）\n\n範例格式：{\"name\": \"雞肉飯\", \"calories\": 650, \"nutrients\": {\"protein\": 25.5, \"fat\": 12.3, \"carbs\": 78.9}}")
            partsArray.put(textPart)

            val imagePart = JSONObject()
            val inlineData = JSONObject()
            inlineData.put("mime_type", "image/jpeg")
            inlineData.put("data", base64)
            imagePart.put("inline_data", inlineData)
            partsArray.put(imagePart)

            contentObject.put("parts", partsArray)
            contentsArray.put(contentObject)
            payloadJson.put("contents", contentsArray)

            val client = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
                .build()

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(payloadJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val respText = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                android.util.Log.e("GeminiAPI", "Error Code: ${response.code}, Body: $respText")
                when (response.code) {
                    408 -> {
                        android.util.Log.w("GeminiAPI", "Request timeout - increasing timeout for next attempt")
                        response.close()
                        return@withContext null
                    }
                    429 -> {
                        android.util.Log.w("GeminiAPI", "Rate limit exceeded - please wait before trying again")
                        response.close()
                        return@withContext null
                    }
                    else -> {
                        response.close()
                        return@withContext null
                    }
                }
            }
            response.close()

            val root = JSONObject(respText)
            val candidates = root.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) return@withContext null

            val content = candidates.optJSONObject(0)?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val textRaw = parts?.optJSONObject(0)?.optString("text") ?: ""

            android.util.Log.d("GeminiAPI", "Raw response: $textRaw")

            val cleanJson = textRaw.replace("```json", "").replace("```", "").trim()

            if (cleanJson.isEmpty()) {
                android.util.Log.e("GeminiAPI", "Empty response after cleaning")
                return@withContext null
            }

            android.util.Log.d("GeminiAPI", "Cleaned JSON: $cleanJson")

            val json = try {
                JSONObject(cleanJson)
            } catch (e: Exception) {
                android.util.Log.e("GeminiAPI", "JSON parse error: ${e.message}")
                val start = cleanJson.indexOf('{')
                val end = cleanJson.lastIndexOf('}')
                if (start >= 0 && end > start) {
                    val substring = cleanJson.substring(start, end + 1)
                    android.util.Log.d("GeminiAPI", "Trying substring: $substring")
                    JSONObject(substring)
                } else {
                    android.util.Log.e("GeminiAPI", "No valid JSON found in response")
                    null
                }
            } ?: return@withContext null

            val name = json.optString("name", "未知食物")
            val calories = json.optDouble("calories", Double.NaN).takeIf { !it.isNaN() }

            android.util.Log.d("GeminiAPI", "Parsed name: $name, calories: $calories")

            val nutrients = json.optJSONObject("nutrients")?.let { nut ->
                android.util.Log.d("GeminiAPI", "Nutrients object: $nut")
                mutableMapOf<String, Double>().also { map ->
                    nut.keys().forEach { key ->
                        val v = nut.optDouble(key)
                        if (!v.isNaN()) {
                            // 標準化鍵名為英文
                            val standardKey = when (key.lowercase()) {
                                "protein", "蛋白質", "蛋白质" -> "protein"
                                "fat", "脂肪" -> "fat"
                                "carbs", "carbohydrates", "碳水化合物", "碳水" -> "carbs"
                                else -> key.lowercase()
                            }
                            map[standardKey] = v
                            android.util.Log.d("GeminiAPI", "Added nutrient $key -> $standardKey: $v")
                        }
                    }
                }
            } ?: run {
                // 如果沒有nutrients對象，嘗試從頂級字段解析
                android.util.Log.d("GeminiAPI", "No nutrients object, trying top-level fields")
                mutableMapOf<String, Double>().also { map ->
                    listOf("protein", "fat", "carbs", "蛋白質", "脂肪", "碳水化合物", "碳水").forEach { key ->
                        val v = json.optDouble(key, Double.NaN)
                        if (!v.isNaN()) {
                            val standardKey = when (key.lowercase()) {
                                "protein", "蛋白質", "蛋白质" -> "protein"
                                "fat", "脂肪" -> "fat"
                                "carbs", "carbohydrates", "碳水化合物", "碳水" -> "carbs"
                                else -> key.lowercase()
                            }
                            map[standardKey] = v
                            android.util.Log.d("GeminiAPI", "Added top-level nutrient $key -> $standardKey: $v")
                        }
                    }
                }
            }

            android.util.Log.d("GeminiAPI", "Final nutrients map: $nutrients")

            // 如果營養成分為空，提供默認估算
            val finalNutrients = if (nutrients.isEmpty()) {
                android.util.Log.w("GeminiAPI", "No nutrients parsed, providing default estimates")
                calories?.let { cal ->
                    mapOf(
                        "protein" to cal * 0.15 / 4,  // 15% of calories from protein
                        "fat" to cal * 0.25 / 9,     // 25% of calories from fat
                        "carbs" to cal * 0.60 / 4    // 60% of calories from carbs
                    )
                } ?: emptyMap()
            } else {
                nutrients
            }

            return@withContext FoodAnalysis(name, calories, finalNutrients)

        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("GeminiAPI", "Exception: ${e.message}")
            return@withContext null
        } finally {
            try { input.close() } catch (_: Exception) {}
        }
    }

    suspend fun debugListModels(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                val apiKey = SecurePrefs.readGeminiKey(context).takeIf { it.isNotBlank() }
                    ?: readBuildConfigGeminiKey().takeIf { it.isNotBlank() }
                    ?: GEMINI_API_KEY

                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey")
                    .get()
                    .build()

                val client = OkHttpClient()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    android.util.Log.d("GeminiList", "✅ Available Models: $body")
                } else {
                    android.util.Log.e("GeminiList", "❌ Failed to list models: ${response.code} - $body")
                }
                response.close()
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("GeminiList", "Exception listing models: ${e.message}")
            }
        }
    }

    suspend fun fetchCaloriesFromEdamam(foodName: String, context: Context): Double? = withContext(Dispatchers.IO) {
        try {
            val appId = SecurePrefs.readEdamamId(context)
            val appKey = SecurePrefs.readEdamamKey(context)
            if (appId.isBlank() || appKey.isBlank()) return@withContext null
            val ingr = URLEncoder.encode(foodName, "UTF-8")
            val url = "https://api.edamam.com/api/food-database/v2/parser?app_id=$appId&app_key=$appKey&ingr=$ingr"
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
            val req = Request.Builder().url(url).get().build()
            val resp = client.newCall(req).execute()
            val body = resp.body?.string() ?: run { resp.close(); return@withContext null }
            resp.close()
            val root = JSONObject(body)
            val hints = root.optJSONArray("hints") ?: return@withContext null
            if (hints.length() == 0) return@withContext null
            val first = hints.optJSONObject(0) ?: return@withContext null
            val food = first.optJSONObject("food") ?: return@withContext null
            val nutrients = food.optJSONObject("nutrients") ?: return@withContext null
            val kcal = nutrients.optDouble("ENERC_KCAL", Double.NaN)
            return@withContext if (!kcal.isNaN()) kcal else null
        } catch (_: Exception) {
            return@withContext null
        }
    }
}

// 輔助函數
private var globalContext: Context? = null

fun initGlobalContext(context: Context) {
    globalContext = context.applicationContext
}

private fun androidContextForService(): Context {
    return globalContext ?: throw IllegalStateException("Global context not initialized. Call initGlobalContext() first.")
}

private fun readBuildConfigGeminiKey(): String {
    // 從 BuildConfig 讀取 API key
    return com.example.fitness.BuildConfig.GEMINI_API_KEY
}
