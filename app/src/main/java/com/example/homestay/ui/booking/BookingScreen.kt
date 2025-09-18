package com.example.homestay.ui.booking

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.Date
import kotlin.math.max
import com.example.homestay.data.model.Booking

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(
    bookingVm: BookingViewModel,
    onPayment: () -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    // ---- UI State ----
    var guests by remember { mutableStateOf(1) }
    var nights by remember { mutableStateOf(1) }
    var pricePerNight by remember { mutableStateOf(120.0) }

    var promoCode by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf(PaymentMethod.CARD) }

    var showPayConfirm by remember { mutableStateOf(false) }
    var showPaySuccess by remember { mutableStateOf(false) }
    var pendingBooking by remember { mutableStateOf<Booking?>(null) }

    // Collect VM states
    val loading by bookingVm.loading.collectAsState(initial = false)
    val error by bookingVm.error.collectAsState(initial = null)

    // ---- Derived Pricing ----
    val subtotal = remember(guests, nights, pricePerNight) {
        // example: price per night * nights (guests could be used to add-on if you want)
        pricePerNight * nights
    }

    val promoDiscount = remember(subtotal, promoCode) {
        // Example rules:
        // - "SAVE10" = 10% off
        // - "FLAT20" = RM 20 off
        // - Otherwise = 0
        when (promoCode.trim().uppercase()) {
            "SAVE10" -> 0.10 * subtotal
            "FLAT20" -> 20.0
            else -> 0.0
        }.let { d -> max(0.0, minOf(d, subtotal)) }
    }

    val fees = remember(subtotal) { 6.0 }               // flat service fee
    val tax = remember(subtotal, promoDiscount, fees) {
        // SST example 6% after discount + fees
        0.06 * (subtotal - promoDiscount + fees)
    }
    val grandTotal = remember(subtotal, promoDiscount, fees, tax) {
        (subtotal - promoDiscount + fees + tax).coerceAtLeast(0.0)
    }

    // ---- Validation ----
    val inputError = remember(guests, nights, pricePerNight) {
        when {
            guests < 1 -> "Guests must be at least 1"
            nights < 1 -> "Nights must be at least 1"
            pricePerNight <= 0 -> "Price per night must be greater than 0"
            else -> null
        }
    }

    // ---- Screen ----
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Book Property") },
                navigationIcon = {
                    TextButton(onClick = onBack, enabled = !loading) { Text("Back") }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Inputs
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = guests.toString(),
                    onValueChange = { v -> guests = v.toIntOrNull()?.coerceAtLeast(0) ?: guests },
                    label = { Text("Guests") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    enabled = !loading
                )
                OutlinedTextField(
                    value = nights.toString(),
                    onValueChange = { v -> nights = v.toIntOrNull()?.coerceAtLeast(0) ?: nights },
                    label = { Text("Nights") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    enabled = !loading
                )
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = pricePerNight.toString(),
                onValueChange = { v -> pricePerNight = v.toDoubleOrNull() ?: pricePerNight },
                label = { Text("Price per night (RM)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                enabled = !loading
            )

            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Payment method:", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.width(12.dp))
                PaymentMethodChip(
                    selected = paymentMethod == PaymentMethod.CARD,
                    label = "Card",
                    onClick = { if (!loading) paymentMethod = PaymentMethod.CARD }
                )
                Spacer(Modifier.width(8.dp))
                PaymentMethodChip(
                    selected = paymentMethod == PaymentMethod.EWALLET,
                    label = "eWallet",
                    onClick = { if (!loading) paymentMethod = PaymentMethod.EWALLET }
                )
                Spacer(Modifier.width(8.dp))
                PaymentMethodChip(
                    selected = paymentMethod == PaymentMethod.PAYPAL,
                    label = "PayPal",
                    onClick = { if (!loading) paymentMethod = PaymentMethod.PAYPAL }
                )
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = promoCode,
                onValueChange = { promoCode = it },
                label = { Text("Promo code (SAVE10 / FLAT20)") },
                enabled = !loading,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))

            // Price breakdown
            PriceRow("Subtotal", subtotal)
            PriceRow("Promo discount", -promoDiscount)
            PriceRow("Service fees", fees)
            PriceRow("Tax (6%)", tax)
            Divider(Modifier.padding(vertical = 8.dp))
            PriceRow("Grand total", grandTotal, bold = true)

            Spacer(Modifier.height(20.dp))

            // Errors: validation or VM
            inputError?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
            }
            error?.let {
                Text("Error: $it", color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
            }

            // Action buttons
            Button(
                onClick = {
                    // Build a booking draft and open confirm dialog
                    val b = Booking(
                        bookingId = UUID.randomUUID().toString(),
                        userId = "sampleUser",   // plug your actual user id
                        homeId = "sampleHome",   // plug your actual selected home id
                        hostId = "sampleHost",
                        numberOfGuests = guests,
                        nights = nights,
                        pricePerNight = pricePerNight,
                        checkInDate = Date(),    // replace with your selected date
                        checkOutDate = Date(),   // replace with your selected date
                        paymentMethod = paymentMethod.name,
                        paymentStatus = "PENDING"
                    )
                    pendingBooking = b
                    showPayConfirm = true
                },
                enabled = !loading && inputError == null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Proceed to Payment")
            }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = onBack,
                enabled = !loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }

            Spacer(Modifier.height(16.dp))

            if (loading) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    // ---- Confirm Dialog ----
    if (showPayConfirm && pendingBooking != null) {
        val b = pendingBooking!!
        AlertDialog(
            onDismissRequest = { if (!loading) showPayConfirm = false },
            title = { Text("Confirm Payment") },
            text = {
                Column {
                    Text("Guests: ${b.numberOfGuests}")
                    Text("Nights: ${b.nights}")
                    Text("Method: ${paymentMethod.display}")
                    Spacer(Modifier.height(8.dp))
                    PriceRow("Subtotal", subtotal)
                    PriceRow("Promo discount", -promoDiscount)
                    PriceRow("Service fees", fees)
                    PriceRow("Tax (6%)", tax)
                    Divider(Modifier.padding(vertical = 8.dp))
                    PriceRow("To pay", grandTotal, bold = true)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !loading,
                    onClick = {
                        // Create then mark paid
                        showPayConfirm = false
                        scope.launch {
                            val res = bookingVm.createBooking(b)
                            if (res.isSuccess) {
                                val txn = UUID.randomUUID().toString()
                                bookingVm.payBooking(b.bookingId, txn)
                                showPaySuccess = true
                            } else {
                                // VM will expose error; no-op here
                            }
                        }
                    }
                ) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(
                    enabled = !loading,
                    onClick = { showPayConfirm = false }
                ) { Text("Cancel") }
            }
        )
    }

    // ---- Success Prompt ----
    if (showPaySuccess) {
        AlertDialog(
            onDismissRequest = {
                showPaySuccess = false
                onPayment()
            },
            title = { Text("Payment successfully") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPaySuccess = false
                        onPayment()
                    }
                ) { Text("OK") }
            }
        )
    }
}

@Composable
private fun PriceRow(label: String, amount: Double, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = if (bold) FontWeight.SemiBold else null)
        Text(
            "RM ${"%.2f".format(amount)}",
            fontWeight = if (bold) FontWeight.SemiBold else null
        )
    }
}

private enum class PaymentMethod(val display: String) {
    CARD("Card"),
    EWALLET("eWallet"),
    PAYPAL("PayPal")
}

@Composable
private fun PaymentMethodChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) }
    )
}
