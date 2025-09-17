package com.example.homestay.ui.booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homestay.data.model.Booking
import com.example.homestay.data.repository.BookingRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

class BookingViewModel(
    private val bookingRepository: BookingRepository
) : ViewModel() {

    // ---- Session ----
    private var userId: String? = null
    fun setCurrentUser(id: String) {
        if (userId == id) return
        userId = id
        loadUserBookings(id)
    }
    fun currentUserId(): String = userId.orEmpty()

    // ---- UI State ----
    private val _bookings = MutableStateFlow<List<Booking>>(emptyList())
    val bookings: StateFlow<List<Booking>> = _bookings.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** Fire-and-forget messages (snackbars/toasts). */
    val message = MutableSharedFlow<String>(extraBufferCapacity = 1)

    // ---- Queries ----
    /** Start/refresh stream of bookings for [forUserId] (or current user). */
    fun loadUserBookings(forUserId: String? = userId) {
        val uid = forUserId
        if (uid.isNullOrEmpty()) {
            _error.value = "User ID is missing. Cannot load bookings."
            return
        }
        viewModelScope.launch {
            _loading.value = true
            try {
                bookingRepository.getBookingsByUser(uid).collect { list ->
                    _bookings.value = list
                }
            } catch (e: Exception) {
                _error.value = "Failed to load bookings: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    // ---- Commands ----
    /** Create a booking and refresh list on success. */
    suspend fun createBooking(booking: Booking): Result<Unit> {
        _loading.value = true
        return try {
            val res = bookingRepository.createBooking(booking)
            if (res.isSuccess) {
                loadUserBookings(booking.userId)
                message.tryEmit("Booking successful!")
            } else {
                _error.value = res.exceptionOrNull()?.message ?: "Failed to create booking"
            }
            res
        } catch (e: Exception) {
            _error.value = e.message
            Result.failure(e)
        } finally {
            _loading.value = false
        }
    }

    /** Cancel an existing booking. */
    fun cancelBooking(bookingId: String) {
        val uid = userId ?: return
        viewModelScope.launch {
            _loading.value = true
            try {
                val res = bookingRepository.cancelBooking(bookingId)
                if (res.isSuccess) {
                    loadUserBookings(uid)
                    message.tryEmit("Booking cancelled")
                } else {
                    _error.value = res.exceptionOrNull()?.message ?: "Failed to cancel booking"
                }
            } finally {
                _loading.value = false
            }
        }
    }

    /** Reschedule dates for a booking. */
    fun rescheduleBooking(bookingId: String, newCheckIn: Date, newCheckOut: Date) {
        val uid = userId ?: return
        viewModelScope.launch {
            _loading.value = true
            try {
                val res = bookingRepository.rescheduleBooking(bookingId, newCheckIn, newCheckOut)
                if (res.isSuccess) {
                    loadUserBookings(uid)
                    message.tryEmit("Booking rescheduled")
                } else {
                    _error.value = res.exceptionOrNull()?.message ?: "Failed to reschedule booking"
                }
            } finally {
                _loading.value = false
            }
        }
    }

    /** Mark booking as paid and (optionally) store a transaction id. */
    fun markBookingAsPaid(bookingId: String, transactionId: String? = null) {
        val uid = currentUserId()
        if (uid.isEmpty()) return
        viewModelScope.launch {
            _loading.value = true
            try {
                val res = bookingRepository.updatePaymentStatus(
                    bookingId = bookingId,
                    paymentStatus = "PAID",
                    transactionId = transactionId
                )
                if (res.isSuccess) {
                    loadUserBookings(uid)
                    message.tryEmit("Payment recorded")
                } else {
                    _error.value = res.exceptionOrNull()?.message ?: "Failed to update payment status"
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }

    /** Convenience wrapper used by your UI. */
    fun payBooking(bookingId: String, transactionId: String) {
        markBookingAsPaid(bookingId, transactionId)
    }

    fun clearError() { _error.value = null }
}
