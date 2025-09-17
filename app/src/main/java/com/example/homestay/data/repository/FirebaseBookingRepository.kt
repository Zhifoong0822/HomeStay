// FirestoreBookingRepository.kt
package com.example.homestay.data.repository

import com.example.homestay.data.model.Booking
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.*

class FirestoreBookingRepository(
    private val firestore: FirebaseFirestore
) : BookingRepository {

    private val bookingsCollection = firestore.collection("bookings")

    override suspend fun createBooking(booking: Booking): Result<String> {
        return try {
            val docRef = bookingsCollection.add(booking.copy(
                createdAt = Date(),
                updatedAt = Date()
            )).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateBooking(booking: Booking): Result<Unit> {
        return try {
            bookingsCollection.document(booking.bookingId)
                .set(booking.copy(updatedAt = Date())).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cancelBooking(bookingId: String): Result<Unit> {
        return try {
            bookingsCollection.document(bookingId)
                .update(mapOf("status" to "CANCELLED", "updatedAt" to Date())).await()
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
            bookingsCollection.document(bookingId)
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
        val listener = bookingsCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) close(e)
                else trySend(snapshot?.toObjects(Booking::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override fun getBookingsByHost(hostId: String): Flow<List<Booking>> = callbackFlow {
        val listener = bookingsCollection
            .whereEqualTo("hostId", hostId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) close(e)
                else trySend(snapshot?.toObjects(Booking::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override fun getBookingsByHome(homeId: String): Flow<List<Booking>> = callbackFlow {
        val listener = bookingsCollection
            .whereEqualTo("homeId", homeId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) close(e)
                else trySend(snapshot?.toObjects(Booking::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getBookingById(bookingId: String): Booking? {
        return try {
            bookingsCollection.document(bookingId).get().await()
                .toObject(Booking::class.java)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun updatePaymentStatus(
        bookingId: String,
        paymentStatus: String,
        transactionId: String?
    ): Result<Unit> {
        return try {
            val updateData = mutableMapOf<String, Any>(
                "paymentStatus" to paymentStatus,
                "updatedAt" to Date()
            )
            transactionId?.let { updateData["transactionId"] = it }
            bookingsCollection.document(bookingId).update(updateData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
