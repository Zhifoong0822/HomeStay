package com.example.homestay.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "home")
data class HomeEntity(
    @PrimaryKey val id: String,       // same as Home.id
    val name: String,
    val location: String,
    val description: String
)