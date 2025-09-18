package com.example.homestay.ui.addPromo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.homestay.data.local.HomeDao
import com.example.homestay.data.local.HomestayDatabase
import com.example.homestay.data.repository.FirebaseRepository
import com.example.homestay.data.repository.HomestayRepository
import com.example.homestay.data.repository.PropertyListingRepository
import com.example.homestay.ui.HostHome.HomeWithDetailsViewModel
import java.net.URLDecoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPromoScreen(
    homeId: String,          // use this for repo operations
    homeName: String,        // use this just for UI text
    repository: HomestayRepository,
    onBackClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    val context = LocalContext.current
    val database = HomestayDatabase.getDatabase(context)

    // ✅ Create the PropertyListingRepository with all required DAOs
    val propertyRepo = PropertyListingRepository(
        homeDao = database.HomeDao(),
        homestayPriceDao = database.homestayPriceDao(),
        promotionDao = database.promotionDao()
    )

    val viewModel: HomeWithDetailsViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(HomeWithDetailsViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return HomeWithDetailsViewModel(
                        homestayRepo = repository,
                        propertyRepo = propertyRepo,
                        firebaseRepo = FirebaseRepository()
                    ) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    )

    val homesWithDetails by viewModel.homesWithDetails.collectAsState()
    val existingPromo = homesWithDetails
        .firstOrNull { it.id == homeId }
        ?.promotion

    var description by rememberSaveable(existingPromo) {
        mutableStateOf(existingPromo?.description ?: "")
    }

    var discount by rememberSaveable(existingPromo) {
        mutableStateOf(existingPromo?.discountPercent?.toString() ?: "")
    }

    var errorMessage by rememberSaveable {
        mutableStateOf<String?>(null)
    }


    Scaffold(
        topBar = {Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onBackClick,
                border = BorderStroke(2.dp, Color(0xFF446F5C)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF446F5C)
                ),
                shape = RoundedCornerShape(50)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                Spacer(Modifier.width(8.dp))
                Text("Back", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onProfileClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF446F5C),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(50),
                elevation = ButtonDefaults.buttonElevation(2.dp),
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Profile",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Profile", fontWeight = FontWeight.Medium)
            }
        } },
        containerColor = Color(0xFFFEF9F3)
    ) { innerPadding ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                "Add Promotion for $homeName",  // show name in UI
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF446F5C)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Promotion Description") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = discount,
                onValueChange = { discount = it },
                label = { Text("Discount %") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )

            if (errorMessage != null) {
                Text(
                    errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val discountValue = discount.toIntOrNull()
                    when {
                        description.isBlank() -> errorMessage = "Description cannot be empty"
                        discountValue == null || discountValue !in 0..100 ->
                            errorMessage = "Discount must be 0–100"
                        else -> {
                            viewModel.addOrUpdatePromotionWithFirebase(homeId, description, discountValue)
                            onBackClick()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF446F5C),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(8.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (existingPromo == null) "Save Promotion" else "Update Promotion",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
