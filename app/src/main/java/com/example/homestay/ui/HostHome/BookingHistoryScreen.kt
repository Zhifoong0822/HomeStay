package com.example.homestay.ui.HostHome

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.homestay.data.model.Booking
import com.example.homestay.data.repository.PropertyListingRepository
import com.example.homestay.ui.booking.BookingViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingHistoryScreen(
    navController: NavController,
    bookingVM: BookingViewModel,
    homeRepo: PropertyListingRepository
) {
    val brandColor = Color(0xFF446F5C)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Collect all bookings for this host
    val bookings by bookingVM.hostBookings.collectAsState(initial = emptyList())

    // Map to store homeId -> homeName
    val homesByHost = remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    // Get current host ID
    val currentHostId = FirebaseAuth.getInstance().currentUser?.uid

    // Filter bookings by this host
    val hostBookings = bookings.filter { it.hostId == currentHostId }

    // Fetch home names asynchronously
    LaunchedEffect(currentHostId) {
        if (currentHostId != null) {
            val homeEntities = homeRepo.getHomesByHostId(currentHostId)
            homesByHost.value = homeEntities.associate { it.id to it.name }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📜 Booking History") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = brandColor,
                    titleContentColor = Color.White
                ),
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFFEF9F3)
    ) { padding ->
        if (hostBookings.isEmpty()) {
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
                items(hostBookings, key = { it.bookingId }) { booking ->
                    val homeName = homesByHost.value[booking.homeId] ?: booking.homeId

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Guest: ${booking.userId}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = brandColor
                            )
                            Text("Homestay: $homeName")
                            Text("Dates: ${booking.checkInDate} – ${booking.checkOutDate}")
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = booking.status,
                                color = if (booking.status.uppercase() == "CONFIRMED") brandColor else Color.Red,
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
