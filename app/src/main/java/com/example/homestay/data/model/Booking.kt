package com.example.homestay.data.model

import com.google.firebase.firestore.DocumentId
import java.util.Date

data class Booking(
    @DocumentId
    val bookingId: String = "",
    val userId: String = "",
    val homeId: String = "",
    val hostId: String = "",
    val checkInDate: Date = Date(),
    val checkOutDate: Date = Date(),
    val numberOfGuests: Int = 1,
    val nights: Int = 1,
    val pricePerNight: Double = 0.0,
    val status: String = "PENDING",          // PENDING, CONFIRMED, RESCHEDULED, CANCELLED
    val paymentStatus: String = "PENDING",   // PENDING, PAID, FAILED
    val transactionId: String? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
) {
    val totalPrice: Double
        get() = pricePerNight * nights
}
