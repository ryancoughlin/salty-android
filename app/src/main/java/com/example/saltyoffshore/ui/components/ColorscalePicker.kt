package com.example.saltyoffshore.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.data.Colorscale

/**
 * Colorscale picker organized by category.
 */
@Composable
fun ColorscalePicker(
    selectedColorscale: Colorscale,
    onColorscaleSelected: (Colorscale) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.padding(16.dp)) {
        // Colorful scales (for primary datasets)
        item {
            Text(
                text = "Colored",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        items(Colorscale.colorfulScales) { colorscale ->
            ColorscaleRow(
                colorscale = colorscale,
                isSelected = colorscale.id == selectedColorscale.id,
                onClick = { onColorscaleSelected(colorscale) }
            )
        }

        // Single color scales (for overlays)
        item {
            Text(
                text = "Single Color",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        items(Colorscale.singleColorScales) { colorscale ->
            ColorscaleRow(
                colorscale = colorscale,
                isSelected = colorscale.id == selectedColorscale.id,
                onClick = { onColorscaleSelected(colorscale) }
            )
        }

        // Neutral scales (for overlays)
        item {
            Text(
                text = "Neutral",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        items(Colorscale.neutralScales) { colorscale ->
            ColorscaleRow(
                colorscale = colorscale,
                isSelected = colorscale.id == selectedColorscale.id,
                onClick = { onColorscaleSelected(colorscale) }
            )
        }
    }
}

@Composable
private fun ColorscaleRow(
    colorscale: Colorscale,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Gradient preview
        ColorscaleGradient(
            colorscale = colorscale,
            modifier = Modifier.weight(1f),
            height = 32
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Name
        Text(
            text = colorscale.name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(80.dp)
        )

        // Selection indicator
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
