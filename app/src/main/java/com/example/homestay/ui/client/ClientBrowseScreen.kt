package com.example.homestay.ui.client

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
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
import java.util.Date


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientBrowseScreen(
    navController: NavController,
    vm: PropertyListingViewModel,
    bookingVm: BookingViewModel,
    onBottomHome: () -> Unit,
    onBottomExplore: () -> Unit, // kept for compatibility (unused)
    onBottomProfile: () -> Unit
) {
    val homes by vm.homes.collectAsState(initial = emptyList())
    val bookings by bookingVm.bookings.collectAsState(initial = emptyList())

    // who is logged in
    val currentUid = remember { FirebaseAuth.getInstance().currentUser?.uid.orEmpty() }

// observe check-in map (homeId|userId -> CheckStatus)
    val checkMap by vm.checkMap.collectAsState()

    var selectedHome by remember { mutableStateOf<Home?>(null) }
    var showBookingHistory by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    // search (top)
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(homes, query) {
        if (query.isBlank()) homes else homes.filter { it.name.contains(query, ignoreCase = true) }
    }

    // helper to read whether this booking is currently checked in
    fun isBookingCheckedIn(homeId: String): Boolean =
        vm.isCheckedIn(homeId, currentUid)

    val buttonsHiddenForBooking = remember { mutableStateMapOf<String, Boolean>() }
    fun isButtonsHidden(b: Booking) = buttonsHiddenForBooking[b.bookingId] == true
    fun hideButtonsFor(b: Booking) { buttonsHiddenForBooking[b.bookingId] = true }


    Scaffold(
        topBar = { TopAppBar(title = { Text("SKY BNB") }) },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = Color(0xFFFEF9F3),
        bottomBar = {
            BottomNavBar(
                onHomeClick = onBottomHome,
                onBookingsClick = {
                    val uid = FirebaseAuth.getInstance().currentUser?.uid
                    if (uid != null) {
                        bookingVm.setCurrentUser(uid)
                        bookingVm.loadUserBookings()
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
            // top search bar
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

            if (filtered.isEmpty()) {
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
                    items(filtered, key = { it.id }) { home ->
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
                onConfirm = { checkIn, checkOut, guests, nights ->
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
                        checkInDate = checkIn,
                        checkOutDate = checkOut,
                        numberOfGuests = guests,
                        nights = nights,
                        pricePerNight = home.pricePerNight
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


        // booking history dialog
        if (showBookingHistory) {
            BookingHistoryDialog(
                bookings = bookings,

                // NEW: show/hide logic for the row of check buttons
                isCheckedIn = { booking -> vm.isCheckedIn(booking.homeId, currentUid) },
                canCheckInNow = { booking ->
                    val now = Date()
                    now >= booking.checkInDate && now < booking.checkOutDate
                },
                isButtonsHidden = { booking -> isButtonsHidden(booking) },

                // NEW: button actions
                onCheckIn = { booking ->
                    val now = Date()
                    if (now >= booking.checkInDate && now < booking.checkOutDate) {
                        scope.launch {
                            vm.toggleCheck(booking.homeId, currentUid)
                            snackbar.showSnackbar("Checked in successfully")
                        }
                    } else {
                        scope.launch { snackbar.showSnackbar("You can only check in on/after the check-in date.") }
                    }
                },
                onCheckOut = { booking ->
                    scope.launch {
                        vm.toggleCheck(booking.homeId, currentUid)
                        snackbar.showSnackbar("Checked out successfully")
                        hideButtonsFor(booking) // hide further check buttons for this booking
                    }
                },

                // existing props
                onDismiss = { showBookingHistory = false },
                onCancel = { id -> scope.launch { bookingVm.cancelBooking(id) } },
                onReschedule = { id ->
                    scope.launch {
                        val newIn = Date()
                        val newOut = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 2) }.time
                        bookingVm.rescheduleBooking(id, newIn, newOut)
                    }
                },
                onPay = { id -> scope.launch { bookingVm.markBookingAsPaid(id) } }
            )
        }
    }
}

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

@Composable
private fun ClientHomeCard(
    home: Home,
    onBookClick: () -> Unit
) {
    Card(onClick = onBookClick) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = home.imageUrls.firstOrNull() ?: home.photoUris.firstOrNull(),
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
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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

    // NEW: check-in/out status + visibility
    isCheckedIn: (Booking) -> Boolean,
    canCheckInNow: (Booking) -> Boolean,
    isButtonsHidden: (Booking) -> Boolean,
    onCheckIn: (Booking) -> Unit,
    onCheckOut: (Booking) -> Unit,

    // existing callbacks
    onDismiss: () -> Unit,
    onCancel: (String) -> Unit,
    onReschedule: (String) -> Unit,
    onPay: (String) -> Unit
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
                    LazyColumn {
                        items(bookings, key = { it.bookingId }) { b ->
                            val checkedIn = isCheckedIn(b)
                            val allowCheckIn = canCheckInNow(b)
                            val hideButtons = isButtonsHidden(b)

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text("Booking ID: ${b.bookingId}", fontWeight = FontWeight.Bold)
                                    Text("Check-in: ${b.checkInDate}")
                                    Text("Check-out: ${b.checkOutDate}")
                                    Text("Guests: ${b.numberOfGuests}")
                                    Text("Nights: ${b.nights}")
                                    Text("Total: $${b.pricePerNight * b.nights}")

                                    Spacer(Modifier.height(8.dp))

                                    // Row 1: usual actions
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        TextButton(onClick = { onCancel(b.bookingId) }) { Text("Cancel") }
                                        TextButton(onClick = { onReschedule(b.bookingId) }) { Text("Reschedule") }
                                        if (b.paymentStatus == "PENDING") {
                                            TextButton(onClick = { onPay(b.bookingId) }) { Text("Pay Now") }
                                        }
                                    }

                                    // Row 2: Check In / Check Out (mutually exclusive)
                                    if (!hideButtons) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            if (!checkedIn) {
                                                Button(
                                                    onClick = { onCheckIn(b) },
                                                    enabled = allowCheckIn
                                                ) { Text("Check In") }
                                            } else {
                                                Button(
                                                    onClick = { onCheckOut(b) },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.secondary
                                                    )
                                                ) { Text("Check Out") }
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



@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookingDialog(
    home: Home,
    onDismiss: () -> Unit,
    onConfirm: (checkIn: Date, checkOut: Date, guests: Int, nights: Int) -> Unit
) {
    // Use text states to avoid the “sticky 1 / always 20” issue
    var guestsText by rememberSaveable { mutableStateOf("1") }
    var nightsText by rememberSaveable { mutableStateOf("1") }
    var checkInDate by remember { mutableStateOf(Date()) }

    // derived checkout
    val checkOutDate = remember(checkInDate, nightsText) {
        val nights = nightsText.toIntOrNull()?.coerceIn(1, 30) ?: 1
        Calendar.getInstance().apply {
            time = checkInDate
            add(Calendar.DAY_OF_YEAR, nights)
        }.time
    }

    var showDatePicker by remember { mutableStateOf(false) }
    val fmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
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
                        onValueChange = { text ->
                            // allow empty while typing; clamp later on confirm
                            if (text.length <= 2 && text.all { it.isDigit() } || text.isEmpty()) {
                                guestsText = text
                            }
                        },
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
                        onValueChange = { text ->
                            if (text.length <= 2 && text.all { it.isDigit() } || text.isEmpty()) {
                                nightsText = text
                            }
                        },
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
                val total = home.pricePerNight * nights
                Text("Total: $${"%.2f".format(total)}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)

                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val g = guestsText.toIntOrNull()?.coerceIn(1, 20) ?: 1
                            val n = nightsText.toIntOrNull()?.coerceIn(1, 30) ?: 1
                            onConfirm(checkInDate, checkOutDate, g, n)
                        }
                    ) { Text("Confirm Booking") }
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