    package com.example.homestay.ui.property

    import android.Manifest
    import android.content.Context
    import android.net.Uri
    import android.util.Log
    import androidx.activity.compose.rememberLauncherForActivityResult
    import androidx.activity.result.PickVisualMediaRequest
    import androidx.activity.result.contract.ActivityResultContracts
    import androidx.compose.foundation.BorderStroke
    import androidx.compose.foundation.layout.*
    import androidx.compose.foundation.lazy.LazyColumn
    import androidx.compose.foundation.lazy.LazyRow
    import androidx.compose.foundation.lazy.items
    import androidx.compose.foundation.shape.RoundedCornerShape
    import androidx.compose.foundation.text.KeyboardOptions
    import androidx.compose.material.icons.Icons
    import androidx.compose.material.icons.automirrored.filled.ArrowBack
    import androidx.compose.material.icons.filled.ArrowBack
    import androidx.compose.material.icons.filled.Delete
    import androidx.compose.material.icons.filled.Save
    import androidx.compose.material3.*
    import androidx.compose.material3.AssistChip
    import androidx.compose.runtime.*
    import androidx.compose.runtime.saveable.rememberSaveable
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.platform.LocalContext
    import androidx.compose.ui.text.font.FontWeight
    import androidx.compose.ui.text.input.KeyboardCapitalization
    import androidx.compose.ui.unit.dp
    import androidx.core.content.FileProvider
    import androidx.navigation.NavController
    import com.example.homestay.data.model.Home
    import com.example.homestay.data.model.HomeFirebase
    import com.example.homestay.data.repository.FirebaseRepository
    import com.example.homestay.ui.HostHome.HomeWithDetailsViewModel
    import kotlinx.coroutines.launch
    import java.io.File
    import java.text.SimpleDateFormat
    import java.util.Date
    import java.util.Locale
    import java.util.UUID

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun HostAddOrEditHomeScreen(
        homeId: String?, // null if adding new home
        homeVM: HomeWithDetailsViewModel,
        propertyVM: PropertyListingViewModel,
        isEdit: Boolean = false,
        onBack: () -> Unit,
        navController: NavController
    ) {
        val homes by homeVM.homesWithDetails.collectAsState()
        val homeDetails = homeId?.let { id -> homes.firstOrNull { it.id == id } }

        var name by remember(homeDetails) { mutableStateOf(homeDetails?.baseInfo?.name ?: "") }
        var location by remember(homeDetails) { mutableStateOf(homeDetails?.baseInfo?.location ?: "") }
        var description by remember(homeDetails) { mutableStateOf(homeDetails?.baseInfo?.description ?: "") }
        var localPrice by remember { mutableStateOf(homeDetails?.price?.toString() ?: "") }

        LaunchedEffect(homeDetails?.price) {
            localPrice = homeDetails?.price?.toString() ?: ""
        }

        val promotion = homeDetails?.promotion
        val snackbarHostState = remember { SnackbarHostState() }

        val firebaseRepo = remember { FirebaseRepository() }
        val coroutineScope = rememberCoroutineScope()



        // chosen image URIs (string form for now)
        val photoUris = remember { mutableStateListOf<String>() }

        // ---- permissions + pickers ----
        val context = LocalContext.current
        val cameraTempUri = remember { mutableStateOf<Uri?>(null) }

        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { /* no-op */ }

        val galleryLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) photoUris.add(uri.toString())
        }

        val cameraLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            if (success) cameraTempUri.value?.let { photoUris.add(it.toString()) }
        }

        fun createImageUri(ctx: Context): Uri {
            val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val image = File.createTempFile("IMG_${time}_", ".jpg", ctx.cacheDir)
            return FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", image)
        }

        fun requestMediaPermissions() {
            val perms = buildList {
                add(Manifest.permission.CAMERA)
                if (android.os.Build.VERSION.SDK_INT >= 33) {
                    add(Manifest.permission.READ_MEDIA_IMAGES)
                } else {
                    add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
            permissionLauncher.launch(perms)
        }
        // -------------------------------

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            containerColor = Color(0xFFFEF9F3)
        ) { padding ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Top
            ) {
                // --- Title ---
                Text(
                    if (isEdit) "Edit Home & Price" else "Add Home & Price",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF446F5C),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // --- Home Details ---
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Home Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))

                // --- Price Section ---
                OutlinedTextField(
                    value = localPrice,
                    onValueChange = { localPrice = it },
                    label = { Text("Price (RM)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF446F5C),
                        unfocusedBorderColor = Color.Gray,
                        cursorColor = Color(0xFF446F5C)
                    )
                )
                Spacer(Modifier.height(16.dp))

                // --- Promotion Section ---
                Text(
                    "Promotion:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF446F5C)
                )
                if (promotion == null) {
                    Text("No Promotion Yet", modifier = Modifier.padding(top = 8.dp))
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text("${promotion.description} - ${promotion.discountPercent}% OFF")
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = { homeVM.deletePromotion(promotion) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete Promo",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))
                // --- Save Button ---
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val newPrice = localPrice.toDoubleOrNull()
                            val updatedHome = Home(
                                id = homeDetails?.id ?: UUID.randomUUID().toString(),
                                name = name,
                                location = location,
                                description = description
                            )
                            val newPriceDouble = localPrice.toDoubleOrNull()
                            if (newPriceDouble != null) {
                                homeVM.updateHome(updatedHome, newPriceDouble)
                            }

                            val promotionModel = promotion?.let {
                                com.example.homestay.data.model.Promotion(
                                    description = it.description,
                                    discountPercent = it.discountPercent
                                )
                            }
                            val homeFirebase = HomeFirebase(
                                id = updatedHome.id,
                                name = updatedHome.name,
                                location = updatedHome.location,
                                description = updatedHome.description,
                                price = newPriceDouble,
                                promotion = promotionModel
                            )

                            firebaseRepo.addHomeToFirebase(homeFirebase)
                            navController.popBackStack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF446F5C),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Save")
                    Spacer(Modifier.width(8.dp))
                    Text("Save", fontWeight = FontWeight.Bold)
                }


                Spacer(Modifier.height(16.dp))

                OutlinedButton(
                    onClick = onBack,
                    border = BorderStroke(2.dp, Color(0xFF446F5C)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF446F5C)),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Back", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
