package com.example.homestay.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PromotionDao {

    @Query("SELECT * FROM promotions WHERE homeId = :homeId LIMIT 1")
    fun getPromotionForHome(homeId: String): Flow<PromotionEntity?>

    @Query("SELECT * FROM promotions")
    fun getAllPromotions(): Flow<List<PromotionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPromotion(promotion: PromotionEntity)

    // 👇 bulk insert
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPromotions(promotions: List<PromotionEntity>)

    @Update
    suspend fun updatePromotion(promotion: PromotionEntity)

    @Delete
    suspend fun deletePromotion(promotion: PromotionEntity)


    @Query("DELETE FROM promotions WHERE homeId = :homeId")
    suspend fun deletePromotionsForHome(homeId: String)
}
