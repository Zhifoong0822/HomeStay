package com.example.homestay.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HomestayPriceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHomestay(homestay: HomestayPrice)

    @Query("SELECT * FROM homestay_price")
    fun getAllHomestays(): Flow<List<HomestayPrice>>

    @Query("DELETE FROM homestay_price WHERE homeId = :homeId")
    suspend fun deletePriceForHome(homeId: String)

    @Query("SELECT * FROM homestay_price WHERE homeId = :homeId LIMIT 1")
    suspend fun getPriceForHome(homeId: String): HomestayPrice?

    @Query("SELECT * FROM homestay_price")
    fun getAllPrices(): Flow<List<HomestayPrice>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrice(price: HomestayPrice)

    @Update
    suspend fun updatePrice(price: HomestayPrice)

    @Query("UPDATE homestay_price SET price = :price WHERE homeId = :homeId")
    fun updatePriceByHomeId(homeId: String, price: Double)

    @Delete
    suspend fun deletePrice(price: HomestayPrice)
}

