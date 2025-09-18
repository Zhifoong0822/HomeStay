package com.example.homestay.ui.property

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.homestay.data.repository.PropertyListingRepository

class PropertyListingVMFactory(
    private val repo: PropertyListingRepository,
    private val savedState: SavedStateHandle
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PropertyListingViewModel(repo, savedState) as T
    }
}