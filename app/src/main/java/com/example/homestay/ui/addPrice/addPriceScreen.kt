//import androidx.compose.foundation.BorderStroke
//import androidx.compose.foundation.background
//import androidx.compose.foundation.border
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.foundation.text.KeyboardOptions
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.ArrowBackIosNew
//import androidx.compose.material.icons.filled.ArrowCircleLeft
//import androidx.compose.material.icons.filled.AttachMoney
//import androidx.compose.material.icons.filled.Person
//import androidx.compose.material.icons.filled.Save
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.text.font.FontFamily
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.input.KeyboardType
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.tooling.preview.Preview
//import androidx.compose.ui.unit.dp
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewmodel.compose.viewModel
//import com.example.homestay.data.local.HomestayDatabase
//import com.example.homestay.data.repository.HomestayRepository
//import com.example.homestay.ui.HostHome.HomeWithDetailsViewModel
//import com.example.homestay.ui.addPrice.AddPriceViewModel
//import com.example.homestay.ui.addPrice.AddPriceViewModelFactory
//
//
//@Composable
//fun AddPriceScreen(
//    homeId: String,
//    homeVM: HomeWithDetailsViewModel,
//    onBackClick: () -> Unit = {},
//    onProfileClick: () -> Unit = {},
//    onSaveClick: () -> Unit = {}
//) {
//    val context = LocalContext.current
//    val db = remember { HomestayDatabase.getDatabase(context) }
//    val repository = remember {
//        HomestayRepository(db.homestayPriceDao(), db.promotionDao())
//    }
//    val viewModel: AddPriceViewModel = viewModel(
//        factory = AddPriceViewModelFactory(repository)
//    )
//    val state by viewModel.uiState.collectAsState()
//
//
//    val homes by homeVM.homesWithDetails.collectAsState()
//    val home = homes.firstOrNull { it.id == homeId }
//    val homestayName = home?.baseInfo?.name ?: "Unknown"
//
//    Scaffold(
//        containerColor = Color(0xFFFEF9F3) // background cream
//    ) { padding ->
//
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(padding)
//                .padding(16.dp),
//            verticalArrangement = Arrangement.Top,
//            horizontalAlignment = Alignment.Start
//        ) {
//            // --- Top Bar Row ---
//            Row(verticalAlignment = Alignment.CenterVertically) {
//                // Back Button
//                OutlinedButton(
//                    onClick = onBackClick,
//                    colors = ButtonDefaults.outlinedButtonColors(
//                        contentColor = Color(0xFF446F5C)
//                    ),
//                    shape = RoundedCornerShape(50),
//                    border = BorderStroke(2.dp, Color(0xFF446F5C)),
//                ) {
//                    Icon(
//                        Icons.Default.ArrowCircleLeft,
//                        contentDescription = "Go Back",
//                        modifier = Modifier.size(20.dp)
//                    )
//                    Spacer(Modifier.width(6.dp))
//                    Text("Back", fontWeight = FontWeight.SemiBold)
//                }
//
//                Spacer(modifier = Modifier.weight(1f))
//
//                // Profile Button
//                Button(
//                    onClick = onProfileClick,
//                    colors = ButtonDefaults.buttonColors(
//                        containerColor = Color(0xFF446F5C),
//                        contentColor = Color.White
//                    ),
//                    shape = RoundedCornerShape(50),
//                    elevation = ButtonDefaults.buttonElevation(2.dp),
//                ) {
//                    Icon(
//                        Icons.Default.Person,
//                        contentDescription = "Profile",
//                        modifier = Modifier.size(20.dp)
//                    )
//                    Spacer(Modifier.width(6.dp))
//                    Text("Profile", fontWeight = FontWeight.Medium)
//                }
//            }
//
//            // --- Title ---
//            Text(
//                text = "Add Price",
//                textAlign = TextAlign.Center,
//                style = MaterialTheme.typography.headlineMedium,
//                fontWeight = FontWeight.Bold,
//                color = Color(0xFF446F5C),
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(vertical = 20.dp)
//            )
//
//            // --- Homestay Name Card ---
//            Card(
//                colors = CardDefaults.cardColors(
//                    containerColor = Color.White
//                ),
//                shape = RoundedCornerShape(12.dp),
//                modifier = Modifier.fillMaxWidth()
//            ) {
//                Text(
//                    text = "🏠 Homestay: $homestayName",
//                    style = MaterialTheme.typography.bodyLarge,
//                    fontWeight = FontWeight.Medium,
//                    color = Color(0xFF446F5C),
//                    modifier = Modifier.padding(16.dp)
//                )
//            }
//
//            Spacer(Modifier.height(20.dp))
//
//            // --- Price Input ---
//            OutlinedTextField(
//                value = state.price,
//                onValueChange = { viewModel.onPriceChange(it) },
//                label = { Text("Enter Price", color = Color(0xFF446F5C)) },
//                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
//                singleLine = true,
//                placeholder = { Text("e.g. 25.00") },
//                leadingIcon = {
//                    Icon(Icons.Default.AttachMoney, contentDescription = null, tint = Color(0xFF446F5C))
//                },
//                colors = OutlinedTextFieldDefaults.colors(
//                    focusedBorderColor = Color(0xFF446F5C),
//                    unfocusedBorderColor = Color.Gray,
//                    cursorColor = Color(0xFF446F5C)
//                ),
//                modifier = Modifier.fillMaxWidth()
//            )
//
//            // --- Error message ---
//            if (state.errorMessage != null) {
//                Text(
//                    text = state.errorMessage!!,
//                    color = MaterialTheme.colorScheme.error,
//                    fontWeight = FontWeight.Medium,
//                    modifier = Modifier.padding(top = 8.dp)
//                )
//            }
//
//            // --- Save Button ---
//            Button(
//                onClick = {
//                    viewModel.onSavePrice(homeId)
//                    onSaveClick()
//                },
//                colors = ButtonDefaults.buttonColors(
//                    containerColor = Color(0xFF446F5C),
//                    contentColor = Color.White
//                ),
//                shape = RoundedCornerShape(16.dp),
//                elevation = ButtonDefaults.buttonElevation(6.dp),
//                modifier = Modifier
//                    .padding(top = 24.dp)
//                    .align(Alignment.End)
//            ) {
//                Icon(
//                    Icons.Default.Save,
//                    contentDescription = "Save Price",
//                    modifier = Modifier.size(20.dp)
//                )
//                Spacer(Modifier.width(8.dp))
//                Text("Save Price", fontWeight = FontWeight.Bold)
//            }
//        }
//    }
//}
//
//
