package com.example.homestay.data.model

data class HomeFirebase(
    val id: String = "",
    val name: String = "",
    val location: String = "",
    val description: String = "",
    val price: Double? = null,
    val promotion: Promotion? = null,
    val imageUrls: List<String> = emptyList(),
    val hostId: String? = null
)

data class Promotion(
    val description: String = "",
    val discountPercent: Int = 0
)



