@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.homestay.ui.property


import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.homestay.data.model.Home

@Composable
fun ClientHomeListScreen(
    vm: PropertyListingViewModel,
    userId: String,
    onCheckInOutClick: (home: Home) -> Unit
) {
    val homes by vm.homes.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("SKY BNB") }) }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(homes, key = { it.id }) { home ->
                    Card(
                        Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(home.name, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(home.location, style = MaterialTheme.typography.bodyMedium)
                            if (home.description.isNotBlank()) {
                                Text(home.description, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            // ✅ Footer with navigation button
            Button(
                onClick = {
                    // pass first home for demo, or adapt to selection
                    val firstHome = homes.firstOrNull()
                    if (firstHome != null) {
                        onCheckInOutClick(firstHome)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Go to Check In / Out")
            }
        }
    }
}