package com.example.homestay.data.repository

import com.example.homestay.data.local.HomeEntity
import com.example.homestay.data.local.HomestayPrice
import com.example.homestay.data.local.HomestayPriceDao
import com.example.homestay.data.local.PromotionDao
import com.example.homestay.data.local.PromotionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class HomestayRepository(
    private val priceDao: HomestayPriceDao,
    private val promotionDao: PromotionDao
) {
    private val _prices = MutableStateFlow<List<HomestayPrice>>(emptyList())
    val prices: StateFlow<List<HomestayPrice>> = _prices.asStateFlow()

    init {
        // Initialize from Room
        priceDao.getAllHomestays().map { _prices.value = it }
    }


    // --- Homestays ---
    fun getAllHomestays(): Flow<List<HomestayPrice>> =
        priceDao.getAllHomestays()



    suspend fun insertHomestay(homestay: HomestayPrice) =
        priceDao.insertHomestay(homestay)

    suspend fun updateHomestay(homestay: HomestayPrice) =
        priceDao.updatePrice(homestay)

    suspend fun getPromotionForHome(homeId: String): PromotionEntity? {
        return promotionDao.getPromotionForHome(homeId)
    }

    suspend fun getPriceForHome(homeId: String): HomestayPrice? {
        return priceDao.getPriceForHome(homeId)
    }



    suspend fun insertPrice(price: HomestayPrice) {
        priceDao.insertPrice(price)
        val current = _prices.value.toMutableList()
        val index = current.indexOfFirst { it.homeId == price.homeId }
        if (index >= 0) current[index] = price else current.add(price)
        _prices.value = current
    }

    suspend fun updatePrice(price: HomestayPrice) = priceDao.updatePrice(price)

    suspend fun deletePriceForHome(homeId: String) =
        priceDao.deletePriceForHome(homeId)

    // --- Promotions ---
    fun getAllPromotions(): Flow<List<PromotionEntity>> =
        promotionDao.getAllPromotions()




    suspend fun insertPromotion(promotion: PromotionEntity) =
        promotionDao.insertPromotion(promotion)

    suspend fun updatePromotion(promotion: PromotionEntity) =
        promotionDao.updatePromotion(promotion)

    suspend fun deletePromotionsForHome(homeId: String) =
        promotionDao.deletePromotionsForHome(homeId)

    suspend fun deletePromotion(promotion: PromotionEntity) =
        promotionDao.deletePromotion(promotion)


    suspend fun replacePromotionForHomestay(homeId: String, promo: PromotionEntity) {
        promotionDao.deletePromotionsForHome(homeId)
        promotionDao.insertPromotion(promo)
    }
}

