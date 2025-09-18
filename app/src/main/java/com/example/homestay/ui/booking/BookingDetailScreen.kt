package com.example.homestay.ui.booking

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.homestay.data.model.Booking
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailScreen(
    booking: Booking,
    bookingVm: BookingViewModel,
    navController: NavHostController,
    homeName: String?,
    onReschedule: (Booking) -> Unit,
    onCancel: (String) -> Unit,
    onPay: (Booking) -> Unit,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Booking Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Top
        ) {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

            Text("Booking ID: ${booking.bookingId}", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                text = homeName?.let { "Home: $it" } ?: "Home: —",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
            )
            Text("Guests: ${booking.numberOfGuests}")
            Text("Check-in: ${booking.checkInDate}")
            Text("Check-out: ${booking.checkOutDate}")
            Text("Nights: ${booking.nights}")
            Text("Price per Night: RM${booking.pricePerNight}")
            Spacer(Modifier.height(4.dp))
            val pricePerNight = booking.pricePerNight
            if (pricePerNight != null) {
                Text("Price / night: RM ${"%.2f".format(pricePerNight)}")
                Text("Total: RM ${"%.2f".format(pricePerNight * booking.nights)}")
            } else {
                Text("Price / night: —")
                Text("Total: —")
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (currentUserId != null) {
                        coroutineScope.launch {
                            bookingVm.cancelBooking(booking.bookingId)
                            onCancel(booking.bookingId)
                            onBack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Cancel Booking")
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = { onReschedule(booking) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reschedule Booking")
            }

            Spacer(Modifier.height(12.dp))

        }
    }
}
