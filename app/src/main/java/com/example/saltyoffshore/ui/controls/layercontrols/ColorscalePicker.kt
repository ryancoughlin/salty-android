package com.example.saltyoffshore.ui.controls.layercontrols

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.data.Colorscale

/**
 * Colorscale picker showing gradient previews in a flow layout.
 * Matches iOS ColorscalePicker — tap to select, selected gets primary border.
 *
 * iOS ref: Views/DatasetControls/Components/LayerControls/ColorscalePicker.swift
 */
@Composable
fun ColorscalePicker(
    colorscales: List<Colorscale>,
    selected: Colorscale?,
    onSelect: (Colorscale) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Colorscale",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            colorscales.forEach { colorscale ->
                ColorscaleChip(
                    colorscale = colorscale,
                    isSelected = selected?.id == colorscale.id,
                    onClick = { onSelect(colorscale) }
                )
            }
        }
    }
}

@Composable
private fun ColorscaleChip(
    colorscale: Colorscale,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = MaterialTheme.shapes.medium
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.outline
    val borderWidth = if (isSelected) 2.dp else 1.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .width(56.dp)
                .height(20.dp)
                .clip(shape)
                .border(borderWidth, borderColor, shape)
        ) {
            val colors = colorscale.colors
            Canvas(
                modifier = Modifier
                    .width(56.dp)
                    .height(20.dp)
            ) {
                if (colors.isEmpty()) return@Canvas
                val segmentWidth = size.width / colors.size
                colors.forEachIndexed { index, colorInt ->
                    drawRect(
                        color = Color(colorInt),
                        topLeft = Offset(index * segmentWidth, 0f),
                        size = Size(segmentWidth + 1f, size.height)
                    )
                }
            }
        }

        Text(
            text = colorscale.name,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
