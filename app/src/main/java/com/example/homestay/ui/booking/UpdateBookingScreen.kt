package com.example.homestay.ui.booking

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateBookingScreen(
    bookingVm: BookingViewModel,
    bookingId: String,
    currentGuests: Int,
    currentNights: Int,
    currentCheckIn: Date,
    navController: NavHostController
) {
    val scope = rememberCoroutineScope()

    var guestsText by rememberSaveable { mutableStateOf(currentGuests.toString()) }
    var nightsText by rememberSaveable { mutableStateOf(currentNights.toString()) }
    var checkInDate by remember { mutableStateOf(currentCheckIn) }

    var showDatePicker by remember { mutableStateOf(false) }
    val fmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    // derived checkout
    val checkOutDate = remember(checkInDate, nightsText) {
        val nights = nightsText.toIntOrNull()?.coerceIn(1, 30) ?: 1
        Calendar.getInstance().apply {
            time = checkInDate
            add(Calendar.DAY_OF_YEAR, nights)
        }.time
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Update Booking") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Edit your booking details", fontWeight = FontWeight.Bold)

            // Guests
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.People, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = guestsText,
                    onValueChange = { text ->
                        if (text.all { it.isDigit() } || text.isEmpty()) guestsText = text
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("Number of Guests") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Nights
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.DateRange, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = nightsText,
                    onValueChange = { text ->
                        if (text.all { it.isDigit() } || text.isEmpty()) nightsText = text
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("Number of Nights") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Check-in date
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
            ) {
                Icon(Icons.Filled.DateRange, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Check-in: ${fmt.format(checkInDate)}", modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(8.dp))
            Text("Check-out: ${fmt.format(checkOutDate)}")

            // Save Button
            Button(
                onClick = {
                    val g = guestsText.toIntOrNull()?.coerceIn(1, 20) ?: currentGuests
                    val n = nightsText.toIntOrNull()?.coerceIn(1, 30) ?: currentNights

                    scope.launch {
                        bookingVm.updateBookingDetails(bookingId, g, n, checkInDate)
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Changes")
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
