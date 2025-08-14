import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowCircleLeft
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.homestay.ui.addPrice.AddPriceViewModel


@Composable
fun AddPriceScreen(
    homestayName: String, // pass this in from the calling screen
    viewModel: AddPriceViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Row() {
            //back button
            OutlinedButton(
                onClick = { /* Navigate back */ },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF4CAF50) // Green text & icon
                ),
                shape = RoundedCornerShape(50), // Pill shape
                border = BorderStroke(2.dp, Color(0xFF4CAF50)),
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Icon(
                    Icons.Default.ArrowCircleLeft,
                    contentDescription = "Go Back",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Back", fontWeight = FontWeight.Bold)
            }


            Spacer(modifier = Modifier.weight(1f))

            //Profile button
            Button(
                onClick = { /* Open profile */ },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3), // Blue
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(4.dp),
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Profile",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Profile", fontWeight = FontWeight.Medium)
            }
        }
            // Title
            Text(
                text = "Add Price",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp) // Inner padding
            )

            // Homestay Name
            Text(
                text = "Homestay: $homestayName",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .border(2.dp, Color.Gray, shape = RoundedCornerShape(8.dp)) // Border
                    .background(Color(0xFFEFEFEF), shape = RoundedCornerShape(8.dp)) // Background
                    .padding(horizontal = 16.dp, vertical = 8.dp) // Inner padding

            )

            // Price input
            OutlinedTextField(
                value = state.price,
                onValueChange = { viewModel.onPriceChange(it) },
                label = { Text("Enter Price") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                placeholder = { Text("e.g. 25.00") },
                leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )

            // Error message
            if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Button(
                onClick = { viewModel.onSavePrice() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50), // Green
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(8.dp),
                modifier = Modifier
                    .padding(top = 16.dp)
                    .align(Alignment.End)
            ) {
                Icon(
                    Icons.Default.Save,
                    contentDescription = "Save Price",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Save Price", fontWeight = FontWeight.Bold)
            }
        }
    }
