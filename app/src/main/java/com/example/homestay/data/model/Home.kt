package com.example.homestay.data.model

import com.example.homestay.data.local.PromotionEntity
import java.util.UUID

data class HomeWithDetails(
    val id: String,
    val baseInfo: Home,                  // from PropertyListingRepository
    val price: Double?,                  // from HomestayPriceDao
    val promotion: PromotionEntity?,     // from PromotionDao
    val checkStatus: CheckStatus?        // from PropertyListingRepository
) {
    val home: Home
        get() = baseInfo
}

data class Home(
    val id: String = UUID.randomUUID().toString(),
    var name: String = "",
    var location: String = "",
    var description: String = "",
    var photoUris: List<String> = emptyList(),
    val hostId: String = ""   ,
    val pricePerNight: Double = 0.0,
    val imageUrls: List<String> = emptyList()

)


data class CheckStatus(
    val homeId: String,
    val userId: String,
    val checkedIn: Boolean,
    val timestampMs: Long = System.currentTimeMillis()
)