package com.example.saltyoffshore.ui.controls.layercontrols

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.data.Dataset
import com.example.saltyoffshore.data.DatasetRenderConfig

/**
 * Unified layer controls for primary and overlay datasets.
 * Single source of truth - same toggles available for both.
 * Matches iOS DatasetLayerControls exactly.
 */
@Composable
fun DatasetLayerControls(
    dataset: Dataset,
    config: DatasetRenderConfig,
    onConfigChanged: (DatasetRenderConfig) -> Unit,
    isPrimary: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // PARTICLES (matches iOS order — particles first)
        if (dataset.hasParticles) {
            ParticlesLayerControl(
                config = config,
                onConfigChanged = onConfigChanged
            )
        }

        // VISUAL
        if (dataset.hasVisualLayer) {
            VisualLayerControl(
                config = config,
                onConfigChanged = onConfigChanged
            )
        }

        // CONTOURS
        if (dataset.hasContours) {
            ContoursLayerControl(
                config = config,
                onConfigChanged = onConfigChanged,
                showDynamicColoring = isPrimary
            )
        }

        // BREAKS
        if (dataset.hasBreaks) {
            BreaksLayerControl(
                config = config,
                onConfigChanged = onConfigChanged
            )
        }

        // ARROWS
        if (dataset.hasArrows) {
            ArrowsLayerControl(
                config = config,
                onConfigChanged = onConfigChanged
            )
        }

        // NUMBERS (bottom)
        if (dataset.hasNumbers) {
            NumbersLayerControl(
                config = config,
                onConfigChanged = onConfigChanged
            )
        }
    }
}
