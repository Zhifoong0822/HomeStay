package com.example.homestay

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Extension property for Context
private val Context.dataStore by preferencesDataStore("user_prefs")

class DataStoreManager(private val context: Context) {

    companion object {
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val USER_ROLE = stringPreferencesKey("user_role")
    }

    // Save login status
    suspend fun setLoginStatus(isLoggedIn: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[IS_LOGGED_IN] = isLoggedIn
            if (!isLoggedIn) {
                prefs.remove(USER_ROLE) // clear role when logging out
            }
        }
    }

    // Save role
    suspend fun setUserRole(role: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_ROLE] = role
        }
    }

    // Read login status
    val isLoggedIn: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[IS_LOGGED_IN] ?: false }

    // Read role
    val userRole: Flow<String?> = context.dataStore.data
        .map { prefs -> prefs[USER_ROLE] }
}