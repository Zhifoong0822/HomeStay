package com.example.homestay.data.repository

import android.util.Log
import com.example.homestay.data.model.HomeFirebase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.suspendCancellableCoroutine
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

import android.content.Context
class FirebaseRepository {
    private val db = FirebaseFirestore.getInstance()
    private val homesCollection = db.collection("homes")

    suspend fun addHomeToFirebase(home: HomeFirebase) {
        homesCollection.document(home.id).set(home)
            .addOnSuccessListener {
                println("Home saved successfully")
            }
            .addOnFailureListener { e ->
                println("Error saving home: $e")
            }
    }

    private suspend fun uploadImagesToStorage(photoUris: List<Uri>): List<String> {
        if (photoUris.isEmpty()) return emptyList()
        val storage = FirebaseStorage.getInstance().reference
        val urls = mutableListOf<String>()
        for (uri in photoUris) {
            val path = "homes/${UUID.randomUUID()}.jpg"
            val ref = storage.child(path)
            ref.putFile(uri).await()                    // upload
            val url = ref.downloadUrl.await().toString()// get HTTPS download URL
            urls += url
        }
        return urls
    }

    /**
     * New: upload images -> write HomeFirebase (with imageUrls) to Firestore.
     * Keeps addHomeToFirebase(...) untouched for compatibility.
     */
    // FirebaseRepository.kt (or wherever you upload)
    suspend fun addHomeWithPhotos(
        context: Context,
        base: HomeFirebase,
        photoUris: List<Uri>
    ) {
        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()
        val storage = FirebaseStorage.getInstance()

        val uid = auth.currentUser?.uid ?: error("Not signed in")
        val homeId = if (base.id.isNotEmpty()) base.id else firestore.collection("homes").document().id
        val folderRef = storage.reference.child("homes").child(homeId)

        val urls = mutableListOf<String>()

        withContext(Dispatchers.IO) {
            for ((i, uri) in photoUris.withIndex()) {
                // 1) Can we open it?
                val input = context.contentResolver.openInputStream(uri)
                    ?: throw IllegalArgumentException("Cannot open image: $uri")

                try {
                    // 2) Upload
                    val ref = folderRef.child("photo_$i.jpg")
                    ref.putStream(input).await()

                    // 3) URL
                    val url = ref.downloadUrl.await().toString()
                    urls += url
                } finally {
                    // Always close
                    try { input.close() } catch (_: Exception) {}
                }
            }

            val data = base.copy(
                id = homeId,
                // include these only if your model has them
                hostId = uid,
                imageUrls = urls
            )
            firestore.collection("homes").document(homeId).set(data).await()
        }
    }

    suspend fun deleteHome(homeId: String) {
        db.collection("homes")
            .document(homeId)
            .delete()
            .addOnSuccessListener { Log.d("Firebase", "Home deleted") }
            .addOnFailureListener { e -> Log.e("Firebase", "Failed to delete home", e) }
    }


    suspend fun getHomesFromFirebase(): List<HomeFirebase> = suspendCancellableCoroutine { cont ->
        homesCollection.get()
            .addOnSuccessListener { result ->
                val homes = result.map { it.toObject(HomeFirebase::class.java) }
                cont.resume(homes) {}
            }
            .addOnFailureListener { e ->
                cont.resumeWith(Result.success(emptyList()))
            }
    }


}


