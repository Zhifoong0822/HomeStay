package com.example.homestay.data.repository

import android.util.Log
import com.example.homestay.data.model.HomeFirebase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.text.get
import kotlin.text.set

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


