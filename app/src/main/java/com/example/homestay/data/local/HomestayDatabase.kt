package com.example.homestay.data.local

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [HomeEntity::class,HomestayPrice::class, PromotionEntity::class], version = 3, exportSchema = false)
abstract class HomestayDatabase : RoomDatabase() {
    abstract fun homestayPriceDao(): HomestayPriceDao
    abstract fun promotionDao(): PromotionDao
    abstract fun HomeDao(): HomeDao

    companion object {
        @Volatile
        private var INSTANCE: HomestayDatabase? = null

        fun getDatabase(context: Context): HomestayDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HomestayDatabase::class.java,
                    "homestay_db"
                ).fallbackToDestructiveMigration().build()
                Log.d("HomestayDatabase", "Building HomestayDatabase v3 at homestay_db")

                INSTANCE = instance
                instance
            }
        }
    }
}


