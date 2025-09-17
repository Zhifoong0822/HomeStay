package com.example.homestay.ui.ForgotPassword

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homestay.AuthViewModel
import com.example.homestay.R

@Composable
fun ForgotPasswordScreen(
    isTablet: Boolean,
    viewModel: AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onBackButtonClicked: () -> Unit,
    modifier: Modifier = Modifier
) {

    val resetPasswordState by viewModel.resetPasswordState.collectAsState()
    var email by remember { mutableStateOf("") }

    LaunchedEffect(resetPasswordState.successMessage) {
        if (resetPasswordState.successMessage != null) {
            //Add a delay before navigating back
            kotlinx.coroutines.delay(2500)
            onBackButtonClicked()
            viewModel.clearResetPasswordMessages()
        }
    }

    Box(
        modifier = Modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFEF9F3),
                        Color(0xFFF8F4EE),
                        Color(0xFFF1EDE6)
                    )
                )
            )
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {

            //Back Button
            OutlinedButton(
                onClick = { onBackButtonClicked() },
                modifier = Modifier
                    .padding(top = 60.dp, start = 20.dp),
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

            Spacer(modifier = Modifier.height(40.dp))

            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = Color(0xFF2D5A4A).copy(alpha = 0.1f)
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(32.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Email Icon
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF4CAF50).copy(alpha = 0.1f),
                                        Color(0xFF2D5A4A).copy(alpha = 0.05f)
                                    )
                                ),
                                shape = RoundedCornerShape(20.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Reset Password",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF2D5A4A),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Enter your email address and we'll send you a link to reset your password.",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF666666),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    //Email
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = {
                            Text(
                                text = "Email",
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2D5A4A)
                            )
                        },
                        placeholder = {
                            Text(
                                text = "Enter your email",
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF999999)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Email",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !resetPasswordState.isLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4CAF50),
                            unfocusedBorderColor = Color(0xFFCCCCCC),
                            focusedLabelColor = Color(0xFF4CAF50),
                            cursorColor = Color(0xFF4CAF50)
                        )
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    //Send Reset Link Button
                    ElevatedButton(
                        onClick = {
                            viewModel.resetPassword(email.trim())
                        },
                        enabled = email.isNotBlank() && !resetPasswordState.isLoading,
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = Color(0xFF4CAF50),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFFCCCCCC),
                            disabledContentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.elevatedButtonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp
                        )
                    ) {
                        Text(
                            text = if (resetPasswordState.isLoading) "Sending..." else "Send Reset Link",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    //Error Message
                    resetPasswordState.errorMessage?.let { errorMsg ->
                        Spacer(modifier = Modifier.height(20.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Color(0xFFFFEBEE),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Text(
                                text = errorMsg,
                                color = Color(0xFFD32F2F),
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    //Success Message
                    resetPasswordState.successMessage?.let { successMsg ->
                        Spacer(modifier = Modifier.height(20.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Color(0xFFE8F5E8),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(16.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = "Success",
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = successMsg,
                                    color = Color(0xFF2E7D32),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}