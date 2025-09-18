// ClientBrowseScreen.kt
package com.example.homestay.ui.client

import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.homestay.data.model.Booking
import com.example.homestay.data.model.Home
import com.example.homestay.ui.booking.BookingViewModel
import com.example.homestay.ui.property.PropertyListingViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientBrowseScreen(
    vm: PropertyListingViewModel,
    bookingVm: BookingViewModel,
    navController: NavController,
    onBottomHome: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onBottomExplore: () -> Unit, // kept for compatibility
    onBottomProfile: () -> Unit
) {
    val homes by vm.homesCloud.collectAsState(initial = emptyList())
    val bookings by bookingVm.bookings.collectAsState(initial = emptyList())

    var selectedHome by remember { mutableStateOf<Home?>(null) }
    var showBookingHistory by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    // search
    var query by rememberSaveable { mutableStateOf("") }
    val filteredHomes = remember(homes, query) {
        if (query.isBlank()) homes
        else homes.filter { it.name.contains(query, ignoreCase = true) }
    }

    // ensure current user set in VM
    LaunchedEffect(Unit) {
        FirebaseAuth.getInstance().currentUser?.uid?.let { bookingVm.setCurrentUser(it) }
    }

    // collect one-shot messages
    LaunchedEffect(Unit) {
        bookingVm.message.collect { msg -> snackbar.showSnackbar(msg) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Available Homes") }) },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = Color(0xFFFEF9F3),
        bottomBar = {
            BottomNavBar(
                onHomeClick = onBottomHome,
                onBookingsClick = {
                    val uid = FirebaseAuth.getInstance().currentUser?.uid
                    if (uid != null) {
                        bookingVm.setCurrentUser(uid)
                        bookingVm.loadUserBookings(uid)
                        showBookingHistory = true
                    } else {
                        scope.launch { snackbar.showSnackbar("Please sign in to view bookings.") }
                    }
                },
                onProfileClick = onBottomProfile
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
        ) {
            // search bar
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                placeholder = { Text("Search by property name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )

            if (filteredHomes.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No properties found.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredHomes, key = { it.id }) { home ->
                        ClientHomeCard(
                            home = home,
                            onBookClick = { selectedHome = home }
                        )
                    }
                }
            }
        }

        // booking dialog
        selectedHome?.let { home ->
            BookingDialog(
                home = home,
                onDismiss = { selectedHome = null },
                onConfirm = { checkInDate, checkOutDate, guests, nights ->
                    val uid = FirebaseAuth.getInstance().currentUser?.uid
                    if (uid == null) {
                        scope.launch { snackbar.showSnackbar("Please sign in to make a booking.") }
                        return@BookingDialog
                    }

                    val booking = Booking(
                        bookingId = UUID.randomUUID().toString(),
                        userId = uid,
                        homeId = home.id,
                        hostId = home.hostId,
                        checkInDate = checkInDate,
                        checkOutDate = checkOutDate,
                        numberOfGuests = guests,
                        nights = nights,
                        pricePerNight = home.pricePerNight ?: 0.0
                    )

                    scope.launch {
                        val result = bookingVm.createBooking(booking)
                        if (result.isSuccess) {
                            snackbar.showSnackbar("Booking successful!")
                            bookingVm.loadUserBookings(uid)
                        } else {
                            snackbar.showSnackbar(result.exceptionOrNull()?.message ?: "Booking failed.")
                        }
                        selectedHome = null
                    }
                }
            )
        }

        val homeById = remember(homes) { homes.associateBy { it.id } }

        // booking history
        if (showBookingHistory) {
            BookingHistoryDialog(
                bookings = bookings,
                homeById = homeById,
                onDismiss = { showBookingHistory = false },
                onCancel = { id -> scope.launch { bookingVm.cancelBooking(id) } },
                onReschedule = { id ->
                    scope.launch {
                        val newIn = Date()
                        val newOut = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 2) }.time
                        bookingVm.rescheduleBooking(id, newIn, newOut)
                    }
                },
                onCheckIn = { id -> scope.launch { bookingVm.checkIn(id) } },
                onCheckOut = { id -> scope.launch { bookingVm.checkOut(id) } }
            )
        }
    }
}

/* ---------- Bottom bar ---------- */

@Composable
private fun BottomNavBar(
    onHomeClick: () -> Unit,
    onBookingsClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = false,
            onClick = onHomeClick,
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = false,
            onClick = onBookingsClick,
            icon = { Icon(Icons.Default.Book, contentDescription = "Bookings") },
            label = { Text("Bookings") }
        )
        NavigationBarItem(
            selected = false,
            onClick = onProfileClick,
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
            label = { Text("Profile") }
        )
    }
}

/* ---------- Home list card ---------- */

@Composable
private fun ClientHomeCard(
    home: Home,
    onBookClick: () -> Unit
) {
    val price = home.pricePerNight ?: 0.0

    Card(onClick = onBookClick) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = home.photoUris.firstOrNull(),
                    contentDescription = home.name,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(home.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(home.location, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "RM ${"%.2f".format(price)} / night",
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
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Filled.Book, contentDescription = "Book")
                Spacer(Modifier.width(8.dp))
                Text("Book Now")
            }
        }
    }
}

/* ---------- Booking History Dialog ---------- */

