package com.example.homestay.ui.property

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import androidx.activity.compose.BackHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostAddOrEditHomeScreen(
    homeId: String?,                 // null if adding new home
    homeVM: HomeWithDetailsViewModel,
    propertyVM: PropertyListingViewModel,
    isEdit: Boolean = false,
    onBack: () -> Unit,
    navController: NavController
) {
    val homes by homeVM.homesWithDetails.collectAsState()
    val homeDetails = homeId?.let { id -> homes.firstOrNull { it.id == id } }

    // --- Bind text inputs directly to VM-backed vars ---
    var name by remember { mutableStateOf(propertyVM.draftName) }
    var location by remember { mutableStateOf(propertyVM.draftLocation) }
    var description by remember { mutableStateOf(propertyVM.draftDescription) }
    var localPrice by remember(homeDetails) { mutableStateOf(homeDetails?.price?.toString() ?: "") }

    LaunchedEffect(name) { propertyVM.draftName = name }
    LaunchedEffect(location) { propertyVM.draftLocation = location }
    LaunchedEffect(description) { propertyVM.draftDescription = description }
    LaunchedEffect(homeDetails?.price) { localPrice = homeDetails?.price?.toString() ?: "" }

    val promotion = homeDetails?.promotion
    val snackbarHostState = remember { SnackbarHostState() }
    val firebaseRepo = remember { FirebaseRepository() }
    val coroutineScope = rememberCoroutineScope()

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val maxContentWidth = when {
        screenWidth < 600 -> screenWidth.dp
        screenWidth < 900 -> 600.dp
        else -> 800.dp
    }

    // ---- Photos: use the VM-owned SnapshotStateList directly ----
    val photoUris = propertyVM.draftPhotoUris

    // ---- permissions + pickers ----
    val context = LocalContext.current
    var cameraTempUri by remember { mutableStateOf<Uri?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* no-op */ }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) propertyVM.appendDraftPhotoUris(listOf(uri.toString())) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success -> if (success) cameraTempUri?.let { propertyVM.appendDraftPhotoUris(listOf(it.toString())) } }

    // Clear drafts for "Add" screen; load drafts for "Edit" screen
    LaunchedEffect(homeId, homeDetails) {
        if (homeId == null) {
            // New home → start with a clean form
            propertyVM.clearDraft()
        } else {
            // Edit existing → prefill from the selected home
            homeDetails?.home?.let { propertyVM.loadDraftFrom(it) }

        }
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

    BackHandler {
        propertyVM.clearDraft()
        onBack()
    }
    // -------------------------------------------------------------

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = Color(0xFFFEF9F3)
    ) { padding ->
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = maxContentWidth)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        if (isEdit) "Edit Home & Price" else "Add Home & Price",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF446F5C),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // --- Inputs ---
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
                    Spacer(Modifier.height(12.dp))

                    // --- Photo buttons ---
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ElevatedButton(
                            onClick = {
                                requestMediaPermissions()
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        ) { Text("+ Gallery") }

                        ElevatedButton(
                            onClick = {
                                requestMediaPermissions()
                                cameraTempUri = createImageUri(context)
                                cameraTempUri?.let { cameraLauncher.launch(it) }
                            }
                        ) { Text("+ Camera") }
                    }

                    // (Optional) quick preview/removal row (icons only)
                    if (photoUris.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        PhotoChips(
                            photoUris = propertyVM.draftPhotoUris,
                            onRemove = { index -> propertyVM.removeDraftPhotoUriAt(index) }
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = localPrice,
                        onValueChange = { localPrice = it },
                        label = { Text("Price (RM)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))

                    // --- Promotion Section ---
                    Text(
                        "Promotion:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF446F5C)
                    )

                    var showDeleteDialog by remember { mutableStateOf(false) }
                    if (promotion == null) {
                        Text("No Promotion Yet", modifier = Modifier.padding(top = 8.dp))
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${promotion.description} - ${promotion.discountPercent}% OFF")
                            Spacer(Modifier.width(8.dp))
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Promo", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        if (showDeleteDialog) {
                            AlertDialog(
                                onDismissRequest = { showDeleteDialog = false },
                                title = { Text("Delete Promotion") },
                                text = { Text("Are you sure you want to delete this promotion?") },
                                confirmButton = {
                                    TextButton(onClick = {
                                        homeVM.deletePromotion(promotion)
                                        showDeleteDialog = false
                                    }) { Text("Yes", color = MaterialTheme.colorScheme.error) }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    // put this near your other locals at the top of the Composable:
                    val context = LocalContext.current

                    // --- Save Button (uploads photos + saves to Firebase) ---
                    Button(
                        onClick = {
                            coroutineScope.launch {


                                // Build the HomeFirebase payload
                                val newPriceDouble = localPrice.toDoubleOrNull()
                                val updatedHome = Home(
                                    id = homeDetails?.id ?: UUID.randomUUID().toString(),
                                    name = name,
                                    location = location,
                                    description = description
                                )
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

                                // Convert & pre-validate photo URIs (skip bad ones)
                                val uriList: List<Uri> = photoUris.mapNotNull {
                                    runCatching { Uri.parse(it) }.getOrNull()
                                }
                                if (uriList.isEmpty()) {
                                    // No photos → just save base data (or your repo can accept empty list)
                                    try {
                                        // If your repo method requires context, use the first variant below.
                                        firebaseRepo.addHomeWithPhotos(
                                            /* context = */ context,  // <-- remove if your signature doesn't take context
                                            base = homeFirebase,
                                            photoUris = emptyList()
                                        )
                                        navController.popBackStack()
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("Save failed: ${e.localizedMessage ?: "unknown error"}")
                                    }
                                    return@launch
                                }

                                // Photos present → upload with error handling
                                try {
                                    // If your repository function signature is:
                                    // suspend fun addHomeWithPhotos(context: Context, base: HomeFirebase, photoUris: List<Uri>)
                                    firebaseRepo.addHomeWithPhotos(
                                        context = context,
                                        base = homeFirebase,
                                        photoUris = uriList
                                    )

                                    // If your signature is WITHOUT context, use this instead:
                                    // firebaseRepo.addHomeWithPhotos(base = homeFirebase, photoUris = uriList)

                                    // Optional: propertyVM.clearDraft()
                                    navController.popBackStack()
                                } catch (e: com.google.firebase.storage.StorageException) {
                                    snackbarHostState.showSnackbar("Upload failed: ${e.message ?: e.errorCode}")
                                } catch (e: IllegalArgumentException) {
                                    snackbarHostState.showSnackbar("Photo error: ${e.message}")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Save failed: ${e.localizedMessage ?: "unknown error"}")
                                }
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
                        onClick = {
                            propertyVM.clearDraft()
                            onBack()
                        },
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
    }
}
