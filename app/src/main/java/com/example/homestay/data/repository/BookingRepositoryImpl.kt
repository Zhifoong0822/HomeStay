package com.example.homestay.data.repository

import com.example.homestay.data.model.Booking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.Date

class BookingRepositoryImpl(
    private val firebaseRepository: FirebaseRepository
) : BookingRepository {

    override suspend fun createBooking(booking: Booking): Result<Unit> = try {
        firebaseRepository.saveBooking(booking)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override fun getBookingsByUser(userId: String): Flow<List<Booking>> = flow {
        try {
            emit(firebaseRepository.getBookingsForUser(userId))
        } catch (_: Exception) {
            emit(emptyList())
        }
    }

    override suspend fun cancelBooking(bookingId: String): Result<Unit> = try {
        firebaseRepository.cancelBooking(bookingId)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun rescheduleBooking(
        bookingId: String,
        newCheckIn: Date,
        newCheckOut: Date
    ): Result<Unit> = try {
        firebaseRepository.rescheduleBooking(bookingId, newCheckIn, newCheckOut)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateBooking(booking: Booking): Result<Unit> = try {
        // Reuse save to upsert
        firebaseRepository.saveBooking(booking)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updatePaymentStatus(
        bookingId: String,
        paymentStatus: String,
        transactionId: String?
    ): Result<Unit> = try {
        // Your FirebaseRepository already has this helper.
        firebaseRepository.updatePaymentStatus(bookingId, paymentStatus, transactionId)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /* ====== New implementations to satisfy the interface ====== */

    // If you truly want hard delete but FirebaseRepository only supports cancel,
    // we map delete -> cancel to keep this legacy impl working.
    override suspend fun deleteBooking(bookingId: String): Result<Unit> = try {
        firebaseRepository.cancelBooking(bookingId)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // Generic status update. FirebaseRepository doesn’t expose a generic status update,
    // so we implement the one case it supports (CANCELLED). Other statuses are no-ops.
    // In production, prefer FirestoreBookingRepository which implements this properly.
    override suspend fun updateStatus(bookingId: String, status: String): Result<Unit> = try {
        if (status.equals("CANCELLED", ignoreCase = true)) {
            firebaseRepository.cancelBooking(bookingId)
        }
        // For other statuses we can’t do anything with this legacy repo, so succeed no-op.
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /* ====== Optional helpers if your interface declares them ====== */

    override fun getBookingsByHost(hostId: String): Flow<List<Booking>> = flow {
        try {
            // If you don’t have this in FirebaseRepository, emit empty list.
            emit(firebaseRepository.getBookingsForHost(hostId))
        } catch (_: Exception) {
            emit(emptyList())
        }
    }

    override fun getBookingsByHome(homeId: String): Flow<List<Booking>> = flow {
        try {
            emit(firebaseRepository.getBookingsForHome(homeId))
        } catch (_: Exception) {
            emit(emptyList())
        }
    }

    override suspend fun getBookingById(bookingId: String): Booking? = try {
        firebaseRepository.getBookingById(bookingId)
    } catch (_: Exception) {
        null
    }
}
