package com.example.homestay.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "homestay_price")
data class HomestayPrice(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val homeId: String,   // 🔗 link to Home.id
    val price: Double? = null
)



