package com.example.homestay.data.local

import com.example.homestay.UserProfile
import com.example.homestay.data.local.UserEntity

// Convert from Room entity → UI model
fun UserEntity.toUserProfile(): UserProfile {
    return com.example.homestay.UserProfile(
        userId = userId,
        username = username,
        email = email,
        gender = gender,
        birthdate = birthdate,
        role = role,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

// Optional: Convert back from UI model → Room entity
fun UserProfile.toUserEntity(password: String): UserEntity {
    return UserEntity(
        userId = userId,
        username = username,
        email = email,
        password = password, // only Room stores this
        gender = gender,
        birthdate = birthdate,
        role = role,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}