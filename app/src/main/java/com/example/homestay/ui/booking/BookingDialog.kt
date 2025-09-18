// BookingDialog.kt
package com.example.homestay.ui.booking

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.homestay.data.model.Home
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleBookingDialog(
    home: Home,
    onDismiss: () -> Unit,
    onConfirm: (checkIn: Date, checkOut: Date, guests: Int, nights: Int) -> Unit
) {
    var guests by remember { mutableStateOf(1) }
    var nights by remember { mutableStateOf(1) }
    var checkInDate by remember { mutableStateOf(Date()) }  // ✅ user-selectable

    // Checkout = checkIn + nights
    val checkOutDate = remember(checkInDate, nights) {
        Calendar.getInstance().apply {
            time = checkInDate
            add(Calendar.DAY_OF_YEAR, nights)
        }.time
    }

    // State for DatePicker
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

                // ✅ Date selection
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

    // ✅ Material Date Picker
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