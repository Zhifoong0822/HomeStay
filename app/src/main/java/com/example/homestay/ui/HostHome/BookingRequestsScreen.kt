
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.homestay.HomeStayScreen
import com.example.homestay.data.model.Booking
import com.example.homestay.ui.booking.BookingViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostBookingRequestsScreen(
    navController: NavController,
    bookingVM: BookingViewModel,
    hostId = hostId
) {
    // Extract hostId from nav arguments
    val hostId = navController.currentBackStackEntry
        ?.arguments
        ?.getString("hostId")
        ?: ""

    val bookings by bookingVM.bookings.collectAsStateWithLifecycle()
    val loading by bookingVM.loading.collectAsStateWithLifecycle()
    val error by bookingVM.error.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Collect fire-and-forget messages
    LaunchedEffect(Unit) {
        bookingVM.message.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Load bookings for this host
    LaunchedEffect(hostId) {
        if (hostId.isNotEmpty()) {
            bookingVM.loadHostBookings(hostId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📩 Booking Requests") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate(HomeStayScreen.HostHome.name) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                error != null -> Text(
                    text = error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> {
                    val pendingBookings = bookings.filter { it.status == "PENDING" }
                    if (pendingBookings.isEmpty()) {
                        Text(
                            "No booking requests",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(pendingBookings, key = { it.bookingId }) { booking ->
                                BookingRequestCard(
                                    booking = booking,
                                    onAccept = {
                                        scope.launch {
                                            bookingVM.updateBookingStatus(booking, "ACCEPTED")
                                        }
                                    },
                                    onReject = {
                                        scope.launch {
                                            bookingVM.updateBookingStatus(booking, "REJECTED")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun BookingRequestCard(
    booking: Booking,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Guest ID: ${booking.userId}", fontWeight = FontWeight.Bold)
            Text("Home ID: ${booking.homeId}")
            Text("Check-in: ${booking.checkInDate}")
            Text("Check-out: ${booking.checkOutDate}")
            Text("Guests: ${booking.numberOfGuests}")

            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = onAccept) {
                    Text("Accept")
                }
                OutlinedButton(onClick = onReject) {
                    Text("Reject")
                }
            }
        }
    }
}