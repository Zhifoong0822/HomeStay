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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.example.homestay.AuthViewModel
import com.example.homestay.data.local.PromotionEntity
import com.example.homestay.data.model.*
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
    authViewModel: AuthViewModel,
    isEdit: Boolean = false,
    onBack: () -> Unit,
    navController: NavController
) {
    val uiState by authViewModel.uiState.collectAsState()
    val hostId = uiState.userProfile?.userId

    LaunchedEffect(hostId) {
        if (!hostId.isNullOrBlank()) {
            Log.d("DEBUG_HOMES", "Loading homes for hostId='$hostId'")
            homeVM.setHostId(hostId)
            homeVM.loadHostHomes(hostId)
        } else {
            Log.d("DEBUG_HOMES", "No hostId found — clearing homes")
            homeVM.clearHomes()
        }
    }

    val homes by homeVM.homesWithDetails.collectAsState(initial = emptyList())
    val homeDetails: HomeWithDetails? = homeId?.let { id -> homes.firstOrNull { it.id == id } }

    var name by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var localPrice by rememberSaveable { mutableStateOf("") }
    var promotion by remember { mutableStateOf<PromotionEntity?>(null) }
    val photoUris = propertyVM.draftPhotoUris

    LaunchedEffect(homeDetails) {
        homeDetails?.let {
            name = it.baseInfo.name
            location = it.baseInfo.location
            description = it.baseInfo.description
            localPrice = it.price?.toString() ?: ""
            promotion = it.promotion
            propertyVM.loadDraftFrom(it.baseInfo)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val firebaseRepo = remember { FirebaseRepository() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* no-op */ }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) propertyVM.appendDraftPhotoUris(listOf(uri.toString())) }

    var cameraTempUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success -> if (success) cameraTempUri?.let { propertyVM.appendDraftPhotoUris(listOf(it.toString())) } }

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

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val maxContentWidth = when {
        screenWidth < 600 -> screenWidth.dp
        screenWidth < 900 -> 600.dp
        else -> 800.dp
    }

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

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ElevatedButton(onClick = {
                            requestMediaPermissions()
                            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }) { Text("+ Gallery") }

                        ElevatedButton(onClick = {
                            requestMediaPermissions()
                            cameraTempUri = createImageUri(context)
                            cameraTempUri?.let { cameraLauncher.launch(it) }
                        }) { Text("+ Camera") }
                    }

                    if (photoUris.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        PhotoChips(
                            photoUris = photoUris,
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
                        val promoNonNull = promotion!!
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${promoNonNull.description} - ${promoNonNull.discountPercent}% OFF")
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
                                        homeVM.deletePromotion(promoNonNull)
                                        promotion = null
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

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val newPriceDouble = localPrice.toDoubleOrNull()
                                val updatedHome = Home(
                                    id = homeDetails?.id ?: UUID.randomUUID().toString(),
                                    name = name,
                                    location = location,
                                    description = description,
                                    hostId = hostId ?: ""
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
                                    promotion = promotionModel,
                                    hostId = hostId ?: ""
                                )

                                val uriList: List<Uri> = photoUris.mapNotNull { runCatching { Uri.parse(it) }.getOrNull() }

                                try {
                                    firebaseRepo.addHomeWithPhotos(context, homeFirebase, uriList)
                                    homeVM.addOrUpdateHomeInList(HomeWithDetails(
                                        id = updatedHome.id,
                                        baseInfo = updatedHome,
                                        price = newPriceDouble,
                                        promotion = promotion,
                                        checkStatus = null
                                    ))
                                    navController.popBackStack()
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
