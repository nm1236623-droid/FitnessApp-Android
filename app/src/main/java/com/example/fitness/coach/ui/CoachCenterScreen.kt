package com.example.fitness.coach.ui

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.fitness.coach.CoachAuthRepository
import com.example.fitness.coach.UserRole
import com.example.fitness.coach.cloud.CloudSyncUseCase
import com.example.fitness.coach.cloud.CoachTrainee
import com.example.fitness.coach.cloud.CoachTraineeDirectoryRepository
import com.example.fitness.coach.local.CoachLocalPlanRepository
import com.example.fitness.data.TrainingPlanRepository
import com.example.fitness.user.UserRoleProfileRepository // ★ 已確認添加此 Import
import com.google.firebase.auth.FirebaseAuth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@Composable
fun CoachCenterScreen(
    authRepository: CoachAuthRepository,
    trainingPlanRepository: TrainingPlanRepository,
    onBack: () -> Unit,
    onNavigateToCreatePlan: () -> Unit,
) {
    val ctx = LocalContext.current

    val roleRepo = remember { UserRoleProfileRepository() }
    var myRole by remember { mutableStateOf<UserRole?>(null) }

    // Coach nickname (stored in users/{uid}.nickname)
    var coachNickname by rememberSaveable { mutableStateOf("") }
    var isEditingNickname by rememberSaveable { mutableStateOf(false) }
    var nicknameDraft by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        // If read fails (e.g., not signed in), treat as not authorized.
        myRole = roleRepo.getMyRole().getOrNull()
        coachNickname = roleRepo.getMyNickname().getOrNull().orEmpty()
        nicknameDraft = coachNickname
    }

    // While loading role, show a lightweight skeleton.
    if (myRole == null) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Text("教練中心", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        return
    }

    if (myRole != UserRole.COACH) {
        // Trainee / non-coach view (polished)
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                    Text("教練中心", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "此功能僅提供給教練帳號使用。\n\n你目前是學員帳號，可使用『學員中心』來接收教練發佈的課表。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onBack) { Text("返回") }
                        Button(
                            onClick = {
                                // Return to workout tab; we can't directly route here, so guide user.
                                scope.launch { snackbarHostState.showSnackbar("請進入『Workout → 教練 / 學員 → 學員中心』") }
                                onBack()
                            }
                        ) { Text("前往學員中心") }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            SnackbarHost(hostState = snackbarHostState)
        }
        return
    }

    // Coach Center local plans are stored separately from personal plans.
    val appCtx = LocalContext.current.applicationContext
    val coachLocalRepo = remember(appCtx) { CoachLocalPlanRepository(appCtx) }
    val plans by coachLocalRepo.plans.collectAsState(initial = emptyList())

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val cloudSync = remember { CloudSyncUseCase(authRepository, trainingPlanRepository) }

    // ===== Trainee directory (coach-side) =====
    val traineeDirectoryRepo = remember { CoachTraineeDirectoryRepository() }
    val trainees = remember { mutableStateListOf<CoachTrainee>() }
    var traineeIdInput by remember { mutableStateOf("") }
    var traineeNameInput by remember { mutableStateOf("") }

    // Firestore nickname cache for completion display (users/{uid}.nickname)
    // Key: traineeId (uid), Value: nickname (may be blank if not set)
    val traineeNicknameCache = remember { mutableStateMapOf<String, String>() }

    // UI selection for filtering reports / publishing
    var traineeDropdownExpanded by remember { mutableStateOf(false) }
    var selectedTraineeId by rememberSaveable { mutableStateOf<String?>(null) } // null = all

    // Build a quick lookup map for completion list display
    val traineeNameById = remember(trainees) {
        trainees.associate { it.traineeId to it.displayName.trim() }
    }

    // keep selection stable when trainee list updates
    LaunchedEffect(trainees) {
        if (selectedTraineeId != null && trainees.none { it.traineeId == selectedTraineeId }) {
            selectedTraineeId = null
        }
    }

    // Listen trainee completion reports (coach-side)
    val completions by cloudSync.coachCompletionsFlow()
        .collectAsState(initial = emptyList<com.example.fitness.coach.cloud.CoachPlanCompletion>())

    val filteredCompletions = remember(completions, selectedTraineeId) {
        val id = selectedTraineeId
        if (id.isNullOrBlank()) completions else completions.filter { it.traineeId == id }
    }

    // Fetch nicknames for the currently visible completion list (latest 50) to keep reads bounded.
    LaunchedEffect(filteredCompletions) {
        val ids = filteredCompletions
            .asSequence()
            .sortedByDescending { it.completedAt }
            .take(50)
            .map { it.traineeId }
            .filter { it.isNotBlank() }
            .distinct()
            .toList()

        ids.forEach { uid ->
            if (!traineeNicknameCache.containsKey(uid)) {
                // best-effort: if it fails, cache blank to avoid spamming reads
                val nick = roleRepo.getNicknameByUid(uid).getOrNull().orEmpty()
                traineeNicknameCache[uid] = nick
            }
        }
    }

    LaunchedEffect(Unit) {
        cloudSync.startListeningCoachCompletions()

        // initial load trainees
        val coachIdNow = FirebaseAuth.getInstance().currentUser?.uid ?: authRepository.ensureUserId()
        traineeDirectoryRepo.listTrainees(coachIdNow)
            .onSuccess { list -> trainees.clear(); trainees.addAll(list) }
    }

    val dtf = remember { DateTimeFormatter.ofPattern("MM/dd HH:mm") }

    var coachId by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        coachId = FirebaseAuth.getInstance().currentUser?.uid ?: authRepository.ensureUserId()
    }

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text("教練中心") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(12.dp)) {

            // Top area: no background, compact height
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (coachId.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "教練ID：$coachId",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (!isEditingNickname && coachNickname.isNotBlank()) {
                                Text(
                                    text = coachNickname,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                val cm = ctx.getSystemService(ClipboardManager::class.java)
                                cm?.setPrimaryClip(ClipData.newPlainText("coachId", coachId))
                                scope.launch { snackbarHostState.showSnackbar("已複製教練ID") }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "複製教練ID")
                        }
                    }

                    // Nickname editor (compact, inline)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isEditingNickname) {
                            OutlinedTextField(
                                value = nicknameDraft,
                                onValueChange = { nicknameDraft = it },
                                modifier = Modifier.weight(1f).height(44.dp),
                                label = { Text("教練暱稱", style = MaterialTheme.typography.labelSmall) },
                                textStyle = MaterialTheme.typography.bodySmall,
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent,
                                )
                            )

                            TextButton(
                                onClick = {
                                    val newName = nicknameDraft.trim()
                                    scope.launch {
                                        val r = roleRepo.updateMyNickname(newName)
                                        if (r.isSuccess) {
                                            coachNickname = newName
                                            isEditingNickname = false
                                            snackbarHostState.showSnackbar("已儲存教練暱稱")
                                        } else {
                                            snackbarHostState.showSnackbar("儲存失敗：${r.exceptionOrNull()?.message ?: ""}")
                                        }
                                    }
                                },
                                enabled = nicknameDraft.trim().isNotEmpty(),
                                modifier = Modifier.height(44.dp)
                            ) { Text("儲存", style = MaterialTheme.typography.bodySmall) }

                            TextButton(
                                onClick = {
                                    nicknameDraft = coachNickname
                                    isEditingNickname = false
                                },
                                modifier = Modifier.height(44.dp)
                            ) { Text("取消", style = MaterialTheme.typography.bodySmall) }
                        } else {
                            Text(
                                text = if (coachNickname.isBlank()) "設定教練暱稱" else "編輯教練暱稱",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = {
                                    nicknameDraft = coachNickname
                                    isEditingNickname = true
                                },
                                modifier = Modifier.height(40.dp)
                            ) { Text("編輯", style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // ===== 新增/管理學員（同一頁即可完成，不用跳畫面） =====
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("學員名單", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "新增後可用於『單選發布』",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = {
                                scope.launch {
                                    val coachIdNow = coachId.ifBlank {
                                        FirebaseAuth.getInstance().currentUser?.uid ?: authRepository.ensureUserId()
                                    }
                                    traineeDirectoryRepo.listTrainees(coachIdNow)
                                        .onSuccess { list -> trainees.clear(); trainees.addAll(list) }
                                        .onFailure { e -> snackbarHostState.showSnackbar("讀取學員失敗：${e.message ?: ""}") }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "刷新學員")
                        }
                    }

                    // Compact add row（更小）
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = traineeIdInput,
                            onValueChange = { traineeIdInput = it },
                            modifier = Modifier.weight(1f).height(44.dp),
                            label = { Text("ID", style = MaterialTheme.typography.labelSmall) },
                            textStyle = MaterialTheme.typography.bodySmall,
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = traineeNameInput,
                            onValueChange = { traineeNameInput = it },
                            modifier = Modifier.weight(1f).height(44.dp),
                            label = { Text("暱稱", style = MaterialTheme.typography.labelSmall) },
                            textStyle = MaterialTheme.typography.bodySmall,
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                val tid = traineeIdInput.trim()
                                if (tid.isBlank()) return@Button
                                scope.launch {
                                    val coachIdNow = coachId.ifBlank {
                                        FirebaseAuth.getInstance().currentUser?.uid ?: authRepository.ensureUserId()
                                    }
                                    val name = traineeNameInput.trim()
                                    traineeDirectoryRepo.upsertTrainee(
                                        coachId = coachIdNow,
                                        traineeId = tid,
                                        displayName = name
                                    )
                                        .onSuccess {
                                            traineeIdInput = ""
                                            traineeNameInput = ""
                                            traineeDirectoryRepo.listTrainees(coachIdNow)
                                                .onSuccess { list ->
                                                    trainees.clear(); trainees.addAll(list)
                                                }
                                            snackbarHostState.showSnackbar("已新增/更新學員")
                                        }
                                        .onFailure { e ->
                                            snackbarHostState.showSnackbar("新增失敗：${e.message ?: ""}")
                                        }
                                }
                            },
                            modifier = Modifier.height(44.dp)
                        ) { Text("新增", style = MaterialTheme.typography.bodySmall) }
                    }

                    // ===== 學員名單內容 =====

                    // 學員列表改為下拉選擇（不影響你上方新增/移除的管理）
                    if (trainees.isNotEmpty()) {
                        ExposedDropdownMenuBox(
                            expanded = traineeDropdownExpanded,
                            onExpandedChange = { traineeDropdownExpanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val selected = trainees.firstOrNull { it.traineeId == selectedTraineeId }
                            val label = when {
                                selectedTraineeId == null -> "全部學員"
                                selected != null -> selected.displayName.ifBlank { selected.traineeId }
                                else -> "選擇學員"
                            }

                            OutlinedTextField(
                                value = label,
                                onValueChange = {},
                                readOnly = true,
                                textStyle = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                                label = { Text("學員", style = MaterialTheme.typography.labelSmall) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = traineeDropdownExpanded) },
                                singleLine = true
                            )

                            ExposedDropdownMenu(
                                expanded = traineeDropdownExpanded,
                                onDismissRequest = { traineeDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("全部學員", style = MaterialTheme.typography.bodySmall) },
                                    onClick = {
                                        selectedTraineeId = null
                                        traineeDropdownExpanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )

                                trainees.forEach { t ->
                                    val text = t.displayName.ifBlank { t.traineeId }
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        },
                                        onClick = {
                                            selectedTraineeId = t.traineeId
                                            traineeDropdownExpanded = false
                                        },
                                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                    )
                                }
                            }
                        }

                    }

                    if (trainees.isEmpty()) {
                        Text(
                            "尚未新增學員",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ===== 教練中心計畫：Create Plan（放在學員回報上方） =====
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("教練課表", style = MaterialTheme.typography.titleMedium)

                    // 显示计划列表
                    if (plans.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(plans) { plan ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp)
                                    ) {
                                        Text(
                                            text = plan.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "${plan.exercises.size} 個動作",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "尚未建立任何計畫",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = onNavigateToCreatePlan,
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Text("建立計畫")
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (filteredCompletions.isNotEmpty()) {
                Text("學員完成回報", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    val listState = rememberLazyListState()
                    val items = remember(filteredCompletions) {
                        filteredCompletions.sortedByDescending { it.completedAt }.take(50)
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(
                            items,
                            key = { _, c -> "${c.traineeId}_${c.completedAt.toEpochMilli()}_${c.planName}" }
                        ) { idx, c ->
                            if (idx > 0) {
                                HorizontalDivider()
                            }

                            val timeText = c.completedAt.atZone(ZoneId.systemDefault()).format(dtf)

                            // Prefer server nickname; fallback to coach-maintained trainee list; then (未命名)
                            val serverNick = traineeNicknameCache[c.traineeId].orEmpty().trim()
                            val localNick = traineeNameById[c.traineeId].orEmpty().trim()
                            val displayName = when {
                                serverNick.isNotBlank() -> serverNick
                                localNick.isNotBlank() -> localNick
                                else -> "（未命名）"
                            }

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "學員：$displayName",
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "已完成：${c.planName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = timeText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}