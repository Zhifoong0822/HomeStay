package com.example.homestay

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.homestay.AuthUiState
import com.example.homestay.LoginState
import com.example.homestay.SignUpState
import com.example.homestay.ResetPasswordState
import com.example.homestay.EditProfileState
import com.example.homestay.data.local.UserDatabase
import com.example.homestay.data.local.UserEntity
import com.example.homestay.data.local.toUserProfile
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first

class AuthViewModel(
    private val context: Context,
    private val authRepository: AuthRepository,
    private val dataStoreManager: DataStoreManager) : ViewModel() {

    private val _authState = MutableStateFlow<AuthResult<FirebaseUser>?>(null)
    val authState: StateFlow<AuthResult<FirebaseUser>?> = _authState

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    private val _loginState = MutableStateFlow(LoginState())
    val loginState = _loginState.asStateFlow()

    private val _signUpState = MutableStateFlow(SignUpState())
    val signUpState = _signUpState.asStateFlow()

    private val _editProfileState = MutableStateFlow(EditProfileState())
    val editProfileState = _editProfileState.asStateFlow()

    private var usernameCheckJob: Job? = null

    private val _resetPasswordState = MutableStateFlow(ResetPasswordState())
    val resetPasswordState = _resetPasswordState.asStateFlow()

    private val _shouldClearLoginForm = MutableStateFlow(false)
    val shouldClearLoginForm = _shouldClearLoginForm.asStateFlow()

    private val _shouldClearSignUpForm = MutableStateFlow(false)
    val shouldClearSignUpForm = _shouldClearSignUpForm.asStateFlow()

    private val _shouldClearEditProfileForm = MutableStateFlow(false)
    val shouldClearEditProfileForm = _shouldClearEditProfileForm.asStateFlow()

    // Initialize the ViewModel by checking current auth state
    init {
        checkCurrentUser()
    }

    // Check if user is already logged in and load their profile
    private fun checkCurrentUser() {
        viewModelScope.launch {
            Log.d("AUTH", "🔍 checkCurrentUser() started")

            try {
                _uiState.value = _uiState.value.copy(isAuthChecking = true, isLoading = true)

                val loggedIn = dataStoreManager.isLoggedIn.first()
                val currentUser = authRepository.getCurrentUser()

                Log.d("AUTH", "📡 DataStore loggedIn=$loggedIn")
                Log.d("AUTH", "📡 Firebase currentUser exists=${currentUser != null}")
                Log.d("AUTH", "📡 Firebase user verified=${currentUser?.isEmailVerified}")

                when {
                    // Case 1: Firebase user exists (REMOVED EMAIL VERIFICATION REQUIREMENT)
                    currentUser != null -> {
                        Log.d("AUTH", "✅ Firebase user found, loading profile...")
                        if (!loggedIn) {
                            dataStoreManager.setLoginStatus(true)
                        }
                        loadUserProfile(currentUser.uid)
                    }

                    // Case 2: DataStore says logged in but no Firebase user (offline mode)
                    loggedIn && currentUser == null -> {
                        Log.d("AUTH", "🔄 Offline mode - checking local data...")

                        // Check Room database directly
                        val db = UserDatabase.getDatabase(context)
                        val localUsers = withContext(Dispatchers.IO) {
                            db.userDao().getAllUsers()
                        }

                        Log.d("AUTH", "📱 Found ${localUsers.size} users in Room database")

                        if (localUsers.isNotEmpty()) {
                            val localUser = localUsers.first()
                            Log.d("AUTH", "📱 Using local user: email=${localUser.email}, role='${localUser.role}'")

                            val userProfile = UserProfile(
                                userId = localUser.userId,
                                username = localUser.username,
                                email = localUser.email,
                                gender = localUser.gender,
                                birthdate = localUser.birthdate,
                                role = localUser.role
                            )

                            // CRITICAL: Update DataStore with the role
                            if (localUser.role.isNotBlank()) {
                                dataStoreManager.setUserRole(localUser.role)
                                Log.d("AUTH", "📱 Set DataStore role to: '${localUser.role}'")
                            }

                            // Small delay for DataStore persistence
                            delay(100)

                            // Verify DataStore values
                            val verifyLoggedIn = dataStoreManager.isLoggedIn.first()
                            val verifyRole = dataStoreManager.userRole.first()
                            Log.d("AUTH", "Offline DataStore verification - isLoggedIn: $verifyLoggedIn, role: '$verifyRole'")

                            _uiState.value = _uiState.value.copy(
                                isAuthChecking = false,
                                isLoggedIn = true,
                                userProfile = userProfile,
                                isLoading = false
                            )

                            Log.d("AUTH", "Offline mode setup complete")
                        } else {
                            Log.d("AUTH", "❌ No local users found, resetting...")
                            dataStoreManager.setLoginStatus(false)
                            dataStoreManager.setUserRole("")
                            _uiState.value = _uiState.value.copy(
                                isAuthChecking = false,
                                isLoggedIn = false,
                                userProfile = null,
                                isLoading = false
                            )
                        }
                    }

                    // Case 3: Not logged in
                    else -> {
                        Log.d("AUTH", "❌ Not logged in, resetting state")
                        dataStoreManager.setLoginStatus(false)
                        dataStoreManager.setUserRole("")
                        _uiState.value = _uiState.value.copy(
                            isAuthChecking = false,
                            isLoggedIn = false,
                            userProfile = null,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("AUTH", "Exception in checkCurrentUser", e)
                _uiState.value = _uiState.value.copy(
                    isAuthChecking = false,
                    isLoggedIn = false,
                    userProfile = null,
                    isLoading = false,
                    errorMessage = "Authentication check failed: ${e.message}"
                )
            }
        }
    }

    // LOGIN
    fun onLoginEmailChange(email: String) {
        _loginState.value = _loginState.value.copy(
            email = email,
            emailError = null,
            errorMessage = null,
            successMessage = null
        )
    }

    fun onLoginPasswordChange(password: String) {
        _loginState.value = _loginState.value.copy(
            password = password,
            passwordError = null,
            errorMessage = null,
            successMessage = null
        )
    }

    fun updateLoginEmail(newEmail: String) {
        _loginState.value = _loginState.value.copy(email = newEmail)
    }

    fun updateLoginPassword(newPassword: String) {
        _loginState.value = _loginState.value.copy(password = newPassword)
    }

    fun login() {
        val email = _loginState.value.email.trim()
        val password = _loginState.value.password.trim()

        var isValid = true
        if (email.isEmpty()) {
            _loginState.value = _loginState.value.copy(emailError = "Email cannot be empty")
            isValid = false
        }
        if (password.isEmpty()) {
            _loginState.value = _loginState.value.copy(passwordError = "Password cannot be empty")
            isValid = false
        }
        if (!isValid) return

        viewModelScope.launch {
            _loginState.value = _loginState.value.copy(isLoading = true, errorMessage = null, successMessage = null)

            try {
                when (val result = authRepository.login(email, password)) {
                    is AuthResult.Success -> {
                        _loginState.value = _loginState.value.copy(
                            isLoading = false,
                            successMessage = if (result.data != null) {
                                "Login successful (Firebase)!"
                            } else {
                                "Login successful (Offline mode)"
                            }
                        )

                        //Load profile either from Firebase or local
                        val uid = result.data?.uid
                        if (uid != null) {
                            //Fetch Firestore profile directly
                            val profileResult = authRepository.getUserProfile(uid)
                            if (profileResult is AuthResult.Success && profileResult.data != null) {
                                val profile = profileResult.data

                                // Get existing Room user data
                                val db = UserDatabase.getDatabase(context)
                                val existingUser = withContext(Dispatchers.IO) {
                                    db.userDao().getUserById(uid)
                                }

                                // Determine final role (preserve Room role if Firestore is empty)
                                val finalRole = when {
                                    profile.role.isNotBlank() -> {
                                        Log.d("LOGIN", "Using Firestore role: '${profile.role}'")
                                        profile.role
                                    }
                                    !existingUser?.role.isNullOrBlank() -> {
                                        Log.d("LOGIN", "Using Room role: '${existingUser!!.role}'")
                                        existingUser.role
                                    }
                                    else -> {
                                        Log.w("LOGIN", "No role found in Firestore or Room!")
                                        ""
                                    }
                                }

                                val updatedProfile = profile.copy(role = finalRole)
                                Log.d("LOGIN", "Final profile role: '${updatedProfile.role}'")

                                // Save to Room
                                val userEntity = UserEntity(
                                    userId = uid,
                                    email = updatedProfile.email,
                                    username = updatedProfile.username,
                                    gender = updatedProfile.gender,
                                    birthdate = updatedProfile.birthdate,
                                    role = finalRole,
                                    password = existingUser?.password ?: "",
                                    createdAt = existingUser?.createdAt ?: System.currentTimeMillis(),
                                    updatedAt = System.currentTimeMillis()
                                )

                                withContext(Dispatchers.IO) {
                                    db.userDao().insertUser(userEntity)
                                    Log.d("LOGIN", "Saved to Room with role: '${userEntity.role}'")
                                }

                                // CRITICAL: Set DataStore values together and wait for completion
                                Log.d("LOGIN", "Setting DataStore values...")
                                dataStoreManager.setLoginStatus(true)
                                dataStoreManager.setUserRole(finalRole)

                                // Small delay to ensure DataStore persistence
                                delay(100)

                                // Verify DataStore values
                                val verifyLoggedIn = dataStoreManager.isLoggedIn.first()
                                val verifyRole = dataStoreManager.userRole.first()
                                Log.d("LOGIN", "DataStore verification - isLoggedIn: $verifyLoggedIn, role: '$verifyRole'")

                                // Update UI state LAST (this triggers navigation)
                                _uiState.value = _uiState.value.copy(
                                    userProfile = updatedProfile,
                                    isLoggedIn = true,
                                    isLoading = false,
                                    isAuthChecking = false // Important: Set this to false
                                )

                                Log.d("LOGIN", "Login complete - UI state updated")

                            } else {
                                // fallback if profile failed
                                Log.w("LOGIN", "Firestore profile load failed, using fallback")
                                loadUserProfile(uid)
                            }
                        } else {
                            // Offline login
                            Log.d("LOGIN", "Offline login - loading local profile")
                            loadLocalUserProfile(email)
                        }
                    }
                    is AuthResult.Error -> {
                        _loginState.value = _loginState.value.copy(
                            isLoading = false,
                            errorMessage = result.exception.message ?: "Login failed"
                        )
                    }
                    else -> {
                        _loginState.value = _loginState.value.copy(
                            isLoading = false,
                            errorMessage = "Unexpected error occurred"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("LOGIN", "Login exception", e)
                _loginState.value = _loginState.value.copy(
                    isLoading = false,
                    errorMessage = "Login failed: ${e.message}"
                )
            }
        }
    }

    // SIGNUP
    fun onSignUpEmailChange(email: String) {
        _signUpState.value = _signUpState.value.copy(
            email = email,
            emailError = null,
            errorMessage = null,
            successMessage = null
        )
    }

    fun onSignUpPasswordChange(password: String) {
        _signUpState.value = _signUpState.value.copy(
            password = password,
            passwordError = null,
            errorMessage = null,
            successMessage = null
        )
    }

    fun onSignUpConfirmPasswordChange(confirmPassword: String) {
        _signUpState.value = _signUpState.value.copy(
            confirmPassword = confirmPassword,
            confirmPasswordError = null,
            errorMessage = null,
            successMessage = null
        )
    }

    fun onSignUpUsernameChange(username: String) {
        _signUpState.value = _signUpState.value.copy(
            username = username,
            usernameError = null,
            errorMessage = null,
            successMessage = null
        )

        if (username.length >= 3) {
            usernameCheckJob?.cancel()
            usernameCheckJob = viewModelScope.launch {
                delay(500) // debounce
                checkUsernameAvailability(username)
            }
        }
    }

    fun onSignUpBirthdateChange(birthdate: String) {
        _signUpState.value = _signUpState.value.copy(
            birthdate = birthdate,
            errorMessage = null
        )
    }

    fun onSignUpGenderChange(gender: String) {
        _signUpState.value = _signUpState.value.copy(
            gender = gender,
            errorMessage = null
        )
    }

    private fun checkUsernameAvailability(username: String) {
        viewModelScope.launch {
            try {
                _signUpState.value = _signUpState.value.copy(isCheckingUsername = true)
                val available = authRepository.isUsernameAvailable(username)
                _signUpState.value = _signUpState.value.copy(
                    isCheckingUsername = false,
                    isUsernameAvailable = available,
                    usernameError = if (!available) "Username already taken" else null
                )
            } catch (e: Exception) {
                _signUpState.value = _signUpState.value.copy(
                    isCheckingUsername = false,
                    usernameError = "Error checking username availability"
                )
            }
        }
    }

    fun signUp() {
        val email = _signUpState.value.email.trim()
        val password = _signUpState.value.password.trim()
        val confirmPassword = _signUpState.value.confirmPassword.trim()
        val username = _signUpState.value.username.trim()
        val gender = _signUpState.value.gender.trim()
        val birthdate = _signUpState.value.birthdate.trim()
        val role = _signUpState.value.role.trim()

        var isValid = true

        // 🔹 Validation
        if (email.isEmpty()) {
            _signUpState.value = _signUpState.value.copy(emailError = "Email cannot be empty")
            isValid = false
        } else if (!authRepository.isValidEmail(email)) {
            _signUpState.value = _signUpState.value.copy(emailError = "Please enter a valid email address")
            isValid = false
        }

        if (username.isEmpty()) {
            _signUpState.value = _signUpState.value.copy(usernameError = "Username cannot be empty")
            isValid = false
        } else if (username.length < 3) {
            _signUpState.value = _signUpState.value.copy(usernameError = "Username must be at least 3 characters")
            isValid = false
        }

        if (password.isEmpty()) {
            _signUpState.value = _signUpState.value.copy(passwordError = "Password cannot be empty")
            isValid = false
        } else if (password.length < 6) {
            _signUpState.value = _signUpState.value.copy(passwordError = "Password must be at least 6 characters")
            isValid = false
        }

        if (confirmPassword.isEmpty()) {
            _signUpState.value = _signUpState.value.copy(confirmPasswordError = "Confirm password cannot be empty")
            isValid = false
        } else if (password != confirmPassword) {
            _signUpState.value = _signUpState.value.copy(confirmPasswordError = "Passwords do not match")
            isValid = false
        }

        if (gender.isEmpty()) {
            _signUpState.value = _signUpState.value.copy(errorMessage = "Please select a gender")
            isValid = false
        }

        if (birthdate.isEmpty()) {
            _signUpState.value = _signUpState.value.copy(errorMessage = "Please select your birthdate")
            isValid = false
        }

        if (role.isEmpty()) {
            _signUpState.value = _signUpState.value.copy(errorMessage = "Please select a role (Guest or Host)")
            isValid = false
        }

        if (!isValid) {
            Log.d("AuthViewModel", "Validation failed - not proceeding with signup")
            return
        }

        val request = SignUpRequest(
            email = email,
            password = password,
            username = username,
            gender = gender,
            birthdate = birthdate,
            role = role
        )

        viewModelScope.launch {
            _signUpState.value = _signUpState.value.copy(isLoading = true, errorMessage = null, successMessage = null)

            try {
                Log.d("AuthViewModel", "=== SIGNUP DEBUG START ===")
                Log.d("AuthViewModel", "Attempting signup for email: $email")

                when (val result = authRepository.signUp(request)) {
                    is AuthResult.Success -> {
                        val firebaseUser = result.data
                        if (firebaseUser == null) {
                            _signUpState.value = _signUpState.value.copy(
                                isLoading = false,
                                errorMessage = "Signup failed: Firebase user is null"
                            )
                            return@launch
                        }

                        Log.d("AuthViewModel", "Signup successful for uid=${firebaseUser.uid}")

                        // 🔹 Save profile to Firestore (including role)
                        try {
                            val firestore = FirebaseFirestore.getInstance()
                            val userProfile = UserProfile(
                                userId = firebaseUser.uid,
                                email = firebaseUser.email ?: email,
                                username = username,
                                gender = gender,
                                birthdate = birthdate,
                                role = role // ✅ force-save role
                            )
                            firestore.collection("users")
                                .document(firebaseUser.uid)
                                .set(userProfile)
                                .await()
                            Log.d("AuthViewModel", "User profile saved to Firestore with role=$role")
                        } catch (e: Exception) {
                            Log.e("AuthViewModel", "Failed to save profile to Firestore", e)
                        }

                        // 🔹 Save profile to Room
                        withContext(Dispatchers.IO) {
                            val db = UserDatabase.getDatabase(context)
                            val userDao = db.userDao()
                            val userEntity = UserEntity(
                                userId = firebaseUser.uid,
                                email = firebaseUser.email ?: email,
                                password = "",
                                username = username,
                                gender = gender,
                                birthdate = birthdate,
                                role = role, // ✅ force-save role locally
                                createdAt = System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )
                            userDao.insertUser(userEntity)
                            Log.d("AuthViewModel", "User saved to Room with role=$role")
                        }

                        _signUpState.value = _signUpState.value.copy(
                            isLoading = false,
                            successMessage = "Account created successfully!"
                        )

                        // Mark login form for clearing
                        markLoginFormForClearing()
                    }

                    is AuthResult.Error -> {
                        Log.e("AuthViewModel", "Signup failed: ${result.exception.message}")
                        val errorMessage = when {
                            result.exception.message?.contains("email-already-in-use", true) == true -> {
                                "This email is already registered. Please use a different email or try logging in."
                            }
                            result.exception.message?.contains("weak-password", true) == true -> {
                                "Password is too weak. Please use a stronger password."
                            }
                            result.exception.message?.contains("invalid-email", true) == true -> {
                                "Invalid email format. Please check your email address."
                            }
                            else -> result.exception.message ?: "Unknown signup error"
                        }

                        _signUpState.value = _signUpState.value.copy(
                            isLoading = false,
                            errorMessage = errorMessage
                        )
                    }

                    else -> {
                        Log.e("AuthViewModel", "Unexpected signup result")
                        _signUpState.value = _signUpState.value.copy(
                            isLoading = false,
                            errorMessage = "Unexpected error occurred during signup"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Signup exception: ${e.javaClass.simpleName} - ${e.message}", e)
                _signUpState.value = _signUpState.value.copy(
                    isLoading = false,
                    errorMessage = "Signup failed: ${e.message}"
                )
            }
        }
    }

    fun onSignUpRoleChange(role: String) {
        _signUpState.value = _signUpState.value.copy(role = role, errorMessage = null)
    }

    fun debugEmailAvailability(email: String) {
        viewModelScope.launch {
            try {
                Log.d("AuthViewModel", "=== DEBUG EMAIL AVAILABILITY ===")
                val result = authRepository.debugEmailAvailability(email)
                Log.d("AuthViewModel", "Email availability result: $result")

                // Show the result to user
                _signUpState.value = _signUpState.value.copy(
                    errorMessage = "DEBUG: $result"
                )
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Debug failed", e)
                _signUpState.value = _signUpState.value.copy(
                    errorMessage = "Debug failed: ${e.message}"
                )
            }
        }
    }

    // Password Reset
    fun resetPassword(email: String) {
        if (email.isBlank()) {
            _resetPasswordState.value = _resetPasswordState.value.copy(
                errorMessage = "Email cannot be empty"
            )
            return
        }

        if (!authRepository.isValidEmail(email)) {
            _resetPasswordState.value = _resetPasswordState.value.copy(
                errorMessage = "Please enter a valid email address"
            )
            return
        }

        viewModelScope.launch {
            _resetPasswordState.value = _resetPasswordState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            try {
                when (val result = authRepository.resetPassword(email)) {
                    is AuthResult.Success -> {
                        _resetPasswordState.value = _resetPasswordState.value.copy(
                            isLoading = false,
                            successMessage = "Password reset email has been sent to $email. Please check your inbox."
                        )
                    }
                    is AuthResult.Error -> {
                        _resetPasswordState.value = _resetPasswordState.value.copy(
                            isLoading = false,
                            errorMessage = handleAuthException(result.exception)
                        )
                    }
                    else -> {
                        _resetPasswordState.value = _resetPasswordState.value.copy(
                            isLoading = false,
                            errorMessage = "Unexpected error occurred"
                        )
                    }
                }
            } catch (e: Exception) {
                _resetPasswordState.value = _resetPasswordState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to send reset email: ${e.message}"
                )
            }
        }
    }

    // Profile Loading
    private fun loadUserProfile(userId: String) {
        viewModelScope.launch {
            try {
                Log.d("AUTH", "🔄 Loading user profile for userId: $userId")

                // First, check what we have in Room database
                val db = UserDatabase.getDatabase(context)
                val existingUser = withContext(Dispatchers.IO) {
                    db.userDao().getUserById(userId)
                }

                Log.d("AUTH", "📱 Existing Room user: ${existingUser != null}")
                if (existingUser != null) {
                    Log.d("AUTH", "📱 Existing Room user role: '${existingUser.role}'")
                }

                when (val result = authRepository.getUserProfile(userId)) {
                    is AuthResult.Success -> {
                        val profile = result.data
                        if (profile != null) {
                            Log.d("AUTH", "🔥 Firestore profile loaded: role='${profile.role}'")

                            // CRITICAL: Preserve role from Room if Firestore role is empty
                            val finalRole = when {
                                profile.role.isNotBlank() -> {
                                    Log.d("AUTH", "✅ Using Firestore role: '${profile.role}'")
                                    profile.role
                                }
                                existingUser?.role?.isNotBlank() == true -> {
                                    Log.d("AUTH", "⚠ Firestore role empty, using Room role: '${existingUser.role}'")
                                    existingUser.role
                                }
                                else -> {
                                    Log.w("AUTH", "❌ Both Firestore and Room roles are empty!")
                                    ""
                                }
                            }

                            val updatedProfile = profile.copy(role = finalRole)
                            Log.d("AUTH", "📋 Final profile role: '${updatedProfile.role}'")

                            // Save to Room with role preservation
                            val userEntity = UserEntity(
                                userId = userId,
                                email = updatedProfile.email,
                                username = updatedProfile.username,
                                gender = updatedProfile.gender,
                                birthdate = updatedProfile.birthdate,
                                role = finalRole, // Use the preserved role
                                password = existingUser?.password ?: "",
                                createdAt = existingUser?.createdAt ?: System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )

                            // Log what we're saving
                            logRoomInsert(userEntity, "PROFILE_LOAD")

                            withContext(Dispatchers.IO) {
                                db.userDao().insertUser(userEntity)
                            }

                            // CRITICAL: Update DataStore and wait for persistence
                            Log.d("AUTH", "Setting DataStore values...")
                            dataStoreManager.setLoginStatus(true)
                            if (finalRole.isNotBlank()) {
                                dataStoreManager.setUserRole(finalRole)
                            }

                            // Small delay to ensure DataStore persistence
                            delay(100)

                            // Verify DataStore values
                            val verifyLoggedIn = dataStoreManager.isLoggedIn.first()
                            val verifyRole = dataStoreManager.userRole.first()
                            Log.d("AUTH", "DataStore verification - isLoggedIn: $verifyLoggedIn, role: '$verifyRole'")

                            // Update UI state LAST (this triggers navigation)
                            _uiState.value = _uiState.value.copy(
                                userProfile = updatedProfile,
                                isLoggedIn = true,
                                isLoading = false,
                                isAuthChecking = false, // CRITICAL for navigation
                                errorMessage = null
                            )

                            Log.d("AUTH", "Profile load complete - UI state updated")

                            // Debug what we just saved
                            debugRoomDatabase()
                        }
                    }
                    is AuthResult.Error -> {
                        Log.e("AUTH", "Failed to load Firestore profile: ${result.exception.message}")

                        // Fallback to Room database
                        if (existingUser != null) {
                            Log.d("AUTH", "📱 Using cached Room profile")
                            val cachedProfile = UserProfile(
                                userId = existingUser.userId,
                                username = existingUser.username,
                                email = existingUser.email,
                                gender = existingUser.gender,
                                birthdate = existingUser.birthdate,
                                role = existingUser.role
                            )

                            // CRITICAL: Set DataStore for fallback case too
                            dataStoreManager.setLoginStatus(true)
                            if (existingUser.role.isNotBlank()) {
                                dataStoreManager.setUserRole(existingUser.role)
                            }

                            // Small delay for DataStore persistence
                            delay(100)

                            // Verify DataStore values
                            val verifyLoggedIn = dataStoreManager.isLoggedIn.first()
                            val verifyRole = dataStoreManager.userRole.first()
                            Log.d("AUTH", "Fallback DataStore verification - isLoggedIn: $verifyLoggedIn, role: '$verifyRole'")

                            _uiState.value = _uiState.value.copy(
                                userProfile = cachedProfile,
                                isLoggedIn = true,
                                isLoading = false,
                                isAuthChecking = false, // CRITICAL for navigation
                                errorMessage = "Using cached profile (offline mode)"
                            )

                            Log.d("AUTH", "Fallback profile load complete")
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isAuthChecking = false,
                                errorMessage = "Failed to load user profile: ${result.exception.message}"
                            )
                        }
                    }
                    else -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isAuthChecking = false,
                            errorMessage = "Unexpected error loading profile"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("AUTH", "Exception in loadUserProfile", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isAuthChecking = false,
                    errorMessage = "Exception loading profile: ${e.message}"
                )
            }
        }
    }

    private fun loadLocalUserProfile(email: String) {
        viewModelScope.launch {
            Log.d("LOGIN", "Loading local profile for email: $email")

            val localUser = authRepository.getLocalUserByEmail(email)
            if (localUser != null) {
                Log.d("LOGIN", "Found local user with role: '${localUser.role}'")

                val finalRole = if (localUser.role.isNotBlank()) {
                    localUser.role
                } else {
                    ""
                }

                // CRITICAL: Set DataStore values first
                dataStoreManager.setLoginStatus(true)
                dataStoreManager.setUserRole(finalRole)

                // Small delay to ensure DataStore persistence
                delay(100)

                // Verify DataStore values
                val verifyLoggedIn = dataStoreManager.isLoggedIn.first()
                val verifyRole = dataStoreManager.userRole.first()
                Log.d("LOGIN", "Local profile DataStore verification - isLoggedIn: $verifyLoggedIn, role: '$verifyRole'")

                // Update UI state (this triggers navigation)
                _uiState.value = _uiState.value.copy(
                    userProfile = UserProfile(
                        userId = localUser.userId,
                        username = localUser.username,
                        email = localUser.email,
                        gender = localUser.gender,
                        birthdate = localUser.birthdate,
                        role = finalRole
                    ),
                    isLoggedIn = true,
                    isLoading = false,
                    isAuthChecking = false // CRITICAL: This allows navigation to trigger
                )

                Log.d("LOGIN", "Local profile login complete")

            } else {
                Log.e("LOGIN", "No local profile found for email: $email")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isAuthChecking = false,
                    errorMessage = "No local profile found"
                )
            }
        }
    }

    fun checkAuthStatus() {
        viewModelScope.launch {
            Log.d("AUTH_STATUS", "🔍 checkAuthStatus() started")

            try {
                _uiState.value = _uiState.value.copy(isAuthChecking = true, isLoading = true)

                val loggedIn = dataStoreManager.isLoggedIn.first()
                val storedRole = dataStoreManager.userRole.first()

                Log.d("AUTH_STATUS", "📡 DataStore loggedIn: $loggedIn")
                Log.d("AUTH_STATUS", "📡 DataStore userRole: '$storedRole'")

                if (!loggedIn) {
                    Log.d("AUTH_STATUS", "❌ User explicitly logged out")
                    _uiState.value = _uiState.value.copy(
                        isAuthChecking = false,
                        isLoggedIn = false,
                        userProfile = null,
                        isLoading = false
                    )
                    return@launch
                }

                val firebaseUser = authRepository.getCurrentUser()
                Log.d("AUTH_STATUS", "🔥 Firebase user exists: ${firebaseUser != null}")

                // Check Room database
                val db = UserDatabase.getDatabase(context)
                val localUsers = withContext(Dispatchers.IO) {
                    db.userDao().getAllUsers()
                }

                Log.d("AUTH_STATUS", "📱 Room users count: ${localUsers.size}")

                when {
                    // Case 1: Firebase user exists, load from Firebase
                    firebaseUser != null -> {
                        Log.d("AUTH_STATUS", "✅ Firebase user found, loading profile...")
                        loadUserProfile(firebaseUser.uid)
                    }

                    // Case 2: No Firebase user but we have local users
                    localUsers.isNotEmpty() -> {
                        val localUser = localUsers.first()
                        Log.d("AUTH_STATUS", "📱 Using local user: email=${localUser.email}, role='${localUser.role}'")

                        val userProfile = UserProfile(
                            userId = localUser.userId,
                            username = localUser.username,
                            email = localUser.email,
                            gender = localUser.gender,
                            birthdate = localUser.birthdate,
                            role = localUser.role
                        )

                        // CRITICAL: Update DataStore with local user data
                        dataStoreManager.setLoginStatus(true)
                        if (localUser.role.isNotBlank()) {
                            dataStoreManager.setUserRole(localUser.role)
                            Log.d("AUTH_STATUS", "📱 Set DataStore role to: '${localUser.role}'")
                        }

                        _uiState.value = _uiState.value.copy(
                            isAuthChecking = false,
                            isLoggedIn = true,
                            userProfile = userProfile,
                            isLoading = false
                        )

                        // Verify DataStore was updated
                        delay(100)
                        val verifyRole = dataStoreManager.userRole.first()
                        Log.d("AUTH_STATUS", "📱 DataStore role after setting: '$verifyRole'")
                    }

                    // Case 3: No users anywhere
                    else -> {
                        Log.d("AUTH_STATUS", "❌ No Firebase user and no local users, resetting")
                        dataStoreManager.setLoginStatus(false)
                        dataStoreManager.setUserRole("")
                        _uiState.value = _uiState.value.copy(
                            isAuthChecking = false,
                            isLoggedIn = false,
                            userProfile = null,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("AUTH_STATUS", "Exception in checkAuthStatus", e)
                _uiState.value = _uiState.value.copy(
                    isAuthChecking = false,
                    isLoggedIn = false,
                    userProfile = null,
                    isLoading = false,
                    errorMessage = "Authentication check failed: ${e.message}"
                )
            }
        }
    }

    fun refreshUserProfile() {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser != null) {
            loadUserProfile(currentUser.uid)
        } else {
            _uiState.value = _uiState.value.copy(
                isLoggedIn = false,
                userProfile = null,
                errorMessage = "No user logged in"
            )
        }
    }

    fun updateUserProfile(updatedProfile: UserProfile) {
        viewModelScope.launch {
            _editProfileState.value = _editProfileState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            try {
                val currentProfile = _uiState.value.userProfile
                if (currentProfile == null) {
                    _editProfileState.value = _editProfileState.value.copy(
                        isLoading = false,
                        errorMessage = "No current profile found"
                    )
                    return@launch
                }

                // Validate username if changed
                if (updatedProfile.username != currentProfile.username) {
                    if (updatedProfile.username.isBlank()) {
                        _editProfileState.value = _editProfileState.value.copy(
                            isLoading = false,
                            errorMessage = "Username cannot be empty"
                        )
                        return@launch
                    }

                    val available = authRepository.isUsernameAvailable(updatedProfile.username)
                    if (!available) {
                        _editProfileState.value = _editProfileState.value.copy(
                            isLoading = false,
                            errorMessage = "Username already taken"
                        )
                        return@launch
                    }
                }

                // Merge role: prioritize updatedProfile, fallback to currentProfile, then Room
                val db = UserDatabase.getDatabase(context)
                val userDao = db.userDao()
                val existingUser = withContext(Dispatchers.IO) { userDao.getUserById(currentProfile.userId) }

                val safeRole = updatedProfile.role
                    .ifBlank { currentProfile.role }
                    .ifBlank { existingUser?.role ?: "" } // default fallback

                // Update Firebase (without role)
                val firebaseUpdates = mapOf(
                    "username" to updatedProfile.username,
                    "gender" to updatedProfile.gender,
                    "birthdate" to updatedProfile.birthdate
                )
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentProfile.userId)
                    .update(firebaseUpdates)
                    .await()

                // Merge final profile
                val finalProfile = updatedProfile.copy(role = safeRole)

                // Update UI immediately
                _uiState.value = _uiState.value.copy(userProfile = finalProfile)

                // Update Room
                withContext(Dispatchers.IO) {
                    val userEntity = UserEntity(
                        userId = finalProfile.userId,
                        email = finalProfile.email,
                        password = existingUser?.password ?: "",
                        username = finalProfile.username,
                        gender = finalProfile.gender,
                        birthdate = finalProfile.birthdate,
                        role = safeRole,
                        createdAt = existingUser?.createdAt ?: System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    userDao.insertUser(userEntity)
                }

                // Update DataStore
                dataStoreManager.setLoginStatus(true)
                dataStoreManager.setUserRole(safeRole)

                _editProfileState.value = _editProfileState.value.copy(
                    isLoading = false,
                    successMessage = "Profile updated successfully!"
                )

                // Optional refresh
                delay(500)
                refreshUserProfile()

            } catch (e: Exception) {
                _editProfileState.value = _editProfileState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to update profile: ${e.message}"
                )
            }
        }
    }

    fun updateNewUsername(value: String) {
        _editProfileState.update { it.copy(newUsername = value) }
    }

    fun updateNewGender(value: String) {
        _editProfileState.update { it.copy(newGender = value) }
    }

    fun updateNewBirthdate(value: String) {
        _editProfileState.update { it.copy(newBirthdate = value) }
    }

    fun logout() {
        val currentUser = authRepository.getCurrentUser()
        viewModelScope.launch {
            //clear local cache when user logs out
            dataStoreManager.setLoginStatus(false)  //set isLoggedIn = false

            authRepository.signOut()

            _uiState.value = _uiState.value.copy(
                isLoggedIn = false,
                userProfile = null,
                errorMessage = null,
                isLoading = false
            )
        }

        // Mark that login form should be cleared next time
        _shouldClearLoginForm.value = true
        _shouldClearSignUpForm.value = true
        _shouldClearEditProfileForm.value = true

        // Clear everything
        clearErrors()
        clearEditProfileForm()
        forceCleanState()
    }

    fun markLoginFormForClearing() {
        _shouldClearLoginForm.value = false
        Log.d("AuthViewModel", "Login form marked for clearing")
    }

    fun markSignUpFormForClearing() {
        _shouldClearSignUpForm.value = false
        Log.d("AuthViewModel", "Sign up form marked for clearing")
    }

    // Auth Exception Handling
    private fun handleAuthException(e: Exception?): String {
        return when (e) {
            is FirebaseAuthWeakPasswordException -> "Password is too weak."
            is FirebaseAuthInvalidCredentialsException -> "Invalid email or password format."
            is FirebaseAuthUserCollisionException -> "This email is already in use."
            is FirebaseAuthInvalidUserException -> "No account found with this email."
            else -> e?.localizedMessage ?: "Authentication failed."
        }
    }

    fun clearLoginForm() {
        _loginState.update { currentState ->
            currentState.copy(
                email = "",
                password = "",
                emailError = null,
                passwordError = null,
                isLoading = false,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun clearSignUpForm() {
        _signUpState.update { currentState ->
            currentState.copy(
                email = "",
                password = "",
                confirmPassword = "",
                username = "",
                gender = "",
                birthdate = "",
                role = "",
                emailError = null,
                passwordError = null,
                confirmPasswordError = null,
                usernameError = null,
                isLoading = false,
                errorMessage = null,
                successMessage = null,
                isCheckingUsername = false,
                isUsernameAvailable = null
            )
        }
    }

    fun clearEditProfileForm() {
        _editProfileState.update { currentState ->
            currentState.copy(
                newUsername = "",
                newGender = "",
                newBirthdate = "",
                errorMessage = null,
                successMessage = null,
                isLoading = false
            )
        }
    }

    fun clearErrors() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
        _loginState.value = _loginState.value.copy(errorMessage = null, successMessage = null)
        _signUpState.value = _signUpState.value.copy(errorMessage = null, successMessage = null)
        _resetPasswordState.value = ResetPasswordState()
        _editProfileState.value = EditProfileState()
    }

    fun clearSignUpMessages() {
        _signUpState.value = _signUpState.value.copy(
            successMessage = null,
            errorMessage = null
        )
    }

    fun clearLoginMessages() {
        _loginState.value = _loginState.value.copy(
            successMessage = null,
            errorMessage = null
        )
    }

    fun clearEditProfileSuccessMessage() {
        _editProfileState.update { it.copy(successMessage = null) }
    }

    fun clearResetPasswordMessages() {
        _resetPasswordState.value = ResetPasswordState()
    }

    fun forceCleanState() {
        viewModelScope.launch {
            // Cancel any ongoing operations
            usernameCheckJob?.cancel()
            usernameCheckJob = null

            _loginState.value = LoginState(
                email = "",
                password = "",
                emailError = null,
                passwordError = null,
                isLoading = false,
                errorMessage = null,
                successMessage = null
            )

            _signUpState.value = SignUpState(
                email = "",
                password = "",
                confirmPassword = "",
                username = "",
                gender = "",
                birthdate = "",
                role = "",
                emailError = null,
                passwordError = null,
                confirmPasswordError = null,
                usernameError = null,
                isLoading = false,
                errorMessage = null,
                successMessage = null,
                isCheckingUsername = false,
                isUsernameAvailable = null
            )

            _resetPasswordState.value = ResetPasswordState()
            _editProfileState.value = EditProfileState(
                newUsername = "",
                newGender = "",
                newBirthdate = "",
                errorMessage = null,
                successMessage = null,
                isLoading = false
            )

            _shouldClearLoginForm.value = false
            _shouldClearSignUpForm.value = false
            _shouldClearEditProfileForm.value = false

            Log.d("AuthViewModel", "All forms force cleared")
            Log.d("AuthViewModel", "Login email after clear: '${_loginState.value.email}'")
        }
    }

    fun deleteAccount(userId: String, email: String, password: String) {
        viewModelScope.launch {
            try {
                Log.d("AuthViewModel", "=== DELETE ACCOUNT DEBUG START ===")

                val currentUser = authRepository.getCurrentUser()
                Log.d("AuthViewModel", "Current user exists: ${currentUser != null}")
                Log.d("AuthViewModel", "Current user email: ${currentUser?.email}")
                Log.d("AuthViewModel", "Current user UID: ${currentUser?.uid}")

                // 1️⃣ Ensure user is signed in
                if (currentUser == null) {
                    Log.e("AuthViewModel", "No user logged in")
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Please log in again to delete your account"
                    )
                    return@launch
                }

                // 2️⃣ Reauthenticate (Firebase requires this for sensitive ops)
                Log.d("AuthViewModel", "Starting reauthentication...")
                val credential = EmailAuthProvider.getCredential(email, password)
                currentUser.reauthenticate(credential).await()
                Log.d("AuthViewModel", "Reauthentication successful")

                // 3️⃣ Delete Firestore user document
                try {
                    Log.d("AuthViewModel", "Deleting Firestore document...")
                    val firestore = FirebaseFirestore.getInstance()
                    firestore.collection("users").document(userId).delete().await()
                    Log.d("AuthViewModel", "Firestore user document deleted")
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "⚠ Firestore delete failed", e)
                }

                // 4️⃣ Delete from local Room DB
                Log.d("AuthViewModel", "Deleting from local Room DB...")
                withContext(Dispatchers.IO) {
                    val db = UserDatabase.getDatabase(context)
                    db.userDao().deleteUserById(userId)
                }
                Log.d("AuthViewModel", "Local Room user deleted")

                // 5️⃣ Delete Firebase Auth account
                Log.d("AuthViewModel", "Deleting Firebase Auth account...")
                currentUser.delete().await()
                Log.d("AuthViewModel", "Firebase Auth account deleted successfully")

                // 6️⃣ Final cleanup → DataStore + UI
                dataStoreManager.setLoginStatus(false)
                clearErrors()
                forceCleanState()

                _uiState.value = _uiState.value.copy(
                    isLoggedIn = false,
                    userProfile = null,
                    isLoading = false,
                    errorMessage = null,
                    successMessage = "Account deleted successfully"
                )
                markLoginFormForClearing()
                markSignUpFormForClearing()

                Log.d("AuthViewModel", "=== DELETE ACCOUNT DEBUG SUCCESS ===")

            } catch (e: FirebaseAuthRecentLoginRequiredException) {
                Log.e("AuthViewModel", "Reauthentication required before delete", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Please log out and log back in, then try deleting your account again."
                )
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Failed to delete account: ${e.javaClass.simpleName}", e)
                Log.e("AuthViewModel", "Error message: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to delete account: ${e.message}"
                )
            }
        }
    }

    private fun debugRoomDatabase() {
        viewModelScope.launch {
            try {
                val db = UserDatabase.getDatabase(context)
                val allUsers = withContext(Dispatchers.IO) {
                    db.userDao().getAllUsers()
                }

                Log.d("ROOM_DEBUG", "=== ROOM DATABASE CONTENT ===")
                Log.d("ROOM_DEBUG", "Total users in Room: ${allUsers.size}")

                allUsers.forEachIndexed { index, user ->
                    Log.d("ROOM_DEBUG", "User $index:")
                    Log.d("ROOM_DEBUG", "  - userId: '${user.userId}'")
                    Log.d("ROOM_DEBUG", "  - email: '${user.email}'")
                    Log.d("ROOM_DEBUG", "  - username: '${user.username}'")
                    Log.d("ROOM_DEBUG", "  - role: '${user.role}'")
                    Log.d("ROOM_DEBUG", "  - gender: '${user.gender}'")
                    Log.d("ROOM_DEBUG", "  - createdAt: ${user.createdAt}")
                    Log.d("ROOM_DEBUG", "  - updatedAt: ${user.updatedAt}")
                }
                Log.d("ROOM_DEBUG", "========================")
            } catch (e: Exception) {
                Log.e("ROOM_DEBUG", "Failed to debug Room database", e)
            }
        }
    }

    private fun logRoomInsert(userEntity: UserEntity, operation: String) {
        Log.d("ROOM_INSERT", "=== $operation ===")
        Log.d("ROOM_INSERT", "Inserting user with:")
        Log.d("ROOM_INSERT", "  - userId: '${userEntity.userId}'")
        Log.d("ROOM_INSERT", "  - email: '${userEntity.email}'")
        Log.d("ROOM_INSERT", "  - role: '${userEntity.role}' (${userEntity.role.length} chars)")
        Log.d("ROOM_INSERT", "  - username: '${userEntity.username}'")
        Log.d("ROOM_INSERT", "  - updatedAt: ${userEntity.updatedAt}")
        Log.d("ROOM_INSERT", "================")
    }

    // Call this function whenever you want to check the current Room state
    fun debugCurrentRoomState() {
        debugRoomDatabase()
    }
}