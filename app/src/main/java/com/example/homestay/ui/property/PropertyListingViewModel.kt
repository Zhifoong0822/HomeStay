package com.example.homestay.ui.property

import android.net.Uri
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import java.util.UUID

class PropertyListingViewModel(
    private val repo: PropertyListingRepository,
    private val handle: SavedStateHandle
) : ViewModel() {

    val checkMap = repo.checkMap
    private var hostId: String = "" // <- store the logged-in hostId

    fun setHostId(id: String) {
        hostId = id
    }


    private object Keys {
        const val NAME = "draftName"
        const val LOCATION = "draftLocation"
        const val DESC = "draftDesc"
        const val DESCRIPTION = "draftDescription"
        const val PHOTO_URIS = "draftPhotoUris" // ArrayList<String>
    }

    /** Local + cloud homes (sorted by name) */
    val homes: StateFlow<List<Home>> = repo.homes
        .map { list -> list.sortedBy { it.name.lowercase() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val homesCloud: StateFlow<List<Home>> = repo.homesFromCloud()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Draft fields persisted via SavedStateHandle */
    var draftName: String
        get() = handle[Keys.NAME] ?: ""
        set(v) { handle[Keys.NAME] = v }

    var draftLocation: String
        get() = handle[Keys.LOCATION] ?: ""
        set(v) { handle[Keys.LOCATION] = v }

    var draftDesc: String
        get() = handle[Keys.DESC] ?: ""
        set(v) { handle[Keys.DESC] = v }

    var draftDescription: String
        get() = handle[Keys.DESCRIPTION] ?: ""
        set(v) { handle[Keys.DESCRIPTION] = v }

    /** Photo URIs: keep a Compose-friendly list in memory,
     * but persist ArrayList<String> in SavedStateHandle.
     */
    val draftPhotoUris: SnapshotStateList<String> = run {
        val saved: ArrayList<String> = handle[Keys.PHOTO_URIS] ?: arrayListOf()
        mutableStateListOf<String>().apply { addAll(saved) }
    }

    fun setDraftPhotoUris(newUris: List<String>) {
        draftPhotoUris.apply {
            clear()
            addAll(newUris)
        }
        handle[Keys.PHOTO_URIS] = ArrayList(draftPhotoUris)
    }

    fun appendDraftPhotoUris(extra: List<String>) =
        setDraftPhotoUris(draftPhotoUris + extra)

    fun removeDraftPhotoUriAt(index: Int) {
        if (index in 0 until draftPhotoUris.size) {
            draftPhotoUris.removeAt(index)
            handle[Keys.PHOTO_URIS] = ArrayList(draftPhotoUris)
        }
    }

    fun loadDraftFrom(home: Home) {
        draftName = home.name
        draftLocation = home.location
        draftDesc = home.description
        // Photos are cloud-only for now; keep as-is unless you load them.
    }

    fun clearDraft() {
        draftName = ""
        draftLocation = ""
        draftDesc = ""
        draftDescription = ""
        draftPhotoUris.clear()
        handle[Keys.PHOTO_URIS] = arrayListOf<String>()
    }

    // ---- Host (local DB) ----
    fun addHome(onDone: () -> Unit) = viewModelScope.launch {
        repo.addHome(Home(name = draftName, location = draftLocation, description = draftDesc))
        clearDraft()
        onDone()
    }

    fun addHomeWithId(onDone: (newId: String) -> Unit) = viewModelScope.launch {
        val newId = UUID.randomUUID().toString()
        repo.addHome(
            Home(
                id = newId,
                name = draftName,
                location = draftLocation,
                description = draftDesc
            )
        )
        clearDraft()
        onDone(newId)
    }

    // ---- Host (cloud) ----
    fun addHomeToCloud(photoUris: List<Uri>, onDone: () -> Unit) {
        val name = draftName
        val loc = draftLocation
        val desc = draftDesc
        viewModelScope.launch {
            repo.addHomeToCloud(name, loc, desc, photoUris, hostId)
            onDone()
        }
    }

    fun updateHome(
        id: String,
        name: String,
        location: String,
        description: String,
        onDone: () -> Unit
    ) = viewModelScope.launch {
        val current = homes.value.firstOrNull { it.id == id } ?: return@launch
        repo.updateHome(current.copy(name = name, location = location, description = description))
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
