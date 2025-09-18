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
import java.util.Calendar
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

    fun loadHostBookings(forHostId: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                bookingRepository.getBookingsByHost(forHostId).collect { list ->
                    _bookings.value = list
                }
            } catch (e: Exception) {
                _error.value = "Failed to load host bookings: ${e.message}"
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

    fun updateBookingDetails(bookingId: String, newGuests: Int, newNights: Int, newCheckIn: Date) {
        val uid = currentUserId()
        if (uid.isEmpty()) return

        viewModelScope.launch {
            _loading.value = true
            try {
                val newCheckOut = Calendar.getInstance().apply {
                    time = newCheckIn
                    add(Calendar.DAY_OF_YEAR, newNights)
                }.time

                val booking = bookingRepository.getBookingById(bookingId)
                if (booking != null) {
                    val updatedBooking = booking.copy(
                        numberOfGuests = newGuests,
                        nights = newNights,
                        checkInDate = newCheckIn,
                        checkOutDate = newCheckOut
                    )
                    val res = bookingRepository.updateBooking(updatedBooking)
                    if (res.isSuccess) {
                        loadUserBookings(uid)
                        message.tryEmit("Booking updated successfully!")
                    } else {
                        _error.value = res.exceptionOrNull()?.message ?: "Failed to update booking"
                    }
                } else {
                    _error.value = "Booking not found"
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
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
                    message.tryEmit("Booking cancelled successfully")
                } else {
                    _error.value = res.exceptionOrNull()?.message ?: "Failed to cancel booking"
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }

    fun updateBooking(booking: Booking) {
        val uid = userId ?: return
        viewModelScope.launch {
            _loading.value = true
            try {
                val res = bookingRepository.updateBooking(booking)
                if (res.isSuccess) {
                    // reload bookings depending on whether it’s guest or host
                    loadUserBookings(uid)
                    message.tryEmit("Booking updated")
                } else {
                    _error.value = res.exceptionOrNull()?.message ?: "Failed to update booking"
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }


    fun updateBookingStatus(booking: Booking, newStatus: String) {
        val updated = booking.copy(
            status = newStatus,
            updatedAt = java.util.Date()
        )
        updateBooking(updated)
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
                    message.tryEmit("Booking rescheduled successfully")
                } else {
                    _error.value = res.exceptionOrNull()?.message ?: "Failed to reschedule booking"
                }
            } catch (e: Exception) {
                _error.value = e.message
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
                    message.tryEmit("Payment recorded successfully")
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

    /** Convenience wrapper used by UI to handle payments easily. */
    fun payBooking(bookingId: String, transactionId: String) {
        markBookingAsPaid(bookingId, transactionId)
    }

    fun clearError() {
        _error.value = null
    }
}
