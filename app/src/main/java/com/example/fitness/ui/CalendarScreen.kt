package com.example.fitness.ui

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitness.ui.theme.TechColors
import com.example.fitness.ui.theme.glassEffect
import com.example.fitness.ui.theme.neonGlowBorder
import com.example.fitness.coach.CoachAuthRepository
import com.example.fitness.coach.cloud.CloudSyncUseCase
import com.example.fitness.coach.cloud.CoachTrainee
import com.example.fitness.coach.cloud.CoachTraineeDirectoryRepository
import com.example.fitness.coach.local.CoachLocalPlanRepository
import com.example.fitness.coach.ui.SingleTraineePicker
import com.example.fitness.data.ExerciseEntry
import com.example.fitness.data.PartExerciseCatalog
import com.example.fitness.data.TrainingPlan
import com.example.fitness.data.TrainingPlanRepository
import com.google.firebase.auth.FirebaseAuth
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.UUID
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AirlineSeatReclineExtra
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material.icons.filled.Upload
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// 輔助資料類別
private data class CoachPartUi(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val contentDescription: String = label,
)

@Composable
fun CalendarScreen(
    repository: TrainingPlanRepository,
    modifier: Modifier = Modifier,
    onOpenPlan: ((String) -> Unit)? = null,
) {
    val ctx = LocalContext.current
    val appCtx = remember(ctx) { ctx.applicationContext }

    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val plansState by repository.plans.collectAsState(initial = emptyList())

    // observe scheduled plans for selected date
    val scheduledForDate by repository.getPlansForDateFlow(selectedDate).collectAsState(initial = emptyList())

    var addDialogOpen by remember { mutableStateOf(false) }
    var createNewPlanDialogOpen by remember { mutableStateOf(false) }

    // Coach functionality
    val scope = rememberCoroutineScope()
    val coachLocalRepo = remember(appCtx) { CoachLocalPlanRepository(appCtx) }
    val authRepository = remember(appCtx) { CoachAuthRepository(appCtx) }
    val cloudSync = remember(authRepository) {
        CloudSyncUseCase(authRepository, TrainingPlanRepository()) // Note: creates new repo instance here, might want to share
    }
    val traineeDirectoryRepo = remember { CoachTraineeDirectoryRepository() }
    val trainees = remember { mutableStateListOf<CoachTrainee>() }
    var coachId by remember { mutableStateOf("") }

    // Load coach ID and trainees
    LaunchedEffect(Unit) {
        coachId = FirebaseAuth.getInstance().currentUser?.uid ?: authRepository.ensureUserId()
        if (coachId.isNotBlank()) {
            traineeDirectoryRepo.listTrainees(coachId).onSuccess { list ->
                trainees.clear()
                trainees.addAll(list)
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
        Column(modifier = modifier.padding(16.dp)) {
            // Header: month navigation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassEffect(cornerRadius = 24.dp)
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { currentMonth = currentMonth.minusMonths(1); selectedDate = currentMonth.atDay(1) }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "上月",
                            tint = TechColors.NeonBlue
                        )
                    }
                    Text(
                        text = "${currentMonth.year} 年 ${currentMonth.month.value} 月", // 中文化
                        style = MaterialTheme.typography.titleLarge,
                        color = TechColors.NeonBlue,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { currentMonth = currentMonth.plusMonths(1); selectedDate = currentMonth.atDay(1) }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "下月",
                            tint = TechColors.NeonBlue
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Day of week headers
            Row(modifier = Modifier.fillMaxWidth()) {
                val dayNames = listOf("日", "一", "二", "三", "四", "五", "六")
                dayNames.forEach { dn ->
                    Text(
                        text = dn,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar grid
            val firstOfMonth = currentMonth.atDay(1)
            val daysInMonth = currentMonth.lengthOfMonth()
            val firstDow = firstOfMonth.dayOfWeek.value % 7 // Java time: Mon=1...Sun=7. We want Sun=0
            val leadingEmpty = firstDow
            val totalCells = leadingEmpty + daysInMonth
            val rows = (totalCells + 6) / 7

            Column(modifier = Modifier.fillMaxWidth()) {
                var day = 1
                for (r in 0 until rows) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (c in 0..6) {
                            val cellIndex = r * 7 + c
                            if (cellIndex < leadingEmpty || day > daysInMonth) {
                                Box(modifier = Modifier.weight(1f).height(48.dp))
                            } else {
                                val thisDate = currentMonth.atDay(day)
                                val isSelected = thisDate == selectedDate
                                val hasPlan = repository.getPlansForDate(thisDate).isNotEmpty()
                                val isToday = thisDate == LocalDate.now()

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) TechColors.NeonBlue.copy(alpha = 0.3f)
                                            else if (isToday) Color.White.copy(alpha = 0.1f)
                                            else Color.Transparent
                                        )
                                        .border(
                                            width = if (isSelected) 1.5.dp else 0.dp,
                                            color = if (isSelected) TechColors.NeonBlue else Color.Transparent,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { selectedDate = thisDate },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = day.toString(),
                                            textAlign = TextAlign.Center,
                                            color = if (isSelected) TechColors.NeonBlue else if (isToday) Color.White else Color.White.copy(alpha = 0.8f),
                                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                                        )
                                        if (hasPlan) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .background(TechColors.NeonGreen, CircleShape)
                                            )
                                        }
                                    }
                                }
                                day++
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Selected date and scheduled plans
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassEffect(cornerRadius = 24.dp)
                    .padding(16.dp)
            ) {
                Column {
                    // Date Header
                    Text(
                        text = "${selectedDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 建立新計畫按鈕 (Primary)
                        Button(
                            onClick = { createNewPlanDialogOpen = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .neonGlowBorder(cornerRadius = 12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TechColors.NeonBlue.copy(alpha = 0.2f),
                                contentColor = TechColors.NeonBlue
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("建立計畫", fontWeight = FontWeight.Bold)
                        }

                        // 加入現有計畫按鈕 (Secondary)
                        OutlinedButton(
                            onClick = { addDialogOpen = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TechColors.NeonBlue.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = TechColors.NeonBlue
                            )
                        ) {
                            Text("加入現有", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Plan List
            if (scheduledForDate.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "此日無訓練計畫",
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(scheduledForDate) { plan ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassEffect(cornerRadius = 16.dp)
                                .clickable(enabled = onOpenPlan != null) {
                                    onOpenPlan?.invoke(plan.id)
                                }
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = plan.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${plan.exercises.size} 個動作",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                }
                                IconButton(
                                    onClick = { repository.removeScheduledPlan(selectedDate, plan.id) }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "移除排定",
                                        tint = Color(0xFFFF6B6B).copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Dialogs ---

        // Add existing plan dialog
        if (addDialogOpen) {
            AlertDialog(
                onDismissRequest = { addDialogOpen = false },
                containerColor = TechColors.DarkBlue.copy(alpha = 0.95f),
                titleContentColor = TechColors.NeonBlue,
                textContentColor = Color.White,
                title = { Text("選擇要加入的訓練計畫", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (plansState.isEmpty()) {
                            Text("目前沒有任何訓練計畫，請先建立一個。", color = Color.White.copy(alpha = 0.7f))
                        } else {
                            plansState.forEach { p ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = p.name,
                                        modifier = Modifier.weight(1f),
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Button(
                                        onClick = {
                                            repository.addScheduledPlan(selectedDate, p.id)
                                            addDialogOpen = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = TechColors.NeonBlue),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Text("加入")
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { addDialogOpen = false }) {
                        Text("取消", color = Color.White.copy(alpha = 0.7f))
                    }
                }
            )
        }

        // Create new plan dialog (Coach style)
        if (createNewPlanDialogOpen) {
            CalendarPlanCreateDialog(
                selectedDate = selectedDate,
                repository = repository,
                coachLocalRepo = coachLocalRepo,
                cloudSync = cloudSync,
                trainees = trainees,
                coachId = coachId,
                onDismiss = { createNewPlanDialogOpen = false }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalendarPlanCreateDialog(
    selectedDate: LocalDate,
    repository: TrainingPlanRepository,
    coachLocalRepo: CoachLocalPlanRepository,
    cloudSync: CloudSyncUseCase,
    trainees: List<CoachTrainee>,
    coachId: String,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var planName by remember { mutableStateOf("") }
    var activeDialog by remember { mutableStateOf<String?>(null) }

    val selectedItems = remember { mutableStateListOf<String>() }
    val selectedSets = remember { mutableStateMapOf<String, Int>() }
    val selectedWeights = remember { mutableStateMapOf<String, Float>() }

    val planExercises = remember { mutableStateListOf<ExerciseEntry>() }

    var publishTarget by remember { mutableStateOf<CoachTrainee?>(trainees.firstOrNull()) }
    var publishExpanded by remember { mutableStateOf(false) }
    var confirmPublish by remember { mutableStateOf(false) }

    val defaultSets = 5
    val defaultWeight = 40f

    fun weightToSets(weight: Float, minSets: Int = 3, maxSets: Int = 12): Int {
        val w = weight.coerceIn(0f, 100f)
        val normalized = (w / 100f)
        val sets = (maxSets - (normalized * (maxSets - minSets))).roundToInt()
        return sets.coerceIn(minSets, maxSets)
    }

    fun resolvedPlanName(): String {
        val trimmed = planName.trim()
        if (trimmed.isNotBlank()) return trimmed

        val base = when (activeDialog) {
            "chest" -> "胸"
            "back" -> "背"
            "legs" -> "腿"
            "arms" -> "手臂"
            "shoulders" -> "肩"
            "abs" -> "腹"
            else -> "訓練"
        }
        return "$base 計畫 ${selectedDate.format(DateTimeFormatter.ofPattern("M/d"))}"
    }

    val parts = remember {
        listOf(
            CoachPartUi("chest", "胸", Icons.Default.Favorite),
            CoachPartUi("back", "背", Icons.Default.AirlineSeatReclineExtra),
            CoachPartUi("legs", "腿", Icons.AutoMirrored.Filled.DirectionsRun),
            CoachPartUi("arms", "手臂", Icons.Default.FitnessCenter),
            CoachPartUi("shoulders", "肩", Icons.Default.AccessibilityNew),
            CoachPartUi("abs", "腹", Icons.Default.SportsMartialArts),
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TechColors.DarkBlue.copy(alpha = 0.95f),
        titleContentColor = TechColors.NeonBlue,
        textContentColor = Color.White,
        title = { Text("為 ${selectedDate.format(DateTimeFormatter.ofPattern("M月d日"))} 建立計畫") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = planName,
                    onValueChange = { planName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("計劃名稱（可選）") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TechColors.NeonBlue,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedLabelColor = TechColors.NeonBlue,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "選擇訓練部位",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Part selection chips (Custom Neon Style)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    parts.forEach { part ->
                        val isSelected = activeDialog == part.key
                        Box(
                            modifier = Modifier
                                .height(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isSelected) TechColors.NeonBlue else Color.White.copy(alpha = 0.1f)
                                )
                                .clickable {
                                    activeDialog = part.key
                                    selectedItems.clear()
                                    selectedSets.clear()
                                    selectedWeights.clear()
                                }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    part.icon,
                                    contentDescription = part.label,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSelected) Color.Black else Color.White
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    part.label,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Plan preview
                if (planExercises.isNotEmpty()) {
                    Text(
                        text = "已加入動作 (${planExercises.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = TechColors.NeonBlue,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        planExercises.take(8).forEach { e ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = e.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${e.sets}組 × ${e.weight?.roundToInt() ?: 0}kg",
                                    color = TechColors.NeonBlue,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    if (planExercises.size > 8) {
                        Text(
                            text = "... 還有 ${planExercises.size - 8} 個動作",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(
                        onClick = { planExercises.clear() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF6B6B))
                    ) {
                        Text("清空所有動作")
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Publish to trainee (if applicable)
                if (trainees.isNotEmpty()) {
                    OutlinedButton(
                        enabled = planExercises.isNotEmpty() && coachId.isNotBlank(),
                        onClick = { confirmPublish = true },
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TechColors.NeonBlue),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TechColors.NeonBlue)
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("發布")
                    }
                }

                // Save locally only
                Button(
                    enabled = planExercises.isNotEmpty(),
                    onClick = {
                        scope.launch {
                            val now = Instant.now()
                            val plan = TrainingPlan(
                                id = UUID.randomUUID().toString(),
                                name = resolvedPlanName(),
                                exercises = planExercises.toList(),
                                createdAt = now,
                                updatedAt = now,
                            )
                            repository.addPlan(plan)
                            repository.addScheduledPlan(selectedDate, plan.id)
                            onDismiss()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = TechColors.NeonBlue)
                ) {
                    Text("儲存")
                }
            }
        },
        dismissButton = {
            // Moved Cancel to the top right 'X' or handled by tapping outside,
            // but for AlertDialog standard, let's keep a text button if needed,
            // or rely on onDismissRequest. Here we just put a spacer to push buttons right if standard logic applies,
            // but since we used a Row in confirmButton, we can omit this or keep it simple.
        }
    )

    // Exercise selection dialog
    if (activeDialog != null) {
        val groupsToShow = remember(activeDialog) {
            val key = activeDialog
            if (key.isNullOrBlank()) emptyList() else PartExerciseCatalog.groupsForPart(key)
        }

        val partDisplayName = when (activeDialog) {
            "chest" -> "胸部"
            "back" -> "背部"
            "legs" -> "腿部"
            "arms" -> "手臂"
            "shoulders" -> "肩部"
            "abs" -> "腹部"
            else -> ""
        }

        AlertDialog(
            onDismissRequest = {
                activeDialog = null
                selectedItems.clear()
                selectedSets.clear()
                selectedWeights.clear()
            },
            containerColor = TechColors.DarkBlue.copy(alpha = 0.95f),
            title = {
                Text(
                    "$partDisplayName 訓練選項",
                    color = TechColors.NeonBlue,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 450.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    groupsToShow.forEach { (group, items) ->
                        Text(
                            text = group,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                        items.forEach { item ->
                            val isSelected = selectedItems.contains(item)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(
                                        if (isSelected) TechColors.NeonBlue.copy(alpha = 0.1f) else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        width = if(isSelected) 1.dp else 0.dp,
                                        color = if(isSelected) TechColors.NeonBlue.copy(alpha=0.5f) else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isSelected) {
                                                selectedItems.remove(item)
                                                selectedSets.remove(item)
                                                selectedWeights.remove(item)
                                            } else {
                                                selectedItems.add(item)
                                                if (!selectedSets.containsKey(item)) selectedSets[item] = defaultSets
                                                if (!selectedWeights.containsKey(item)) selectedWeights[item] = defaultWeight
                                            }
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = null, // Handled by Row click
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = TechColors.NeonBlue,
                                            checkmarkColor = Color.Black,
                                            uncheckedColor = Color.White.copy(alpha = 0.5f)
                                        )
                                    )
                                    Text(
                                        text = item,
                                        color = if (isSelected) TechColors.NeonBlue else Color.White.copy(alpha = 0.8f),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }

                                // Controls visible only when selected
                                if (isSelected) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(start = 48.dp, end = 16.dp, bottom = 12.dp)
                                    ) {
                                        Text(
                                            text = "重量: ${selectedWeights[item]?.roundToInt() ?: defaultWeight.toInt()} kg",
                                            color = TechColors.NeonBlue,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Slider(
                                            value = selectedWeights[item] ?: defaultWeight,
                                            onValueChange = { v ->
                                                selectedWeights[item] = v
                                                selectedSets[item] = weightToSets(v)
                                            },
                                            valueRange = 0f..100f,
                                            colors = SliderDefaults.colors(
                                                thumbColor = TechColors.NeonBlue,
                                                activeTrackColor = TechColors.NeonBlue,
                                                inactiveTrackColor = TechColors.NeonBlue.copy(alpha = 0.3f)
                                            )
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 4.dp)
                                        ) {
                                            Text("組數:", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
                                            Spacer(modifier = Modifier.width(12.dp))

                                            // Custom - Button
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color.White.copy(alpha = 0.1f))
                                                    .clickable {
                                                        val cur = selectedSets[item] ?: defaultSets
                                                        selectedSets[item] = (cur - 1).coerceAtLeast(1)
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("-", color = Color.White, fontWeight = FontWeight.Bold)
                                            }

                                            Text(
                                                text = " ${selectedSets[item] ?: defaultSets} ",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 12.dp)
                                            )

                                            // Custom + Button
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color.White.copy(alpha = 0.1f))
                                                    .clickable {
                                                        val cur = selectedSets[item] ?: defaultSets
                                                        selectedSets[item] = (cur + 1).coerceAtMost(20)
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("+", color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newEntries = selectedItems.map { item ->
                            val sets = selectedSets[item] ?: defaultSets
                            val weight = selectedWeights[item] ?: defaultWeight
                            ExerciseEntry(
                                name = item,
                                reps = null,
                                sets = sets,
                                weight = weight
                            )
                        }
                        planExercises.addAll(newEntries)
                        activeDialog = null
                        selectedItems.clear()
                        selectedSets.clear()
                        selectedWeights.clear()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TechColors.NeonBlue)
                ) { Text("確定加入") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        activeDialog = null
                        selectedItems.clear()
                        selectedSets.clear()
                        selectedWeights.clear()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.6f))
                ) { Text("取消") }
            }
        )
    }

    // Publish confirmation dialog
    if (confirmPublish) {
        AlertDialog(
            onDismissRequest = { confirmPublish = false },
            containerColor = TechColors.DarkBlue.copy(alpha = 0.95f),
            titleContentColor = TechColors.NeonBlue,
            title = { Text("選擇發布學員") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "發布給：",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Spacer(Modifier.height(12.dp))
                    SingleTraineePicker(
                        trainees = trainees,
                        expanded = publishExpanded,
                        onExpandedChange = { publishExpanded = it },
                        selected = publishTarget,
                        onSelected = { publishTarget = it }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = publishTarget ?: return@Button
                        scope.launch {
                            val now = Instant.now()
                            val plan = TrainingPlan(
                                id = UUID.randomUUID().toString(),
                                name = resolvedPlanName(),
                                exercises = planExercises.toList(),
                                createdAt = now,
                                updatedAt = now,
                                publishedAt = now,
                            )

                            // Save locally
                            repository.addPlan(plan)
                            repository.addScheduledPlan(selectedDate, plan.id)

                            // Publish to trainee with scheduled date
                            val result = cloudSync.publishCoachLocalPlanToTrainee(
                                coachId = coachId,
                                traineeId = target.traineeId,
                                plan = plan
                            )

                            confirmPublish = false
                            onDismiss()
                        }
                    },
                    enabled = publishTarget != null && coachId.isNotBlank() && planExercises.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = TechColors.NeonBlue)
                ) { Text("確認發布") }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmPublish = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.6f))
                ) { Text("取消") }
            }
        )
    }
}