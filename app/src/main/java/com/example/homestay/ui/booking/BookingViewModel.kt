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

    private var hostId: String? = null
    fun setCurrentHost(id: String) {
        if (hostId == id) return
        hostId = id
        loadHostBookings(id)
    }

    fun currentHostId(): String = hostId.orEmpty()

    private val _hostBookings = MutableStateFlow<List<Booking>>(emptyList())
    val hostBookings: StateFlow<List<Booking>> = _hostBookings.asStateFlow()


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
                val res = bookingRepository.deleteBooking(bookingId)   // << delete, not mark cancelled
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

    fun loadHostBookings(forHostId: String? = hostId) {
        val hid = forHostId
        if (hid.isNullOrEmpty()) {
            _error.value = "Host ID is missing. Cannot load bookings."
            return
        }
        viewModelScope.launch {
            _loading.value = true
            try {
                bookingRepository.getBookingsByHost(hid).collect { list ->
                    _hostBookings.value = list
                }
            } catch (e: Exception) {
                _error.value = "Failed to load host bookings: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    /** Update the status of a booking (for host actions: Accept/Reject) */
    fun updateBookingStatus(bookingId: String, newStatus: String) {
        val hid = currentHostId()
        if (hid.isEmpty()) return

        viewModelScope.launch {
            _loading.value = true
            try {
                val booking = bookingRepository.getBookingById(bookingId)
                if (booking != null) {
                    // Only allow host to update their own booking
                    if (booking.hostId != hid) {
                        _error.value = "Cannot update booking that doesn't belong to this host"
                        return@launch
                    }

                    val updatedBooking = booking.copy(
                        status = newStatus,
                        updatedAt = Date()
                    )
                    val res = bookingRepository.updateBooking(updatedBooking)
                    if (res.isSuccess) {
                        loadHostBookings(hid)
                        message.tryEmit("Booking status updated to $newStatus")
                    } else {
                        _error.value = res.exceptionOrNull()?.message ?: "Failed to update status"
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


    // BookingViewModel.kt  (inside class BookingViewModel)

    fun checkIn(bookingId: String) {
        val uid = userId ?: return
        viewModelScope.launch {
            _loading.value = true
            try {
                val res = bookingRepository.updateStatus(bookingId, "CHECKED_IN")
                if (res.isSuccess) {
                    loadUserBookings(uid)
                    message.tryEmit("Checked in successfully")
                } else {
                    _error.value = res.exceptionOrNull()?.message ?: "Failed to check in"
                }
            } finally { _loading.value = false }
        }
    }

    fun checkOut(bookingId: String) {
        val uid = userId ?: return
        viewModelScope.launch {
            _loading.value = true
            try {
                val res = bookingRepository.updateStatus(bookingId, "COMPLETED")
                if (res.isSuccess) {
                    loadUserBookings(uid)
                    message.tryEmit("Checked out successfully")
                } else {
                    _error.value = res.exceptionOrNull()?.message ?: "Failed to check out"
                }
            } finally { _loading.value = false }
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
