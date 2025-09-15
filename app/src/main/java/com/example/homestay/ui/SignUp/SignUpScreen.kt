package com.example.homestay.ui.SignUp

import android.app.DatePickerDialog
import android.util.Log
import android.widget.DatePicker
import androidx.benchmark.traceprocessor.Row
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.homestay.AuthViewModel
import com.example.homestay.R
import java.util.Calendar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(isTablet: Boolean,
                 viewModel: AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
                 onSuccess:() -> Unit,
                 onBackButtonClicked:() -> Unit,
                 modifier: Modifier = Modifier){

    val signUpState by viewModel.signUpState.collectAsState()
    val scrollState = rememberScrollState()
    val shouldClearForm by viewModel.shouldClearSignUpForm.collectAsState()
    val roleOptions = listOf("Guest","Host")
    val genderOptions = listOf("Male","Female","Prefer not to say")
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            val newDate = "$dayOfMonth/${month + 1}/$year"
            viewModel.onSignUpBirthdateChange(newDate)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
    )

    val snackbarHostState = remember { SnackbarHostState() }

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
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->

        Column(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(scrollState)
            .background(color = Color(0xFFFEF9F3))) {

            //Back Button
            OutlinedButton(onClick = {
                onBackButtonClicked()
                viewModel.viewModelScope.launch {
                    delay(1000)
                    viewModel.clearSignUpForm()
                } },
                modifier = Modifier.padding(top = 60.dp)
                    .padding(start = 25.dp)) {

                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black,
                    modifier = Modifier.size(35.dp)
                )
            }

            Text(
                text = stringResource(R.string.sign_up),
                fontSize = 41.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .padding(top = 20.dp)
                    .padding(horizontal = 27.dp)
            )
            Spacer(modifier = Modifier.height(22.dp))

            //Username
            OutlinedTextField(
                value = signUpState.username,
                onValueChange = { viewModel.onSignUpUsernameChange(it) },
                label = {
                    Text(text = "Username",
                        fontWeight = FontWeight.Bold)
                },
                placeholder = {
                    Text(text = "Enter username",
                        fontWeight = FontWeight.Medium)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF446F5C),
                    unfocusedBorderColor = Color(0xFF446F5C),
                    errorBorderColor = Color.Red
                ),
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
            )
            if (!signUpState.usernameError.isNullOrEmpty()) {
                Text(
                    text = signUpState.usernameError!!,
                    color = Color.Red,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            //Role Selection
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(40.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 8.dp)
                ){
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
                            text = if (role == "Guest") "Guest" else "Host",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            //Gender
            ExposedDropdownMenuBox(expanded = expanded,
                onExpandedChange = { expanded = !expanded }) {

                OutlinedTextField(
                    value = signUpState.gender,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Gender",
                        fontWeight = FontWeight.Bold) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF446F5C),
                        unfocusedBorderColor = Color(0xFF446F5C),
                        errorBorderColor = Color.Red
                    ),
                    modifier = Modifier.menuAnchor()
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                )

                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    genderOptions.forEach { gender ->
                        DropdownMenuItem(
                            text = { Text(text = gender, fontWeight = FontWeight.Bold) },
                            onClick = {
                                viewModel.onSignUpGenderChange(gender)
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(29.dp))

            //Birthdate
            OutlinedTextField(value = signUpState.birthdate,
                onValueChange = {},
                label = { Text("Birthdate",
                    fontWeight = FontWeight.Bold) },
                readOnly = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF446F5C),
                    unfocusedBorderColor = Color(0xFF446F5C),
                    errorBorderColor = Color.Red
                ),
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 24.dp),
                trailingIcon = {
                    IconButton(onClick = { datePickerDialog.show() }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Select birthdate")
                    }
                })

            Spacer(modifier = Modifier.height(29.dp))

            //Email
            OutlinedTextField(
                value = signUpState.email,
                onValueChange = { viewModel.onSignUpEmailChange(it) },
                label = {
                    Text(text = "Email",
                        fontWeight = FontWeight.Bold)
                },
                placeholder = {
                    Text(text = "Enter email",
                        fontWeight = FontWeight.Bold)
                },
                isError = signUpState.emailError != null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF446F5C),
                    unfocusedBorderColor = Color(0xFF446F5C),
                    errorBorderColor = Color.Red
                ),
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
            )
            if (signUpState.emailError != null) {
                Text(text = signUpState.emailError!!, color = Color.Red)
            }

            Spacer(modifier = Modifier.height(29.dp))

            //Password
            OutlinedTextField(
                value = signUpState.password,
                onValueChange = { viewModel.onSignUpPasswordChange(it) },
                label = { Text("Password",
                    fontWeight = FontWeight.Bold) },

                placeholder = { Text("Enter password",
                    fontWeight = FontWeight.Bold) },

                isError = signUpState.passwordError != null,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = if (passwordVisible) "Hide password" else "Show password")
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF446F5C),
                    unfocusedBorderColor = Color(0xFF446F5C),
                    errorBorderColor = Color.Red
                ),
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
            )
            if (signUpState.passwordError != null) {
                Text(text = signUpState.passwordError!!,
                    color = Color.Red,
                    modifier = Modifier.padding(horizontal = 24.dp))
            }

            Spacer(modifier = Modifier.height(29.dp))

            //Confirm Password
            OutlinedTextField(
                value = signUpState.confirmPassword,
                onValueChange = { viewModel.onSignUpConfirmPasswordChange(it) },
                label = {
                    Text(text = "Confirm password",
                        fontWeight = FontWeight.Bold)
                },
                placeholder = {
                    Text(text = "Enter password",
                        fontWeight = FontWeight.Bold)
                },
                isError = signUpState.confirmPasswordError != null,
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(imageVector = image, contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password")
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF446F5C),
                    unfocusedBorderColor = Color(0xFF446F5C),
                    errorBorderColor = Color.Red
                ),
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
            )
            if (!signUpState.confirmPasswordError.isNullOrEmpty()) {
                Text(
                    text = signUpState.confirmPasswordError!!,
                    color = Color.Red,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            //Sign Up Button
            ElevatedButton(
                onClick = { viewModel.signUp() },
                modifier = Modifier.align(Alignment.CenterHorizontally),
                enabled = !signUpState.isLoading,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xFF446F5C),
                    contentColor = Color.White
                )) {

                Text(
                    if (signUpState.isLoading) "Signing Up..." else "Sign Up",
                    fontSize = 23.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            signUpState.errorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = Color.Red, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp))
            }
        }
    }
}