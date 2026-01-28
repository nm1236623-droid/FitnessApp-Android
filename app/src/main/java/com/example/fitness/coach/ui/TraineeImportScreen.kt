package com.example.fitness.coach.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.fitness.coach.CoachAuthRepository
import com.example.fitness.coach.CoachDisplayNameRepository
import com.example.fitness.coach.cloud.CloudSyncUseCase
import com.example.fitness.coach.cloud.CoachTraineeDirectoryRepository
import com.example.fitness.data.TrainingPlanRepository
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import com.example.fitness.coach.cloud.RemotePlanItem
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MenuAnchorType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TraineeImportScreen(
    trainingPlanRepository: TrainingPlanRepository,
    onBack: () -> Unit,
) {
    val ctx = LocalContext.current
    val appCtx = remember(ctx) { ctx.applicationContext }
    val clipboardManager = remember(ctx) {
        ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
    }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val authRepository = remember { CoachAuthRepository(ctx.applicationContext) }
    val cloudSync = remember { CloudSyncUseCase(authRepository, trainingPlanRepository) }

    // NEW: repositories needed to log completion into training history
    val activityRepository = remember(appCtx) { com.example.fitness.activity.ActivityLogRepository(context = appCtx) }
    val trainingRecordRepository = remember { com.example.fitness.data.FirebaseTrainingRecordRepository() }

    val displayNameRepo = remember(appCtx) { CoachDisplayNameRepository(appCtx) }
    val traineeDisplayName by displayNameRepo.traineeDisplayName.collectAsState(initial = "")

    var traineeNameInput by remember { mutableStateOf("") }

    // Remote coach-published plans (do NOT mix with local plans)
    val remoteItems by cloudSync.remoteItemsFlow().collectAsState(initial = emptyList())

    var coachIdInput by remember { mutableStateOf("") }

    var traineeId by remember { mutableStateOf<String?>(null) }
    val joinedCoaches = remember { mutableStateListOf<String>() }
    val traineeDirectoryRepo = remember { CoachTraineeDirectoryRepository() }

    // CHANGED: avoid mutableStateMapOf (may be missing import/compat)
    // and keep cache as a normal mutable state map.
    var coachNameCache by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    suspend fun refreshCoachNames(ids: List<String>) {
        ids.distinct().filter { it.isNotBlank() }.forEach { id ->
            cloudSync.getCoachDisplayNameById(id)
                .onSuccess { name ->
                    coachNameCache = coachNameCache + (id to (name?.takeIf { it.isNotBlank() } ?: ""))
                }
        }
    }

    suspend fun refreshMembershipsAndNames(showToast: Boolean) {
        cloudSync.getJoinedCoachIds()
            .onSuccess { ids ->
                joinedCoaches.clear(); joinedCoaches.addAll(ids.sorted())
                refreshCoachNames(ids)
                if (showToast) snackbarHostState.showSnackbar("已更新教練清單")
            }
            .onFailure { e ->
                if (showToast) snackbarHostState.showSnackbar("更新失敗：${e.message ?: "未知錯誤"}")
            }
    }

    // Auto reconnect sync: when screen opens, read memberships and start listening
    LaunchedEffect(Unit) {
        traineeId = cloudSync.getCurrentTraineeId()

        // Start auto reconnect (listen memberships -> listen plans)
        cloudSync.startAutoReconnectSync()

        // Also load memberships once for UI
        refreshMembershipsAndNames(showToast = false)

        // best-effort: register trainee display name into each coach's directory
        val tid = traineeId
        val nameForDirectory = traineeDisplayName.ifBlank { traineeNameInput.trim().ifBlank { "學員" } }
        if (!tid.isNullOrBlank() && joinedCoaches.isNotEmpty()) {
            joinedCoaches.forEach { coachId ->
                traineeDirectoryRepo.upsertTrainee(
                    coachId = coachId,
                    traineeId = tid,
                    displayName = nameForDirectory,
                )
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        com.example.fitness.ui.ScreenHeader(
            title = "學員中心",
            onBack = onBack
        )

        Spacer(Modifier.height(10.dp))

        // 合併卡片：暱稱資訊 + 已加入教練 + 加入並同步（更小更緊湊）
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // A) 狀態列：暱稱 · TraineeId + 複製 + 刷新
                val displayName = traineeDisplayName.ifBlank { traineeNameInput.trim().ifBlank { "學員" } }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tid = traineeId

                    Text(
                        text = if (tid.isNullOrBlank()) "載入中…" else "$displayName · $tid",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    IconButton(
                        onClick = {
                            if (tid.isNullOrBlank()) return@IconButton
                            clipboardManager?.setPrimaryClip(
                                android.content.ClipData.newPlainText("traineeId", tid)
                            )
                            scope.launch { snackbarHostState.showSnackbar("已複製學員ID") }
                        },
                        enabled = !tid.isNullOrBlank()
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "複製學員ID")
                    }

                    IconButton(
                        onClick = { scope.launch { refreshMembershipsAndNames(showToast = true) } }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "更新教練清單")
                    }
                }

                // B) 暱稱列：同一行輸入 + 小按鈕（更省高度）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = traineeNameInput,
                        onValueChange = { traineeNameInput = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("暱稱") },
                        placeholder = { Text("例如：小明") },
                        singleLine = true
                    )

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                displayNameRepo.setTraineeDisplayName(traineeNameInput)

                                // best-effort: update directory entry in all joined coaches
                                val tid = traineeId
                                val nameForDirectory = traineeNameInput.trim().ifBlank { "學員" }
                                if (!tid.isNullOrBlank()) {
                                    joinedCoaches.forEach { coachId ->
                                        traineeDirectoryRepo.upsertTrainee(
                                            coachId = coachId,
                                            traineeId = tid,
                                            displayName = nameForDirectory,
                                        )
                                    }
                                }

                                snackbarHostState.showSnackbar("已儲存")
                            }
                        },
                        modifier = Modifier.height(44.dp),
                        shape = MaterialTheme.shapes.large
                    ) { Text("儲存") }

                    TextButton(
                        onClick = { traineeNameInput = traineeDisplayName.ifBlank { "學員" } },
                        modifier = Modifier.height(44.dp)
                    ) { Text("重設") }
                }

                // C) 已加入教練：下拉選單 + 顯示教練名稱（從 Firestore 反查 coachIdDirectory）
                var coachDropdownExpanded by remember { mutableStateOf(false) }
                var selectedCoachId by remember(joinedCoaches) {
                    mutableStateOf(joinedCoaches.firstOrNull() ?: "")
                }

                if (joinedCoaches.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = coachDropdownExpanded,
                        onExpandedChange = { coachDropdownExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val selectedName = coachNameCache[selectedCoachId].orEmpty().trim()
                        val selectedLabel = selectedName.ifBlank { "(未設定暱稱)" }

                        OutlinedTextField(
                            value = selectedLabel,
                            onValueChange = {},
                            readOnly = true,
                            textStyle = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true),
                            label = { Text("教練", style = MaterialTheme.typography.labelSmall) },
                            singleLine = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = coachDropdownExpanded) },
                        )

                        DropdownMenu(
                            expanded = coachDropdownExpanded,
                            onDismissRequest = { coachDropdownExpanded = false },
                            modifier = Modifier.heightIn(max = 260.dp)
                        ) {
                            joinedCoaches.sorted().forEach { id ->
                                val name = coachNameCache[id].orEmpty().trim()
                                val itemLabel = name.ifBlank { "(未設定暱稱)" }

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            itemLabel,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    onClick = {
                                        selectedCoachId = id
                                        coachDropdownExpanded = false
                                        coachIdInput = id
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        "尚未加入任何教練",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // D) 加入教練並同步
                OutlinedTextField(
                    value = coachIdInput,
                    onValueChange = { coachIdInput = it },
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    textStyle = MaterialTheme.typography.bodySmall,
                    label = { Text("教練 ID", style = MaterialTheme.typography.labelSmall) },
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            val coachId = coachIdInput.trim()
                            if (coachId.isBlank()) return@Button
                            scope.launch {
                                val r = cloudSync.joinCoachAndStartListening(coachId)
                                if (r.isSuccess) {
                                    snackbarHostState.showSnackbar("已加入並開始同步")

                                    // refresh list + name cache from Firestore
                                    refreshMembershipsAndNames(showToast = false)

                                    // 若是第一次加入，讓下拉預設選到第一個
                                    if (selectedCoachId.isBlank() && joinedCoaches.isNotEmpty()) {
                                        selectedCoachId = joinedCoaches.first()
                                    }

                                    // best-effort: upsert trainee display name into coach directory
                                    val tid = traineeId
                                    val nameForDirectory = traineeDisplayName.ifBlank { traineeNameInput.trim().ifBlank { "學員" } }
                                    if (!tid.isNullOrBlank()) {
                                        traineeDirectoryRepo.upsertTrainee(
                                            coachId = coachId,
                                            traineeId = tid,
                                            displayName = nameForDirectory,
                                        )
                                    }
                                } else snackbarHostState.showSnackbar("加入/同步失敗：${r.exceptionOrNull()?.message ?: ""}")
                            }
                        },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = MaterialTheme.shapes.large
                    ) { Text("加入並同步", style = MaterialTheme.typography.bodySmall) }

                    OutlinedButton(
                        onClick = {
                            cloudSync.stopListening()
                            scope.launch { snackbarHostState.showSnackbar("已停止同步") }
                        },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = MaterialTheme.shapes.large
                    ) { Text("停止同步", style = MaterialTheme.typography.bodySmall) }
                }
            }
        }

        // --- Remote coach-published plans ---
        Spacer(Modifier.height(14.dp))
        com.example.fitness.ui.ScreenHeader(
            title = "教練發佈的計畫",
            subtitle = if (remoteItems.isEmpty()) "尚未收到計畫（先加入教練，且教練需按『發布雲端』）" else "已收到 ${remoteItems.size} 個",
            onBack = null
        )
        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(remoteItems.sortedBy { it.plan.name }) { item ->
                val p = item.plan
                RemotePlanExpandableCard(
                    item = item,
                    joinedCoachIds = joinedCoaches.toList(),
                    onMarkCompleted = { coachIdsToNotify ->
                        scope.launch {
                            val targets = coachIdsToNotify.distinct().filter { it.isNotBlank() }
                            if (targets.isEmpty()) {
                                snackbarHostState.showSnackbar("尚未加入教練，無法回報完成")
                                return@launch
                            }

                            // Calculate estimated calories for this plan
                            fun estimatePlanCalories(plan: com.example.fitness.data.TrainingPlan, userWeightKg: Double = 70.0): Double {
                                val minutesPerSet = 0.5 // 30 seconds per set
                                val met = 6.0 // general resistance training MET
                                var totalMinutes = 0.0
                                plan.exercises.forEach { e ->
                                    val sets = (e.sets ?: 3).toDouble()
                                    totalMinutes += sets * minutesPerSet
                                }
                                return com.example.fitness.calorie.CalorieCalculator.estimateCalories(met, userWeightKg, totalMinutes)
                            }

                            val estimatedCalories = estimatePlanCalories(p)

                            // 1) Record into trainee training history (local + optional Firestore) with calories
                            cloudSync.recordCoachPlanCompletionToHistory(
                                activityRepository = activityRepository,
                                trainingRecordRepository = trainingRecordRepository,
                                plan = p,
                                estimatedCalories = estimatedCalories
                            )

                            // 2) Notify coach(es)
                            val results = targets.map { coachId ->
                                cloudSync.reportCoachPlanCompletion(coachId = coachId, plan = p)
                            }

                            val failed = results.firstOrNull { it.isFailure }?.exceptionOrNull()
                            if (failed != null) {
                                snackbarHostState.showSnackbar("回報失敗：${failed.message ?: "未知錯誤"}")
                            } else {
                                snackbarHostState.showSnackbar("已回報完成給教練（${targets.size} 位）")
                            }
                        }
                    },
                    onDelete = {
                        scope.launch {
                            if (item.isInbox) {
                                cloudSync.markInboxPlanRead(p.id)
                                snackbarHostState.showSnackbar("已標記已讀：${p.name}")
                            } else {
                                cloudSync.removeRemotePlan(p.id)
                                snackbarHostState.showSnackbar("已從清單移除：${p.name}")
                            }
                        }
                    }
                )
            }

            if (remoteItems.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                            Text("目前沒有可接收的計畫", style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
            }
        }

        SnackbarHost(hostState = snackbarHostState)
    }
}

