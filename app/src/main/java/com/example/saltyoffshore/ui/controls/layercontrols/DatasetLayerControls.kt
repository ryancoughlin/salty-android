package com.example.saltyoffshore.ui.controls.layercontrols

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.data.Dataset
import com.example.saltyoffshore.data.DatasetRenderingSnapshot
import com.example.saltyoffshore.data.DatasetType

/**
 * Unified layer controls for primary and overlay datasets.
 * Single source of truth - same toggles available for both.
 * Matches iOS DatasetLayerControls exactly.
 */
@Composable
fun DatasetLayerControls(
    dataset: Dataset,
    snapshot: DatasetRenderingSnapshot,
    onVisualToggle: () -> Unit,
    onVisualOpacity: (Float) -> Unit,
    onContoursToggle: () -> Unit,
    onContoursOpacity: (Float) -> Unit,
    onArrowsToggle: () -> Unit,
    onArrowsOpacity: (Float) -> Unit,
    onBreaksToggle: () -> Unit,
    onBreaksOpacity: (Float) -> Unit,
    onNumbersToggle: () -> Unit,
    onNumbersOpacity: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val datasetType = DatasetType.fromRawValue(dataset.type)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // VISUAL
        if (dataset.hasVisualLayer) {
            VisualLayerControl(
                enabled = snapshot.visualEnabled,
                onEnabledChanged = { onVisualToggle() },
                opacity = snapshot.visualOpacity.toFloat(),
                onOpacityChanged = onVisualOpacity
            )
        }

        // CONTOURS
        if (dataset.hasContours) {
            ContoursLayerControl(
                enabled = snapshot.contourEnabled,
                onEnabledChanged = { onContoursToggle() },
                opacity = snapshot.contourOpacity.toFloat(),
                onOpacityChanged = onContoursOpacity
            )
        }

        // BREAKS
        if (dataset.hasBreaks) {
            BreaksLayerControl(
                enabled = snapshot.breaksEnabled,
                onEnabledChanged = { onBreaksToggle() },
                opacity = snapshot.breaksOpacity.toFloat(),
                onOpacityChanged = onBreaksOpacity
            )
        }

        // ARROWS (currents only)
        if (dataset.hasArrows) {
            ArrowsLayerControl(
                enabled = snapshot.arrowsEnabled,
                onEnabledChanged = { onArrowsToggle() },
                opacity = snapshot.arrowsOpacity.toFloat(),
                onOpacityChanged = onArrowsOpacity
            )
        }

        // NUMBERS
        if (dataset.hasNumbers) {
            NumbersLayerControl(
                enabled = snapshot.numbersEnabled,
                onEnabledChanged = { onNumbersToggle() },
                opacity = snapshot.numbersOpacity.toFloat(),
                onOpacityChanged = onNumbersOpacity
            )
        }
    }
}
