package com.example.homestay.ui.booking

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.homestay.data.model.Booking

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingHistoryScreen(
    bookingVm: BookingViewModel,
    onReschedule: (String) -> Unit,
    onCancel: (String) -> Unit,
    onBack: () -> Unit
) {
    val bookings by bookingVm.bookings.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Bookings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            items(bookings) { booking ->
                BookingCard(
                    booking = booking,
                    onReschedule = { onReschedule(booking.bookingId) },
                    onCancel = { onCancel(booking.bookingId) }
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun BookingCard(
    booking: Booking,
    onReschedule: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Booking ID: ${booking.bookingId}", style = MaterialTheme.typography.bodySmall)
            Text("Check-in: ${booking.checkInDate}", style = MaterialTheme.typography.bodyMedium)
            Text("Check-out: ${booking.checkOutDate}", style = MaterialTheme.typography.bodyMedium)
            Text("Guests: ${booking.numberOfGuests}", style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(8.dp))

            Row {
                OutlinedButton(onClick = onReschedule) {
                    Text("Reschedule")
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}