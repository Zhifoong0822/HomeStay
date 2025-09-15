    package com.example.homestay.ui.HostHome

    import android.util.Log
    import androidx.lifecycle.ViewModel
    import androidx.lifecycle.viewModelScope
    import com.example.homestay.data.local.HomeEntity
    import com.example.homestay.data.local.HomestayPrice
    import com.example.homestay.data.local.PromotionEntity
    import com.example.homestay.data.model.*
    import com.example.homestay.data.repository.FirebaseRepository
    import com.example.homestay.data.repository.HomestayRepository
    import com.example.homestay.data.repository.PropertyListingRepository
    import kotlinx.coroutines.flow.*
    import kotlinx.coroutines.launch

    class HomeWithDetailsViewModel(
        private val firebaseRepo: FirebaseRepository,
        private val homestayRepo: HomestayRepository,
        private val propertyRepo: PropertyListingRepository
    ) : ViewModel() {

        private val _hostId = MutableStateFlow<String?>(null)
        fun setHostId(hostId: String?) { _hostId.value = hostId }

        private val _homesWithDetails = MutableStateFlow<List<HomeWithDetails>>(emptyList())
        val homesWithDetails: StateFlow<List<HomeWithDetails>> = _homesWithDetails

        private val _hostHomes = MutableStateFlow<List<HomeEntity>>(emptyList())
        val hostHomes: StateFlow<List<HomeEntity>> = _hostHomes

        init {
            // Automatically load homes whenever hostId is set (including first login)
            _hostId
                .filterNotNull()               // ignore null hostIds
                .distinctUntilChanged()        // only react to changes
                .onEach { hostId ->
                    Log.d("DEBUG_HOMES", "HostId detected: $hostId")
                    loadHostHomes(hostId)
                }
                .launchIn(viewModelScope)      // launches immediately
        }

        fun clearHomes() {
            _homesWithDetails.value = emptyList()
        }

        fun loadHostHomes(hostId: String) {
            _homesWithDetails.value = emptyList() // clear previous homes
            Log.d("DEBUG_HOMES", "Starting to load homes for hostId: $hostId")

            viewModelScope.launch {
                try {
                    // --- 1. Load from local DB ---
                    val localHomes: List<HomeEntity> = propertyRepo.getHomesByHostId(hostId)
                    Log.d("DEBUG_HOMES", "Local homes fetched: ${localHomes.size}")
                    localHomes.forEach { Log.d("DEBUG_HOMES", "Local Home: ${it.name}, hostId: ${it.hostId}") }

                    val localHomesWithDetails: List<HomeWithDetails> = localHomes.map { home ->
                        val price = homestayRepo.getPriceForHome(home.id)?.price
                        val promo = homestayRepo.getPromotionForHome(home.id)

                        HomeWithDetails(
                            id = home.id,
                            baseInfo = home.toHome(),
                            price = price,
                            promotion = promo,
                            checkStatus = null
                        )
                    }

                    // --- 2. Load from Firebase ---
                    val firebaseHomes = firebaseRepo.getHomesFromFirebase()
                    Log.d("DEBUG_HOMES", "Firebase homes fetched: ${firebaseHomes.size}")
                    firebaseHomes.forEach { Log.d("DEBUG_HOMES", "Firebase Home: ${it.name}, hostId: ${it.hostId}") }

                    val firebaseHomesWithDetails: List<HomeWithDetails> = firebaseHomes
                        .filter { it.hostId == hostId }
                        .map { it.toHomeWithDetails() }

                    Log.d("DEBUG_HOMES", "Firebase homes after filtering: ${firebaseHomesWithDetails.size}")

                    // --- Combine and remove duplicates ---
                    _homesWithDetails.value = (localHomesWithDetails + firebaseHomesWithDetails)
                        .distinctBy { it.id }

                    Log.d("DEBUG_HOMES", "Final combined homes count: ${_homesWithDetails.value.size}")
                    _homesWithDetails.value.forEach {
                        Log.d("DEBUG_HOMES", "Home: ${it.baseInfo.name}, ID: ${it.id}, Price: ${it.price}, Promo: ${it.promotion?.description}")
                    }

                } catch (e: Exception) {
                    Log.e("DEBUG_HOMES", "Error loading homes: ${e.message}", e)
                }
            }
        }


        private fun HomeFirebase.toHomeWithDetails() = HomeWithDetails(
            id = id,
            baseInfo = Home(id = id, name = name, location = location, description = description, hostId = hostId),
            price = price,
            promotion = promotion?.let { PromotionEntity(homeId = id, description = it.description, discountPercent = it.discountPercent) },
            checkStatus = null
        )

        private fun HomeEntity.toHome() = Home(
            id = id,
            name = name,
            location = location,
            description = description,
            hostId = hostId
        )

        // --- Price operations ---
        fun updatePrice(homeId: String, newPrice: Double) {
            viewModelScope.launch {
                homestayRepo.insertPrice(HomestayPrice(homeId = homeId, price = newPrice))
            }
        }

        fun deleteHomeCompletely(homeId: String) {
            viewModelScope.launch {
                // --- Room ---
                homestayRepo.deletePriceForHome(homeId)
                homestayRepo.deletePromotionsForHome(homeId)
                propertyRepo.removeHome(homeId)

                // --- Firebase ---
                firebaseRepo.deleteHome(homeId)

                _homesWithDetails.value = _homesWithDetails.value.filter { it.id != homeId }

            }
        }

        fun addOrUpdatePromotionWithFirebase(homeId: String, desc: String, discount: Int) {
            viewModelScope.launch {
                homestayRepo.replacePromotionForHomestay(
                    homeId,
                    PromotionEntity(homeId = homeId, description = desc, discountPercent = discount)
                )

                val homeWithDetails = propertyRepo.getHomeWithDetails(homeId)
                if (homeWithDetails != null) {
                    val promoModel = Promotion(description = desc, discountPercent = discount)
                    val homeFirebase = HomeFirebase(
                        id = homeWithDetails.id,
                        name = homeWithDetails.baseInfo.name,
                        location = homeWithDetails.baseInfo.location,
                        description = homeWithDetails.baseInfo.description,
                        price = homeWithDetails.price,
                        promotion = promoModel,
                        hostId = homeWithDetails.baseInfo.hostId
                    )
                    firebaseRepo.addHomeToFirebase(homeFirebase)
                }
            }
        }

        fun syncHomesFromFirebase() {
            viewModelScope.launch {
                val firebaseHomes = firebaseRepo.getHomesFromFirebase()
                firebaseHomes.forEach { homeFirebase ->
                    val home = Home(
                        id = homeFirebase.id,
                        name = homeFirebase.name,
                        location = homeFirebase.location,
                        description = homeFirebase.description,
                        hostId = homeFirebase.hostId
                    )
                    propertyRepo.addOrUpdateHome(home)

                    homeFirebase.price?.let { price ->
                        homestayRepo.insertPrice(HomestayPrice(homeId = home.id, price = price))
                    }

                    homeFirebase.promotion?.let { promo ->
                        homestayRepo.replacePromotionForHomestay(
                            home.id,
                            PromotionEntity(
                                homeId = home.id,
                                description = promo.description,
                                discountPercent = promo.discountPercent
                            )
                        )
                    }
                }
            }
        }

        fun deletePromotion(promotion: PromotionEntity) {
            viewModelScope.launch {
                homestayRepo.deletePromotion(promotion)
            }
        }

        fun addHome(
            name: String,
            location: String,
            description: String,
            hostId: String,
            onAdded: (String) -> Unit
        ) {
            viewModelScope.launch {
                val newHome = Home(
                    id = java.util.UUID.randomUUID().toString(),
                    name = name,
                    location = location,
                    description = description,
                    hostId = hostId
                )
                propertyRepo.addHome(newHome)
                onAdded(newHome.id)
            }
        }

        fun addHomeWithId(home: Home, price: Double?, onComplete: (String) -> Unit) {
            val newHome = home.copy(id = home.id.ifBlank { generateHomeId() })
            viewModelScope.launch {
                propertyRepo.addOrUpdateHome(newHome)
                if (price != null) homestayRepo.insertPrice(HomestayPrice(homeId = newHome.id, price = price))
                onComplete(newHome.id)
            }
        }

        private fun generateHomeId(): String {
            return "home_" + System.currentTimeMillis()
        }

        fun updateHome(updated: Home, newPrice: Double? = null) {
            viewModelScope.launch {
                propertyRepo.addOrUpdateHome(updated)
                if (newPrice != null) {
                    homestayRepo.insertPrice(HomestayPrice(homeId = updated.id, price = newPrice))
                }
            }
        }

        fun removeHome(id: String) {
            viewModelScope.launch { propertyRepo.removeHome(id) }
        }

        fun toggleCheckInOut(homeId: String, userId: String) {
            viewModelScope.launch { propertyRepo.toggleCheckInOut(homeId, userId) }
        }

        fun addOrUpdateHomeInList(home: HomeWithDetails) {
            val currentList = _homesWithDetails.value.toMutableList()
            val index = currentList.indexOfFirst { it.id == home.id }

            if (index >= 0) {
                currentList[index] = home
            } else {
                currentList.add(home)
            }

            _homesWithDetails.value = currentList
        }

    }
