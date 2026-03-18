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
    onVisualOpacity: (Double) -> Unit,
    onContoursToggle: () -> Unit,
    onContoursOpacity: (Double) -> Unit,
    onArrowsToggle: () -> Unit,
    onArrowsOpacity: (Double) -> Unit,
    onBreaksToggle: () -> Unit,
    onBreaksOpacity: (Double) -> Unit,
    onNumbersToggle: () -> Unit,
    onNumbersOpacity: (Double) -> Unit,
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
                onToggle = { onVisualToggle() },
                opacity = snapshot.visualOpacity,
                onOpacityChanged = onVisualOpacity
            )
        }

        // CONTOURS
        if (dataset.hasContours) {
            ContoursLayerControl(
                enabled = snapshot.contourEnabled,
                onToggle = { onContoursToggle() },
                opacity = snapshot.contourOpacity,
                onOpacityChanged = onContoursOpacity
            )
        }

        // BREAKS
        if (dataset.hasBreaks) {
            BreaksLayerControl(
                enabled = snapshot.breaksEnabled,
                onToggle = { onBreaksToggle() },
                opacity = snapshot.breaksOpacity,
                onOpacityChanged = onBreaksOpacity
            )
        }

        // ARROWS (currents only)
        if (dataset.hasArrows) {
            ArrowsLayerControl(
                enabled = snapshot.arrowsEnabled,
                onToggle = { onArrowsToggle() },
                opacity = snapshot.arrowsOpacity,
                onOpacityChanged = onArrowsOpacity
            )
        }

        // NUMBERS
        if (dataset.hasNumbers) {
            NumbersLayerControl(
                enabled = snapshot.numbersEnabled,
                onToggle = { onNumbersToggle() },
                opacity = snapshot.numbersOpacity,
                onOpacityChanged = onNumbersOpacity
            )
        }
    }
}
