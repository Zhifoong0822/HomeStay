package com.example.homestay.ui.booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homestay.data.model.Booking
import com.example.homestay.data.repository.BookingRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class BookingViewModel(
    private val bookingRepository: BookingRepository
) : ViewModel() {

    /** Stores current logged-in user ID */
    private var currentUserId: String? = null

    private val _bookings = MutableStateFlow<List<Booking>>(emptyList())
    val bookings: StateFlow<List<Booking>> = _bookings.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * Sets the current userId so you don't need to keep passing it
     */
    fun setCurrentUser(userId: String) {
        currentUserId = userId
    }

    /**
     * Load bookings for current user
     */
    fun loadUserBookings(userId: String? = currentUserId) {
        if (userId.isNullOrEmpty()) {
            _error.value = "User ID is missing. Cannot load bookings."
            return
        }

        viewModelScope.launch {
            bookingRepository.getBookingsByUser(userId)
                .catch { e ->
                    _error.value = "Failed to load bookings: ${e.message}"
                }
                .collect { bookings ->
                    _bookings.value = bookings
                }
        }
    }

    /**
     * Create a new booking and refresh list
     */
    suspend fun createBooking(booking: Booking): Result<String> {
        _loading.value = true
        return try {
            val result = bookingRepository.createBooking(booking)
            if (result.isSuccess) {
                loadUserBookings(booking.userId)
                result
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
            }
        } finally {
            _loading.value = false
        }
    }

    /**
     * Cancel booking by ID
     */
    fun cancelBooking(bookingId: String) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            val result = bookingRepository.cancelBooking(bookingId)
            if (result.isSuccess) {
                loadUserBookings(userId)
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to cancel booking"
            }
        }
    }

    /**
     * Reschedule booking
     */
    fun rescheduleBooking(bookingId: String, newCheckIn: Date, newCheckOut: Date) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            val result = bookingRepository.rescheduleBooking(bookingId, newCheckIn, newCheckOut)
            if (result.isSuccess) {
                loadUserBookings(userId)
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to reschedule booking"
            }
        }
    }

    /**
     * Mark booking as paid with a transaction ID
     */
    fun payBooking(bookingId: String, transactionId: String) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            val result = bookingRepository.updatePaymentStatus(bookingId, "PAID", transactionId)
            if (result.isSuccess) {
                loadUserBookings(userId)
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to update payment status"
            }
        }
    }

    /**
     * Mark booking as paid without a transaction ID
     */
    fun markBookingAsPaid(bookingId: String) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            try {
                bookingRepository.updatePaymentStatus(bookingId, "PAID")
                loadUserBookings(userId)
            } catch (e: Exception) {
                _error.value = "Failed to update payment status: ${e.message}"
            }
        }
    }

    /** Clear error message */
    fun clearError() {
        _error.value = null
    }
}
