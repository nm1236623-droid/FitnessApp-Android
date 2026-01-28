package com.example.fitness.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fitness.localization.LocaleManager
import com.example.fitness.localization.SupportedLanguage
import com.example.fitness.nutrition.AdvancedNutritionRepository
import com.example.fitness.ui.theme.ColorScheme
import com.example.fitness.ui.theme.TechColors
import com.example.fitness.ui.theme.ThemeManager
import com.example.fitness.ui.theme.ThemeMode
import com.example.fitness.ui.theme.glassEffect
import com.example.fitness.ui.theme.neonGlowBorder
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themeManager = remember { ThemeManager.getInstance(context) }
    val localeManager = remember { LocaleManager.getInstance(context) }
    val nutritionRepo = remember { AdvancedNutritionRepository(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    val currentTheme by themeManager.themeMode.collectAsState(initial = ThemeMode.AUTO)
    val currentColorScheme by themeManager.colorScheme.collectAsState(initial = ColorScheme.NEON_BLUE)
    val currentLanguage by localeManager.currentLanguage.collectAsState(initial = SupportedLanguage.CHINESE_TRADITIONAL)
    val waterGoal by nutritionRepo.waterGoal.collectAsState(initial = 2000.0)

    var showThemeDialog by remember { mutableStateOf(false) }
    var showColorSchemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showWaterGoalDialog by remember { mutableStateOf(false) }

    // Water intake today
    var todayWaterIntake by remember { mutableStateOf(0.0) }

    LaunchedEffect(Unit) {
        todayWaterIntake = nutritionRepo.getTodayWaterIntake()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(TechColors.DarkBlue, TechColors.DeepPurple)))
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassEffect(20.dp)
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, null, tint = TechColors.NeonBlue)
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                "進階設定",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TechColors.NeonBlue
                            )
                            Text(
                                "個性化您的體驗",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(0.7f)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Theme Section
                SettingSection("外觀設定") {
                    SettingItem(
                        icon = Icons.Default.Palette,
                        title = "主題模式",
                        subtitle = currentTheme.name,
                        onClick = { showThemeDialog = true }
                    )

                    SettingItem(
                        icon = Icons.Default.ColorLens,
                        title = "顏色方案",
                        subtitle = currentColorScheme.name,
                        onClick = { showColorSchemeDialog = true }
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Language Section
                SettingSection("語言設定") {
                    SettingItem(
                        icon = Icons.Default.Language,
                        title = "應用程式語言",
                        subtitle = currentLanguage.displayName,
                        onClick = { showLanguageDialog = true }
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Water Tracking Section
                SettingSection("水分追蹤") {
                    // Water goal
                    SettingItem(
                        icon = Icons.Default.WaterDrop,
                        title = "每日目標",
                        subtitle = "${waterGoal.toInt()} ml",
                        onClick = { showWaterGoalDialog = true }
                    )

                    Spacer(Modifier.height(12.dp))

                    // Today's water intake with progress
                    WaterIntakeCard(
                        consumed = todayWaterIntake,
                        goal = waterGoal,
                        onAddWater = { amount ->
                            scope.launch {
                                val result = nutritionRepo.logWaterIntake(amount)
                                if (result.isSuccess) {
                                    todayWaterIntake += amount
                                    snackbarHostState.showSnackbar("已記錄 ${amount.toInt()} ml")
                                }
                            }
                        }
                    )
                }

                Spacer(Modifier.height(32.dp))
            }

            // Dialogs
            if (showThemeDialog) {
                ThemeSelectionDialog(
                    currentTheme = currentTheme,
                    onThemeSelected = { theme ->
                        scope.launch {
                            themeManager.setThemeMode(theme)
                            showThemeDialog = false
                        }
                    },
                    onDismiss = { showThemeDialog = false }
                )
            }

            if (showColorSchemeDialog) {
                ColorSchemeSelectionDialog(
                    currentScheme = currentColorScheme,
                    onSchemeSelected = { scheme ->
                        scope.launch {
                            themeManager.setColorScheme(scheme)
                            showColorSchemeDialog = false
                        }
                    },
                    onDismiss = { showColorSchemeDialog = false }
                )
            }

            if (showLanguageDialog) {
                LanguageSelectionDialog(
                    currentLanguage = currentLanguage,
                    onLanguageSelected = { language ->
                        scope.launch {
                            localeManager.setLanguage(language)
                            showLanguageDialog = false
                            snackbarHostState.showSnackbar("語言已更新，請重啟應用程式")
                        }
                    },
                    onDismiss = { showLanguageDialog = false }
                )
            }

            if (showWaterGoalDialog) {
                WaterGoalDialog(
                    currentGoal = waterGoal,
                    onGoalSet = { goal ->
                        scope.launch {
                            nutritionRepo.setWaterGoal(goal)
                            showWaterGoalDialog = false
                        }
                    },
                    onDismiss = { showWaterGoalDialog = false }
                )
            }
        }
    }
}

@Composable
fun SettingSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassEffect(24.dp)
            .padding(20.dp)
    ) {
        Column {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun SettingItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            null,
            tint = TechColors.NeonBlue,
            modifier = Modifier.size(24.dp)
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(0.6f)
            )
        }

        Icon(
            Icons.Default.ChevronRight,
            null,
            tint = Color.White.copy(0.5f)
        )
    }
}

