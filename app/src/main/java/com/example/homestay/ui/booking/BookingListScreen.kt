package com.example.homestay.ui.booking

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.homestay.data.model.Booking

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingListScreen(
    bookingVm: BookingViewModel,
    userId: String,
    homeName: String?,
    onBookingClick: (Booking) -> Unit
) {
    val bookings by bookingVm.bookings.collectAsState()
    val loading by bookingVm.loading.collectAsState()

    LaunchedEffect(Unit) {
        bookingVm.loadUserBookings(userId)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("My Bookings") }) }
    ) { padding ->
        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(bookings) { booking ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clickable { onBookingClick(booking) }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Property: ${booking.homeId}", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = homeName?.let { "Home: $it" } ?: "Home: —",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                            )
                            Text("Check-in: ${booking.checkInDate}")
                            Text("Check-out: ${booking.checkOutDate}")
                            Text("Total: RM${booking.totalPrice}")
                            Text("Status: ${booking.status}")
                            Text("Payment: ${booking.paymentStatus}")
                        }
                    }
                }
            }
        }
    }
}
