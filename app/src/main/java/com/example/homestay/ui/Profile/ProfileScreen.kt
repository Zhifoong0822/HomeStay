package com.example.homestay.ui.Profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.example.homestay.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homestay.AuthViewModel
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Logout
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.PasswordVisualTransformation

@Composable
fun ProfileScreen(
    isTablet: Boolean,
    viewModel: AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onBackButtonClicked: () -> Unit,
    onDeleteAccountClicked: () -> Unit = {},
    onLogoutClicked: () -> Unit = {},
    onEditProfileClicked: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState = viewModel.uiState.collectAsState()
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    val editProfileState = viewModel.editProfileState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.refreshUserProfile()
    }

    LaunchedEffect(editProfileState.value.successMessage) {
        editProfileState.value.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearEditProfileForm()
        }
    }

    Scaffold(
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) }
    ) { padding ->
        val topPadding = if (isTablet) 30.dp else 32.dp
        val widthPadding = if (isTablet) 250.dp else 100.dp
        val imageSize = if (isTablet) 180.dp else 100.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = Color(0xFFFEF9F3))
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                //Back Button
                OutlinedButton( onClick = { onBackButtonClicked() },
                    modifier = Modifier.padding(top = 35.dp, start = 10.dp)
                ){
                    Icon( imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.Black,
                        modifier = Modifier.size(30.dp) ) }

                Spacer(modifier = Modifier.weight(1.2f))

                Text(text = "PROFILE",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color = Color.Black,
                    modifier = Modifier.padding(top = 32.dp))

                Spacer(modifier = Modifier.weight(1f))

                //Edit profile
                OutlinedButton(
                    onClick = { onEditProfileClicked() },
                    modifier = Modifier.padding(top = topPadding, end = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profile",
                        tint = Color.Blue,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Edit",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Blue
                    )
                }
            }

            Spacer(modifier = Modifier.height(25.dp))

            //Profile Picture
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(R.drawable.profile_pic),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(imageSize)
                        .clip(CircleShape)
                        .border(3.dp, Color.White, CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(33.dp))

            uiState.value.userProfile?.let { profile ->
                OutlinedCard(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF1FAF5)
                    ),
                    border = BorderStroke(4.dp, Color(0xFF446F5C)),
                    elevation = CardDefaults.cardElevation(0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Username",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray)
                        Text(profile.username,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black)

                        Spacer(modifier = Modifier.height(6.dp))

                        Text("Email",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray)
                        Text(profile.email,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black)

                        Spacer(modifier = Modifier.height(6.dp))

                        Text("Gender",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray)
                        Text(profile.gender,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black)

                        Spacer(modifier = Modifier.height(6.dp))

                        Text("Birthdate",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray)
                        Text(profile.birthdate,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black)
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    //Delete account
                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.fillMaxWidth(0.7f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Account",
                            tint = Color.Red,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Delete Account",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                    }

                    //Delete Account Confirmation Prompt
                    if (showDeleteDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            confirmButton = {
                                OutlinedButton(
                                    onClick = {
                                        val userId = uiState.value.userProfile?.userId
                                        val email = uiState.value.userProfile?.email ?: ""

                                        if (!userId.isNullOrEmpty() && email.isNotEmpty() && passwordInput.isNotEmpty()) {
                                            viewModel.deleteAccount(userId, email, passwordInput)
                                            onDeleteAccountClicked()
                                            showDeleteDialog = false
                                        }
                                    }
                                ) {
                                    Text("Delete Account")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteDialog = false }) {
                                    Text("Cancel")
                                }
                            },
                            title = { Text("Confirm Delete") },
                            text = {
                                Column {
                                    Text("Please enter your password to confirm account deletion.")
                                    OutlinedTextField(
                                        value = passwordInput,
                                        onValueChange = { passwordInput = it },
                                        label = { Text("Password") },
                                        visualTransformation = PasswordVisualTransformation()
                                    )
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    //Logout
                    OutlinedButton(
                        onClick = {
                            viewModel.logout()
                            onLogoutClicked() },
                        modifier = Modifier.fillMaxWidth(0.7f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Log out",
                            tint = Color.Red,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Log out",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                    }
                }
            } ?: Text(
                text = "No profile loaded",
                modifier = Modifier.padding(16.dp),
                color = Color.Red
            )
        }
    }
}