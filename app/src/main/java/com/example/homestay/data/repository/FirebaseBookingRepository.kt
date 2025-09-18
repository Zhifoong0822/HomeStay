// FirestoreBookingRepository.kt
package com.example.homestay.data.repository

import com.example.homestay.data.model.Booking
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObjects
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

class FirestoreBookingRepository(
    private val firestore: FirebaseFirestore
) : BookingRepository {

    private val bookings = firestore.collection("bookings")

    /* -------------------- Create / Update / Delete -------------------- */

    override suspend fun createBooking(booking: Booking): Result<Unit> = try {
        val id = if (booking.bookingId.isNotBlank()) booking.bookingId else bookings.document().id
        val now = Date()
        bookings.document(id)
            .set(booking.copy(bookingId = id, createdAt = now, updatedAt = now))
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateBooking(booking: Booking): Result<Unit> = try {
        bookings.document(booking.bookingId)
            .set(booking.copy(updatedAt = Date()))
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteBooking(bookingId: String): Result<Unit> = try {
        bookings.document(bookingId).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /* -------------------- Status helpers -------------------- */

    override suspend fun cancelBooking(bookingId: String): Result<Unit> = try {
        bookings.document(bookingId)
            .update(mapOf("status" to "CANCELLED", "updatedAt" to Date()))
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** Generic status updater (used for CHECKED_IN, COMPLETED, etc.). */
    override suspend fun updateStatus(bookingId: String, status: String): Result<Unit> = try {
        bookings.document(bookingId)
            .update(mapOf("status" to status, "updatedAt" to Date()))
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun rescheduleBooking(
        bookingId: String,
        newCheckIn: Date,
        newCheckOut: Date
    ): Result<Unit> = try {
        bookings.document(bookingId)
            .update(
                mapOf(
                    "checkInDate" to newCheckIn,
                    "checkOutDate" to newCheckOut,
                    "status" to "RESCHEDULED",
                    "updatedAt" to Date()
                )
            )
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updatePaymentStatus(
        bookingId: String,
        paymentStatus: String,
        transactionId: String?
    ): Result<Unit> = try {
        val data = mutableMapOf<String, Any>(
            "paymentStatus" to paymentStatus,
            "updatedAt" to Date()
        )
        if (transactionId != null) data["transactionId"] = transactionId
        bookings.document(bookingId).update(data).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /* -------------------- Queries -------------------- */

    override fun getBookingsByUser(userId: String): Flow<List<Booking>> = callbackFlow {
        val reg = bookings.whereEqualTo("userId", userId)
            .addSnapshotListener { snap, e ->
                if (e != null) close(e)
                else trySend(snap?.toObjects<Booking>() ?: emptyList())
            }
        awaitClose { reg.remove() }
    }

    override fun getBookingsByHost(hostId: String): Flow<List<Booking>> = callbackFlow {
        val reg = bookings.whereEqualTo("hostId", hostId)
            .addSnapshotListener { snap, e ->
                if (e != null) close(e)
                else trySend(snap?.toObjects<Booking>() ?: emptyList())
            }
        awaitClose { reg.remove() }
    }

    override fun getBookingsByHome(homeId: String): Flow<List<Booking>> = callbackFlow {
        val reg = bookings.whereEqualTo("homeId", homeId)
            .addSnapshotListener { snap, e ->
                if (e != null) close(e)
                else trySend(snap?.toObjects<Booking>() ?: emptyList())
            }
        awaitClose { reg.remove() }
    }

    override suspend fun getBookingById(bookingId: String): Booking? = try {
        bookings.document(bookingId).get().await().toObject(Booking::class.java)
    } catch (_: Exception) {
        null
    }
}
