package com.example.fitness.auth

import android.util.Log
import com.example.fitness.firebase.AuthRepository
import com.google.firebase.auth.FirebaseUser

class SessionManager(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository,
    private val nowMs: () -> Long = { System.currentTimeMillis() },
) {
    companion object {
        // 30 days
        private const val SESSION_MAX_INACTIVE_MS: Long = 30L * 24L * 60L * 60L * 1000L
    }

    /**
     * Returns true if the session is valid and user can remain signed in.
     * If invalid/expired, it signs out and clears stored session metadata.
     */
    suspend fun validateOrLogout(): Boolean {
        val user: FirebaseUser? = authRepository.currentUser()
        if (user == null) {
            Log.d("SessionManager", "validateOrLogout: no firebase user -> signed out")
            sessionRepository.clear()
            return false
        }

        // If uid changed (e.g., switch user), treat as expired and force clean state.
        val storedUid = sessionRepository.getUid()
        if (storedUid != null && storedUid != user.uid) {
            Log.w("SessionManager", "validateOrLogout: uid mismatch stored=$storedUid current=${user.uid} -> logout")
            logout()
            return false
        }

        val lastActive = sessionRepository.getLastActiveEpochMillis()
        // If we have no stored timestamp, default to treating it as "active now" to avoid
        // unexpectedly logging out existing users after upgrading.
        if (lastActive == null) {
            Log.d("SessionManager", "validateOrLogout: lastActive missing -> treat active now")
            sessionRepository.setUid(user.uid)
            sessionRepository.setLastActive(nowMs())
            return true
        }

        val delta = nowMs() - lastActive
        // If device clock moved backwards, don't expire.
        val effectiveDelta = if (delta < 0) 0 else delta
        if (effectiveDelta > SESSION_MAX_INACTIVE_MS) {
            Log.w("SessionManager", "validateOrLogout: expired effectiveDelta=$effectiveDelta ms -> logout")
            logout()
            return false
        }

        Log.d("SessionManager", "validateOrLogout: ok effectiveDelta=$effectiveDelta ms")
        return true
    }

    /** Call when user performs an authenticated action or app comes to foreground. */
    suspend fun touchIfSignedIn() {
        val user = authRepository.currentUser() ?: return
        sessionRepository.setUid(user.uid)
        sessionRepository.setLastActive(nowMs())
    }

    suspend fun logout() {
        authRepository.signOut()
        sessionRepository.clear()
    }
}
