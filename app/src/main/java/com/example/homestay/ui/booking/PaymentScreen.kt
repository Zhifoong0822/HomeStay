package com.example.homestay.ui.booking

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PaymentScreen(
    onBack: () -> Unit,
    onConfirm: () -> Unit
) {
    Column(Modifier.padding(16.dp)) {
        Text("Payment", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        Text("💳 Enter payment details here... (mock)")

        Spacer(Modifier.height(16.dp))

        Button(onClick = onConfirm) {
            Text("Confirm Payment")
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(onClick = onBack) {
            Text("Back")
        }
    }
}