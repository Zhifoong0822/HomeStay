package com.example.homestay.ui.booking

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.homestay.data.model.Booking
import kotlinx.coroutines.launch
import java.util.*

@Composable
fun BookingScreen(
    bookingVm: BookingViewModel,
    onPayment: () -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var guests by remember { mutableStateOf(1) }
    var nights by remember { mutableStateOf(1) }
    val loading by bookingVm.loading.collectAsState()
    val error by bookingVm.error.collectAsState()

    Column(Modifier.padding(16.dp)) {
        Text("Book Property", style = MaterialTheme.typography.titleLarge)

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = guests.toString(),
            onValueChange = { guests = it.toIntOrNull() ?: 1 },
            label = { Text("Guests") }
        )

        OutlinedTextField(
            value = nights.toString(),
            onValueChange = { nights = it.toIntOrNull() ?: 1 },
            label = { Text("Nights") }
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                val booking = Booking(
                    bookingId = UUID.randomUUID().toString(),
                    userId = "sampleUser",
                    homeId = "sampleHome",
                    hostId = "sampleHost",
                    numberOfGuests = guests,
                    nights = nights,
                    pricePerNight = 100.0,
                    checkInDate = Date(),
                    checkOutDate = Date()
                )
                scope.launch {
                    val result = bookingVm.createBooking(booking)
                    if (result.isSuccess) {
                        onPayment()
                    }
                }
            },
            enabled = !loading
        ) {
            Text("Proceed to Payment")
        }

        if (error != null) {
            Text("Error: $error", color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(onClick = onBack) {
            Text("Back")
        }
    }
}
