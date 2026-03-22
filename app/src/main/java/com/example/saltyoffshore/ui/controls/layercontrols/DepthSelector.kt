package com.example.saltyoffshore.ui.controls.layercontrols

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Depth selector for multi-depth datasets.
 * Uses SegmentedButtonRow for <=5 depths, FlowRow of FilterChips for more.
 *
 * Matches iOS DepthSelector behavior.
 *
 * iOS ref: Views/DatasetControls/Components/DepthSelector.swift
 */
@Composable
fun DepthSelector(
    depths: List<Int>,
    selectedDepth: Int,
    onDepthSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (depths.size <= 1) return

    Column(modifier = modifier) {
        Text(
            text = "Depth",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (depths.size <= 5) {
            SingleChoiceSegmentedButtonRow {
                depths.forEachIndexed { index, depth ->
                    SegmentedButton(
                        selected = depth == selectedDepth,
                        onClick = { onDepthSelected(depth) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = depths.size
                        )
                    ) {
                        Text(depthLabel(depth))
                    }
                }
            }
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                depths.forEach { depth ->
                    FilterChip(
                        selected = depth == selectedDepth,
                        onClick = { onDepthSelected(depth) },
                        label = { Text(depthLabel(depth)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }
    }
}

private fun depthLabel(depth: Int): String {
    return if (depth == 0) "Surface" else "${depth}m"
}
