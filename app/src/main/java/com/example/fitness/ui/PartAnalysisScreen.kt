package com.example.fitness.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitness.activity.ActivityLogRepository
import com.example.fitness.data.PartAnalysisRepository
import com.example.fitness.data.TrainingPlanRepository
import com.example.fitness.inbody.InBodyRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.example.fitness.ui.theme.glassEffect
import com.example.fitness.ui.theme.TechColors
import com.example.fitness.ui.theme.neonGlowBorder

// ★★★ 動作分類器 ★★★
object ExerciseClassifier {
    fun getPartForExercise(exerciseName: String): String {
        val name = exerciseName.lowercase()
        return when {
            // 胸部
            name.contains("臥推") || name.contains("飛鳥") || name.contains("胸") ||
                    name.contains("push up") || name.contains("bench") || name.contains("chest") ||
                    name.contains("pec") || name.contains("dip") -> "chest"

            // 背部
            name.contains("划船") || name.contains("引體") || name.contains("背") ||
                    name.contains("拉") || name.contains("row") || name.contains("pull") ||
                    name.contains("lat") || name.contains("back") -> "back"

            // 腿部
            name.contains("蹲") || name.contains("腿") || name.contains("硬舉") ||
                    name.contains("leg") || name.contains("squat") || name.contains("deadlift") ||
                    name.contains("lunge") || name.contains("calf") -> "legs"

            // 肩部
            name.contains("肩") || name.contains("推舉") || name.contains("平舉") ||
                    name.contains("shoulder") || name.contains("press") || name.contains("raise") ||
                    name.contains("deltoid") || name.contains("military") -> "shoulders"

            // 手臂
            name.contains("彎舉") || name.contains("三頭") || name.contains("二頭") ||
                    name.contains("臂") || name.contains("curl") || name.contains("extension") ||
                    name.contains("tricep") || name.contains("bicep") || name.contains("skull") -> "arms"

            // 腹部
            name.contains("腹") || name.contains("捲腹") || name.contains("核心") ||
                    name.contains("abs") || name.contains("crunch") || name.contains("plank") ||
                    name.contains("sit up") -> "abs"

            // 其他
            else -> "other"
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartAnalysisScreen(
    repository: TrainingPlanRepository,
    activityRepository: ActivityLogRepository,
    inBodyRepo: InBodyRepository,
    onDone: () -> Unit,
) {
    val ctx = LocalContext.current
    val partRepo = remember { PartAnalysisRepository(ctx) }
    val entries by partRepo.data.collectAsState(initial = emptyList())

    val partOptions: List<Pair<String, String>> = listOf(
        "胸" to "chest",
        "背" to "back",
        "腿" to "legs",
        "肩" to "shoulders",
        "手臂" to "arms",
        "腹" to "abs",
        "其他" to "other"
    )

    var selectedPartKey by rememberSaveable { mutableStateOf("chest") }
    var selectedExercise by rememberSaveable { mutableStateOf<String?>(null) }
    var exerciseDropdownExpanded by remember { mutableStateOf(false) }

    val byDate: Map<LocalDate, Map<String, Float>> = remember(entries) {
        entries.associate { LocalDate.parse(it.date) to it.exerciseWeights }
    }

    val filteredExerciseNames: List<String> = remember(byDate, selectedPartKey) {
        byDate
            .values
            .flatMap { it.keys }
            .toSet()
            .filter { exerciseName ->
                ExerciseClassifier.getPartForExercise(exerciseName) == selectedPartKey
            }
            .sorted()
    }

    LaunchedEffect(filteredExerciseNames) {
        if (filteredExerciseNames.isNotEmpty()) {
            if (selectedExercise == null || !filteredExerciseNames.contains(selectedExercise)) {
                selectedExercise = filteredExerciseNames.first()
            }
        } else {
            selectedExercise = null
        }
    }

    val chartPoints: List<Pair<LocalDate, Float>> = remember(selectedExercise, byDate) {
        val ex = selectedExercise ?: return@remember emptyList()
        byDate
            .mapNotNull { (date, map) -> map[ex]?.let { date to it } }
            .sortedBy { it.first }
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
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassEffect(cornerRadius = 20.dp)
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDone) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = TechColors.NeonBlue
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            "部位分析",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TechColors.NeonBlue
                        )
                        Text(
                            "追蹤各部位動作重量趨勢",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Part Selector - Renamed component usage
                AnalysisSectionCard(title = "選擇訓練部位") {
                    val rows = partOptions.chunked(4)

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        rows.forEach { row ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                row.forEach { (label, key) ->
                                    val isSelected = selectedPartKey == key
                                    val modifier = if (isSelected) {
                                        Modifier
                                            .weight(1f)
                                            .neonGlowBorder(cornerRadius = 12.dp)
                                            .background(
                                                TechColors.NeonBlue.copy(alpha = 0.2f),
                                                RoundedCornerShape(12.dp)
                                            )
                                    } else {
                                        Modifier
                                            .weight(1f)
                                            .glassEffect(cornerRadius = 12.dp)
                                    }

                                    Box(
                                        modifier = modifier
                                            .height(45.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { selectedPartKey = key },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) TechColors.NeonBlue else Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }

                // 2. Exercise Selector - Renamed component usage
                AnalysisSectionCard(title = "選擇動作") {
                    if (filteredExerciseNames.isEmpty()) {
                        EmptyStateCard("此部位尚無動作紀錄\n(若有紀錄，請查看「其他」分類)")
                    } else {
                        ExposedDropdownMenuBox(
                            expanded = exerciseDropdownExpanded,
                            onExpandedChange = { exerciseDropdownExpanded = !exerciseDropdownExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            NeonOutlinedTextField(
                                value = selectedExercise ?: "請選擇動作",
                                onValueChange = {},
                                readOnly = true,
                                label = "動作名稱",
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.FitnessCenter,
                                        contentDescription = null,
                                        tint = TechColors.NeonBlue
                                    )
                                },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = exerciseDropdownExpanded)
                                },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                            )

                            ExposedDropdownMenu(
                                expanded = exerciseDropdownExpanded,
                                onDismissRequest = { exerciseDropdownExpanded = false },
                                containerColor = TechColors.DarkBlue.copy(alpha = 0.95f),
                                modifier = Modifier.background(TechColors.DarkBlue.copy(alpha = 0.95f))
                            ) {
                                filteredExerciseNames.forEach { ex ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = ex,
                                                color = if (selectedExercise == ex) TechColors.NeonBlue else Color.White,
                                                fontWeight = if (selectedExercise == ex) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            selectedExercise = ex
                                            exerciseDropdownExpanded = false
                                        },
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                                }
                            }
                        }
                    }
                }

                // 3. Statistics Grid
                if (chartPoints.isNotEmpty()) {
                    val values = chartPoints.map { it.second }
                    val latest = values.lastOrNull() ?: 0f
                    val earliest = values.firstOrNull() ?: 0f
                    val change = latest - earliest
                    val max = values.maxOrNull() ?: 0f
                    val average = values.average().toFloat()

                    // Renamed component usage
                    AnalysisSectionCard(title = "重量統計") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                NeonStatCard(
                                    label = "最新重量",
                                    value = "%.1f".format(latest),
                                    unit = "kg",
                                    modifier = Modifier.weight(1f)
                                )
                                NeonStatCard(
                                    label = "總進步",
                                    value = "%+.1f".format(change),
                                    unit = "kg",
                                    valueColor = if (change >= 0) TechColors.NeonBlue else Color(0xFFFF6B6B),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                NeonStatCard(
                                    label = "歷史最大",
                                    value = "%.1f".format(max),
                                    unit = "kg",
                                    modifier = Modifier.weight(1f)
                                )
                                NeonStatCard(
                                    label = "平均重量",
                                    value = "%.1f".format(average),
                                    unit = "kg",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // 4. Chart Section
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassEffect(cornerRadius = 24.dp)
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "重量趨勢",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = selectedExercise ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                }
                                Icon(
                                    Icons.AutoMirrored.Filled.ShowChart,
                                    contentDescription = null,
                                    tint = TechColors.NeonBlue,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Spacer(Modifier.height(20.dp))

                            LineChart(
                                dataPoints = chartPoints,
                                dateFormatter = DateTimeFormatter.ofPattern("MM/dd", Locale.getDefault()),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp)
                            )
                        }
                    }

                } else if (selectedExercise != null) {
                    EmptyStateCard("此動作尚無足夠數據繪製圖表")
                } else if (filteredExerciseNames.isNotEmpty()) {
                    EmptyStateCard("請選擇一個動作以檢視分析")
                }

                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

// --- Helper Components ---

// ★★★ Renamed to AnalysisSectionCard to prevent ambiguity with other files ★★★
@Composable
private fun AnalysisSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassEffect(cornerRadius = 24.dp)
            .padding(20.dp)
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun NeonStatCard(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    valueColor: Color = TechColors.NeonBlue
) {
    Box(
        modifier = modifier
            .glassEffect(cornerRadius = 16.dp)
            .padding(16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = valueColor
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun NeonOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    readOnly: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        readOnly = readOnly,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        singleLine = true,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = TechColors.NeonBlue,
            unfocusedBorderColor = TechColors.NeonBlue.copy(alpha = 0.5f),
            focusedLabelColor = TechColors.NeonBlue,
            unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
            cursorColor = TechColors.NeonBlue,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent
        )
    )
}

@Composable
private fun EmptyStateCard(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassEffect(cornerRadius = 20.dp)
            .padding(30.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}