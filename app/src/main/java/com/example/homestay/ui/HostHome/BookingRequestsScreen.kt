package com.example.homestay.ui.HostHome

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

// temporary mock data
data class BookingRequest(
    val id: Int,
    val guestName: String,
    val homestayName: String,
    val dates: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingRequestsScreen(navController: NavController) {
    var requests by remember {
        mutableStateOf(
            listOf(
                BookingRequest(1, "Alice", "Seaview Villa", "12–15 Sept"),
                BookingRequest(2, "Bob", "Mountain Cabin", "20–22 Sept"),
                BookingRequest(3, "Charlie", "City Apartment", "1–3 Oct")
            )
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val primaryColor = Color(0xFF446F5C)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📩 Booking Requests", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryColor
                )
            )
        },
        bottomBar = {
            HostBottomBar(navController = navController)
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        if (requests.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No booking requests yet")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(requests, key = { it.id }) { request ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Guest: ${request.guestName}", style = MaterialTheme.typography.titleMedium)
                            Text("Homestay: ${request.homestayName}")
                            Text("Dates: ${request.dates}")
                            Spacer(Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        requests = requests.filterNot { it.id == request.id }
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Accepted booking for ${request.guestName}")
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = primaryColor
                                    )
                                ) {
                                    Text("Accept")
                                }
                                OutlinedButton(
                                    onClick = {
                                        requests = requests.filterNot { it.id == request.id }
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Rejected booking for ${request.guestName}")
                                        }
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = primaryColor
                                    )
                                ) {
                                    Text("Reject")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
