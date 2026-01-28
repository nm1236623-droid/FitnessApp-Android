package com.example.fitness.ui

import android.util.Patterns
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitness.auth.RememberMeRepository
import com.example.fitness.coach.UserRole
import com.example.fitness.firebase.AuthRepository
import com.example.fitness.ui.theme.TechColors
import com.example.fitness.ui.theme.glassEffect
import com.example.fitness.ui.theme.neonGlowBorder
import com.example.fitness.user.UserRoleProfileRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreen(
    modifier: Modifier = Modifier,
    authRepository: AuthRepository = AuthRepository(),
    onSignedIn: () -> Unit,
    // Google 登入接收 UserRole
    onGoogleSignIn: (UserRole) -> Unit = {},
    // Phone 登入不需接收 UserRole (因為 Activity 內部有選擇)
    onPhoneSignIn: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var showSignUp by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    val ctx = LocalContext.current
    val appCtx = remember(ctx) { ctx.applicationContext }
    val rememberMeRepo = remember(appCtx) { RememberMeRepository(appCtx) }

    val roleRepo = remember { UserRoleProfileRepository() }

    var rememberMeEnabled by remember { mutableStateOf(true) }

    // 角色選擇狀態 (預設學員)
    var selectedRole by remember { mutableStateOf(UserRole.TRAINEE) }

    LaunchedEffect(Unit) {
        rememberMeEnabled = rememberMeRepo.isRememberMeEnabled()
    }

    var showForgotPassword by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var sendingReset by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(TechColors.DarkBlue, TechColors.DeepPurple)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .glassEffect(cornerRadius = 24.dp)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome Back",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = TechColors.NeonBlue
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Sign in to access your fitness journey",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )

            Spacer(Modifier.height(24.dp))

            var email by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }
            var passwordVisible by remember { mutableStateOf(false) }
            var loading by remember { mutableStateOf(false) }

            NeonSignInTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email",
                icon = { Icon(Icons.Default.Email, contentDescription = null, tint = TechColors.NeonBlue) }
            )

            Spacer(Modifier.height(16.dp))

            NeonSignInTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TechColors.NeonBlue) },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = TechColors.NeonBlue.copy(alpha = 0.7f)
                        )
                    }
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        resetEmail = email.trim()
                        showForgotPassword = true
                    },
                    enabled = !loading && !sendingReset
                ) {
                    Text("Forgot password?", color = TechColors.NeonBlue.copy(alpha = 0.8f))
                }
            }

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        val newValue = !rememberMeEnabled
                        rememberMeEnabled = newValue
                        scope.launch { rememberMeRepo.setRememberMeEnabled(newValue) }
                    }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = rememberMeEnabled,
                    onCheckedChange = { checked ->
                        rememberMeEnabled = checked
                        scope.launch { rememberMeRepo.setRememberMeEnabled(checked) }
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = TechColors.NeonBlue,
                        uncheckedColor = Color.White.copy(alpha = 0.6f),
                        checkmarkColor = Color.Black
                    )
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Remember me",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Text(
                        text = "保持登入狀態（30 天未活躍自動登出）",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    loading = true
                    scope.launch {
                        val result = authRepository.signInWithEmail(email.trim(), password)

                        if (result.isSuccess && result.getOrNull() != null) {
                            roleRepo.getMyRole()
                            if (!rememberMeEnabled) {
                                authRepository.signOut()
                            }
                            loading = false
                            onSignedIn()
                        } else {
                            loading = false
                            snackbarHostState.showSnackbar(result.exceptionOrNull()?.localizedMessage ?: "Sign in failed")
                        }
                    }
                },
                enabled = !loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .neonGlowBorder(cornerRadius = 12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TechColors.NeonBlue.copy(alpha = 0.2f),
                    contentColor = TechColors.NeonBlue,
                    disabledContainerColor = Color.Gray.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = TechColors.NeonBlue,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Signing In...")
                } else {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Sign In", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.2f))
                Text(
                    text = "OR CONTINUE WITH",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.2f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 角色選擇區 (針對 Google 登入新用戶)
            Text(
                text = "SELECT ROLE FOR NEW ACCOUNT",
                style = MaterialTheme.typography.labelSmall,
                color = TechColors.NeonBlue,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NeonSignInRoleOption(
                    text = "Trainee",
                    selected = selectedRole == UserRole.TRAINEE,
                    onClick = { selectedRole = UserRole.TRAINEE },
                    modifier = Modifier.weight(1f)
                )
                NeonSignInRoleOption(
                    text = "Coach",
                    selected = selectedRole == UserRole.COACH,
                    onClick = { selectedRole = UserRole.COACH },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Google Button
                NeonSocialButton(
                    text = "Google",
                    icon = null,
                    onClick = { onGoogleSignIn(selectedRole) },
                    modifier = Modifier.weight(1f)
                )

                // Phone Button
                NeonSocialButton(
                    text = "Phone",
                    icon = Icons.Default.Smartphone,
                    onClick = onPhoneSignIn,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(24.dp))

            OutlinedButton(
                onClick = { showSignUp = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                border = BorderStroke(
                    1.dp,
                    Brush.linearGradient(
                        listOf(
                            TechColors.NeonBlue.copy(alpha = 0.5f),
                            TechColors.NeonBlue.copy(alpha = 0.1f)
                        )
                    )
                ),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = TechColors.NeonBlue)
                Spacer(Modifier.width(8.dp))
                Text("Create Account")
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) { data ->
            Snackbar(
                containerColor = TechColors.NeonBlue,
                contentColor = Color.Black,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(data.visuals.message, fontWeight = FontWeight.Bold)
            }
        }

        if (showForgotPassword) {
            AlertDialog(
                onDismissRequest = { if (!sendingReset) showForgotPassword = false },
                containerColor = TechColors.DarkBlue.copy(alpha = 0.95f),
                titleContentColor = TechColors.NeonBlue,
                textContentColor = Color.White,
                title = { Text("Reset password", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Enter your email. We'll send you a password reset link.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        NeonSignInTextField(
                            value = resetEmail,
                            onValueChange = { resetEmail = it },
                            label = "Email",
                            icon = { Icon(Icons.Default.Email, contentDescription = null, tint = TechColors.NeonBlue) },
                            enabled = !sendingReset
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = !sendingReset,
                        onClick = {
                            val emailToSend = resetEmail.trim()
                            if (emailToSend.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(emailToSend).matches()) {
                                scope.launch { snackbarHostState.showSnackbar("Please enter a valid email") }
                                return@TextButton
                            }
                            sendingReset = true
                            scope.launch {
                                val result = authRepository.sendPasswordResetEmail(emailToSend)
                                sendingReset = false
                                if (result.isSuccess) {
                                    showForgotPassword = false
                                    snackbarHostState.showSnackbar("Reset email sent")
                                } else {
                                    snackbarHostState.showSnackbar(
                                        result.exceptionOrNull()?.localizedMessage ?: "Failed to send reset email"
                                    )
                                }
                            }
                        }
                    ) {
                        if (sendingReset) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = TechColors.NeonBlue)
                        } else {
                            Text("Send", color = TechColors.NeonBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        enabled = !sendingReset,
                        onClick = { showForgotPassword = false }
                    ) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                    }
                }
            )
        }

        if (showSignUp) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .clickable { showSignUp = false },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .wrapContentHeight()
                        .clickable(enabled = false) {}
                ) {
                    SignUpScreen(
                        authRepository = authRepository,
                        onSignedUp = {},
                        onNavigateToSignIn = { showSignUp = false },
                        modifier = Modifier.fillMaxWidth(),
                        // ★★★ 修正這裡的語法錯誤 ★★★
                        // 這裡接收 SignUpScreen 傳回來的 role，並呼叫外部傳入的 Callback
                        onGoogleSignUp = { role -> onGoogleSignIn(role) },
                        // 電話登入雖然不需要傳角色（因為有自己的頁面），但為了符合介面定義，我們接收 role 但忽略它
                        onPhoneSignUp = { _ -> onPhoneSignIn() }
                    )
                }
            }
        }
    }
}

// --- Local Components ---

@Composable
private fun NeonSignInRoleOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) TechColors.NeonBlue.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f)
            )
            .border(
                1.dp,
                if (selected) TechColors.NeonBlue else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) TechColors.NeonBlue else Color.White.copy(alpha = 0.6f),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

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

@Composable
private fun NeonSignInTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        label = { Text(label) },
        leadingIcon = icon,
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = TechColors.NeonBlue,
            unfocusedBorderColor = TechColors.NeonBlue.copy(alpha = 0.5f),
            focusedLabelColor = TechColors.NeonBlue,
            unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
            cursorColor = TechColors.NeonBlue,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledBorderColor = Color.Gray.copy(alpha = 0.3f),
            disabledTextColor = Color.Gray
        )
    )
}