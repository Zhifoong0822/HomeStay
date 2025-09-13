package com.example.homestay.ui.HostHome

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

// Mock data class (later replace with real Booking entity)
data class PastBooking(
    val id: Int,
    val guestName: String,
    val homestayName: String,
    val dates: String,
    val status: String // "Accepted" or "Rejected"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingHistoryScreen(navController: NavController) {
    // Temporary data
    val pastBookings = listOf(
        PastBooking(1, "Alice", "Seaview Villa", "1–3 Sept", "Accepted"),
        PastBooking(2, "Bob", "Mountain Cabin", "5–8 Sept", "Rejected"),
        PastBooking(3, "Charlie", "City Apartment", "12–15 Aug", "Accepted"),
        PastBooking(4, "Diana", "Beach Bungalow", "20–22 Aug", "Accepted")
    )

    val brandColor = Color(0xFF446F5C)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📜 Booking History") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = brandColor,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            HostBottomBar(navController = navController)
        },
        containerColor = Color(0xFFFEF9F3) // same background as HomeScreen
    ) { padding ->
        if (pastBookings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No booking history yet", color = brandColor, fontWeight = FontWeight.Medium)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(pastBookings, key = { it.id }) { booking ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Guest: ${booking.guestName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = brandColor)
                            Text("Homestay: ${booking.homestayName}")
                            Text("Dates: ${booking.dates}")
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = booking.status,
                                color = if (booking.status == "Accepted") brandColor else Color.Red,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
