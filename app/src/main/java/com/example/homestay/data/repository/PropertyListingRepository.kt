package com.example.homestay.data.repository

import com.example.homestay.data.local.HomeDao
import com.example.homestay.data.local.HomeEntity
import com.example.homestay.data.local.HomestayPriceDao
import com.example.homestay.data.local.PromotionDao
import com.example.homestay.data.model.CheckStatus
import com.example.homestay.data.model.Home
import com.example.homestay.data.model.HomeWithDetails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class PropertyListingRepository(
    private val homeDao: HomeDao,
    private val homestayPriceDao: HomestayPriceDao,
    private val promotionDao: PromotionDao
) {
    private val _homes = MutableStateFlow<List<Home>>(emptyList())
    val homes: StateFlow<List<Home>> = _homes.asStateFlow()

    // keyed by (userId, homeId)
    private val _checkMap = MutableStateFlow<Map<String, CheckStatus>>(emptyMap())
    val checkMap: StateFlow<Map<String, CheckStatus>> = _checkMap.asStateFlow()

    init {
        // Launch a coroutine to observe DB changes
        CoroutineScope(Dispatchers.IO).launch {
            homeDao.getAllHomesFlow().collect { entities ->
                _homes.value = entities.map { it.toHome() }
            }
        }
    }
    // ---- Host ops ----
    suspend fun addHome(home: Home) {
        homeDao.insertHome(home.toEntity())
    }

    suspend fun updateHome(home: Home) {
        homeDao.updateHome(home.toEntity())
    }

    suspend fun removeHome(id: String) {
        homeDao.deleteHomeById(id)
    }

    // ---- Client ops ----
    suspend fun toggleCheckInOut(homeId: String, userId: String) {
        val key = "$userId|$homeId"
        val current = _checkMap.value[key]
        val next = if (current?.checkedIn == true) {
            current.copy(checkedIn = false, timestampMs = System.currentTimeMillis())
        } else {
            CheckStatus(homeId, userId, checkedIn = true)
        }
        _checkMap.value = _checkMap.value.toMutableMap().apply { put(key, next) }
    }

    fun checkStatus(homeId: String, userId: String): CheckStatus? {
        return _checkMap.value["$userId|$homeId"]
    }

    suspend fun addOrUpdateHome(home: Home) {
        homeDao.insertHome(
            HomeEntity(
                id = home.id,
                name = home.name,
                location = home.location,
                description = home.description
            )
        )
    }

    suspend fun getHomeWithDetails(id: String): HomeWithDetails? {
        val homeEntity = homeDao.getHomeById(id) ?: return null
        val price = homestayPriceDao.getPriceForHome(id)?.price
        val promotion = promotionDao.getPromotionForHome(id)
        return HomeWithDetails(
            id = homeEntity.id,
            baseInfo = homeEntity.toHome(),
            price = price,
            promotion = promotion,
            checkStatus = null
        )
    }

    // Extension functions for mapping
    fun HomeEntity.toHome(): Home = Home(
        id = id,
        name = name,
        location = location,
        description = description
    )

    fun Home.toEntity(): HomeEntity = HomeEntity(
        id = id,
        name = name,
        location = location,
        description = description
    )
}

