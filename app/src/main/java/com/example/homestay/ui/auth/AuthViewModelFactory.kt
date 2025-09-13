package com.example.homestay

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class AuthViewModelFactory(
    private val context: Context,
    private val dataStoreManager: DataStoreManager
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> {
                try {
                    AuthViewModel(
                        context = context,
                        AuthRepository(context),
                        dataStoreManager
                    ) as T
                } catch (e: Exception) {
                    Log.e("AuthViewModelFactory", "Failed to create AuthViewModel", e)
                    throw RuntimeException("Failed to create AuthViewModel", e)
                }
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}