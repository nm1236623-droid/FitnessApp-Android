package com.example.fitness.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.fitness.coach.UserRole
import com.example.fitness.firebase.AuthRepository
import com.example.fitness.ui.theme.TechColors
import com.example.fitness.ui.theme.glassEffect
import com.example.fitness.ui.theme.neonGlowBorder
import com.example.fitness.user.UserRoleProfileRepository
import kotlinx.coroutines.launch

@Composable
fun SignUpScreen(
    modifier: Modifier = Modifier,
    authRepository: AuthRepository = remember { AuthRepository() },
    onSignedUp: () -> Unit = {},
    onNavigateToSignIn: () -> Unit = {},
    // ★ 修改：讓 Callback 可以接收選擇的角色
    onGoogleSignUp: (UserRole) -> Unit = {},
    onPhoneSignUp: (UserRole) -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // 角色選擇 (預設為學員)
    var selectedRole by remember { mutableStateOf(UserRole.TRAINEE) }

    // 頭像處理
    var avatarBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // --- Launchers (相機/相簿邏輯) ---
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp: Bitmap? ->
        if (bmp != null) {
            avatarBitmap = bmp
            scope.launch { snackbarHostState.showSnackbar("Avatar captured") }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                @Suppress("DEPRECATION")
                val bmp = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                avatarBitmap = bmp
                scope.launch { snackbarHostState.showSnackbar("Avatar selected") }
            } catch (t: Throwable) {
                scope.launch { snackbarHostState.showSnackbar("Failed to load image") }
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) cameraLauncher.launch(null)
        else scope.launch { snackbarHostState.showSnackbar("Camera permission denied") }
    }

    var avatarChooserOpen by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(TechColors.DarkBlue, TechColors.DeepPurple)))
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // --- Header ---
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
                        IconButton(onClick = onNavigateToSignIn) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TechColors.NeonBlue)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("CREATE ACCOUNT", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TechColors.NeonBlue, letterSpacing = 1.sp)
                            Text("Join the Future of Fitness", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- Main Form Card ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassEffect(cornerRadius = 24.dp)
                        .padding(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {

                        // --- Avatar Circle ---
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(110.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .border(2.dp, TechColors.NeonBlue, CircleShape)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (avatarBitmap != null) {
                                    Image(bitmap = avatarBitmap!!.asImageBitmap(), contentDescription = "Avatar", modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                                } else {
                                    Icon(Icons.Default.Person, "Placeholder", tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = (-4).dp, y = (-4).dp)
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(TechColors.NeonBlue)
                                    .clickable { avatarChooserOpen = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CameraAlt, "Edit", tint = Color.Black, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // --- Text Fields & Role ---
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            NeonSignUpTextField(value = nickname, onValueChange = { nickname = it }, label = "Nickname", placeholder = "Your display name")

                            // Role Selection
                            Text("SELECT ROLE", style = MaterialTheme.typography.labelSmall, color = TechColors.NeonBlue, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                NeonRoleOption("Trainee", selectedRole == UserRole.TRAINEE, { selectedRole = UserRole.TRAINEE }, Modifier.weight(1f))
                                NeonRoleOption("Coach", selectedRole == UserRole.COACH, { selectedRole = UserRole.COACH }, Modifier.weight(1f))
                            }

                            NeonSignUpTextField(value = email, onValueChange = { email = it }, label = "Email", placeholder = "name@example.com")
                            NeonSignUpTextField(value = password, onValueChange = { password = it }, label = "Password", isPassword = true, placeholder = "Min 6 characters")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (error != null) {
                            Text(error!!, color = Color(0xFFFF6B6B), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))
                        }

                        // --- Email Sign Up Button ---
                        Button(
                            onClick = {
                                error = null
                                if (email.isBlank() || password.length < 6) {
                                    error = "Invalid email or password (min 6 chars)"
                                    return@Button
                                }
                                loading = true
                                scope.launch {
                                    try {
                                        val res = authRepository.signUpWithEmail(email.trim(), password)
                                        if (res.isSuccess && res.getOrNull() != null) {
                                            // Email 註冊成功後，儲存角色資料
                                            val roleRepo = UserRoleProfileRepository()
                                            val saveRole = roleRepo.upsertMyRole(role = selectedRole, nickname = nickname.takeIf { it.isNotBlank() })
                                            if (saveRole.isFailure) {
                                                error = "Failed to save profile"
                                                loading = false
                                                return@launch
                                            }
                                            authRepository.signOut()
                                            scope.launch { snackbarHostState.showSnackbar("Account created! Please sign in.") }
                                            onSignedUp()
                                            onNavigateToSignIn()
                                        } else {
                                            error = res.exceptionOrNull()?.localizedMessage ?: "Sign up failed"
                                        }
                                    } catch (e: Exception) {
                                        error = e.localizedMessage
                                    } finally {
                                        loading = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .neonGlowBorder(cornerRadius = 16.dp),
                            enabled = !loading,
                            colors = ButtonDefaults.buttonColors(containerColor = TechColors.NeonBlue.copy(alpha = 0.2f), contentColor = TechColors.NeonBlue, disabledContainerColor = Color.White.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            if (loading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = TechColors.NeonBlue, strokeWidth = 2.dp)
                            } else {
                                Text("SIGN UP", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // --- ★★★ 社交帳號註冊區塊 (傳遞選擇的角色) ★★★ ---
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.2f))
                            Text(
                                text = "OR CONNECT WITH",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.2f))
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Google Button
                            NeonSocialButton(
                                text = "Google",
                                icon = null,
                                onClick = { onGoogleSignUp(selectedRole) }, // ★ 傳遞選中的角色
                                modifier = Modifier.weight(1f)
                            )

                            // ★ Phone Button
                            NeonSocialButton(
                                text = "Phone",
                                icon = Icons.Default.Smartphone,
                                onClick = { onPhoneSignUp(selectedRole) }, // ★ 傳遞選中的角色 (雖然電話登入頁面有自己的選擇，但這裡傳過去也無妨或可作為預設值)
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Already have an account?", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = onNavigateToSignIn) {
                        Text("Sign In", color = TechColors.NeonBlue, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Avatar Chooser Dialog (保持不變)
    if (avatarChooserOpen) {
        AlertDialog(
            onDismissRequest = { avatarChooserOpen = false },
            containerColor = TechColors.DarkBlue.copy(alpha = 0.95f),
            titleContentColor = TechColors.NeonBlue,
            textContentColor = Color.White,
            title = { Text("Profile Photo", fontWeight = FontWeight.Bold) },
            text = { Text("Choose a source for your avatar") },
            confirmButton = {
                TextButton(onClick = {
                    avatarChooserOpen = false
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        cameraLauncher.launch(null)
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }) { Text("Camera", color = TechColors.NeonBlue) }
            },
            dismissButton = {
                TextButton(onClick = {
                    avatarChooserOpen = false
                    galleryLauncher.launch("image/*")
                }) { Text("Gallery", color = Color.White.copy(alpha = 0.7f)) }
            }
        )
    }
}

// ... (NeonSocialButton, NeonSignUpTextField, NeonRoleOption 等元件保持不變) ...
@Composable
private fun NeonSocialButton(
    text: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(50.dp)
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.05f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else if (text == "Google") {
                Text(
                    text = "G",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            Text(
                text = text,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NeonSignUpTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String? = null,
    isPassword: Boolean = false
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it, color = Color.White.copy(alpha = 0.3f)) } },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = if (isPassword) KeyboardOptions(keyboardType = KeyboardType.Password) else KeyboardOptions.Default,
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle password",
                        tint = TechColors.NeonBlue
                    )
                }
            }
        } else null,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = TechColors.NeonBlue,
            unfocusedBorderColor = TechColors.NeonBlue.copy(alpha = 0.3f),
            focusedLabelColor = TechColors.NeonBlue,
            unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
            cursorColor = TechColors.NeonBlue,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent
        )
    )
}

@Composable
private fun NeonRoleOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) TechColors.NeonBlue.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f)
            )
            .border(
                1.dp,
                if (selected) TechColors.NeonBlue else Color.Transparent,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) TechColors.NeonBlue else Color.White.copy(alpha = 0.6f),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}