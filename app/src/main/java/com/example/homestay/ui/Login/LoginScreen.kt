package com.example.homestay.ui.Login

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.homestay.AuthViewModel
import com.example.homestay.LoginState
import com.example.homestay.R
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewModelScope
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
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        val titleFontSize = if (isTablet) 45.sp else 32.sp
        val buttonFontSize = if (isTablet) 20.sp else 18.sp
        val forgotPasswordFontSize = if (isTablet) 18.sp else 16.sp

        if (isTablet) {
            //Tablet layout
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color(0xFFFEF9F3))
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp)
                    .padding(innerPadding)
            ) {
                OutlinedButton(
                    onClick = { onBackButtonClicked()
                        viewModel.viewModelScope.launch {
                            delay(1000)
                            viewModel.clearLoginForm()
                        }},
                    modifier = Modifier.padding(top = 40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.Black,
                        modifier = Modifier.size(35.dp)
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))

                Text(
                    text = stringResource(R.string.login),
                    fontSize = titleFontSize,
                    fontWeight = FontWeight.ExtraBold,
                )

                Spacer(modifier = Modifier.height(30.dp))

                OutlinedTextField(
                    value = loginState.email,
                    onValueChange = viewModel::onLoginEmailChange,
                    label = { Text("Email", fontWeight = FontWeight.Bold) },
                    placeholder = { Text("Enter email", fontWeight = FontWeight.Medium) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(
                        imageVector = Icons.Filled.Email,
                        contentDescription = "Email"
                    ) }
                )

                Spacer(modifier = Modifier.height(35.dp))

                OutlinedTextField(
                    value = loginState.password,
                    onValueChange = viewModel::onLoginPasswordChange,
                    label = { Text("Password", fontWeight = FontWeight.Bold) },
                    placeholder = { Text("Enter password", fontWeight = FontWeight.Medium) },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = if (passwordVisible) "Hide password" else "Show password")
                        }
                    },
                    singleLine = true,
                    leadingIcon = { Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Password"
                    ) }
                )

                Spacer(modifier = Modifier.height(23.dp))

                Text(
                    text = "Forgot password?",
                    style = TextStyle(
                        fontSize = forgotPasswordFontSize,
                        textDecoration = TextDecoration.Underline
                    ),
                    fontWeight = FontWeight.Bold,
                    color = Color.Blue,
                    modifier = Modifier.clickable { onForgotPasswordClicked() }
                )

                Spacer(modifier = Modifier.height(60.dp))

                ElevatedButton(
                    onClick = viewModel::login,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    enabled = !loginState.isLoading,
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = Color(0xFF446F5C),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = if (loginState.isLoading) "Logging In..." else "Login",
                        fontSize = buttonFontSize,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }

                loginState.errorMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = it, color = Color.Red, fontWeight = FontWeight.Bold)
                }
            }

        } else {
            //Phone layout
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color(0xFFFEF9F3))
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp)
                    .padding(innerPadding)
            ) {
                OutlinedButton(
                    onClick = { onBackButtonClicked()
                        viewModel.viewModelScope.launch {
                            delay(1000)
                            viewModel.clearLoginForm()
                        }},
                    modifier = Modifier.padding(top = 40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.Black,
                        modifier = Modifier.size(35.dp)
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))

                Text(
                    text = stringResource(R.string.login),
                    fontSize = titleFontSize,
                    fontWeight = FontWeight.ExtraBold,
                )

                Spacer(modifier = Modifier.height(30.dp))

                OutlinedTextField(
                    value = loginState.email,
                    onValueChange = viewModel::onLoginEmailChange,
                    label = { Text("Email", fontWeight = FontWeight.Bold) },
                    placeholder = { Text("Enter email", fontWeight = FontWeight.Medium) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(
                        imageVector = Icons.Filled.Email,
                        contentDescription = "Email"
                    ) }
                )

                Spacer(modifier = Modifier.height(35.dp))

                OutlinedTextField(
                    value = loginState.password,
                    onValueChange = viewModel::onLoginPasswordChange,
                    label = { Text("Password", fontWeight = FontWeight.Bold) },
                    placeholder = { Text("Enter password", fontWeight = FontWeight.Medium) },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = if (passwordVisible) "Hide password" else "Show password")
                        }
                    },
                    singleLine = true,
                    leadingIcon = { Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Password"
                    ) }
                )

                Spacer(modifier = Modifier.height(23.dp))

                Text(
                    text = ("Forgot password?"),
                    style = TextStyle(
                        fontSize = forgotPasswordFontSize,
                        textDecoration = TextDecoration.Underline
                    ),
                    fontWeight = FontWeight.Bold,
                    color = Color.Blue,
                    modifier = Modifier.clickable { onForgotPasswordClicked() }
                )

                Spacer(modifier = Modifier.height(60.dp))

                ElevatedButton(
                    onClick = viewModel::login,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    enabled = !loginState.isLoading,
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = Color(0xFF446F5C),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = if (loginState.isLoading) "Logging In..." else "Login",
                        fontSize = buttonFontSize,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }

                loginState.errorMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = it, color = Color.Red, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}