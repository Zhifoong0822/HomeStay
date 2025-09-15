package com.example.homestay.ui.client

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.homestay.data.model.Home
import com.example.homestay.ui.property.PropertyListingViewModel
import com.example.homestay.ui.common.isTablet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientBrowseScreen(
    vm: PropertyListingViewModel,
    onProfileClick: () -> Unit,
    onBottomHome: () -> Unit,
    onBottomExplore: () -> Unit,  // current screen
    onBottomSettings: () -> Unit,
) {
    val homes by vm.homesCloud.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(homes, query) {
        if (query.isBlank()) homes
        else homes.filter { it.name.contains(query, ignoreCase = true) }
    }
    val tablet = isTablet()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Available Homes") },
                actions = {
                    TextButton(onClick = onProfileClick) { Text("Profile") }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = false,
                    onClick = onBottomHome,
                    icon = { Icon(Icons.Filled.Home, null) },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = true,
                    onClick = onBottomExplore,
                    icon = { Icon(Icons.Filled.Explore, null) },
                    label = { Text("Explore") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onBottomSettings,
                    icon = { Icon(Icons.Filled.Settings, null) },
                    label = { Text("Settings") }
                )
            }
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
        ) {
            // Search bar
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                placeholder = { Text("Search by property name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No properties found.")
                }
            } else {
                if (!tablet) {
                    // PHONE: vertical list
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filtered, key = { it.id }) { home ->
                            ClientHomeCard(home)
                        }
                    }
                } else {
                    // TABLET: adaptive grid (nice cards in columns)
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 320.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        items(filtered, key = { it.id }) { home ->
                            ClientHomeCard(home)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClientHomeCard(home: Home) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = home.photoUris.firstOrNull(),
                contentDescription = home.name,
                modifier = Modifier.size(72.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(home.name, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(home.location, style = MaterialTheme.typography.bodyMedium)
                if (home.description.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(home.description, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
