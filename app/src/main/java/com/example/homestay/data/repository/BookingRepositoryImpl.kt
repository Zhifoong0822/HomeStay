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

    override suspend fun updateBooking(booking: Booking): Result<Unit> = try {
        firebaseRepository.saveBooking(booking)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
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

    override suspend fun updatePaymentStatus(
        bookingId: String,
        paymentStatus: String,
        transactionId: String?
    ): Result<Unit> = try {
        firebaseRepository.updatePaymentStatus(bookingId, paymentStatus, transactionId)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override fun getBookingsByUser(userId: String): Flow<List<Booking>> = flow {
        try {
            emit(firebaseRepository.getBookingsForUser(userId))
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override fun getBookingsByHost(hostId: String): Flow<List<Booking>> = flow {
        try {
            emit(firebaseRepository.getBookingsForHost(hostId))
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override fun getBookingsByHome(homeId: String): Flow<List<Booking>> = flow {
        try {
            emit(firebaseRepository.getBookingsForHome(homeId))
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override suspend fun getBookingById(bookingId: String): Booking? = try {
        firebaseRepository.getBookingById(bookingId)
    } catch (e: Exception) {
        null
    }
}