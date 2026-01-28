package com.example.fitness.ui.social

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.fitness.social.*
import com.example.fitness.ui.theme.TechColors
import com.example.fitness.ui.theme.glassEffect
import com.example.fitness.ui.theme.neonGlowBorder
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialCenterScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { SocialRepository() }
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("好友", "挑戰", "排行榜")

    var friends by remember { mutableStateOf<List<Friendship>>(emptyList()) }
    var challenges by remember { mutableStateOf<List<Challenge>>(emptyList()) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var showCreateChallengeDialog by remember { mutableStateOf(false) }

    // 載入好友列表
    LaunchedEffect(Unit) {
        repository.getFriends().collect { friends = it }
    }

    // 載入挑戰列表
    LaunchedEffect(Unit) {
        repository.getActiveChallenges().collect { challenges = it }
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
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassEffect(cornerRadius = 20.dp)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, null, tint = TechColors.NeonBlue)
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "社交中心",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TechColors.NeonBlue
                            )
                            Text(
                                "與好友一起健身",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(0.7f)
                            )
                        }

                        // Action buttons
                        when (selectedTab) {
                            0 -> IconButton(onClick = { showSearchDialog = true }) {
                                Icon(Icons.Default.PersonAdd, null, tint = TechColors.NeonBlue)
                            }
                            1 -> IconButton(onClick = { showCreateChallengeDialog = true }) {
                                Icon(Icons.Default.Add, null, tint = TechColors.NeonBlue)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = TechColors.NeonBlue
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    title,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Content
                when (selectedTab) {
                    0 -> FriendsTab(friends, repository, scope, snackbarHostState)
                    1 -> ChallengesTab(challenges, repository, scope, snackbarHostState)
                    2 -> LeaderboardTab(repository, scope)
                }
            }

            // Dialogs
            if (showSearchDialog) {
                SearchUserDialog(
                    repository = repository,
                    onDismiss = { showSearchDialog = false },
                    onUserSelected = { userId, userName ->
                        scope.launch {
                            val result = repository.sendFriendRequest(userId, userName)
                            if (result.isSuccess) {
                                snackbarHostState.showSnackbar("已發送好友邀請")
                            } else {
                                snackbarHostState.showSnackbar("發送失敗")
                            }
                            showSearchDialog = false
                        }
                    }
                )
            }

            if (showCreateChallengeDialog) {
                CreateChallengeDialog(
                    repository = repository,
                    onDismiss = { showCreateChallengeDialog = false },
                    onCreated = {
                        showCreateChallengeDialog = false
                        scope.launch {
                            snackbarHostState.showSnackbar("挑戰創建成功！")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun FriendsTab(
    friends: List<Friendship>,
    repository: SocialRepository,
    scope: kotlinx.coroutines.CoroutineScope,
    snackbarHostState: SnackbarHostState
) {
    if (friends.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .glassEffect(20.dp)
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.People,
                    null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.White.copy(0.5f)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "還沒有好友",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    "點擊右上角新增好友",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(0.7f)
                )
            }
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(friends) { friend ->
                FriendCard(friend)
            }
        }
    }
}

@Composable
fun FriendCard(friend: Friendship) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassEffect(16.dp)
            .clickable { /* Navigate to friend profile */ }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(TechColors.NeonBlue.copy(0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    friend.friendName.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TechColors.NeonBlue
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    friend.friendName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "好友自 ${friend.createdAt.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(0.6f)
                )
            }

            Icon(
                Icons.Default.Message,
                null,
                tint = TechColors.NeonBlue,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun ChallengesTab(
    challenges: List<Challenge>,
    repository: SocialRepository,
    scope: kotlinx.coroutines.CoroutineScope,
    snackbarHostState: SnackbarHostState
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(challenges) { challenge ->
            ChallengeCard(
                challenge = challenge,
                onJoin = {
                    scope.launch {
                        val result = repository.joinChallenge(challenge.id)
                        if (result.isSuccess) {
                            snackbarHostState.showSnackbar("已加入挑戰！")
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun ChallengeCard(
    challenge: Challenge,
    onJoin: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassEffect(16.dp)
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        challenge.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TechColors.NeonBlue
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        challenge.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(0.8f)
                    )
                }

                Chip(challenge.type.name)
            }

            Spacer(Modifier.height(12.dp))

            // Goal
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Flag,
                    null,
                    tint = TechColors.NeonBlue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "目標: ${challenge.goal.target} ${challenge.goal.unit}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }

            Spacer(Modifier.height(8.dp))

            // Participants
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.People,
                    null,
                    tint = Color.White.copy(0.7f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "${challenge.participants.size} 人參與",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(0.7f)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Join button
            Button(
                onClick = onJoin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .neonGlowBorder(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TechColors.NeonBlue.copy(0.2f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("加入挑戰", fontWeight = FontWeight.Bold, color = TechColors.NeonBlue)
            }
        }
    }
}

@Composable
fun Chip(text: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = TechColors.NeonBlue.copy(0.2f)
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = TechColors.NeonBlue,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun LeaderboardTab(
    repository: SocialRepository,
    scope: kotlinx.coroutines.CoroutineScope
) {
    var leaderboard by remember { mutableStateOf<List<LeaderboardEntry>>(emptyList()) }
    var selectedMetric by remember { mutableStateOf(ChallengeMetric.TOTAL_CALORIES_BURNED) }

    LaunchedEffect(selectedMetric) {
        scope.launch {
            val result = repository.getGlobalLeaderboard(selectedMetric)
            if (result.isSuccess) {
                leaderboard = result.getOrDefault(emptyList())
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Metric selector
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassEffect(12.dp)
                .padding(12.dp)
        ) {
            Text(
                "總消耗卡路里排行榜",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(Modifier.height(16.dp))

        // Leaderboard list
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(leaderboard.size) { index ->
                LeaderboardEntryCard(leaderboard[index])
            }
        }
    }
}

@Composable
fun LeaderboardEntryCard(entry: LeaderboardEntry) {
    val rankColor = when (entry.rank) {
        1 -> Color(0xFFFFD700) // Gold
        2 -> Color(0xFFC0C0C0) // Silver
        3 -> Color(0xFFCD7F32) // Bronze
        else -> Color.White.copy(0.7f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassEffect(12.dp)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(rankColor.copy(0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${entry.rank}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = rankColor
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.userName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "${entry.score.toInt()} ${entry.metric}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(0.7f)
                )
            }

            if (entry.rank <= 3) {
                Icon(
                    Icons.Default.Star,
                    null,
                    tint = rankColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun SearchUserDialog(
    repository: SocialRepository,
    onDismiss: () -> Unit,
    onUserSelected: (String, String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TechColors.DarkBlue.copy(0.95f),
        title = {
            Text("搜尋好友", fontWeight = FontWeight.Bold, color = TechColors.NeonBlue)
        },
        text = {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        if (it.length >= 2) {
                            scope.launch {
                                val result = repository.searchUsers(it)
                                searchResults = result.getOrDefault(emptyList())
                            }
                        }
                    },
                    label = { Text("輸入用戶名稱") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = TechColors.NeonBlue,
                        unfocusedBorderColor = Color.White.copy(0.5f)
                    )
                )

                Spacer(Modifier.height(12.dp))

                searchResults.forEach { user ->
                    TextButton(
                        onClick = { onUserSelected(user.id, user.displayName) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(user.displayName, color = Color.White)
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
fun CreateChallengeDialog(
    repository: SocialRepository,
    onDismiss: () -> Unit,
    onCreated: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TechColors.DarkBlue.copy(0.95f),
        title = {
            Text("創建挑戰", fontWeight = FontWeight.Bold, color = TechColors.NeonBlue)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("挑戰名稱") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = TechColors.NeonBlue,
                        unfocusedBorderColor = Color.White.copy(0.5f)
                    )
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("描述") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = TechColors.NeonBlue,
                        unfocusedBorderColor = Color.White.copy(0.5f)
                    )
                )

                OutlinedTextField(
                    value = target,
                    onValueChange = { if (it.all { c -> c.isDigit() }) target = it },
                    label = { Text("目標數值") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = TechColors.NeonBlue,
                        unfocusedBorderColor = Color.White.copy(0.5f)
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launch {
                        val challenge = Challenge(
                            id = "",
                            title = title,
                            description = description,
                            type = ChallengeType.WEEKLY,
                            goal = ChallengeGoal(
                                metric = ChallengeMetric.TOTAL_CALORIES_BURNED,
                                target = target.toDoubleOrNull() ?: 0.0,
                                unit = "kcal"
                            ),
                            startDate = java.time.Instant.now(),
                            endDate = java.time.Instant.now().plusSeconds(7 * 24 * 3600),
                            createdBy = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
                        )

                        repository.createChallenge(challenge)
                        onCreated()
                    }
                }
            ) {
                Text("創建", color = TechColors.NeonBlue, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Color.White.copy(0.7f))
            }
        }
    )
}
