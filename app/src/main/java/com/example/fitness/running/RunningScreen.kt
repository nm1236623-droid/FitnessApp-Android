package com.example.fitness.running

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Input
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitness.activity.ActivityLogRepository
import com.example.fitness.activity.ActivityRecord
import com.example.fitness.network.GeminiClient
import com.example.fitness.timer.StopwatchService
import com.example.fitness.ui.theme.TechColors
import com.example.fitness.ui.theme.glassEffect
import com.example.fitness.ui.theme.neonGlowBorder
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

@Composable
fun CardioScreen(
    repository: CardioRepository,
    activityRepository: ActivityLogRepository,
    userWeightKg: Float = 70f,
    onDone: () -> Unit = {}
) {
    // 狀態管理
    var minutes by remember { mutableIntStateOf(30) }
    var seconds by remember { mutableIntStateOf(0) }
    var speed by remember { mutableIntStateOf(5) }
    var incline by remember { mutableIntStateOf(1) }

    var cardioType by remember { mutableStateOf(CardioType.WALK_OR_JOG) }
    var typeExpanded by remember { mutableStateOf(false) }

    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val records by repository.records.collectAsState()

    // 背景漸層
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
                .padding(top = 16.dp, start = 16.dp, end = 16.dp) // 底部不留 padding，給 Timer Bar 用
        ) {
            // --- 1. Header (極簡化) ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                IconButton(
                    onClick = onDone,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TechColors.NeonBlue, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "Cardio Tracker",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.weight(1f))
                // 儲存按鈕移到 Header 右上角，節省空間
                Button(
                    onClick = {
                        val dur = minutes * 60 + seconds
                        if (dur <= 0) return@Button
                        val spd = speed.toFloat()
                        val inc = incline.toFloat()
                        val now = Instant.now()
                        isSaving = true
                        saveError = null
                        scope.launch {
                            try {
                                val kcalFloat = GeminiClient.estimateCardioCaloriesWithGemini(
                                    cardioType = cardioType, durationSec = dur, weightKg = userWeightKg, avgSpeedKph = spd
                                )
                                val kcal = kcalFloat.toDouble()
                                repository.add(CardioRecord(timestamp = now, durationSeconds = dur, averageSpeedKph = spd, inclinePercent = inc, calories = kcal, cardioType = cardioType))
                                val actType = "cardio_" + cardioType.name.lowercase(Locale.US)
                                activityRepository.add(ActivityRecord(id = UUID.randomUUID().toString(), planId = null, type = actType, start = now, end = now.plusSeconds(dur.toLong()), calories = kcal))
                                minutes = 30; seconds = 0
                            } catch (e: Exception) {
                                saveError = e.message
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    enabled = !isSaving,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp).neonGlowBorder(cornerRadius = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TechColors.NeonBlue.copy(alpha = 0.2f), contentColor = TechColors.NeonBlue)
                ) {
                    if (isSaving) CircularProgressIndicator(Modifier.size(16.dp), color = TechColors.NeonBlue, strokeWidth = 2.dp)
                    else Text("儲存", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // --- 2. Compact Inputs (緊湊輸入區) ---

            // 2.1 類型選擇 (扁平長條)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .glassEffect(cornerRadius = 10.dp)
                    .clickable { typeExpanded = true }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("類型:", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                    Spacer(Modifier.width(8.dp))
                    Text(cardioType.displayName, style = MaterialTheme.typography.bodyMedium, color = TechColors.NeonBlue, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.ArrowDropDown, null, tint = Color.White.copy(alpha = 0.6f))
                }
                DropdownMenu(
                    expanded = typeExpanded,
                    onDismissRequest = { typeExpanded = false },
                    modifier = Modifier.background(TechColors.DarkBlue.copy(alpha = 0.95f)).border(1.dp, TechColors.NeonBlue.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                ) {
                    CardioType.entries.forEach { t ->
                        DropdownMenuItem(
                            text = { Text(t.displayName, color = Color.White) },
                            onClick = { cardioType = t; typeExpanded = false }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // 2.2 參數選擇 (四格緊湊排列)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactParamItem(
                    label = "分",
                    value = minutes.toString(),
                    modifier = Modifier.weight(1f),
                    onClick = { /* trigger dialog handled inside */ },
                    onValueChange = { minutes = it },
                    range = 0..600
                )
                CompactParamItem(
                    label = "秒",
                    value = seconds.toString(),
                    modifier = Modifier.weight(1f),
                    onClick = { },
                    onValueChange = { seconds = it },
                    range = 0..59
                )
                CompactParamItem(
                    label = "速度",
                    value = speed.toString(),
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Speed,
                    onClick = { },
                    onValueChange = { speed = it },
                    range = 1..50
                )
                CompactParamItem(
                    label = "坡度",
                    value = incline.toString(),
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Landscape, // Terrain alternative
                    onClick = { },
                    onValueChange = { incline = it },
                    range = 0..20
                )
            }

            if (saveError != null) {
                // ★★★ 修正處：改用 bodySmall ★★★
                Text(text = saveError!!, color = Color(0xFFFF6B6B), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            }

            Spacer(Modifier.height(12.dp))

            // --- 3. History List (佔據最大面積) ---
            Text(
                "歷史紀錄",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // 使用 LazyColumn 並給予 weight(1f)，讓它填滿中間區域
            if (records.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("尚無紀錄", color = Color.White.copy(alpha = 0.3f))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp) // 預留底部空間給 Timer Bar
                ) {
                    items(records) { rec ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassEffect(cornerRadius = 12.dp)
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rec.cardioType.displayName, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold)
                                Text(DateTimeFormatter.ofPattern("MM/dd HH:mm").withZone(ZoneId.systemDefault()).format(rec.timestamp), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.5f))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${"%.0f".format(rec.calories)} kcal", style = MaterialTheme.typography.bodyMedium, color = TechColors.NeonBlue, fontWeight = FontWeight.Bold)
                                Text("${rec.durationSeconds / 60} min", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }
        }

        // --- 4. Mini Floating Timer (底部微型計時器) ---
        MiniTimerBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            onFillTime = { m, s ->
                minutes = m
                seconds = s
            }
        )
    }
}

// --- Components ---

@Composable
fun CompactParamItem(
    label: String,
    value: String,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .height(50.dp)
            .glassEffect(cornerRadius = 10.dp)
            .clickable { showDialog = true }
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, null, tint = TechColors.NeonBlue, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(2.dp))
                }
                Text(value, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showDialog) {
        // 重用之前的滾輪選擇器 Dialog 邏輯
        WheelPickerDialog(
            title = label,
            initialValue = value.toIntOrNull() ?: range.first,
            range = range,
            onDismiss = { showDialog = false },
            onConfirm = {
                onValueChange(it)
                showDialog = false
            }
        )
    }
}

@Composable
fun MiniTimerBar(
    modifier: Modifier = Modifier,
    onFillTime: (Int, Int) -> Unit
) {
    val elapsedMs by StopwatchService.elapsedMs.collectAsState(initial = 0L)
    val isRunning = StopwatchService.isRunning
    val totalSec = (elapsedMs / 1000L).coerceAtLeast(0L)
    val mm = totalSec / 60
    val ss = totalSec % 60

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp) // 很矮的高度
            .glassEffect(cornerRadius = 28.dp) // 膠囊狀
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(28.dp))
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Time Display
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bolt, null, tint = TechColors.NeonBlue, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${mm.toString().padStart(2, '0')}:${ss.toString().padStart(2, '0')}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TechColors.NeonBlue,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Controls (Icon Only)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Reset
                IconButton(onClick = { StopwatchService.stopAndReset() }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Refresh, null, tint = Color.White.copy(alpha = 0.6f))
                }
                // Play/Pause
                IconButton(
                    onClick = { if (isRunning) StopwatchService.pause() else StopwatchService.start() },
                    modifier = Modifier.size(32.dp).background(TechColors.NeonBlue.copy(alpha=0.2f), CircleShape)
                ) {
                    Icon(
                        if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        null,
                        tint = TechColors.NeonBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
                // Fill (Apply)
                IconButton(
                    onClick = {
                        if (isRunning) StopwatchService.pause()
                        onFillTime(mm.toInt(), ss.toInt())
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Input, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun WheelPickerDialog(
    title: String,
    initialValue: Int,
    range: IntRange,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TechColors.DarkBlue.copy(alpha = 0.95f),
        titleContentColor = TechColors.NeonBlue,
        title = {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        },
        text = {
            val listState = rememberLazyListState()
            val itemHeight = 40.dp
            LaunchedEffect(Unit) {
                val initialIndex = (initialValue - range.first).coerceIn(0, (range.last - range.first))
                listState.scrollToItem(initialIndex)
            }
            Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.fillMaxWidth().height(itemHeight).clip(RoundedCornerShape(8.dp)).background(TechColors.NeonBlue.copy(alpha = 0.2f)))
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 55.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    items(range.toList()) { num ->
                        Box(modifier = Modifier.fillMaxWidth().height(itemHeight).clickable { onConfirm(num) }, contentAlignment = Alignment.Center) {
                            Text(num.toString(), style = MaterialTheme.typography.titleLarge, color = if (num == initialValue) TechColors.NeonBlue else Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = Color.White.copy(alpha = 0.7f)) }
        }
    )
}