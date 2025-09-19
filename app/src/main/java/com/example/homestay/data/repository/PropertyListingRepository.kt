package com.example.homestay.data.repository

import android.net.Uri
import com.example.homestay.data.local.HomeDao
import com.example.homestay.data.local.HomeEntity
import com.example.homestay.data.local.HomestayPriceDao
import com.example.homestay.data.local.PromotionDao
import com.example.homestay.data.model.CheckStatus
import com.example.homestay.data.model.Home
import com.example.homestay.data.model.HomeWithDetails
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import com.example.homestay.data.model.HomeFirebase
class PropertyListingRepository(
    private val homeDao: HomeDao,
    private val homestayPriceDao: HomestayPriceDao,
    private val promotionDao: PromotionDao
) {
    private val _homes = MutableStateFlow<List<Home>>(emptyList())
    val homes: StateFlow<List<Home>> = _homes.asStateFlow()

    // keyed by (userId|homeId)
    private val _checkMap = MutableStateFlow<Map<String, CheckStatus>>(emptyMap())
    val checkMap: StateFlow<Map<String, CheckStatus>> = _checkMap.asStateFlow()

    init {
        // stream local DB to _homes
        CoroutineScope(Dispatchers.IO).launch {
            homeDao.getAllHomesFlow().collect { entities ->
                _homes.value = entities.map { it.toHome() }
            }
        }
    }

    // ---------------- Local (Room) ----------------

    suspend fun addHome(home: Home) = homeDao.insertHome(home.toEntity())
    suspend fun updateHome(home: Home) = homeDao.updateHome(home.toEntity())
    suspend fun removeHome(id: String) = homeDao.deleteHomeById(id)

    suspend fun addOrUpdateHome(home: Home) {
        homeDao.insertHome(
            HomeEntity(
                id = home.id,
                name = home.name,
                location = home.location,
                description = home.description,
                hostId = home.hostId
            )
        )
    }

    suspend fun getHomesByHostId(hostId: String) = homeDao.getHomesByHostId(hostId)

    // Make sure to import FirebaseFirestore
    private val homesCollection = FirebaseFirestore.getInstance().collection("homes")

    suspend fun getHomeNameById(homeId: String): String? {
        return try {
            val doc = homesCollection.document(homeId).get().await()
            doc.getString("name") // Firestore field for home name
        } catch (e: Exception) {
            null
        }
    }


    suspend fun getHomeWithDetails(id: String): HomeWithDetails? {
        val homeEntity = homeDao.getHomeById(id) ?: return null
        val price = homestayPriceDao.getPriceForHome(id)?.price
        val promotion = promotionDao.getPromotionForHome(id).firstOrNull()
        return HomeWithDetails(
            id = homeEntity.id,
            baseInfo = homeEntity.toHome(),
            price = price,
            promotion = promotion,
            checkStatus = null

        )
    }



    // ---------------- Firestore + Storage ----------------

    private suspend fun uploadImagesToStorage(uris: List<Uri>): List<String> {
        if (uris.isEmpty()) return emptyList()
        val storage = FirebaseStorage.getInstance().reference
        val urls = mutableListOf<String>()
        for (uri in uris) {
            val filename = "homes/${UUID.randomUUID()}.jpg"
            val ref = storage.child(filename)
            ref.putFile(uri).await()
            urls += ref.downloadUrl.await().toString()
        }
        return urls
    }

    /**
     * Upload images to Storage, then create a Home document in Firestore.
     * Writes BOTH `imageUrls` and `photoUris` for backward compatibility.
     */
    suspend fun addHomeToCloud(
        name: String,
        location: String,
        desc: String,
        photoUris: List<Uri>,
        hostId: String
    ) {
        val imageUrls = uploadImagesToStorage(photoUris)
        val homeId = UUID.randomUUID().toString()

        // Write as a map so we can control field names.
        val data = hashMapOf(
            "id" to homeId,
            "hostId" to hostId,
            "name" to name,
            "location" to location,
            "description" to desc,
            // new canonical field
            "imageUrls" to imageUrls,
            // legacy field so old readers keep working
            "photoUris" to imageUrls
        )

        FirebaseFirestore.getInstance()
            .collection("homes")
            .document(homeId)
            .set(data)
            .await()
    }

    /**
     * Live stream of homes from Firestore.
     * Accepts either `imageUrls` or `photoUris` from the document
     * and maps into Home.photoUris so UI loads images.
     */
    fun homesFromCloud(): Flow<List<Home>> = callbackFlow {
        val reg = FirebaseFirestore.getInstance()
            .collection("homes")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    close(err)
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { doc ->
                    val id = doc.getString("id") ?: doc.id
                    val hostId = doc.getString("hostId").orEmpty()
                    val name = doc.getString("name").orEmpty()
                    val location = doc.getString("location").orEmpty()
                    val description = doc.getString("description").orEmpty()

                    // Read either field name; prefer new imageUrls
                    @Suppress("UNCHECKED_CAST")
                    val imageUrls = (doc.get("imageUrls") as? List<*>)?.filterIsInstance<String>()
                        ?: (doc.get("photoUris") as? List<*>)?.filterIsInstance<String>()
                        ?: emptyList()

                    val hf = doc.toObject(HomeFirebase::class.java) ?: return@mapNotNull null
                    Home(
                        id = id,
                        name = name,
                        location = location,
                        description = description,
                        hostId = hostId,
                        // map into existing UI field
                        photoUris = imageUrls,
                        pricePerNight = hf.price ?: 0.0

                    )
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    // ---------------- Client check-in/out demo state ----------------

    suspend fun toggleCheckInOut(homeId: String, userId: String) {
        val key = "$userId|$homeId"
        val current = _checkMap.value[key]
        val next = if (current?.checkedIn == true) {
            current.copy(checkedIn = false, timestampMs = System.currentTimeMillis())
        } else {
            CheckStatus(homeId, userId, checkedIn = true)
        }
        _checkMap.value = _checkMap.value.toMutableMap().apply { put(key, next) }
    }

    fun checkStatus(homeId: String, userId: String): CheckStatus? =
        _checkMap.value["$userId|$homeId"]

    // ---------------- Mappers ----------------

    private fun HomeEntity.toHome(): Home = Home(
        id = id,
        name = name,
        location = location,
        description = description,
        hostId = hostId
    )

    private fun Home.toEntity(): HomeEntity = HomeEntity(
        id = id,
        name = name,
        location = location,
        description = description,
        hostId = hostId
    )
}
