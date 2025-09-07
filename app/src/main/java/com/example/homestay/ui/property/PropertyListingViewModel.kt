package com.example.homestay.ui.property

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homestay.data.model.Home
import com.example.homestay.data.repository.PropertyListingRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.collections.sortedBy

class PropertyListingViewModel(
    private val repo: PropertyListingRepository,
    private val savedState: SavedStateHandle
) : ViewModel() {

    val homes: StateFlow<List<Home>> = repo.homes
        .map { it.sortedBy { h -> h.name.lowercase() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // persist form fields across rotation
    var draftName: String
        get() = savedState["draftName"] ?: ""
        set(v) { savedState["draftName"] = v }

    var draftLocation: String
        get() = savedState["draftLocation"] ?: ""
        set(v) { savedState["draftLocation"] = v }

    var draftDesc: String
        get() = savedState["draftDesc"] ?: ""
        set(v) { savedState["draftDesc"] = v }

    fun loadDraftFrom(home: Home) {
        draftName = home.name
        draftLocation = home.location
        draftDesc = home.description
    }

    fun clearDraft() {
        draftName = ""; draftLocation = ""; draftDesc = ""
    }

    // ---- Host ----
    fun addHome(onDone: () -> Unit) = viewModelScope.launch {
        repo.addHome(Home(name = draftName, location = draftLocation, description = draftDesc))
        clearDraft()
        onDone()
    }


    fun addHomeWithId(onDone: (newId: String) -> Unit) = viewModelScope.launch {
        val newId = UUID.randomUUID().toString()
        val home = Home(
            id = newId,
            name = draftName,
            location = draftLocation,
            description = draftDesc
        )
        repo.addHome(home)
        clearDraft()
        onDone(newId)
    }


    fun updateHome(
        id: String,
        name: String,
        location: String,
        description: String,
        onDone: () -> Unit
    ) = viewModelScope.launch {
        val current = homes.value.firstOrNull { it.id == id } ?: return@launch
        repo.updateHome(current.copy(
            name = name,
            location = location,
            description = description
        ))
        onDone()
    }

    fun removeHome(id: String) = viewModelScope.launch { repo.removeHome(id) }

    // ---- Client ----
    fun toggleCheck(homeId: String, userId: String) = viewModelScope.launch {
        repo.toggleCheckInOut(homeId, userId)
    }

    fun isCheckedIn(homeId: String, userId: String): Boolean {
        return repo.checkStatus(homeId, userId)?.checkedIn == true
    }
}
