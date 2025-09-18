package com.example.homestay.ui.HostHome

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.homestay.data.model.HomeWithDetails
import java.net.URLEncoder
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.lazy.grid.items
import com.example.homestay.HomeStayScreen
import com.google.firebase.auth.FirebaseAuth


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homestays: List<HomeWithDetails>,
    onProfileClick: () -> Unit,
    onAddHomeClick: () -> Unit,
    onEditClick: (HomeWithDetails) -> Unit,
    onDeleteClick: (HomeWithDetails) -> Unit,
    onAddPromoClick: (HomeWithDetails) -> Unit,
    navController: NavController
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarMessage = navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getLiveData<String>("snackbar_message")
        ?.observeAsState()

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp

    val columns = if (screenWidth < 700) 1 else 2

    LaunchedEffect(snackbarMessage?.value) {
        snackbarMessage?.value?.let { message ->
            snackbarHostState.showSnackbar(message)
            navController.currentBackStackEntry
                ?.savedStateHandle
                ?.remove<String>("snackbar_message")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("🏡 My Homestays") },
                actions = {
                    Button(
                        onClick = onProfileClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF446F5C),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(50)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = "Profile", modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Profile", fontWeight = FontWeight.Medium)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddHomeClick,
                text = { Text("Add Home") },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add") },
                containerColor = Color(0xFF446F5C),
                contentColor = Color.White
            )
        },

        containerColor = Color(0xFFFEF9F3),
        bottomBar = {
            HostBottomBar(navController)
            }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Text(
                text = "Your registered homestays:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(homestays) { homestay ->
                    HomestayCard(
                        homestay = homestay,
                        onEditClick = { onEditClick(homestay) },
                        onDeleteClick = { onDeleteClick(homestay) },
                        onAddPromoClick = { onAddPromoClick(homestay) }
                    )
                }
            }
        }
    }
}



@Composable
fun HomestayCard(
    homestay: HomeWithDetails,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onAddPromoClick: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth() // fills the grid column
            .wrapContentHeight() // height adapts to content
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // --- Title & Actions ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = homestay.baseInfo.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)  // Text takes all remaining space
                        .padding(end = 8.dp)
                )

                Row {
                    OutlinedButton(
                        onClick = onEditClick,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF446F5C)),
                        modifier = Modifier.wrapContentWidth()
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                        Spacer(Modifier.width(4.dp))
                        Text("Edit")
                    }

                    Spacer(Modifier.width(4.dp)) // spacing between buttons

                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        modifier = Modifier.wrapContentWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                        Spacer(Modifier.width(4.dp))
                        Text("Delete")
                    }
                }
            }


            Spacer(Modifier.height(6.dp))

            // --- Location ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Place, contentDescription = "Location", tint = Color(0xFF446F5C), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Location: ${homestay.baseInfo.location}", color = Color(0xFF446F5C), fontWeight = FontWeight.Medium)
            }

            Spacer(Modifier.height(4.dp))

            // --- Description ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = "Description", tint = Color.DarkGray, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Description: ${homestay.baseInfo.description}", color = Color.DarkGray)
            }

            Spacer(Modifier.height(4.dp))

            // --- Price ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AttachMoney, contentDescription = "Price", tint = Color(0xFF446F5C), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (homestay.price != null) "Price: RM ${homestay.price} / night" else "Price: Not set yet",
                    color = Color(0xFF446F5C),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(4.dp))

            // --- Promotion ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, contentDescription = "Promotion", tint = if (homestay.promotion != null) Color(0xFF446F5C) else Color.Gray, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (homestay.promotion != null) "Promotion: ${homestay.promotion.description} (${homestay.promotion.discountPercent}% OFF)" else "No promotions yet",
                    color = if (homestay.promotion != null) Color(0xFF446F5C) else Color.Gray,
                    fontWeight = if (homestay.promotion != null) FontWeight.Medium else FontWeight.Normal
                )
            }

            Spacer(Modifier.height(10.dp))

            // --- Add Promotion Button ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onAddPromoClick,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF446F5C))
                ) { Text("Add Promotion") }
            }
        }
    }

    // --- Delete Confirmation Dialog ---
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirm Delete") },
            text = { Text("Are you sure you want to delete ${homestay.baseInfo.name} from your list?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteClick()
                        showDeleteDialog = false
                    }
                ) { Text("Yes", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}


@Composable
fun HomeScreenWrapper(homeVM: HomeWithDetailsViewModel, navController: NavController) {
    val homes by homeVM.homesWithDetails.collectAsState()

    val hostId = FirebaseAuth.getInstance().currentUser?.uid

    LaunchedEffect(hostId) {
        if (hostId != null) {
            homeVM.setHostId(hostId)
            homeVM.syncHomesFromFirebase()
        }
    }

    HomeScreen(
        homestays = homes,
        onProfileClick = { navController.navigate(HomeStayScreen.Profile.name) },
        onAddHomeClick = { navController.navigate("addHome") },
        onEditClick = { homestay -> navController.navigate("editHome/${homestay.id}") },
        onDeleteClick = { homestay ->
            homeVM.deleteHomeCompletely(homestay.id)
        },
        onAddPromoClick = { homestay ->
            val encodedName = URLEncoder.encode(homestay.baseInfo.name, "UTF-8")
            navController.navigate("addPromo/${homestay.id}/$encodedName")
        },
        navController = navController
    )
}

