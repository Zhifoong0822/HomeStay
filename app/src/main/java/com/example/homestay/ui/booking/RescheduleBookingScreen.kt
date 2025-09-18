package com.example.homestay.ui.booking

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun RescheduleBookingScreen(
    bookingId: String,
    bookingVm: BookingViewModel,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var newNights by remember { mutableStateOf(1) }

    Column(Modifier.padding(16.dp)) {
        Text("Reschedule Booking", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = newNights.toString(),
            onValueChange = { newNights = it.toIntOrNull() ?: 1 },
            label = { Text("New Nights") }
        )

        Spacer(Modifier.height(16.dp))

        Button(onClick = {
            scope.launch {
                // TODO: call update booking in repository
                onBack()
            }
        }) {
            Text("Confirm Reschedule")
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(onClick = onBack) {
            Text("Back")
        }
    }
}