@Composable
fun WaterIntakeCard(
    consumed: Double,
    goal: Double,
    onAddWater: (Double) -> Unit
) {
    val progress = (consumed / goal).toFloat().coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(0.05f))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "今日攝取",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "${consumed.toInt()} / ${goal.toInt()} ml",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TechColors.NeonBlue,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF00B4DB),
                                    Color(0xFF0083B0)
                                )
                            )
                        )
                )
            }

            Spacer(Modifier.height(16.dp))

            // Quick add buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(250.0, 500.0, 750.0).forEach { amount ->
                    Button(
                        onClick = { onAddWater(amount) },
                        modifier = Modifier.weight(1f).height(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00B4DB).copy(0.2f)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            "+${amount.toInt()}",
                            color = Color(0xFF00B4DB),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ThemeSelectionDialog(
    currentTheme: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TechColors.DarkBlue.copy(0.95f),
        title = {
            Text("選擇主題", fontWeight = FontWeight.Bold, color = TechColors.NeonBlue)
        },
        text = {
            Column {
                ThemeMode.values().forEach { theme ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onThemeSelected(theme) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentTheme == theme,
                            onClick = { onThemeSelected(theme) },
                            colors = RadioButtonDefaults.colors(selectedColor = TechColors.NeonBlue)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            theme.name,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Color.White.copy(0.7f))
            }
        }
    )
}

@Composable
fun ColorSchemeSelectionDialog(
    currentScheme: ColorScheme,
    onSchemeSelected: (ColorScheme) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TechColors.DarkBlue.copy(0.95f),
        title = {
            Text("選擇顏色方案", fontWeight = FontWeight.Bold, color = TechColors.NeonBlue)
        },
        text = {
            Column {
                ColorScheme.values().forEach { scheme ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSchemeSelected(scheme) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Color preview
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(getColorForScheme(scheme))
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            scheme.name,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        if (currentScheme == scheme) {
                            Icon(
                                Icons.Default.Check,
                                null,
                                tint = TechColors.NeonBlue
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Color.White.copy(0.7f))
            }
        }
    )
}

@Composable
fun LanguageSelectionDialog(
    currentLanguage: SupportedLanguage,
    onLanguageSelected: (SupportedLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TechColors.DarkBlue.copy(0.95f),
        title = {
            Text("選擇語言", fontWeight = FontWeight.Bold, color = TechColors.NeonBlue)
        },
        text = {
            Column {
                SupportedLanguage.values().forEach { language ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onLanguageSelected(language) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentLanguage == language,
                            onClick = { onLanguageSelected(language) },
                            colors = RadioButtonDefaults.colors(selectedColor = TechColors.NeonBlue)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            language.displayName,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Color.White.copy(0.7f))
            }
        }
    )
}

@Composable
fun WaterGoalDialog(
    currentGoal: Double,
    onGoalSet: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var goalText by remember { mutableStateOf(currentGoal.toInt().toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TechColors.DarkBlue.copy(0.95f),
        title = {
            Text("設定每日目標", fontWeight = FontWeight.Bold, color = TechColors.NeonBlue)
        },
        text = {
            OutlinedTextField(
                value = goalText,
                onValueChange = { if (it.all { c -> c.isDigit() }) goalText = it },
                label = { Text("目標 (ml)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = TechColors.NeonBlue,
                    unfocusedBorderColor = Color.White.copy(0.5f)
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    goalText.toDoubleOrNull()?.let { onGoalSet(it) }
                }
            ) {
                Text("確定", color = TechColors.NeonBlue, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Color.White.copy(0.7f))
            }
        }
    )
}

fun getColorForScheme(scheme: ColorScheme): Color {
    return when (scheme) {
        ColorScheme.NEON_BLUE -> Color(0xFF00D4FF)
        ColorScheme.PURPLE -> Color(0xFF9C27B0)
        ColorScheme.GREEN -> Color(0xFF4CAF50)
        ColorScheme.ORANGE -> Color(0xFFFF9800)
        ColorScheme.RED -> Color(0xFFF44336)
        ColorScheme.CUSTOM -> Color(0xFF00D4FF)
    }
}
