package com.example.homestay.ui.ForgotPassword

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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

    // Handle success message and auto-navigate back after delay
    LaunchedEffect(resetPasswordState.successMessage) {
        if (resetPasswordState.successMessage != null) {
            //Add a delay before navigating back
            kotlinx.coroutines.delay(2500)
            onBackButtonClicked()
            viewModel.clearResetPasswordMessages()
        }
    }

    Column(
        modifier = Modifier
            .background(color = Color(0xFFFEF9F3))
            .fillMaxSize()
    ) {

        //Back Button
        OutlinedButton(onClick = { onBackButtonClicked() },
            modifier = Modifier.padding(top = 100.dp)
                .padding(start = 25.dp)) {

            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.Black,
                modifier = Modifier.size(35.dp)
            )
        }

        Text(
            text = "Reset Password",
            fontSize = 42.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier
                .padding(top = 60.dp)
                .padding(horizontal = 30.dp)
        )

        Spacer(modifier = Modifier.height(50.dp))

        Text(
            text = "Enter your email address and we'll send you a link to reset your password.",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .padding(horizontal = 30.dp)
                .fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(40.dp))

        //Email
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = {
                Text(
                    text = "Email",
                    fontWeight = FontWeight.Bold
                )
            },
            placeholder = {
                Text(
                    text = "Enter your email",
                    fontWeight = FontWeight.Medium
                )
            },
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth(),
            enabled = !resetPasswordState.isLoading
        )

        Spacer(modifier = Modifier.height(50.dp))

        //Send Reset Link Button
        ElevatedButton(
            onClick = {
                viewModel.resetPassword(email.trim())
            },
            enabled = email.isNotBlank() && !resetPasswordState.isLoading,
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
                text = if (resetPasswordState.isLoading) "Sending..." else "Send Reset Link",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        //Error Message
        resetPasswordState.errorMessage?.let { errorMsg ->
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = errorMsg,
                color = Color.Red,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        //Success Message
        resetPasswordState.successMessage?.let { successMsg ->
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = successMsg,
                color = Color(0xFF4CAF50),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
            )
        }
    }
}