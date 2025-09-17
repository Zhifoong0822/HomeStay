package com.example.homestay.ui.booking

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun CancelBookingScreen(
    bookingId: String,
    bookingVm: BookingViewModel,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    Column(Modifier.padding(16.dp)) {
        Text("Cancel Booking", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        Text("Are you sure you want to cancel booking: $bookingId ?")

        Spacer(Modifier.height(16.dp))

        Button(onClick = {
            scope.launch {
                // TODO: call cancel booking in repository
                onBack()
            }
        }) {
            Text("Confirm Cancel")
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(onClick = onBack) {
            Text("Back")
        }
    }
}
