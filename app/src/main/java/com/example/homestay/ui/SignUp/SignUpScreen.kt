package com.example.homestay.ui.SignUp

import android.app.DatePickerDialog
import android.util.Log
import android.widget.DatePicker
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.example.homestay.AuthViewModel
import com.example.homestay.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    isTablet: Boolean,
    viewModel: AuthViewModel = viewModel(),
    onSuccess: () -> Unit,
    onBackButtonClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val signUpState by viewModel.signUpState.collectAsState()
    val scrollState = rememberScrollState()
    val shouldClearForm by viewModel.shouldClearSignUpForm.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    val roleOptions = listOf("Guest", "Host")
    val genderOptions = listOf("Male", "Female", "Prefer not to say")
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var genderExpanded by remember { mutableStateOf(false) }

    val contentPadding = if (isTablet) 40.dp else 24.dp
    val titleFontSize = if (isTablet) 36.sp else 33.sp
    val buttonHeight = if (isTablet) 56.dp else 52.dp
    val buttonFontSize = if (isTablet) 18.sp else 16.sp

    val gradientBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFEF9F3),
            Color(0xFFF5F0EA),
            Color(0xFFEFE8E0)
        )
    )

    // Date picker dialog
    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            val newDate = "$dayOfMonth/${month + 1}/$year"
            viewModel.onSignUpBirthdateChange(newDate)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    //clear only when shouldClearForm = true
    LaunchedEffect(shouldClearForm) {
        if (shouldClearForm) {
            viewModel.forceCleanState()
        }
    }

    LaunchedEffect(signUpState.successMessage) {
        if (signUpState.successMessage != null) {
            snackbarHostState.showSnackbar(signUpState.successMessage!!)
            onSuccess()
            viewModel.clearSignUpForm()
            viewModel.clearSignUpMessages()
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
                            viewModel.clearSignUpForm()
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

            Spacer(modifier = Modifier.height(2.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.sign_up),
                    fontSize = titleFontSize,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF446F5C),
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

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
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    //Username
                    Column {
                        OutlinedTextField(
                            value = signUpState.username,
                            onValueChange = viewModel::onSignUpUsernameChange,
                            label = { Text("Username", fontWeight = FontWeight.Medium) },
                            placeholder = { Text("Enter your username", color = Color(0xFF9CA3AF)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = "Username",
                                    tint = Color(0xFF446F5C)
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF446F5C),
                                focusedLabelColor = Color(0xFF446F5C),
                                cursorColor = Color(0xFF446F5C),
                                errorBorderColor = Color(0xFFDC2626)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            isError = !signUpState.usernameError.isNullOrEmpty()
                        )
                        if (!signUpState.usernameError.isNullOrEmpty()) {
                            Text(
                                text = signUpState.usernameError!!,
                                color = Color(0xFFDC2626),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                            )
                        }
                    }

                    //Role Selection
                    Column {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                roleOptions.forEach { role ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { viewModel.onSignUpRoleChange(role) }
                                    ) {
                                        RadioButton(
                                            selected = (signUpState.role == role),
                                            onClick = { viewModel.onSignUpRoleChange(role) },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = Color(0xFF446F5C)
                                            )
                                        )
                                        Text(
                                            text = role,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    //Gender
                    ExposedDropdownMenuBox(
                        expanded = genderExpanded,
                        onExpandedChange = { genderExpanded = !genderExpanded }
                    ) {
                        OutlinedTextField(
                            value = signUpState.gender,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Gender", fontWeight = FontWeight.Medium) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Wc,
                                    contentDescription = "Gender",
                                    tint = Color(0xFF446F5C)
                                )
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF446F5C),
                                focusedLabelColor = Color(0xFF446F5C),
                                cursorColor = Color(0xFF446F5C)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = genderExpanded,
                            onDismissRequest = { genderExpanded = false }
                        ) {
                            genderOptions.forEach { gender ->
                                DropdownMenuItem(
                                    text = { Text(text = gender, fontWeight = FontWeight.Medium) },
                                    onClick = {
                                        viewModel.onSignUpGenderChange(gender)
                                        genderExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    //Birthdate
                    OutlinedTextField(
                        value = signUpState.birthdate,
                        onValueChange = {},
                        label = { Text("Birthdate", fontWeight = FontWeight.Medium) },
                        placeholder = { Text("Select your birthdate", color = Color(0xFF9CA3AF)) },
                        readOnly = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.DateRange,
                                contentDescription = "Birthdate",
                                tint = Color(0xFF446F5C)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { datePickerDialog.show() }) {
                                Icon(
                                    Icons.Default.CalendarToday,
                                    contentDescription = "Select birthdate",
                                    tint = Color(0xFF446F5C)
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF446F5C),
                            focusedLabelColor = Color(0xFF446F5C),
                            cursorColor = Color(0xFF446F5C)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Email field
                    Column {
                        OutlinedTextField(
                            value = signUpState.email,
                            onValueChange = viewModel::onSignUpEmailChange,
                            label = { Text("Email Address", fontWeight = FontWeight.Medium) },
                            placeholder = { Text("Enter your email", color = Color(0xFF9CA3AF)) },
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
                                cursorColor = Color(0xFF446F5C),
                                errorBorderColor = Color(0xFFDC2626)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            isError = signUpState.emailError != null
                        )
                        if (signUpState.emailError != null) {
                            Text(
                                text = signUpState.emailError!!,
                                color = Color(0xFFDC2626),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                            )
                        }
                    }

                    //Password
                    Column {
                        OutlinedTextField(
                            value = signUpState.password,
                            onValueChange = viewModel::onSignUpPasswordChange,
                            label = { Text("Password", fontWeight = FontWeight.Medium) },
                            placeholder = { Text("Enter your password", color = Color(0xFF9CA3AF)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = "Password",
                                    tint = Color(0xFF446F5C)
                                )
                            },
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
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF446F5C),
                                focusedLabelColor = Color(0xFF446F5C),
                                cursorColor = Color(0xFF446F5C),
                                errorBorderColor = Color(0xFFDC2626)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            isError = signUpState.passwordError != null
                        )
                        if (signUpState.passwordError != null) {
                            Text(
                                text = signUpState.passwordError!!,
                                color = Color(0xFFDC2626),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                            )
                        }
                    }

                    //Confirm Password
                    Column {
                        OutlinedTextField(
                            value = signUpState.confirmPassword,
                            onValueChange = viewModel::onSignUpConfirmPasswordChange,
                            label = { Text("Confirm Password", fontWeight = FontWeight.Medium) },
                            placeholder = { Text("Confirm your password", color = Color(0xFF9CA3AF)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = "Confirm Password",
                                    tint = Color(0xFF446F5C)
                                )
                            },
                            trailingIcon = {
                                val image = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(
                                        imageVector = image,
                                        contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
                                        tint = Color(0xFF6B7280)
                                    )
                                }
                            },
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF446F5C),
                                focusedLabelColor = Color(0xFF446F5C),
                                cursorColor = Color(0xFF446F5C),
                                errorBorderColor = Color(0xFFDC2626)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            isError = !signUpState.confirmPasswordError.isNullOrEmpty()
                        )
                        if (!signUpState.confirmPasswordError.isNullOrEmpty()) {
                            Text(
                                text = signUpState.confirmPasswordError!!,
                                color = Color(0xFFDC2626),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    //Sign Up Button
                    ElevatedButton(
                        onClick = viewModel::signUp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(buttonHeight),
                        enabled = !signUpState.isLoading,
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
                        if (signUpState.isLoading) {
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
                                    text = "Signing Up...",
                                    fontSize = buttonFontSize,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        } else {
                            Text(
                                text = "Sign Up",
                                fontSize = buttonFontSize,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    signUpState.errorMessage?.let { errorMessage ->
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