@Composable
fun BookingHistoryDialog(
    bookings: List<Booking>,
    homeById: Map<String, Home>,
    onDismiss: () -> Unit,
    onCancel: (String) -> Unit,
    onReschedule: (String) -> Unit,
    onCheckIn: (String) -> Unit,
    onCheckOut: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("My Bookings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))

                if (bookings.isEmpty()) {
                    Text("No bookings yet.")
                } else {
                    val visibleBookings = remember(bookings) {
                        bookings.filter { it.status?.uppercase(Locale.getDefault()) != "CANCELLED" }
                    }

                    LazyColumn {
                        items(visibleBookings, key = { it.bookingId }) { booking ->
                            val home = homeById[booking.homeId]
                            val pricePerNight = home?.pricePerNight ?: booking.pricePerNight ?: 0.0
                            val total = pricePerNight * booking.nights

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text("Booking ID: ${booking.bookingId}", fontWeight = FontWeight.Bold)
                                    if (home != null) Text("Home: ${home.name}")
                                    Text("Check-in: ${booking.checkInDate}")
                                    Text("Check-out: ${booking.checkOutDate}")
                                    Text("Guests: ${booking.numberOfGuests}")
                                    Text("Nights: ${booking.nights}")
                                    Text("Price: RM ${"%.2f".format(pricePerNight)} / night")
                                    Text("Total: RM ${"%.2f".format(total)}")

                                    Spacer(Modifier.height(8.dp))

                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)   // space between buttons
                                    ) {
                                        //cancel
                                        FilledTonalButton(
                                            onClick = { onCancel(booking.bookingId) },
                                            modifier =  Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.Cancel, contentDescription = "Cancel")
                                            Spacer(Modifier.width(6.dp))
                                            Text("Cancel")
                                        }

                                        //reschedule
                                        FilledTonalButton(
                                            onClick = { onReschedule(booking.bookingId) },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.EventRepeat, contentDescription = "Reschedule")
                                            Spacer(Modifier.width(6.dp))
                                            Text("Reschedule")
                                        }
                                    }

                                    Spacer(Modifier.height(8.dp))

                                    // Row 2 (actually a Box): primary action isolated so it can't collapse
                                    val primaryColors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )

                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        when (booking.status ) {
                                            "CHECKED_IN" -> {
                                                Button(
                                                    onClick = { onCheckOut(booking.bookingId) },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.primary,
                                                        contentColor = MaterialTheme.colorScheme.onPrimary ),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Icon(Icons.Default.ExitToApp, contentDescription = "Check out")
                                                    Spacer(Modifier.width(8.dp))
                                                    Text("Check Out")
                                                }
                                            }
                                            "COMPLETED", "CANCELLED" -> {
                                                // no primary action
                                            }
                                            else -> {
                                                Button(
                                                    onClick = { onCheckIn(booking.bookingId) },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.primary,
                                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                                    ),
                                                    modifier = Modifier.fillMaxWidth(),
                                                ) {
                                                    Icon(Icons.Default.Login, contentDescription = "Check in")
                                                    Spacer(Modifier.width(8.dp))
                                                    Text("Check In")
                                                }
                                            }
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


/* ---------- Booking Dialog (make a new booking) ---------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookingDialog(
    home: Home,
    onDismiss: () -> Unit,
    onConfirm: (checkInDate: Date, checkOutDate: Date, guests: Int, nights: Int) -> Unit
) {
    var guestsText by rememberSaveable { mutableStateOf("1") }
    var nightsText by rememberSaveable { mutableStateOf("1") }
    var checkInDate by remember { mutableStateOf(Date()) }

    val checkOutDate = remember(checkInDate, nightsText) {
        val n = nightsText.toIntOrNull()?.coerceIn(1, 30) ?: 1
        Calendar.getInstance().apply {
            time = checkInDate
            add(Calendar.DAY_OF_YEAR, n)
        }.time
    }

    var showDatePicker by remember { mutableStateOf(false) }
    val fmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val price = home.pricePerNight ?: 0.0

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(Modifier.padding(24.dp)) {
                Text("Book ${home.name}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.People, contentDescription = null)
                    Spacer(Modifier.width(8.dp)); Text("Guests:"); Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = guestsText,
                        onValueChange = { t -> if (t.length <= 2 && (t.all(Char::isDigit) || t.isEmpty())) guestsText = t },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(80.dp),
                        singleLine = true
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.DateRange, contentDescription = null)
                    Spacer(Modifier.width(8.dp)); Text("Nights:"); Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = nightsText,
                        onValueChange = { t -> if (t.length <= 2 && (t.all(Char::isDigit) || t.isEmpty())) nightsText = t },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(80.dp),
                        singleLine = true
                    )
                }

                Spacer(Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }
                ) {
                    Icon(Icons.Filled.DateRange, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Check-in: ${fmt.format(checkInDate)}")
                }

                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.DateRange, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Check-out: ${fmt.format(checkOutDate)}")
                }

                Spacer(Modifier.height(16.dp))

                val nights = nightsText.toIntOrNull()?.coerceIn(1, 30) ?: 1
                val total = price * nights
                Text("Total: RM ${"%.2f".format(total)}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)

                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val g = guestsText.toIntOrNull()?.coerceIn(1, 20) ?: 1
                        val n = nightsText.toIntOrNull()?.coerceIn(1, 30) ?: 1
                        onConfirm(checkInDate, checkOutDate, g, n)
                    }) { Text("Confirm Booking") }
                }
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = checkInDate.time)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { checkInDate = Date(it) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = state, showModeToggle = false)
        }
    }
}