@Composable
private fun RemotePlanExpandableCard(
    item: RemotePlanItem,
    modifier: Modifier = Modifier,
    joinedCoachIds: List<String> = emptyList(),
    onMarkCompleted: ((coachIdsToNotify: List<String>) -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmComplete by remember { mutableStateOf(false) }

    // UI-only completion state (MVP). Consider persisting if you want it to survive app restart.
    var locallyCompleted by remember(item.plan.id) { mutableStateOf(false) }

    val publishedText = remember(item.plan.publishedAt) {
        item.plan.publishedAt?.atZone(ZoneId.systemDefault())?.format(DateTimeFormatter.ofPattern("MM/dd HH:mm"))
    }

    val sourceText = item.sourceCoachDisplayName?.takeIf { it.isNotBlank() }
        ?: item.sourceCoachId?.takeIf { it.isNotBlank() }
        ?: joinedCoachIds.firstOrNull()
        ?: "(未知)"

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("刪除計畫") },
            text = {
                Text(
                    "確定要刪除『${item.plan.name}』嗎？\n\n此動作只會從你的學員端清單移除，不會刪除教練雲端資料（重新同步仍可能再出現）。"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDelete?.invoke()
                    }
                ) { Text("刪除") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("取消") }
            }
        )
    }

    if (confirmComplete) {
        AlertDialog(
            onDismissRequest = { confirmComplete = false },
            title = { Text("回報完成") },
            text = {
                val coachText = when {
                    joinedCoachIds.isEmpty() -> "（你目前尚未加入任何教練）"
                    joinedCoachIds.size == 1 -> "教練：${joinedCoachIds.first()}"
                    else -> "你已加入 ${joinedCoachIds.size} 位教練，會全部回報。"
                }
                Text("確定要回報你已完成『${item.plan.name}』嗎？\n$coachText")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmComplete = false
                        locallyCompleted = true
                        onMarkCompleted?.invoke(joinedCoachIds)
                    }
                ) { Text("回報") }
            },
            dismissButton = {
                TextButton(onClick = { confirmComplete = false }) { Text("取消") }
            }
        )
    }

    OutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !locallyCompleted) { expanded = !expanded },
        shape = MaterialTheme.shapes.large,
        colors = androidx.compose.material3.CardDefaults.outlinedCardColors(
            containerColor = if (locallyCompleted) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.plan.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (locallyCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = buildString {
                            append("動作數：${item.plan.exercises.size}")
                            if (publishedText != null) append("  ·  發佈：$publishedText")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "來源：$sourceText" + if (item.isInbox) "  ·  指定發布" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (onMarkCompleted != null) {
                    IconButton(onClick = { if (!locallyCompleted) confirmComplete = true }, enabled = !locallyCompleted) {
                        Icon(
                            imageVector = if (locallyCompleted) Icons.Default.CheckCircle else Icons.Default.CheckCircle,
                            contentDescription = "完成並回報"
                        )
                    }
                }

                if (onDelete != null) {
                    IconButton(onClick = { if (!locallyCompleted) confirmDelete = true }, enabled = !locallyCompleted) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "刪除計畫")
                    }
                }

                IconButton(onClick = { expanded = !expanded }, enabled = !locallyCompleted) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "收合" else "展開"
                    )
                }
            }

            if (expanded && !locallyCompleted) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                if (item.plan.exercises.isEmpty()) {
                    Text(
                        text = "（此計畫沒有動作）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    item.plan.exercises.forEach { e ->
                        val meta = buildList {
                            e.sets?.let { add("${it}組") }
                            e.reps?.let { add("${it}下") }
                            e.weight?.let { add("${it}kg") }
                        }.joinToString(" · ")

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = e.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (meta.isNotBlank()) {
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = meta,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}
