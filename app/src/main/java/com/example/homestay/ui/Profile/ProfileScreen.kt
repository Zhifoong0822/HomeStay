package com.example.homestay.ui.Profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.material3.ButtonDefaults
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
        val message = editProfileState.value.successMessage
        if (!message.isNullOrEmpty()) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearEditProfileSuccessMessage()
            viewModel.clearEditProfileForm()
        }
    }

    Scaffold(
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) }
    ) { padding ->
        val topPadding = if (isTablet) 30.dp else 32.dp
        val widthPadding = if (isTablet) 250.dp else 100.dp
        val imageSize = if (isTablet) 180.dp else 120.dp

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFEF9F3),
                            Color(0xFFF8F4EE)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    //Back Button
                    OutlinedButton(
                        onClick = { onBackButtonClicked() },
                        modifier = Modifier.padding(top = 37.dp, start = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF2D5A4A)
                        ),
                        border = BorderStroke(1.5.dp, Color(0xFF2D5A4A)),
                        elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF2D5A4A),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1.2f))

                    //Profile Title
                    Text(
                        text = "PROFILE",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        color = Color(0xFF2D5A4A),
                        modifier = Modifier.padding(top = 33.dp)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    //Edit Profile Button
                    OutlinedButton(
                        onClick = { onEditProfileClicked() },
                        modifier = Modifier.padding(top = topPadding, end = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFF4CAF50),
                            contentColor = Color.White
                        ),
                        border = BorderStroke(0.dp, Color.Transparent),
                        elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Profile",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Edit",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(35.dp))

                //Profile Picture
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(imageSize + 8.dp)
                            .shadow(
                                elevation = 8.dp,
                                shape = CircleShape,
                                ambientColor = Color(0xFF2D5A4A).copy(alpha = 0.1f)
                            )
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color.White, Color(0xFFF5F5F5))
                                ),
                                shape = CircleShape
                            )
                    ) {
                        Image(
                            painter = painterResource(R.drawable.profile_pic),
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .size(imageSize)
                                .clip(CircleShape)
                                .border(4.dp, Color.White, CircleShape)
                                .align(Alignment.Center)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                uiState.value.userProfile?.let { profile ->
                    OutlinedCard(
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        border = BorderStroke(2.dp, Color(0xFF4CAF50)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(16.dp),
                                ambientColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                            )
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ProfileField("Username", profile.username)
                            Spacer(modifier = Modifier.height(4.dp))
                            ProfileField("Email", profile.email)
                            Spacer(modifier = Modifier.height(4.dp))
                            ProfileField("Gender", profile.gender)
                            Spacer(modifier = Modifier.height(4.dp))
                            ProfileField("Birthdate", profile.birthdate)
                        }
                    }

                    Spacer(modifier = Modifier.height(50.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        //Delete account
                        OutlinedButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFFE53E3E)
                            ),
                            border = BorderStroke(2.dp, Color(0xFFE53E3E)),
                            elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Account",
                                tint = Color(0xFFE53E3E),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Delete Account",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFE53E3E)
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
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = Color(0xFFE53E3E),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Delete Account")
                                    }
                                },
                                dismissButton = {
                                    TextButton(
                                        onClick = { showDeleteDialog = false },
                                        colors = ButtonDefaults.textButtonColors(
                                            contentColor = Color(0xFF666666)
                                        )
                                    ) {
                                        Text("Cancel")
                                    }
                                },
                                title = {
                                    Text(
                                        "Confirm Delete",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2D5A4A)
                                    )
                                },
                                text = {
                                    Column {
                                        Text(
                                            "Please enter your password to confirm account deletion.",
                                            color = Color(0xFF666666),
                                            modifier = Modifier.padding(bottom = 12.dp)
                                        )
                                        OutlinedTextField(
                                            value = passwordInput,
                                            onValueChange = { passwordInput = it },
                                            label = { Text("Password") },
                                            visualTransformation = PasswordVisualTransformation(),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(16.dp)
                            )
                        }

                        //Logout Button
                        OutlinedButton(
                            onClick = {
                                viewModel.logout()
                                onLogoutClicked()
                            },
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFFFF6B6B),
                                contentColor = Color.White
                            ),
                            border = BorderStroke(0.dp, Color.Transparent),
                            elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Log out",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Log out",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                } ?: Text(
                    text = "No profile loaded",
                    modifier = Modifier.padding(16.dp),
                    color = Color(0xFFE53E3E),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ProfileField(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF666666),
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF2D5A4A)
        )
    }
}