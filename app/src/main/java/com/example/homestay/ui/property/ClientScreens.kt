package com.example.homestay.ui.property

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
@Composable
fun ClientHomeListScreen(
    vm: PropertyListingViewModel,
    userId: String,     // plug your real user later
) {
    val homes = vm.homes.collectAsState().value

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(homes, key = { it.id }) { home ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(home.name, style = MaterialTheme.typography.titleMedium)
                    Text(home.location, style = MaterialTheme.typography.bodyMedium)
                    Text(home.description, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(6.dp))
                    val checkedIn = vm.isCheckedIn(home.id, userId)
                    Button(
                        onClick = { vm.toggleCheck(home.id, userId) },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(if (checkedIn) "Check out" else "Check in")
                    }
                }
            }
        }
    }
}
