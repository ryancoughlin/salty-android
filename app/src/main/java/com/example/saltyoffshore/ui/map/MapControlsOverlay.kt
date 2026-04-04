package com.example.saltyoffshore.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
 */
@Composable
fun MapControlsOverlay(
    viewModel: AppViewModel,
    coordinator: MapSheetCoordinator,
    modifier: Modifier = Modifier,
) {
    val datasetState by viewModel.datasetStore.state.collectAsState()
    val prefsState by viewModel.userPreferencesStore.state.collectAsState()

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
                distanceUnits = prefsState.currentDistanceUnits,
                onUndo = { viewModel.measurementState.undoLastPoint() },
                onClear = { viewModel.measurementState.clearAll() },
                onDone = { viewModel.measurementState.exit() }
            )
        }

        // Quick actions bar (presets, variables, depth)
        if (!viewModel.measurementState.isActive) {
            val dataset = datasetState.selectedDataset!!
            val datasetType = DatasetType.fromRawValue(dataset.type) ?: DatasetType.SST
            val entry = datasetState.selectedEntry
            val rangeKey = datasetType.rangeKey
            val rangeData = entry?.ranges?.get(rangeKey)
            val valueRange = if (rangeData?.min != null && rangeData.max != null) {
                rangeData.min..rangeData.max
            } else {
                0.0..1.0
            }
            val config = datasetState.primaryConfig

            QuickActionsBar(
                datasetType = datasetType,
                variables = datasetType.displayVariables,
                selectedVariable = config?.selectedVariable(dataset) ?: datasetType.primaryVariable,
                onVariableSelected = { viewModel.datasetStore.selectVariable(it) },
                availableDepths = dataset.availableDepths ?: listOf(0),
                selectedDepth = datasetState.depthFilterState.selectedDepth,
                onDepthSelected = { viewModel.datasetStore.onDepthSelected(it) },
                allPresets = datasetState.allPresets,
                selectedPreset = config?.selectedPreset,
                currentValue = datasetState.primaryValue.value,
                valueRange = valueRange,
                isLoadingPresets = datasetState.isLoadingPresets,
                onPresetSelected = { viewModel.datasetStore.applyPreset(it) }
            )
        }

        // Bottom panel — dataset control
        if (!viewModel.measurementState.isActive) SaltyDatasetControl(
            dataset = datasetState.selectedDataset!!,
            entry = datasetState.selectedEntry,
            snapshot = datasetState.renderingSnapshot,
            primaryValue = datasetState.primaryValue,
            isExpanded = datasetState.isDatasetControlCollapsed,
            primaryConfig = datasetState.primaryConfig,
            onConfigChanged = { viewModel.datasetStore.updatePrimaryConfig(it) },
            onEntrySelected = { viewModel.datasetStore.selectEntry(it) },
            onExpandToggle = {
                viewModel.datasetStore.toggleDatasetControl()
            },
            onChange = { coordinator.openSheet(MapSheet.DatasetSelector) }
        )
    }
}
