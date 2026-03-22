package com.example.saltyoffshore.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.data.CurrentValue
import com.example.saltyoffshore.data.Dataset
import com.example.saltyoffshore.data.DatasetConfiguration
import com.example.saltyoffshore.data.DatasetRenderingSnapshot
import com.example.saltyoffshore.data.DatasetType
import com.example.saltyoffshore.data.TimeEntry
import com.example.saltyoffshore.ui.theme.SaltyLayout
import com.example.saltyoffshore.ui.theme.SaltyType
import com.example.saltyoffshore.ui.theme.Spacing

/**
 * Bottom dataset control panel matching iOS MapControlsContainer bottom controls.
 * Uses Material 3 Surface with tonal elevation for native dark-mode appearance.
 *
 * Layout:
 * - Row 1: Dataset name + Change/Collapse buttons
 * - Row 2: GradientScaleBar (color ramp, min/max labels, current value pointer)
 * - Row 3: TimelineControl (scrubber with time display)
 *
 * iOS ref: Map/Controls/MapControlsContainer.swift → bottomControls()
 */
@Composable
fun SaltyDatasetControl(
    dataset: Dataset,
    entry: TimeEntry?,
    snapshot: DatasetRenderingSnapshot,
    currentValue: CurrentValue = CurrentValue.None,
    onEntrySelected: (TimeEntry) -> Unit,
    onExpand: () -> Unit = {},
    onChange: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val datasetType = DatasetType.fromRawValue(dataset.type)
    val config = datasetType?.let { DatasetConfiguration.forDatasetType(it) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.large),
        shape = RoundedCornerShape(SaltyLayout.cardCornerRadius),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp,
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: Dataset name + buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dataset.name,
                    style = SaltyType.body,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                ) {
                    TextButton(onClick = onChange) {
                        Icon(
                            Icons.Default.SwapVert,
                            contentDescription = "Change",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(Spacing.small))
                        Text(
                            "Change",
                            style = SaltyType.caption,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onExpand) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "Collapse",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Row 2: Gradient scale bar with current value pointer
            entry?.let { currentEntry ->
                val rangeKey = datasetType?.rangeKey ?: dataset.type
                val range = currentEntry.ranges?.get(rangeKey)
                val colorscale = snapshot.selectedColorscale
                    ?: datasetType?.defaultColorscale

                if (colorscale != null) {
                    // Extract numeric value from crosshair reading
                    val pointerValue = (currentValue as? CurrentValue.Value)?.value

                    // Filter range from snapshot
                    val filterRange = if (snapshot.isFilterActive) {
                        snapshot.filterMin..snapshot.filterMax
                    } else null

                    val dataMin = range?.min ?: snapshot.dataMin
                    val dataMax = range?.max ?: snapshot.dataMax

                    GradientScaleBar(
                        min = dataMin,
                        max = dataMax,
                        colorscale = colorscale,
                        currentValue = pointerValue,
                        filterRange = filterRange,
                        fullRange = dataMin..dataMax,
                        apiUnit = config?.unit ?: com.example.saltyoffshore.data.DatasetUnit.FAHRENHEIT,
                        datasetType = datasetType,
                        decimalPlaces = config?.decimalPlaces ?: 1
                    )
                }
            }

            // Row 3: Timeline
            TimelineControl(
                entries = dataset.entries ?: emptyList(),
                selectedEntry = entry,
                onEntrySelected = onEntrySelected
            )
        }
    }
}
