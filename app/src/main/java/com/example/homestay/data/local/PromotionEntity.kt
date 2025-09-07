    package com.example.homestay.data.local

    import androidx.room.Entity
    import androidx.room.PrimaryKey
    @Entity(tableName = "promotions")
    data class PromotionEntity(
        @PrimaryKey(autoGenerate = true) val id: Int = 0,
        val homeId: String,   // ✅ Link to Home.id
        val description: String = "",
        val discountPercent: Int = 0
    )