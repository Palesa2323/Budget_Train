package com.example.budgettrain.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository {
    private val auth = FirebaseAuth.getInstance()

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val currentUserId: String?
        get() = auth.currentUser?.uid

    suspend fun register(email: String, password: String, username: String): Result<FirebaseUser> {
        return try {
            android.util.Log.d("FirebaseAuth", "Attempting to register user: $email")
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            android.util.Log.d("FirebaseAuth", "User registered successfully with UID: ${result.user?.uid}")
            // Store username in Firestore user document
            result.user?.let { user ->
                FirebaseRepository.updateUserProfile(user.uid, username, email)
            }
            Result.success(result.user!!)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseAuth", "Registration failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        auth.signOut()
    }

    fun isLoggedIn(): Boolean {
        return auth.currentUser != null
    }
}

