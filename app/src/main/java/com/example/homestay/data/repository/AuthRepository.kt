package com.example.homestay

import android.content.Context
import android.util.Log
import com.example.homestay.data.local.UserDao
import com.example.homestay.data.local.UserEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.example.homestay.data.local.UserDatabase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class UserProfile(
    val userId: String = "",
    val username: String = "",
    val email: String = "",
    val gender: String = "",
    val birthdate: String = "",
    val role: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class SignUpRequest(
    val username: String,
    val email: String,
    val password: String,
    val gender: String,
    val birthdate: String,
    val role: String
)

sealed class AuthResult<out T> {
    data class Success<out T>(val data: T) : AuthResult<T>()
    data class Error(val exception: Exception) : AuthResult<Nothing>()
    object Loading : AuthResult<Nothing>()
}

class AuthRepository(private val context: Context) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val userDao: UserDao = UserDatabase.getDatabase(context).userDao()

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val USERNAME_FIELD = "username"
    }

    // Get current authenticated user
    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    // Check if user is logged in
    fun isUserLoggedIn(): Boolean = getCurrentUser() != null

    // LOGIN
    suspend fun login(email: String, password: String): AuthResult<FirebaseUser?> {
        Log.d(
            "FirebaseDebug",
            "Firebase project ID = ${FirebaseFirestore.getInstance().app.options.projectId}"
        )

        return try {
            // Try Firebase login
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.let { user ->
                // sync to local database
                val profileResult = getUserProfile(user.uid)
                if (profileResult is AuthResult.Success) {
                    saveUserToLocal(profileResult.data, password)
                }
                AuthResult.Success(user)
            } ?: AuthResult.Error(Exception("Login failed: User is null"))
        } catch (e: Exception) {
            // Try local database
            val localUser = userDao.getUserByEmail(email)
            if (localUser != null && localUser.password == password) {
                AuthResult.Success(null) // offline login works
            } else {
                AuthResult.Error(e)
            }
        }
    }

    // Fetch by email for offline login
    suspend fun getLocalUserByEmail(email: String): UserEntity? {
        return userDao.getUserByEmail(email)
    }

    // SIGNUP
    suspend fun signUp(signUpRequest: SignUpRequest): AuthResult<FirebaseUser> {
        return try {
            if (!isUsernameAvailable(signUpRequest.username)) {
                return AuthResult.Error(Exception("Username is already taken"))
            }

            val authResult = auth.createUserWithEmailAndPassword(
                signUpRequest.email,
                signUpRequest.password
            ).await()

            val user = authResult.user
            if (user != null) {
                val userProfile = UserProfile(
                    userId = user.uid,
                    username = signUpRequest.username,
                    email = signUpRequest.email,
                    gender = signUpRequest.gender,
                    birthdate = signUpRequest.birthdate,
                    role = signUpRequest.role
                )

                try {
                    saveUserProfile(userProfile)
                    saveUserToLocal(userProfile, signUpRequest.password)
                    return AuthResult.Success(user)
                } catch (e: Exception) {
                    android.util.Log.e("AuthRepository", "Firestore write failed", e)
                    return AuthResult.Error(Exception("Firestore write failed: ${e.message}"))
                }
            } else {
                return AuthResult.Error(Exception("Failed to create user account"))
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Signup failed", e)
            return AuthResult.Error(Exception("Signup failed: ${e.message}"))
        }
    }

    //Insertion (local db)
    suspend fun saveUserToLocal(userProfile: UserProfile, password: String = "") {
        val entity = UserEntity(
            userId = userProfile.userId,
            username = userProfile.username,
            email = userProfile.email,
            password = password,
            gender = userProfile.gender,
            birthdate = userProfile.birthdate,
            role = userProfile.role,
            createdAt = userProfile.createdAt,
            updatedAt = userProfile.updatedAt
        )
        userDao.insertUser(entity)
    }

    //Fetch (local db)
    suspend fun getUserFromLocal(userId: String): UserProfile? {
        val entity = userDao.getUserById(userId)
        return entity?.let {
            UserProfile(
                userId = it.userId,
                username = it.username,
                email = it.email,
                gender = it.gender,
                birthdate = it.birthdate,
                role = it.role,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
            )
        }
    }

    //Add local DB clear (logout or delete)
    suspend fun clearLocalUser(userId: String) {
        userDao.deleteUserById(userId)
    }

    // Save user profile to Firestore
    private suspend fun saveUserProfile(userProfile: UserProfile) {
        try {
            firestore.collection(USERS_COLLECTION)
                .document(userProfile.userId)
                .set(userProfile)
                .await()
        } catch (e: Exception) {
            throw Exception("Firestore save failed: ${e.message}", e)
        }
    }

    // Check if username is available
    suspend fun isUsernameAvailable(username: String): Boolean {
        return try {
            val querySnapshot = firestore.collection(USERS_COLLECTION)
                .whereEqualTo(USERNAME_FIELD, username)
                .limit(1)
                .get()
                .await()
            querySnapshot.isEmpty
        } catch (e: Exception) {
            // If we can't check, assume it's available to avoid blocking user
            true
        }
    }

    suspend fun isUsernameAvailableForUser(username: String, currentUserId: String): Boolean {
        return try {
            val querySnapshot = firestore.collection(USERS_COLLECTION)
                .whereEqualTo(USERNAME_FIELD, username)
                .limit(2) // Get up to 2 results to check
                .get()
                .await()

            Log.d("AuthRepository", "Found ${querySnapshot.size()} document(s) with this username")
            // If no documents, username is available
            if (querySnapshot.isEmpty) {
                Log.d("AuthRepository", "Username is available (no documents found)")
                return true
            }

            // If only one document and it belongs to current user, username is available
            if (querySnapshot.size() == 1) {
                val document = querySnapshot.documents[0]
                val docUserId = document.getString("userId") ?: ""
                Log.d("AuthRepository", "Found document userId: $docUserId")

                val available = docUserId == currentUserId
                Log.d("AuthRepository", "Is username available for current user? $available")
                return available
            }

            Log.d("AuthRepository", "Username is taken by multiple users")
            // More than one document means username is taken by others
            return false
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error checking username availability", e)
            // If we can't check, assume it's not available to be safe
            false
        }
    }

    // Get user profile from Firestore
    suspend fun getUserProfile(userId: String): AuthResult<UserProfile> {
        return try {
            val document = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()

            if (document.exists()) {
                val userProfile = document.toObject(UserProfile::class.java)
                if (userProfile != null) {
                    AuthResult.Success(userProfile)
                } else {
                    AuthResult.Error(Exception("Failed to parse user profile"))
                }
            } else {
                AuthResult.Error(Exception("User profile not found"))
            }
        } catch (e: Exception) {
            AuthResult.Error(e)
        }
    }

    // Update user profile
    suspend fun updateUserProfile(userProfile: UserProfile): AuthResult<UserProfile> {
        return try {
            val isAvailable = isUsernameAvailableForUser(userProfile.username, userProfile.userId)
            if (!isAvailable) {
                return AuthResult.Error(Exception("Username is already taken"))
            }

            val updatedProfile = userProfile.copy(updatedAt = System.currentTimeMillis())
            firestore.collection(USERS_COLLECTION)
                .document(userProfile.userId)
                .set(updatedProfile)
                .await()

            saveUserToLocal(updatedProfile)
            AuthResult.Success(updatedProfile)
        } catch (e: Exception) {
            AuthResult.Error(e)
        }
    }

    // Reset password
    suspend fun resetPassword(email: String): AuthResult<Unit> {
        return try {
            // Add comprehensive logging
            android.util.Log.d("AuthRepository", "Attempting password reset for email: $email")

            // Validate email format first
            if (!isValidEmail(email)) {
                android.util.Log.e("AuthRepository", "Invalid email format: $email")
                return AuthResult.Error(Exception("Invalid email format"))
            }

            // Check if user exists first (optional - Firebase won't tell us anyway for security)
            android.util.Log.d("AuthRepository", "Sending password reset email...")

            // Send the reset email
            auth.sendPasswordResetEmail(email).await()

            android.util.Log.d("AuthRepository", "Password reset email sent successfully to: $email")
            AuthResult.Success(Unit)

        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Password reset failed", e)

            // Enhanced error handling
            val errorMessage = when {
                e.message?.contains("network", true) == true ->
                    "Network error. Please check your internet connection."
                e.message?.contains("user-not-found", true) == true ->
                    "No account found with this email address."
                e.message?.contains("invalid-email", true) == true ->
                    "Invalid email address format."
                e.message?.contains("too-many-requests", true) == true ->
                    "Too many reset attempts. Please try again later."
                else ->
                    "Failed to send reset email. Error: ${e.message}"
            }

            AuthResult.Error(Exception(errorMessage))
        }
    }

    suspend fun testPasswordReset(email: String): String {
        return try {
            android.util.Log.d("AuthRepository", "=== PASSWORD RESET DEBUG START ===")
            android.util.Log.d("AuthRepository", "Firebase Auth instance: ${auth != null}")
            android.util.Log.d("AuthRepository", "Email to reset: $email")
            android.util.Log.d("AuthRepository", "Email is valid: ${isValidEmail(email)}")

            // Check if Firebase is properly initialized
            val currentUser = auth.currentUser
            android.util.Log.d("AuthRepository", "Current user exists: ${currentUser != null}")

            // Attempt the reset
            auth.sendPasswordResetEmail(email).await()

            android.util.Log.d("AuthRepository", "=== PASSWORD RESET DEBUG SUCCESS ===")
            "Debug: Password reset email sent successfully! Check your email and spam folder."

        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "=== PASSWORD RESET DEBUG FAILED ===", e)
            android.util.Log.e("AuthRepository", "Error type: ${e.javaClass.simpleName}")
            android.util.Log.e("AuthRepository", "Error message: ${e.message}")
            android.util.Log.e("AuthRepository", "Error cause: ${e.cause}")

            "Debug failed: ${e.message}"
        }
    }

    suspend fun deleteAccount(userId: String, email: String, password: String): AuthResult<Unit> {
        return try {
            val currentUser = auth.currentUser

            if (currentUser != null) {
                try {
                    // 1. Reauthenticate first (required by Firebase)
                    val credential = EmailAuthProvider.getCredential(email, password)
                    currentUser.reauthenticate(credential).await()
                    Log.d("AuthRepository", "User reauthenticated successfully")

                    // 2. Delete profile from Firestore BEFORE deleting auth account
                    // (because once auth is deleted, we lose access to Firebase services)
                    try {
                        firestore.collection("users").document(userId).delete().await()
                        Log.d("AuthRepository", "Firestore document deleted")
                    } catch (e: FirebaseFirestoreException) {
                        Log.w("AuthRepository", "Firestore delete failed, continuing with auth deletion", e)
                        // Continue even if Firestore deletion fails
                    }

                    // 3. Delete from Firebase Auth (this will sign out the user)
                    currentUser.delete().await()
                    Log.d("AuthRepository", "Firebase Auth account deleted")

                } catch (e: FirebaseAuthInvalidCredentialsException) {
                    Log.e("AuthRepository", "Invalid credentials during reauthentication", e)
                    throw e
                } catch (e: FirebaseAuthRecentLoginRequiredException) {
                    Log.e("AuthRepository", "Recent login required", e)
                    throw e
                }
            } else {
                Log.w("AuthRepository", "No current user found, proceeding with local deletion only")
            }

            // 4. Always delete from local Room DB (even if Firebase operations fail)
            withContext(Dispatchers.IO) {
                try {
                    val db = UserDatabase.getDatabase(context)
                    db.userDao().deleteUserById(userId)
                    Log.d("AuthRepository", "Local user data deleted from Room DB")
                } catch (e: Exception) {
                    Log.e("AuthRepository", "Failed to delete local user data", e)
                    throw e
                }
            }

            AuthResult.Success(Unit)

        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Log.e("AuthRepository", "Invalid password provided", e)
            AuthResult.Error(Exception("Invalid password. Please check your password and try again."))
        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            Log.e("AuthRepository", "Recent login required", e)
            AuthResult.Error(Exception("Please log out and log back in, then try deleting your account again."))
        } catch (e: Exception) {
            Log.e("AuthRepository", "Account deletion failed", e)
            AuthResult.Error(Exception("Failed to delete account: ${e.message}"))
        }
    }

    // Alternative version that throws exceptions instead of returning AuthResult
    suspend fun deleteAccountThrows(userId: String, email: String, password: String) {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            try {
                // 1. Reauthenticate first
                val credential = EmailAuthProvider.getCredential(email, password)
                currentUser.reauthenticate(credential).await()

                // 2. Delete profile from Firestore BEFORE deleting auth
                try {
                    firestore.collection("users").document(userId).delete().await()
                } catch (e: FirebaseFirestoreException) {
                    // Log but don't fail - continue with auth deletion
                    Log.w("AuthRepository", "Firestore delete failed", e)
                }

                // 3. Delete from Firebase Auth
                currentUser.delete().await()

            } catch (e: FirebaseAuthInvalidCredentialsException) {
                throw Exception("Invalid password. Please check your password and try again.")
            } catch (e: FirebaseAuthRecentLoginRequiredException) {
                throw Exception("Please log out and log back in, then try deleting your account again.")
            }
        }

        // 4. Always delete from local Room DB
        withContext(Dispatchers.IO) {
            val db = UserDatabase.getDatabase(context)
            db.userDao().deleteUserById(userId)
        }
    }

    // Validate email format
    fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    // Validate password strength
    fun isValidPassword(password: String): Pair<Boolean, String?> {
        return when {
            password.length < 6 -> false to "Password must be at least 6 characters"
            !password.any { it.isDigit() } -> false to "Password must contain at least one number"
            !password.any { it.isUpperCase() } -> false to "Password must contain at least one uppercase letter"
            !password.any { it.isLowerCase() } -> false to "Password must contain at least one lowercase letter"
            else -> true to null
        }
    }

    // Sign out
    fun signOut() {
        auth.signOut()
    }

    // Validate username format
    fun isValidUsername(username: String): Pair<Boolean, String?> {
        return when {
            username.isBlank() -> false to "Username is required"
            username.length < 3 -> false to "Username must be at least 3 characters"
            username.length > 20 -> false to "Username must be less than 20 characters"
            !username.matches(Regex("^[a-zA-Z0-9_]+$")) ->
                false to "Username can only contain letters, numbers, and underscores"
            username.startsWith("_") || username.endsWith("_") ->
                false to "Username cannot start or end with underscore"
            else -> true to null
        }
    }

    suspend fun checkIfEmailExistsInFirebaseAuth(email: String): Boolean {
        return try {
            Log.d("AuthRepository", "Checking if email exists in Firebase Auth: $email")

            // Try to send a password reset email - if user doesn't exist, it will fail
            auth.sendPasswordResetEmail(email).await()

            // If we reach here, user exists (or Firebase doesn't tell us for security reasons)
            Log.d("AuthRepository", "Email exists in Firebase Auth or Firebase won't tell us")
            true
        } catch (e: Exception) {
            Log.d("AuthRepository", "Error checking email: ${e.message}")
            // Firebase doesn't always tell us if user doesn't exist for security reasons
            false
        }
    }

    suspend fun debugEmailAvailability(email: String, tempPassword: String = "TempPass123!"): String {
        return try {
            Log.d("AuthRepository", "=== DEBUG EMAIL AVAILABILITY START ===")

            // Try to create an account with this email
            val result = auth.createUserWithEmailAndPassword(email, tempPassword).await()

            // If successful, immediately delete it
            result.user?.delete()?.await()

            Log.d("AuthRepository", "Email is available: $email")
            "Email is AVAILABLE"

        } catch (e: Exception) {
            Log.e("AuthRepository", "Email check failed: ${e.javaClass.simpleName} - ${e.message}")

            when {
                e.message?.contains("email-already-in-use", true) == true -> {
                    "Email is TAKEN by another account"
                }
                e.message?.contains("invalid-email", true) == true -> {
                    "Email format is INVALID"
                }
                e.message?.contains("weak-password", true) == true -> {
                    "Email appears AVAILABLE (password was too weak)"
                }
                else -> {
                    "Error checking email: ${e.message}"
                }
            }
        }
    }
}