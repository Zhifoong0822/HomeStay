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
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.update

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
            dataStoreManager.isLoggedIn.collect { loggedIn ->
                val currentUser = authRepository.getCurrentUser()

                if (loggedIn && currentUser != null && currentUser.isEmailVerified) {
                    _uiState.value = _uiState.value.copy(
                        isLoggedIn = true,
                        isLoading = true
                    )
                    loadUserProfile(currentUser.uid)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoggedIn = false,
                        userProfile = null,
                        isLoading = false
                    )
                }
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
                        //set isLoggedIn = true
                        viewModelScope.launch {
                            dataStoreManager.setLoginStatus(true)
                        }
                        //Load profile either from Firebase or local
                        val uid = result.data?.uid
                        if (uid != null) {
                            loadUserProfile(uid) // Firebase profile

                            //ROOM DB INSERT AFTER LOGIN
                            result.data?.let { firebaseUser ->
                                val db = UserDatabase.getDatabase(context)
                                val userDao = db.userDao()
                                viewModelScope.launch(Dispatchers.IO) {
                                    val safeRole = _signUpState.value.role
                                    if (safeRole.isBlank()) {
                                        Log.e("AuthViewModel", "Skipping Room insert - role is empty")
                                        return@launch
                                    }
                                    val userEntity = UserEntity(
                                        userId = firebaseUser.uid,
                                        email = firebaseUser.email ?: "",
                                        password = "",
                                        username = _loginState.value.email, // fallback username
                                        gender = "",
                                        birthdate = "",
                                        role = safeRole,
                                        createdAt = System.currentTimeMillis(),
                                        updatedAt = System.currentTimeMillis()
                                    )
                                    userDao.insertUser(userEntity)
                                }
                            }
                        } else {
                            loadLocalUserProfile(email) // Local profile
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
        val role = _signUpState.value.role

        var isValid = true

        // Validate email
        if (email.isEmpty()) {
            _signUpState.value = _signUpState.value.copy(emailError = "Email cannot be empty")
            isValid = false
        } else if (!authRepository.isValidEmail(email)) {
            _signUpState.value = _signUpState.value.copy(emailError = "Please enter a valid email address")
            isValid = false
        }

        // Validate username
        if (username.isEmpty()) {
            _signUpState.value = _signUpState.value.copy(usernameError = "Username cannot be empty")
            isValid = false
        } else if (username.length < 3) {
            _signUpState.value = _signUpState.value.copy(usernameError = "Username must be at least 3 characters")
            isValid = false
        }

        // Validate password
        if (password.isEmpty()) {
            _signUpState.value = _signUpState.value.copy(passwordError = "Password cannot be empty")
            isValid = false
        } else if (password.length < 6) {
            _signUpState.value = _signUpState.value.copy(passwordError = "Password must be at least 6 characters")
            isValid = false
        }

        // Validate confirm password
        if (confirmPassword.isEmpty()) {
            _signUpState.value = _signUpState.value.copy(confirmPasswordError = "Confirm password cannot be empty")
            isValid = false
        } else if (password != confirmPassword) {
            _signUpState.value = _signUpState.value.copy(confirmPasswordError = "Passwords do not match")
            isValid = false
        }

        // Validate gender
        if (gender.isEmpty()) {
            _signUpState.value = _signUpState.value.copy(errorMessage = "Please select a gender")
            isValid = false
        }

        // Validate birthdate
        if (birthdate.isEmpty()) {
            _signUpState.value = _signUpState.value.copy(errorMessage = "Please select your birthdate")
            isValid = false
        }

        // Validate role
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
                        Log.d("AuthViewModel", "Signup successful")
                        _signUpState.value = _signUpState.value.copy(
                            isLoading = false,
                            successMessage = "Account created successfully!"
                        )

                        //set shouldClearForm = true
                        markLoginFormForClearing()

                        //Room DB insertion
                        result.data?.let { firebaseUser ->
                            val db = UserDatabase.getDatabase(context)
                            val userDao = db.userDao()
                            viewModelScope.launch(Dispatchers.IO) {
                                if (role.isBlank()) {
                                    Log.e("AuthViewModel", "Skipping Room insert - role is empty")
                                    return@launch
                                }
                                val userEntity = UserEntity(
                                    userId = firebaseUser.uid,
                                    email = firebaseUser.email ?: "",
                                    password = "",
                                    username = username,
                                    gender = gender,
                                    birthdate = birthdate,
                                    role = role,
                                    createdAt = System.currentTimeMillis(),
                                    updatedAt = System.currentTimeMillis()
                                )
                                userDao.insertUser(userEntity)
                            }
                        }
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
                when (val result = authRepository.getUserProfile(userId)) {
                    is AuthResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            userProfile = result.data,
                            isLoggedIn = true,
                            isLoading = false,
                            errorMessage = null
                        )
                        authRepository.saveUserToLocal(result.data)
                    }
                    is AuthResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Failed to load user profile: ${result.exception.message}"
                        )
                        //try local database
                        val cached = authRepository.getUserFromLocal(userId)
                        if (cached != null) {
                            _uiState.value = _uiState.value.copy(
                                userProfile = cached,
                                isLoggedIn = true,
                                isLoading = false,
                                errorMessage = "Using cached profile (offline mode)"
                            )
                        }
                    }
                    else -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Unexpected error loading profile"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Exception loading profile: ${e.message}"
                )
            }
        }
    }

    private fun loadLocalUserProfile(email: String) {
        viewModelScope.launch {
            val localUser = authRepository.getLocalUserByEmail(email)
            if (localUser != null) {
                _uiState.value = _uiState.value.copy(
                    userProfile = UserProfile(
                        userId = localUser.userId,
                        username = localUser.username,
                        email = localUser.email,
                        gender = localUser.gender,
                        birthdate = localUser.birthdate
                    ),
                    isLoggedIn = true,
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "No local profile found"
                )
            }
        }
    }

    fun refreshUserProfile() {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser != null) {
            _uiState.value = _uiState.value.copy(isLoading = true)
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

                // Check if username is actually changing
                if (updatedProfile.username != currentProfile.username) {
                    Log.d("EditProfile", "Username IS changing - checking availability")

                    if (updatedProfile.username.isBlank()) {
                        _editProfileState.value = _editProfileState.value.copy(
                            isLoading = false,
                            errorMessage = "Username cannot be empty"
                        )
                        return@launch
                    }

                } else {
                    Log.d("EditProfile", "Username NOT changing - skipping check")
                }

                when (val result = authRepository.updateUserProfile(updatedProfile)) {
                    is AuthResult.Success -> {
                        _editProfileState.value = _editProfileState.value.copy(
                            isLoading = false,
                            successMessage = "Profile updated successfully!"
                        )

                        // Update UI state immediately
                        _uiState.value = _uiState.value.copy(
                            userProfile = result.data
                        )

                        // Refresh to ensure consistency
                        delay(500)
                        refreshUserProfile()
                    }

                    is AuthResult.Error -> {
                        _editProfileState.value = _editProfileState.value.copy(
                            isLoading = false,
                            errorMessage = result.exception.message ?: "Failed to update profile"
                        )
                    }

                    else -> {
                        _editProfileState.value = _editProfileState.value.copy(
                            isLoading = false,
                            errorMessage = "Unexpected error occurred"
                        )
                    }
                }

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
        }

        authRepository.signOut()

        // Mark that login form should be cleared next time
        _shouldClearLoginForm.value = true
        _shouldClearSignUpForm.value = true
        _shouldClearEditProfileForm.value = true

        // Clear everything
        clearErrors()
        clearEditProfileForm()
        forceCleanState()

        _uiState.value = _uiState.value.copy(
            isLoggedIn = false,
            userProfile = null
        )
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

                // 1️⃣ Check if user is signed in
                if (currentUser == null) {
                    Log.e("AuthViewModel", "No user logged in")
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Please log in again to delete your account"
                    )
                    return@launch
                }

                // 2️⃣ Reauthenticate (required by Firebase before delete)
                Log.d("AuthViewModel", "Starting reauthentication...")
                val credential = EmailAuthProvider.getCredential(email, password)
                currentUser.reauthenticate(credential).await()
                Log.d("AuthViewModel", "Reauthentication successful")

                // 3️⃣ Delete Firestore user document FIRST
                try {
                    Log.d("AuthViewModel", "Deleting Firestore document...")
                    val firestore = FirebaseFirestore.getInstance()
                    firestore.collection("users").document(userId).delete().await()
                    Log.d("AuthViewModel", "Firestore user document deleted")
                } catch (e: FirebaseFirestoreException) {
                    Log.e("AuthViewModel", "Firestore delete failed", e)
                }

                // 4️⃣ Delete local Room database record
                Log.d("AuthViewModel", "Deleting from local Room DB...")
                withContext(Dispatchers.IO) {
                    val db = UserDatabase.getDatabase(context)
                    db.userDao().deleteUserById(userId)
                    Log.d("AuthViewModel", "Local Room user deleted")
                }

                // 5️⃣ Delete Firebase Auth account (ONLY ONCE)
                Log.d("AuthViewModel", "Deleting Firebase Auth account...")
                currentUser.delete().await()
                Log.d("AuthViewModel", "Firebase Auth account deleted successfully")

                // 6️⃣ Clear UI state and login status
                Log.d("AuthViewModel", "Clearing app state...")
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
                // Mark that login form should be cleared
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
}