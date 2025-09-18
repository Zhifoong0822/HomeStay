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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailScreen(
    booking: Booking,
    bookingVm: BookingViewModel,
    navController: NavHostController,
    onReschedule: (Booking) -> Unit,
    onCancel: (String) -> Unit,
    onPay: (Booking) -> Unit,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // --- Date formatting helper ---
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val checkInFormatted = remember(booking.checkInDate) { dateFormat.format(booking.checkInDate) }
    val checkOutFormatted = remember(booking.checkOutDate) { dateFormat.format(booking.checkOutDate) }

    // --- Dialog States ---
    var showCancelDialog by remember { mutableStateOf(false) }
    var showRescheduleDialog by remember { mutableStateOf(false) }

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

            // Booking Details
            Text("Booking ID: ${booking.bookingId}", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Guests: ${booking.numberOfGuests}")
            Text("Check-in: $checkInFormatted")
            Text("Check-out: $checkOutFormatted")
            Text("Nights: ${booking.nights}")
            Text("Price per Night: RM${booking.pricePerNight}")
            Spacer(Modifier.height(4.dp))
            Text(
                "Total Price: RM${booking.totalPrice}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))

            // Cancel Booking Button
            Button(
                onClick = { showCancelDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Cancel Booking")
            }

            Spacer(Modifier.height(12.dp))

            // Reschedule Booking Button
            Button(
                onClick = { showRescheduleDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reschedule Booking")
            }

            Spacer(Modifier.height(12.dp))

            // Pay Now Button - only visible if payment is still pending
            if (booking.paymentStatus == "PENDING") {
                Button(
                    onClick = { onPay(booking) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("Pay Now")
                }
            }
        }
    }

    // ---- Cancel Confirmation Dialog ----
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Booking") },
            text = { Text("Are you sure you want to cancel this booking? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelDialog = false
                        coroutineScope.launch {
                            bookingVm.cancelBooking(booking.bookingId)
                            onCancel(booking.bookingId)
                            onBack()
                        }
                    }
                ) { Text("Yes, Cancel") }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("No")
                }
            }
        )
    }

    // ---- Reschedule Confirmation Dialog ----
    if (showRescheduleDialog) {
        AlertDialog(
            onDismissRequest = { showRescheduleDialog = false },
            title = { Text("Reschedule Booking") },
            text = { Text("Do you want to reschedule this booking to different dates?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRescheduleDialog = false
                        onReschedule(booking)
                    }
                ) { Text("Yes, Reschedule") }
            },
            dismissButton = {
                TextButton(onClick = { showRescheduleDialog = false }) {
                    Text("No")
                }
            }
        )
    }
}