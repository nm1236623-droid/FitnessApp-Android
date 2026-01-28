package com.example.fitness.user

import com.example.fitness.coach.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class UserRoleProfileRepository(
    private val db: FirebaseFirestore = Firebase.firestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {

    // Internal DTO for Type-Safe Firestore reads/writes
    private data class UserProfileDto(
        val role: String? = null,
        val nickname: String? = null,
        val createdAtEpochMs: Any? = null,
        val updatedAtEpochMs: Any? = null
    )

    suspend fun getMyNickname(): Result<String> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Not signed in"))

        return try {
            val snap = db.collection("users").document(uid).get().await()
            val nickname = snap.getString("nickname")?.trim().orEmpty()
            Result.success(nickname)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun updateMyNickname(nickname: String): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Not signed in"))

        return try {
            val updates = mapOf(
                "nickname" to nickname.trim(),
                "updatedAtEpochMs" to FieldValue.serverTimestamp()
            )

            db.collection("users").document(uid)
                .set(updates, SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun upsertMyRole(role: UserRole, nickname: String? = null): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Not signed in"))

        return try {
            val docRef = db.collection("users").document(uid)
            val cleanNickname = nickname?.trim()

            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)

                if (!snapshot.exists()) {
                    val newUser = UserProfileDto(
                        role = role.name,
                        nickname = cleanNickname ?: "",
                        createdAtEpochMs = FieldValue.serverTimestamp(),
                        updatedAtEpochMs = FieldValue.serverTimestamp()
                    )
                    transaction.set(docRef, newUser)
                } else {
                    val updates = mutableMapOf<String, Any>(
                        "role" to role.name,
                        "updatedAtEpochMs" to FieldValue.serverTimestamp()
                    )
                    if (!cleanNickname.isNullOrBlank()) {
                        updates["nickname"] = cleanNickname
                    }
                    transaction.set(docRef, updates, SetOptions.merge())
                }
            }.await()

            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun getMyRole(): Result<UserRole> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Not signed in"))

        return try {
            val snap = db.collection("users").document(uid).get().await()

            if (!snap.exists()) {
                return Result.success(UserRole.TRAINEE)
            }

            val roleStr = snap.getString("role")
            val role = try {
                if (roleStr != null) UserRole.valueOf(roleStr) else UserRole.TRAINEE
            } catch (e: IllegalArgumentException) {
                UserRole.TRAINEE
            }
            Result.success(role)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun getNicknameByUid(uid: String): Result<String> {
        if (uid.isBlank()) return Result.success("")

        return try {
            val snap = db.collection("users").document(uid).get().await()
            Result.success(snap.getString("nickname")?.trim().orEmpty())
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}