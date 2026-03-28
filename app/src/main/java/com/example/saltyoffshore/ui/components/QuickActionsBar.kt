package com.example.saltyoffshore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.data.DatasetPreset
import com.example.saltyoffshore.data.DatasetType
import com.example.saltyoffshore.data.DatasetVariable
import com.example.saltyoffshore.data.PresetConfiguration
import com.example.saltyoffshore.data.displayVariables
import com.example.saltyoffshore.data.hasMultipleVariables
import com.example.saltyoffshore.ui.theme.Spacing

/**
 * Horizontal scrollable bar for quick-access chips: variables, depth, presets.
 * Sits above the bottom dataset control panel.
 *
 * Sections separated by vertical dividers. Order: variables → depth → presets.
 *
 * iOS ref: Map/Controls/QuickActionsBar.swift
 */
@Composable
fun QuickActionsBar(
    datasetType: DatasetType,
    // Variable state
    variables: List<DatasetVariable>,
    selectedVariable: DatasetVariable,
    onVariableSelected: (DatasetVariable) -> Unit,
    // Depth state
    availableDepths: List<Int>,
    selectedDepth: Int,
    onDepthSelected: (Int) -> Unit,
    // Preset state
    allPresets: List<DatasetPreset>,
    selectedPreset: DatasetPreset?,
    currentValue: Double?,
    valueRange: ClosedFloatingPointRange<Double>,
    isLoadingPresets: Boolean,
    onPresetSelected: (DatasetPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    val showVariables = datasetType.hasMultipleVariables
    val showDepth = availableDepths.size > 1
    val showPresets = PresetConfiguration.supportsPresets(datasetType) &&
            (allPresets.isNotEmpty() || isLoadingPresets)

    // Don't render if nothing to show
    if (!showVariables && !showDepth && !showPresets) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = Spacing.small)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.small)
    ) {
        // Variables section
        if (showVariables) {
            VariableQuickActions(
                variables = variables,
                selectedVariable = selectedVariable,
                onVariableSelected = onVariableSelected
            )

            if (showDepth || showPresets) {
                QuickActionsDivider()
            }
        }

        // Depth section
        if (showDepth) {
            DepthQuickAction(
                availableDepths = availableDepths,
                selectedDepth = selectedDepth,
                onDepthSelected = onDepthSelected
            )

            if (showPresets) {
                QuickActionsDivider()
            }
        }

        // Presets section
        if (showPresets) {
            PresetQuickActions(
                datasetType = datasetType,
                allPresets = allPresets,
                selectedPreset = selectedPreset,
                currentValue = currentValue,
                valueRange = valueRange,
                isLoadingPresets = isLoadingPresets,
                onPresetSelected = onPresetSelected
            )
        }
    }
}

/** Vertical divider between quick action sections */
@Composable
private fun QuickActionsDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(24.dp)
            .clip(RoundedCornerShape(1.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
    )
}
