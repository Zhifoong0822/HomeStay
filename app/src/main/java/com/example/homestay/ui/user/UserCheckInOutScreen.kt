@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.homestay.ui.user

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.homestay.ui.property.PropertyListingViewModel
import com.example.homestay.ui.common.isTablet

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
            Box(
                Modifier
                    .padding(pad)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Home not found.")
            }
            return@Scaffold
        }

        // State survives rotation
        var checkedIn by rememberSaveable { mutableStateOf(vm.isCheckedIn(homeId, userId)) }

        val tablet = isTablet()

        if (!tablet) {
            // ---------- PHONE LAYOUT ----------
            Column(
                Modifier
                    .padding(pad)
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PropertyHeader(home.name, home.location, home.description)

                CheckInCard(
                    checkedIn = checkedIn,
                    onToggle = {
                        vm.toggleCheck(homeId, userId)
                        checkedIn = !checkedIn
                    }
                )
            }
        } else {
            // ---------- TABLET LAYOUT ----------
            Row(
                Modifier
                    .padding(pad)
                    .padding(16.dp)
                    .fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    PropertyHeader(home.name, home.location, home.description)
                }
                Column(Modifier.weight(1f)) {
                    CheckInCard(
                        checkedIn = checkedIn,
                        onToggle = {
                            vm.toggleCheck(homeId, userId)
                            checkedIn = !checkedIn
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PropertyHeader(name: String, location: String, description: String) {
    Text(name, style = MaterialTheme.typography.titleLarge)
    Text(location, style = MaterialTheme.typography.bodyMedium)
    if (description.isNotBlank()) {
        Spacer(Modifier.height(4.dp))
        Text(description, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun CheckInCard(checkedIn: Boolean, onToggle: () -> Unit) {
    ElevatedCard {
        Column(Modifier.padding(16.dp)) {
            Text("Status", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(if (checkedIn) "You are currently checked in." else "You are not checked in.")
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onToggle,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(if (checkedIn) "Check Out" else "Check In")
            }
        }
    }
}
