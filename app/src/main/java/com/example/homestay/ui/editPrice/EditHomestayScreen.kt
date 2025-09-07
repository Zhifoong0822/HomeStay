//package com.example.homestay.ui.editPrice
//
//import androidx.compose.foundation.BorderStroke
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Delete
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import androidx.navigation.NavController
//import com.example.homestay.data.local.HomestayPrice
//import com.example.homestay.data.local.PromotionEntity
//import com.example.homestay.data.model.HomeWithDetails
//import com.example.homestay.ui.HostHome.HomeWithDetailsViewModel
//
//@Composable
//fun EditPriceScreen(
//    homestayName: String,
//    viewModel: HomeWithDetailsViewModel,   // ✅ use the new ViewModel
//    onBackClick: () -> Unit,
//    navController: NavController
//) {
//    val homeDetails by viewModel.homesWithDetails.collectAsState(initial = emptyList())
//
//    val home: HomeWithDetails? = homeDetails.firstOrNull { it.baseInfo.name == homestayName }
//    val price: Double? = home?.price
//    val homestayPromotion: PromotionEntity? = home?.promotion
//
//    var priceInput by remember(price) {
//        mutableStateOf(price?.toString() ?: "")
//    }
//
//    val snackbarHostState = remember { SnackbarHostState() }
//
//    Scaffold(
//        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
//        containerColor = Color(0xFFFEF9F3) // cream background
//    ) { padding ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(padding)
//                .padding(16.dp),
//            verticalArrangement = Arrangement.Top
//        ) {
//            // Title
//            Text(
//                "Edit Price",
//                style = MaterialTheme.typography.headlineSmall,
//                fontWeight = FontWeight.Bold,
//                color = Color(0xFF446F5C),
//                modifier = Modifier.padding(bottom = 16.dp)
//            )
//
//            // Homestay Name + Price Input
//            home?.let {
//                Text(
//                    "🏠 Homestay: ${it.baseInfo.name}",
//                    style = MaterialTheme.typography.titleMedium,
//                    fontWeight = FontWeight.Medium,
//                    modifier = Modifier.padding(bottom = 8.dp)
//                )
//                OutlinedTextField(
//                    value = priceInput,
//                    onValueChange = { newPrice -> priceInput = newPrice },
//                    label = { Text("Price (RM)") },
//                    singleLine = true,
//                    modifier = Modifier.fillMaxWidth()
//                )
//            }
//
//            Spacer(Modifier.height(24.dp))
//
//            // Promotions list
//            Text(
//                "Promotions:",
//                style = MaterialTheme.typography.titleMedium,
//                fontWeight = FontWeight.Medium,
//                color = Color(0xFF446F5C)
//            )
//
//            if (homestayPromotion == null) {
//                Text("No Promotion Yet", modifier = Modifier.padding(top = 8.dp))
//            } else {
//                Row(
//                    verticalAlignment = Alignment.CenterVertically,
//                    modifier = Modifier.padding(vertical = 4.dp)
//                ) {
//                    Text("${homestayPromotion.description} - ${homestayPromotion.discountPercent}% OFF")
//                    Spacer(Modifier.width(8.dp))
//                    IconButton(onClick = { viewModel.deletePromotion(homestayPromotion) }) {
//                        Icon(
//                            Icons.Default.Delete,
//                            contentDescription = "Delete Promo",
//                            tint = MaterialTheme.colorScheme.error
//                        )
//                    }
//                }
//            }
//
//            Spacer(Modifier.height(32.dp))
//
//            // Save button
//            Button(
//                onClick = {
//                    val newPrice = priceInput.toDoubleOrNull()
//                    if (newPrice != null) {
//                        viewModel.updatePrice(homestayName, newPrice)
//                        navController.previousBackStackEntry
//                            ?.savedStateHandle
//                            ?.set("snackbar_message", "Price updated successfully!")
//                        navController.popBackStack()
//                    }
//                },
//                colors = ButtonDefaults.buttonColors(
//                    containerColor = Color(0xFF446F5C),
//                    contentColor = Color.White
//                ),
//                shape = RoundedCornerShape(12.dp),
//                modifier = Modifier.fillMaxWidth()
//            ) {
//                Text("Save Price", fontWeight = FontWeight.Bold)
//            }
//
//            Spacer(Modifier.height(16.dp))
//
//            // Back button
//            OutlinedButton(
//                onClick = onBackClick,
//                border = BorderStroke(2.dp, Color(0xFF446F5C)),
//                colors = ButtonDefaults.outlinedButtonColors(
//                    contentColor = Color(0xFF446F5C)
//                ),
//                shape = RoundedCornerShape(50),
//                modifier = Modifier.fillMaxWidth()
//            ) {
//                Text("Back", fontWeight = FontWeight.Bold)
//            }
//        }
//    }
//}
