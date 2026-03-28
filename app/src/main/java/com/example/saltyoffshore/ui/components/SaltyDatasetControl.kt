package com.example.saltyoffshore.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import com.example.saltyoffshore.ui.theme.SaltyMotion
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.data.CurrentValue
import com.example.saltyoffshore.data.Dataset
import com.example.saltyoffshore.data.DatasetConfiguration
import com.example.saltyoffshore.data.DatasetRenderConfig
import com.example.saltyoffshore.data.DatasetRenderingSnapshot
import com.example.saltyoffshore.data.DatasetType
import com.example.saltyoffshore.data.TimeEntry
import com.example.saltyoffshore.ui.controls.layercontrols.DatasetLayerControls
import com.example.saltyoffshore.ui.components.entrygallery.EntryGalleryView

import com.example.saltyoffshore.ui.theme.SaltyType
import com.example.saltyoffshore.ui.theme.Spacing

/**
 * Bottom dataset control panel matching iOS DatasetControl.
 * Supports collapsed (compact) and expanded (full layer controls) modes.
 *
 * Collapsed: Dataset name + gradient bar + timeline
 * Expanded: + DatasetLayerControls inline
 *
 * iOS ref: Views/DatasetControls/DatasetControl.swift
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaltyDatasetControl(
    dataset: Dataset,
    entry: TimeEntry?,
    snapshot: DatasetRenderingSnapshot,
    primaryValue: CurrentValue = CurrentValue(),
    isExpanded: Boolean = false,
    selectedDepth: Int = 0,
    primaryConfig: DatasetRenderConfig? = null,
    onConfigChanged: ((DatasetRenderConfig) -> Unit)? = null,
    onEntrySelected: (TimeEntry) -> Unit,
    onExpandToggle: () -> Unit = {},
    onChange: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val datasetType = DatasetType.fromRawValue(dataset.type)
    val config = datasetType?.let { DatasetConfiguration.forDatasetType(it) }
    var showEntryGallery by remember { mutableStateOf(false) }

    // Entry Gallery sheet
    if (showEntryGallery) {
        ModalBottomSheet(
            onDismissRequest = { showEntryGallery = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            EntryGalleryView(
                dataset = dataset,
                selectedDepth = selectedDepth,
                currentEntryId = entry?.id,
                onEntrySelected = { selectedEntry ->
                    onEntrySelected(selectedEntry)
                    showEntryGallery = false
                },
                onDismiss = { showEntryGallery = false }
            )
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.large),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp,
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium)
                .animateContentSize(
                    animationSpec = SaltyMotion.springMedium()
                ),
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
                            contentDescription = "Change dataset",
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
                    IconButton(onClick = onExpandToggle) {
                        Icon(
                            if (isExpanded) Icons.Default.KeyboardArrowDown
                            else Icons.Default.KeyboardArrowUp,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Row 2: Gradient scale bar
            entry?.let { currentEntry ->
                val rangeKey = datasetType?.rangeKey ?: dataset.type
                val range = currentEntry.ranges?.get(rangeKey)
                val colorscale = snapshot.selectedColorscale
                    ?: datasetType?.defaultColorscale

                if (colorscale != null) {
                    val pointerValue = primaryValue.value

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

            // Row 3: Gallery button + Timeline (matches iOS PrimaryDatasetPage timelineRow)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { showEntryGallery = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = "Entry gallery",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    TimelineControl(
                        entries = dataset.entries ?: emptyList(),
                        selectedEntry = entry,
                        onEntrySelected = onEntrySelected
                    )
                }
            }

            // Expanded: Layer controls inline (matches iOS ExpandedDatasetView)
            AnimatedVisibility(
                visible = isExpanded && primaryConfig != null && onConfigChanged != null,
                enter = expandVertically(
                    animationSpec = SaltyMotion.springMedium()
                ) + fadeIn(),
                exit = shrinkVertically(
                    animationSpec = SaltyMotion.springMedium()
                ) + fadeOut()
            ) {
                if (primaryConfig != null && onConfigChanged != null) {
                    Column(
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        DatasetLayerControls(
                            dataset = dataset,
                            config = primaryConfig,
                            onConfigChanged = onConfigChanged,
                            isPrimary = true
                        )
                    }
                }
            }
        }
    }
}
