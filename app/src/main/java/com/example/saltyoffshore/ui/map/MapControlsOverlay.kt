package com.example.saltyoffshore.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.saltyoffshore.data.DatasetConfiguration
import com.example.saltyoffshore.data.DatasetType
import com.example.saltyoffshore.data.displayVariables
import com.example.saltyoffshore.data.primaryVariable
import com.example.saltyoffshore.ui.components.QuickActionsBar
import com.example.saltyoffshore.ui.components.SaltyDatasetControl
import com.example.saltyoffshore.ui.controls.RightSideToolbar
import com.example.saltyoffshore.ui.measurement.MeasureModeOverlay
import com.example.saltyoffshore.ui.theme.Spacing
import com.example.saltyoffshore.viewmodel.AppViewModel

/**
 * Bottom controls overlay — matches iOS MapControlsOverlay.swift.
 *
 * Extracted into its own composable so it has its own recomposition scope.
 * When sheet state changes in MapScreen, this composable does NOT recompose
 * because sheet state isn't read here.
 *
 * In iOS terms: MapControlsOverlay receives explicit params and only rebuilds
 * when the controls' own data changes (dataset, presets, measurement state).
 */
@Composable
fun MapControlsOverlay(
    viewModel: AppViewModel,
    coordinator: MapSheetCoordinator,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        // Right toolbar (right-aligned, above quick actions + bottom panel)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = Spacing.large),
            horizontalArrangement = Arrangement.End
        ) {
            RightSideToolbar(
                onFilterClick = { coordinator.openSheet(MapSheet.DatasetFilter) },
                onLayersClick = { coordinator.openSheet(MapSheet.Layers) },
                onToolsClick = { coordinator.openSheet(MapSheet.Tools) },
                onMeasureClick = {
                    if (viewModel.measurementState.isActive) {
                        viewModel.measurementState.exit()
                    } else {
                        viewModel.measurementState.enter()
                    }
                }
            )
        }

        Spacer(Modifier.height(Spacing.medium))

        // Measurement mode overlay (replaces dataset control when active)
        if (viewModel.measurementState.isActive) {
            MeasureModeOverlay(
                totalDistanceMeters = viewModel.measurementState.totalDistanceMeters,
                hasMeasurements = viewModel.measurementState.hasMeasurements,
                canUndo = viewModel.measurementState.canUndo,
                distanceUnits = viewModel.currentDistanceUnits,
                onUndo = { viewModel.measurementState.undoLastPoint() },
                onClear = { viewModel.measurementState.clearAll() },
                onDone = { viewModel.measurementState.exit() }
            )
        }

        // Quick actions bar (presets, variables, depth)
        if (!viewModel.measurementState.isActive) {
            val dataset = viewModel.selectedDataset!!
            val datasetType = DatasetType.fromRawValue(dataset.type) ?: DatasetType.SST
            val entry = viewModel.selectedEntry
            val rangeKey = datasetType.rangeKey
            val rangeData = entry?.ranges?.get(rangeKey)
            val valueRange = if (rangeData?.min != null && rangeData.max != null) {
                rangeData.min..rangeData.max
            } else {
                0.0..1.0
            }
            val config = viewModel.primaryConfig

            QuickActionsBar(
                datasetType = datasetType,
                variables = datasetType.displayVariables,
                selectedVariable = config?.selectedVariable(dataset) ?: datasetType.primaryVariable,
                onVariableSelected = { viewModel.selectVariable(it) },
                availableDepths = dataset.availableDepths ?: listOf(0),
                selectedDepth = viewModel.depthFilterState.selectedDepth,
                onDepthSelected = { viewModel.onDepthSelected(it) },
                allPresets = viewModel.allPresets,
                selectedPreset = config?.selectedPreset,
                currentValue = viewModel.primaryValue.value,
                valueRange = valueRange,
                isLoadingPresets = viewModel.isLoadingPresets,
                onPresetSelected = { viewModel.applyPreset(it) }
            )
        }

        // Bottom panel — dataset control
        if (!viewModel.measurementState.isActive) SaltyDatasetControl(
            dataset = viewModel.selectedDataset!!,
            entry = viewModel.selectedEntry,
            snapshot = viewModel.renderingSnapshot,
            primaryValue = viewModel.primaryValue,
            isExpanded = viewModel.isDatasetControlCollapsed,
            primaryConfig = viewModel.primaryConfig,
            onConfigChanged = { viewModel.updatePrimaryConfig(it) },
            onEntrySelected = { viewModel.selectEntry(it) },
            onExpandToggle = {
                viewModel.isDatasetControlCollapsed = !viewModel.isDatasetControlCollapsed
            },
            onChange = { coordinator.openSheet(MapSheet.DatasetSelector) }
        )
    }
}
