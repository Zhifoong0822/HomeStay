package com.example.homestay.ui.HostHome

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.homestay.data.model.Booking
import com.example.homestay.data.repository.PropertyListingRepository
import com.example.homestay.ui.booking.BookingViewModel
import com.example.homestay.ui.property.PropertyListingViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingRequestsScreen(
    navController: NavController,
    bookingVM: BookingViewModel,
    propertyVM: PropertyListingViewModel) {
    val primaryColor = Color(0xFF446F5C)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val bookings by bookingVM.hostBookings.collectAsState(initial = emptyList())
    val pendingBookings = bookings.filter { it.status == "PENDING" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📩 Booking Requests", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = primaryColor),
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                }
            )
        },
        bottomBar = { HostBottomBar(navController = navController) },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->

        if (pendingBookings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No pending booking requests")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(pendingBookings, key = { it.bookingId }) { booking ->

                    // State to hold the home name
                    val homeName = remember(booking.homeId) { mutableStateOf("Loading...") }

                    // Fetch home name asynchronously
                    LaunchedEffect(booking.homeId) {
                        val name = propertyVM.getHomeNameById(booking.homeId)
                        homeName.value = name ?: "Unknown Home"
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Guest: ${booking.userId}", style = MaterialTheme.typography.titleMedium)
                            Text("Homestay: ${homeName.value}")
                            Text("Dates: ${booking.checkInDate} – ${booking.checkOutDate}")
                            Spacer(Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            bookingVM.updateBookingStatus(
                                                booking.bookingId,
                                                "CONFIRMED"
                                            )
                                            snackbarHostState.showSnackbar("Accepted booking for ${booking.userId}")
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                                ) { Text("Accept") }

                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            bookingVM.updateBookingStatus(
                                                booking.bookingId,
                                                "CANCELLED"
                                            )
                                            snackbarHostState.showSnackbar("Rejected booking for ${booking.userId}")
                                        }
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor)
                                ) { Text("Reject") }
                            }
                        }
                    }
                }
            }
        }
    }
}
