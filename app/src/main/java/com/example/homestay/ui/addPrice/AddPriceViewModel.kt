package com.example.homestay.ui.addPrice

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.homestay.data.local.HomestayPrice
import com.example.homestay.data.repository.HomestayRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AddPriceUiState(
    val price: String = "",
    val errorMessage: String? = null
)

class AddPriceViewModel(
    private val repository: HomestayRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddPriceUiState())
    val uiState: StateFlow<AddPriceUiState> = _uiState

    fun onPriceChange(newPrice: String) {
        _uiState.value = _uiState.value.copy(price = newPrice, errorMessage = null)
    }

    fun onSavePrice(homeId: String) {
        val priceValue = _uiState.value.price.toDoubleOrNull()
        if (priceValue == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Invalid price")
            return
        }

        viewModelScope.launch {
            // fetch existing price from Room
            val existing = repository.getPriceForHome(homeId)
            if (existing != null) {
                val updated = existing.copy(price = priceValue)
                repository.updatePrice(updated)

                // save to Firestore
                saveToFirestore(updated)
            } else {
                val newHomestayPrice = HomestayPrice(
                    homeId = homeId,
                    price = priceValue
                )
                repository.insertPrice(newHomestayPrice)

                // save to Firestore
                saveToFirestore(newHomestayPrice)
            }
        }
    }

    private fun saveToFirestore(price: HomestayPrice) {
        val db = FirebaseFirestore.getInstance()
        db.collection("homestays")
            .document(price.homeId)   // use homeId as the Firestore doc id
            .set(price)
            .addOnSuccessListener {
                Log.d("Firestore", "Saved to Firestore! -> ${price.homeId}")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error saving", e)
            }
    }
}

class AddPriceViewModelFactory(
    private val repository: HomestayRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddPriceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddPriceViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
