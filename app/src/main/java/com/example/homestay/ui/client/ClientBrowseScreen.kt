// ClientBrowseScreen.kt
package com.example.homestay.ui.client

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.homestay.data.model.Home
import com.example.homestay.data.model.Booking
import com.example.homestay.ui.booking.BookingViewModel
import com.example.homestay.ui.property.PropertyListingViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientBrowseScreen(
    vm: PropertyListingViewModel,
    bookingVm: BookingViewModel,
    onBottomHome: () -> Unit,
    onBottomExplore: () -> Unit,
    onBottomProfile: () -> Unit
) {
    val homes by vm.homes.collectAsState(initial = emptyList())
    val bookings by bookingVm.bookings.collectAsState(initial = emptyList())

    var selectedHome by remember { mutableStateOf<Home?>(null) }
    var showBookingHistory by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                onHomeClick = onBottomHome,
                onExploreClick = onBottomExplore,
                onProfileClick = onBottomProfile,
                onBookingsClick = { showBookingHistory = true }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            items(homes) { home ->
                ClientHomeCard(
                    home = home,
                    onBookClick = { selectedHome = home }
                )
            }
        }

        // Booking Dialog (when clicking Book Now)
        selectedHome?.let { home ->
            SimpleBookingDialog(
                home = home,
                onDismiss = { selectedHome = null },
                onConfirm = { checkIn, checkOut, guests, nights ->
                    val booking = Booking(
                        bookingId = UUID.randomUUID().toString(),
                        userId = "CURRENT_USER_ID", // TODO: replace with FirebaseAuth
                        homeId = home.id,
                        hostId = home.hostId,
                        checkInDate = checkIn,
                        checkOutDate = checkOut,
                        numberOfGuests = guests,
                        nights = nights,
                        pricePerNight = home.pricePerNight
                    )

                    coroutineScope.launch {
                        bookingVm.createBooking(booking)
                    }
                    selectedHome = null
                }
            )
        }

        // Booking History Dialog (from bottom bar)
        if (showBookingHistory) {
            BookingHistoryDialog(
                bookings = bookings,
                onDismiss = { showBookingHistory = false },
                onCancel = { bookingId -> coroutineScope.launch { bookingVm.cancelBooking(bookingId) } },
                onReschedule = { bookingId ->
                    coroutineScope.launch {
                        val newCheckIn = Date()
                        val newCheckOut = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 2) }.time
                        bookingVm.rescheduleBooking(bookingId, newCheckIn, newCheckOut)
                    }
                },
                onPay = { bookingId ->
                    coroutineScope.launch { bookingVm.markBookingAsPaid(bookingId) }
                }
            )
        }
    }
}

@Composable
fun BottomNavigationBar(
    onHomeClick: () -> Unit,
    onExploreClick: () -> Unit,
    onProfileClick: () -> Unit,
    onBookingsClick: () -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") },
            selected = false,
            onClick = onHomeClick
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Search, contentDescription = "Explore") },
            label = { Text("Explore") },
            selected = false,
            onClick = onExploreClick
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
            label = { Text("Profile") },
            selected = false,
            onClick = onProfileClick
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Book, contentDescription = "Bookings") },
            label = { Text("Bookings") },
            selected = false,
            onClick = onBookingsClick
        )
    }
}

@Composable
private fun ClientHomeCard(
    home: Home,
    onBookClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onBookClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = home.photoUris.firstOrNull(),
                    contentDescription = home.name,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        home.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        home.location,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "$${home.pricePerNight}/night",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (home.description.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    home.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onBookClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Filled.Book, contentDescription = "Book")
                Spacer(Modifier.width(8.dp))
                Text("Book Now")
            }
        }
    }
}

@Composable
fun BookingHistoryDialog(
    bookings: List<Booking>,
    onDismiss: () -> Unit,
    onCancel: (String) -> Unit,
    onReschedule: (String) -> Unit,
    onPay: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "My Bookings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))

                if (bookings.isEmpty()) {
                    Text("No bookings yet.")
                } else {
                    LazyColumn {
                        items(bookings) { booking ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text("Booking ID: ${booking.bookingId}", fontWeight = FontWeight.Bold)
                                    Text("Check-in: ${booking.checkInDate}")
                                    Text("Check-out: ${booking.checkOutDate}")
                                    Text("Guests: ${booking.numberOfGuests}")
                                    Text("Nights: ${booking.nights}")
                                    Text("Total: $${booking.pricePerNight * booking.nights}")

                                    Spacer(Modifier.height(8.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        TextButton(onClick = { onCancel(booking.bookingId) }) {
                                            Text("Cancel")
                                        }
                                        TextButton(onClick = { onReschedule(booking.bookingId) }) {
                                            Text("Reschedule")
                                        }
                                        TextButton(onClick = { onPay(booking.bookingId) }) {
                                            Text("Pay Now")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Close")
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleBookingDialog(
    home: Home,
    onDismiss: () -> Unit,
    onConfirm: (checkIn: Date, checkOut: Date, guests: Int, nights: Int) -> Unit
) {
    var guests by remember { mutableIntStateOf(1) }
    var nights by remember { mutableIntStateOf(1) }
    var checkInDate by remember { mutableStateOf(Date()) }

    val checkOutDate = remember(checkInDate, nights) {
        Calendar.getInstance().apply {
            time = checkInDate
            add(Calendar.DAY_OF_YEAR, nights)
        }.time
    }

    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Book ${home.name}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Guests input
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.People, contentDescription = "Guests")
                    Spacer(Modifier.width(8.dp))
                    Text("Guests:")
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = guests.toString(),
                        onValueChange = {
                            val newValue = it.toIntOrNull() ?: 1
                            guests = newValue.coerceIn(1, 20)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(80.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Nights input
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.DateRange, contentDescription = "Nights")
                    Spacer(Modifier.width(8.dp))
                    Text("Nights:")
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = nights.toString(),
                        onValueChange = {
                            val newValue = it.toIntOrNull() ?: 1
                            nights = newValue.coerceIn(1, 30)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(80.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Check-in
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                ) {
                    Icon(Icons.Filled.DateRange, contentDescription = "Dates")
                    Spacer(Modifier.width(8.dp))
                    Text("Check-in: ${dateFormatter.format(checkInDate)}")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Check-out
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.DateRange, contentDescription = "Dates")
                    Spacer(Modifier.width(8.dp))
                    Text("Check-out: ${dateFormatter.format(checkOutDate)}")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Price calculation
                val totalPrice = home.pricePerNight * nights
                Text(
                    "Total: $${"%.2f".format(totalPrice)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Cancel")
                    }

                    Spacer(Modifier.width(8.dp))

                    Button(
                        onClick = { onConfirm(checkInDate, checkOutDate, guests, nights) }
                    ) {
                        Text("Confirm Booking")
                    }
                }
            }
        }
    }

    // Date Picker
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = checkInDate.time)

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        checkInDate = Date(it)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = false
            )
        }
    }
}
