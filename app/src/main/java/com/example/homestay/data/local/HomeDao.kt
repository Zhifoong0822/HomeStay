package com.example.homestay.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface HomeDao {
    data class HomeWithPriceRow(
        val id: String,
        val name: String,
        val location: String,
        val description: String,
        val hostId: String,
        val price: Double?        // nullable because LEFT JOIN
    )


    @Query("SELECT * FROM home")
    suspend fun getAllHomes(): List<HomeEntity>

    @Query("SELECT * FROM home")
    fun getAllHomesFlow(): kotlinx.coroutines.flow.Flow<List<HomeEntity>>

    @Query("SELECT * FROM home WHERE id = :id")
    suspend fun getHomeById(id: String): HomeEntity?


    @Query("SELECT * FROM home WHERE hostId = :hostId")
    suspend fun getHomesByHostId(hostId: String): List<HomeEntity>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHome(home: HomeEntity)

    @Update
    suspend fun updateHome(home: HomeEntity)

    @Query("DELETE FROM home WHERE id = :id")
    suspend fun deleteHomeById(id: String)


}