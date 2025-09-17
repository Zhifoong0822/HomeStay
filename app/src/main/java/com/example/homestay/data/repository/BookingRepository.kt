package com.example.homestay.data.repository

import com.example.homestay.data.model.Booking
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface BookingRepository {
    suspend fun createBooking(booking: Booking): Result<String>
    suspend fun updateBooking(booking: Booking): Result<Unit>
    suspend fun cancelBooking(bookingId: String): Result<Unit>
    suspend fun rescheduleBooking(bookingId: String, newCheckIn: Date, newCheckOut: Date): Result<Unit>
    fun getBookingsByUser(userId: String): Flow<List<Booking>>
    fun getBookingsByHost(hostId: String): Flow<List<Booking>>
    fun getBookingsByHome(homeId: String): Flow<List<Booking>>
    suspend fun getBookingById(bookingId: String): Booking?
    suspend fun updatePaymentStatus(bookingId: String, paymentStatus: String, transactionId: String? = null): Result<Unit>
}
