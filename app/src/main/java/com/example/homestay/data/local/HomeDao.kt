package com.example.homestay.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HomeDao {

    @Query("SELECT * FROM home")
    suspend fun getAllHomes(): List<HomeEntity>

    @Query("SELECT * FROM home")
    fun getAllHomesFlow(): kotlinx.coroutines.flow.Flow<List<HomeEntity>>

    @Query("SELECT * FROM home WHERE id = :id")
    suspend fun getHomeById(id: String): HomeEntity?


    @Query("SELECT * FROM home WHERE hostId = :hostId")
    suspend fun getHomesByHostId(hostId: String): List<HomeEntity>


    @Query("SELECT * FROM home WHERE hostId = :hostId")
    fun observeHomesByHostId(hostId: String): Flow<List<HomeEntity>>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHome(home: HomeEntity)

    @Update
    suspend fun updateHome(home: HomeEntity)

    @Query("DELETE FROM home WHERE id = :id")
    suspend fun deleteHomeById(id: String)
}