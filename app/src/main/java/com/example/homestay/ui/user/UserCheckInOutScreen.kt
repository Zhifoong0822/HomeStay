@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.homestay.ui.user

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.homestay.ui.property.PropertyListingViewModel

@Composable
fun UserCheckInOutScreen(
    vm: PropertyListingViewModel,
    homeId: String,
    userId: String,
    onBack: () -> Unit
) {
    val homes by vm.homes.collectAsState()
    val home = homes.firstOrNull { it.id == homeId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(home?.name ?: "Home") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                }
            )
        }
    ) { pad ->
        if (home == null) {
            Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Home not found.")
            }
            return@Scaffold
        }

        val checkedIn = vm.isCheckedIn(homeId, userId)

        Column(
            Modifier
                .padding(pad)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(home.name, style = MaterialTheme.typography.titleLarge)
            Text(home.location, style = MaterialTheme.typography.bodyMedium)
            if (home.description.isNotBlank()) {
                Text(home.description, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(16.dp))
            ElevatedCard {
                Column(Modifier.padding(16.dp)) {
                    Text("Status", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(if (checkedIn) "You are currently checked in." else "You are not checked in.")
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { vm.toggleCheck(homeId, userId) },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(if (checkedIn) "Check Out" else "Check In")
                    }
                }
            }
        }
    }
}
