package com.example.fitness.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider // 記得加這行
import kotlinx.coroutines.tasks.await

class AuthRepository(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) {

    fun currentUser(): FirebaseUser? = auth.currentUser

    suspend fun signUpWithEmail(email: String, password: String): Result<FirebaseUser?> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            Result.success(result.user)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser?> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(result.user)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    // ★ 新增這個函式來支援 Google 登入
    suspend fun firebaseSignInWithGoogle(idToken: String): Result<FirebaseUser?> {
        return try {
            // 1. 將 Google 傳來的 idToken 轉換成 Firebase 憑證
            val credential = GoogleAuthProvider.getCredential(idToken, null)

            // 2. 用這個憑證登入 Firebase
            val result = auth.signInWithCredential(credential).await()
            Result.success(result.user)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    fun signOut() {
        auth.signOut()
    }
}