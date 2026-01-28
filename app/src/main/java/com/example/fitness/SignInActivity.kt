package com.example.fitness

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.example.fitness.auth.SessionManager
import com.example.fitness.auth.SessionRepository
import com.example.fitness.coach.UserRole
import com.example.fitness.firebase.AuthRepository
import com.example.fitness.ui.SignInScreen
import com.example.fitness.user.UserRoleProfileRepository
// 確保這三個 Import 存在 (如果 Sync Gradle 後還是紅字，請檢查第一步是否成功)
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

class SignInActivity : ComponentActivity() {

    private val authRepository = AuthRepository()
    private val roleRepository = UserRoleProfileRepository()

    // 儲存使用者選擇的角色（用於 Google 新帳號註冊）
    private var selectedRoleForNewAccount: UserRole = UserRole.TRAINEE

    private val sessionManager by lazy {
        SessionManager(authRepository, SessionRepository(this.applicationContext))
    }

    // Google 登入選項設定
    private val googleSignInOptions by lazy {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }

    // Google 登入客戶端
    private val googleSignInClient by lazy {
        GoogleSignIn.getClient(this, googleSignInOptions)
    }

    // 處理 Google 登入結果
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // 使用 try-catch 避免取消登入時崩潰
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken

            if (idToken != null) {
                firebaseAuthWithGoogle(idToken)
            } else {
                showToast("Google ID Token is null")
            }
        } catch (e: ApiException) {
            // 狀態碼 12500 通常是 SHA-1 指紋沒設好
            // 狀態碼 12501 通常是使用者取消登入
            showToast("Google sign in failed code: ${e.statusCode}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            val user = authRepository.currentUser()
            if (user != null && sessionManager.validateOrLogout()) {
                navigateToMain()
            }
        }

        setContent {
            SignInScreen(
                authRepository = authRepository,
                // ★ 修正點：這裡原本直接呼叫 suspend 函式導致報錯
                onSignedIn = {
                    lifecycleScope.launch {
                        sessionManager.touchIfSignedIn()
                        navigateToMain()
                    }
                },
                onGoogleSignIn = { selectedRole ->
                    // 儲存用戶選擇的角色
                    selectedRoleForNewAccount = selectedRole
                    launchGoogleSignIn()
                },
                onPhoneSignIn = {
                    showToast("Phone sign in coming soon!")
                }
            )
        }
    }

    private fun launchGoogleSignIn() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        lifecycleScope.launch {
            val result = authRepository.firebaseSignInWithGoogle(idToken)

            result.fold(
                onSuccess = { user ->
                    if (user != null) {
                        // 檢查是否為新用戶（首次登入）
                        lifecycleScope.launch {
                            try {
                                val existingRole = roleRepository.getMyRole()

                                // 如果沒有角色資料，表示是新用戶，需要儲存選擇的角色
                                if (existingRole == null) {
                                    val saveResult = roleRepository.upsertMyRole(
                                        role = selectedRoleForNewAccount,
                                        nickname = user.displayName
                                    )

                                    if (saveResult.isSuccess) {
                                        showToast("Welcome ${user.displayName} - Role: ${selectedRoleForNewAccount.name}")
                                    } else {
                                        showToast("Account created but failed to save profile")
                                    }
                                } else {
                                    showToast("Welcome back ${user.displayName}")
                                }

                                sessionManager.touchIfSignedIn()
                                navigateToMain()
                            } catch (e: Exception) {
                                showToast("Error checking user profile: ${e.message}")
                                navigateToMain()
                            }
                        }
                    }
                },
                onFailure = { e ->
                    showToast("Firebase Auth failed: ${e.message}")
                }
            )
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}