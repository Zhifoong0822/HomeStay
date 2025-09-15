package com.example.homestay.ui.property

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults

/**
 * Simple horizontal row of removable "photo chips".
 * Uses LazyRow for stability (instead of FlowRow).
 */
@Composable
fun PhotoChips(
    photoUris: List<String>,
    onRemove: (index: Int) -> Unit
) {
    if (photoUris.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(photoUris) { index, _ ->
                OutlinedButton(
                    onClick = { onRemove(index) },
                    border = BorderStroke(1.dp, Color(0xFF446F5C)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF446F5C)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove")
                    Spacer(Modifier.width(6.dp))
                    Text("Photo ${index + 1}")
                }
            }
        }
    }
}
