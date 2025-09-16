package com.example.homestay.ui.Login

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.example.homestay.AuthViewModel
import com.example.homestay.LoginState
import com.example.homestay.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    isTablet: Boolean,
    viewModel: AuthViewModel = viewModel(),
    onBackButtonClicked: () -> Unit,
    onSuccess: () -> Unit,
    onForgotPasswordClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val loginState by viewModel.loginState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val shouldClearForm by viewModel.shouldClearLoginForm.collectAsState()
    val scrollState = rememberScrollState()
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val contentPadding = if (isTablet) 40.dp else 24.dp
    val titleFontSize = if (isTablet) 36.sp else 33.sp
    val buttonHeight = if (isTablet) 56.dp else 52.dp
    val buttonFontSize = if (isTablet) 18.sp else 16.sp
    val forgotPasswordFontSize = if (isTablet) 17.sp else 15.sp

    val gradientBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFEF9F3),
            Color(0xFFF5F0EA),
            Color(0xFFEFE8E0)
        )
    )

    //clear only when shouldClearForm = true
    LaunchedEffect(shouldClearForm) {
        if (shouldClearForm) {
            Log.d("LoginScreen", "Clearing form due to navigation")
            viewModel.forceCleanState()
        }
    }

    LaunchedEffect(loginState.successMessage, uiState.userProfile) {
        if (loginState.successMessage != null && uiState.userProfile != null) {
            snackbarHostState.showSnackbar("Login successful")
            onSuccess()
            viewModel.clearLoginMessages()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { snackbarData ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF446F5C)),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = snackbarData.visuals.message,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(brush = gradientBackground)
                .verticalScroll(scrollState)
                .padding(horizontal = contentPadding)
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            //Back Button
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(top = 22.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(
                    onClick = {
                        onBackButtonClicked()
                        viewModel.viewModelScope.launch {
                            delay(1000)
                            viewModel.clearLoginForm()
                        }
                    },
                    modifier = Modifier
                        .shadow(4.dp, RoundedCornerShape(12.dp))
                        .background(Color.White, RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF446F5C),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.login),
                    fontSize = titleFontSize,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF446F5C),
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    //Email
                    OutlinedTextField(
                        value = loginState.email,
                        onValueChange = viewModel::onLoginEmailChange,
                        label = { Text("Email Address", fontWeight = FontWeight.Medium) },
                        placeholder = { Text("Enter your email", color = Color(0xFF9CA3AF)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Email,
                                contentDescription = "Email",
                                tint = Color(0xFF446F5C)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF446F5C),
                            focusedLabelColor = Color(0xFF446F5C),
                            cursorColor = Color(0xFF446F5C)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    //Password
                    OutlinedTextField(
                        value = loginState.password,
                        onValueChange = viewModel::onLoginPasswordChange,
                        label = { Text("Password", fontWeight = FontWeight.Medium) },
                        placeholder = { Text("Enter your password", color = Color(0xFF9CA3AF)) },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = image,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = Color(0xFF6B7280)
                                )
                            }
                        },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "Password",
                                tint = Color(0xFF446F5C)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF446F5C),
                            focusedLabelColor = Color(0xFF446F5C),
                            cursorColor = Color(0xFF446F5C)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    //Forgot Password
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Text(
                            text = "Forgot password?",
                            style = TextStyle(
                                fontSize = forgotPasswordFontSize,
                                textDecoration = TextDecoration.Underline
                            ),
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF446F5C),
                            modifier = Modifier.clickable { onForgotPasswordClicked() }
                        )
                    }

                    Spacer(modifier = Modifier.height(33.dp))

                    //Login Button
                    ElevatedButton(
                        onClick = viewModel::login,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(buttonHeight),
                        enabled = !loginState.isLoading &&
                                loginState.email.isNotBlank() &&
                                loginState.password.isNotBlank(),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = Color(0xFF446F5C),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFFE5E7EB),
                            disabledContentColor = Color(0xFF9CA3AF)
                        ),
                        elevation = ButtonDefaults.elevatedButtonElevation(
                            defaultElevation = 6.dp,
                            pressedElevation = 2.dp,
                            disabledElevation = 0.dp
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (loginState.isLoading) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Logging In...",
                                    fontSize = buttonFontSize,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        } else {
                            Text(
                                text = "Login",
                                fontSize = buttonFontSize,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    loginState.errorMessage?.let { errorMessage ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFEE2E2)
                            )
                        ) {
                            Text(
                                text = errorMessage,
                                color = Color(0xFFDC2626),
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(12.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}