package com.example.homestay.ui.EditProfile

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homestay.AuthViewModel
import com.example.homestay.R
import com.example.homestay.UserProfile
import java.util.Calendar

@Composable
fun EditProfileScreen(
    isTablet: Boolean,
    viewModel: AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onBackButtonClicked: () -> Unit,
    onSaveSuccess: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val editProfileState by viewModel.editProfileState.collectAsState()

    var newUsername by remember { mutableStateOf("") }
    var newGender by remember { mutableStateOf("") }
    var newBirthdate by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                val newDate = String.format("%02d/%02d/%d", dayOfMonth, month + 1, year)
                newBirthdate = newDate
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    LaunchedEffect(editProfileState.successMessage) {
        editProfileState.successMessage?.let {
            onSaveSuccess()
        }
    }

    // Initialize fields with current profile data
    LaunchedEffect(uiState.userProfile) {
        uiState.userProfile?.let { profile ->
            // Don't overwrite user input, only set initial values if fields are empty
            if (newUsername.isEmpty()) newUsername = ""
            if (newGender.isEmpty()) newGender = ""
            if (newBirthdate.isEmpty()) newBirthdate = ""
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        Column(
            modifier = Modifier
                .background(color = Color(0xFFFEF9F3))
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            //Back Button
            OutlinedButton(
                onClick = { onBackButtonClicked() },
                modifier = Modifier.padding(top = 90.dp)
                    .padding(start = 22.dp)
            ) {

                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black,
                    modifier = Modifier.size(35.dp)
                )
            }

            Text(
                text = "Edit Profile",
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .padding(top = 35.dp)
                    .padding(horizontal = 25.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            uiState.userProfile?.let { profile ->
                Text(
                    text = "Current Username:",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 25.dp)
                )

                Text(
                    text = profile.username.ifEmpty { "Not set" },
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(horizontal = 25.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(7.dp))

                //New Username
                OutlinedTextField(
                    value = newUsername,
                    onValueChange = { newUsername = it },
                    label = {
                        Text(
                            text = "New Username",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    placeholder = {
                        Text(
                            text = "Enter new username",
                            fontWeight = FontWeight.Medium
                        )
                    },
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth(),
                    enabled = !editProfileState.isLoading
                )

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "Current Email:",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 25.dp)
                )

                Text(
                    text = profile.email.ifEmpty { "Not set" },
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(horizontal = 25.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Email cannot be changed from this screen",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray,
                    style = androidx.compose.ui.text.TextStyle(
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    ),
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "Current Gender:",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 25.dp)
                )

                Text(
                    text = profile.gender.ifEmpty { "Not set" },
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(horizontal = 25.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(7.dp))

                //New Gender
                OutlinedTextField(
                    value = newGender,
                    onValueChange = { },
                    readOnly = true,
                    label = {
                        Text(
                            text = "New Gender",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    placeholder = {
                        Text(
                            text = "Select new gender",
                            fontWeight = FontWeight.Medium
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Dropdown"
                            )
                        }
                    },
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth()
                        .clickable { expanded = !expanded },
                    enabled = !editProfileState.isLoading
                )

                // Gender Dropdown Menu
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    listOf("Male", "Female", "Prefer not to say").forEach { genderOption ->
                        DropdownMenuItem(
                            text = { Text(genderOption) },
                            onClick = {
                                newGender = genderOption
                                expanded = false
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "Current Birthdate:",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 25.dp)
                )

                Text(
                    text = profile.birthdate.ifEmpty { "Not set" },
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(horizontal = 25.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(7.dp))

                //New Birthdate
                OutlinedTextField(
                    value = newBirthdate,
                    onValueChange = { newBirthdate = it },
                    label = {
                        Text(
                            "Birthdate",
                            fontWeight = FontWeight.Bold
                        )
                    },
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
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = "Select birthdate"
                            )
                        }
                    })

                Spacer(modifier = Modifier.height(30.dp))

                //Save Button
                ElevatedButton(
                    onClick = {
                        val updatedProfile = UserProfile(
                            userId = profile.userId,
                            username = if (newUsername.isNotBlank()) newUsername.trim() else profile.username,
                            email = profile.email, //Email cannot be changed
                            gender = if (newGender.isNotBlank()) newGender.trim() else profile.gender,
                            birthdate = if (newBirthdate.isNotBlank()) newBirthdate.trim() else profile.birthdate,
                            createdAt = profile.createdAt
                        )
                        viewModel.updateUserProfile(updatedProfile)
                    },
                    enabled = !editProfileState.isLoading &&
                            (newUsername.isNotBlank() || newGender.isNotBlank() || newBirthdate.isNotBlank()),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = Color(0xFF446F5C),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = if (editProfileState.isLoading) "Saving..." else "Save Changes",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

            } ?: run {
                Text(
                    text = "Loading profile...",
                    fontSize = 18.sp,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            //Error Message
            editProfileState.errorMessage?.let { errorMsg ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMsg,
                    color = Color.Red,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    }
}