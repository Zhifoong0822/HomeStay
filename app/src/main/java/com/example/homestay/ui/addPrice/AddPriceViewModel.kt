package com.example.homestay.ui.addPrice


import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AddPriceUiState(
    val price: String = "",
    val errorMessage: String? = null
)

class AddPriceViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AddPriceUiState())
    val uiState = _uiState.asStateFlow()

    fun onPriceChange(newPrice: String) {
        _uiState.value = _uiState.value.copy(price = newPrice)
    }

    fun onSavePrice() {
        val priceValue = _uiState.value.price
        if (priceValue.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Price cannot be empty")
        } else {
            _uiState.value = _uiState.value.copy(errorMessage = null)
            // TODO: Save to database or repository
            println("Saving price: $priceValue") // Temporary debug log
        }
    }
}
