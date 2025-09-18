package com.example.homestay.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.homestay.data.model.Booking
import com.example.homestay.data.model.HomeFirebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.UUID
import com.google.firebase.firestore.ktx.toObject

class FirebaseRepository {

    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }


    // Collections
    private val homesCol get() = db.collection("homes")
    private val bookingsCol get() = db.collection("bookings")

    // -------------------------------
    // HOMES
    // -------------------------------

    /** Simple set (no photos). */
    suspend fun addHomeToFirebase(home: HomeFirebase) {
        homesCol.document(home.id).set(home).await()
    }

    /**
     * Upload photos to Storage under /homes/{homeId}/photo_{i}.jpg,
     * then save Firestore doc with the HTTPS download URLs.
     *
     * Keeps compatibility with your HomeFirebase model (expects imageUrls, hostId fields).
     */
    suspend fun addHomeWithPhotos(
        context: Context,
        base: HomeFirebase,
        photoUris: List<Uri>
    ) {
        val uid = auth.currentUser?.uid ?: error("Not signed in")
        val homeId = base.id.ifEmpty { homesCol.document().id }
        val folderRef = storage.reference.child("homes").child(homeId)

        val urls = mutableListOf<String>()

        withContext(Dispatchers.IO) {
            for ((i, uri) in photoUris.withIndex()) {
                val input = context.contentResolver.openInputStream(uri)
                    ?: throw IllegalArgumentException("Cannot open image: $uri")

                try {
                    val ref = folderRef.child("photo_$i.jpg")
                    ref.putStream(input).await()
                    val url = ref.downloadUrl.await().toString()
                    urls += url
                } finally {
                    try { input.close() } catch (_: Exception) {}
                }
            }

            val data = base.copy(
                id = homeId,
                hostId = uid,
                imageUrls = urls
            )
            homesCol.document(homeId).set(data).await()
        }
    }

    suspend fun deleteHome(homeId: String) {
        homesCol.document(homeId).delete().await()
    }

    suspend fun getHomesFromFirebase(): List<HomeFirebase> {
        val snap = homesCol.get().await()
        return snap.toObjects()
    }

    // -------------------------------
    // BOOKINGS
    // -------------------------------

    /** Create or overwrite a booking. */
    suspend fun saveBooking(booking: Booking): Result<Unit> = runCatching {
        bookingsCol.document(booking.bookingId).set(booking).await()
    }

    /** Fetch all bookings for a user (one-shot). */
    suspend fun getBookingsForUser(userId: String): List<Booking> {
        val snap = bookingsCol.whereEqualTo("userId", userId).get().await()
        return snap.toObjects()
    }

    /** Set status = CANCELLED and updatedAt = now. */
    suspend fun cancelBooking(bookingId: String): Result<Unit> = runCatching {
        bookingsCol.document(bookingId)
            .update(mapOf("status" to "CANCELLED", "updatedAt" to Date()))
            .await()
    }

    /** Update dates, set status = RESCHEDULED, and updatedAt = now. */
    suspend fun rescheduleBooking(
        bookingId: String,
        newCheckIn: Date,
        newCheckOut: Date
    ): Result<Unit> = runCatching {
        bookingsCol.document(bookingId).update(
            mapOf(
                "checkInDate" to newCheckIn,
                "checkOutDate" to newCheckOut,
                "status" to "RESCHEDULED",
                "updatedAt" to Date()
            )
        ).await()
    }

    /**
     * Update payment status (e.g., "PAID", "PENDING", "FAILED"),
     * optionally attach a transactionId, and bump updatedAt.
     */
    suspend fun updatePaymentStatus(
        bookingId: String,
        paymentStatus: String = "PAID",
        transactionId: String? = null
    ): Result<Unit> = runCatching {
        val data = mutableMapOf<String, Any>(
            "paymentStatus" to paymentStatus,
            "updatedAt" to Date()
        )
        if (transactionId != null) data["transactionId"] = transactionId
        bookingsCol.document(bookingId).update(data).await()
    }

    suspend fun getBookingById(bookingId: String): Booking? {
        val doc = bookingsCol.document(bookingId).get().await()
        return doc.toObject<Booking>()
    }

    suspend fun getBookingsForHome(homeId: String): List<Booking> {
        val snap = bookingsCol
            .whereEqualTo("homeId", homeId)
            .get()
            .await()
        return snap.toObjects()
    }


    suspend fun getBookingsForHost(hostId: String): List<Booking> {
        val snap = bookingsCol
            .whereEqualTo("hostId", hostId) // 🔑 filter by hostId
            .get()
            .await()
        return snap.toObjects(Booking::class.java)
    }


    private suspend fun uploadImagesToStorage(photoUris: List<Uri>): List<String> {
        if (photoUris.isEmpty()) return emptyList()
        val root = storage.reference.child("homes/${UUID.randomUUID()}")
        val urls = mutableListOf<String>()
        for ((i, uri) in photoUris.withIndex()) {
            val ref = root.child("photo_$i.jpg")
            ref.putFile(uri).await()
            urls += ref.downloadUrl.await().toString()
        }
        return urls
    }


}
