package com.example.homestay.data.repository

import com.example.homestay.data.model.Booking
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.*

class BookingRepositoryImpl(
    private val firestore: FirebaseFirestore
) : BookingRepository {

    private val bookingCollection = firestore.collection("bookings")

    override suspend fun createBooking(booking: Booking): Result<String> {
        return try {
            val docRef = bookingCollection.document()
            val newBooking = booking.copy(
                bookingId = docRef.id,
                createdAt = Date(),
                updatedAt = Date()
            )
            docRef.set(newBooking).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateBooking(booking: Booking): Result<Unit> {
        return try {
            bookingCollection.document(booking.bookingId)
                .set(booking.copy(updatedAt = Date()))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cancelBooking(bookingId: String): Result<Unit> {
        return try {
            bookingCollection.document(bookingId)
                .update("status", "CANCELLED", "updatedAt", Date())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun rescheduleBooking(
        bookingId: String,
        newCheckIn: Date,
        newCheckOut: Date
    ): Result<Unit> {
        return try {
            bookingCollection.document(bookingId)
                .update(
                    mapOf(
                        "checkInDate" to newCheckIn,
                        "checkOutDate" to newCheckOut,
                        "status" to "RESCHEDULED",
                        "updatedAt" to Date()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getBookingsByUser(userId: String): Flow<List<Booking>> = callbackFlow {
        val listener = bookingCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    close(e)
                    return@addSnapshotListener
                }
                val bookings = snapshot?.documents?.mapNotNull { it.toObject<Booking>() } ?: emptyList()
                trySend(bookings)
            }
        awaitClose { listener.remove() }
    }

    override fun getBookingsByHost(hostId: String): Flow<List<Booking>> = callbackFlow {
        val listener = bookingCollection
            .whereEqualTo("hostId", hostId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    close(e)
                    return@addSnapshotListener
                }
                val bookings = snapshot?.documents?.mapNotNull { it.toObject<Booking>() } ?: emptyList()
                trySend(bookings)
            }
        awaitClose { listener.remove() }
    }

    override fun getBookingsByHome(homeId: String): Flow<List<Booking>> = callbackFlow {
        val listener = bookingCollection
            .whereEqualTo("homeId", homeId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    close(e)
                    return@addSnapshotListener
                }
                val bookings = snapshot?.documents?.mapNotNull { it.toObject<Booking>() } ?: emptyList()
                trySend(bookings)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getBookingById(bookingId: String): Booking? {
        val doc = bookingCollection.document(bookingId).get().await()
        return doc.toObject<Booking>()
    }

    override suspend fun updatePaymentStatus(
        bookingId: String,
        paymentStatus: String,
        transactionId: String?
    ): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>(
                "paymentStatus" to paymentStatus,
                "updatedAt" to Date()
            )
            transactionId?.let { updates["transactionId"] = it }

            bookingCollection.document(bookingId)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
