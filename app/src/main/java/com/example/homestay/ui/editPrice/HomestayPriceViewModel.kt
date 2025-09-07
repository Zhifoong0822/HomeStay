package com.example.homestay.ui.HostHome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homestay.data.model.*
import com.example.homestay.data.local.HomestayPrice
import com.example.homestay.data.local.PromotionEntity
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


    val homesWithDetails: StateFlow<List<HomeWithDetails>> =
        combine(
            propertyRepo.homes,
            homestayRepo.getAllHomestays(),// <-- Use the StateFlow from repo
            homestayRepo.getAllPromotions(),
            propertyRepo.checkMap
        ) { homes, prices, promos, checkMap ->
            homes.map { home ->
                val price = prices.firstOrNull { it.homeId == home.id }?.price
                val promo = promos.firstOrNull { it.homeId == home.id }
                val check = checkMap["${"currentUser"}|${home.id}"]
                HomeWithDetails(
                    id = home.id,
                    baseInfo = home,
                    price = price,
                    promotion = promo,
                    checkStatus = check
                )
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // --- Price ops (Room) ---
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
        }
    }


    fun addOrUpdatePromotionWithFirebase(homeId: String, desc: String, discount: Int) {
        viewModelScope.launch {
            // Save promo to Room
            homestayRepo.replacePromotionForHomestay(
                homeId,
                PromotionEntity(homeId = homeId, description = desc, discountPercent = discount)
            )

            // Get updated details
            val homeWithDetails = propertyRepo.getHomeWithDetails(homeId)

            if (homeWithDetails != null) {
                val promoModel = com.example.homestay.data.model.Promotion(
                    description = desc,
                    discountPercent = discount
                )

                val homeFirebase = com.example.homestay.data.model.HomeFirebase(
                    id = homeWithDetails.id,
                    name = homeWithDetails.baseInfo.name,
                    location = homeWithDetails.baseInfo.location,
                    description = homeWithDetails.baseInfo.description,
                    price = homeWithDetails.price,
                    promotion = promoModel
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
                    description = homeFirebase.description
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
        onAdded: (String) -> Unit
    ) {
        viewModelScope.launch {
            val newHome = Home(
                id = java.util.UUID.randomUUID().toString(),
                name = name,
                location = location,
                description = description
            )
            propertyRepo.addHome(newHome)
            onAdded(newHome.id) // return the new home ID
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
            // Persist home info to PropertyListingRepository / Room
            propertyRepo.addOrUpdateHome(updated)

            // Update price if provided
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
}
