package com.example.homestay.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userId: String,
    val username: String,
    val email: String,
    val password: String,
    val gender: String,
    val birthdate: String,
    val role: String,
    val createdAt: Long,
    val updatedAt: Long
)