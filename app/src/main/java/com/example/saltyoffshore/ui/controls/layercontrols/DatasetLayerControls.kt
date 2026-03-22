package com.example.saltyoffshore.ui.controls.layercontrols

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.data.Dataset
import com.example.saltyoffshore.data.DatasetRenderConfig
import com.example.saltyoffshore.data.DatasetType
import com.example.saltyoffshore.data.EntryOverride
import com.example.saltyoffshore.data.availableVariables

/**
 * Unified layer controls for primary and overlay datasets.
 * Renders depth selector, variable selector, then layer toggles.
 * Matches iOS DatasetLayerControls exactly.
 *
 * Order: DepthSelector → VariableSelector → Particles → Visual → Contours → Breaks → Arrows → Numbers
 *
 * iOS ref: Views/DatasetControls/Components/LayerControls/DatasetLayerControls.swift
 */
@Composable
fun DatasetLayerControls(
    dataset: Dataset,
    config: DatasetRenderConfig,
    onConfigChanged: (DatasetRenderConfig) -> Unit,
    isPrimary: Boolean = true,
    modifier: Modifier = Modifier
) {
    val datasetType = DatasetType.fromRawValue(dataset.type)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // DEPTH SELECTOR (for multi-depth datasets)
        if (dataset.hasMultipleDepths) {
            val depths = dataset.availableDepths ?: listOf(0)
            val selectedDepth = config.entryOverride?.depth ?: 0
            DepthSelector(
                depths = depths,
                selectedDepth = selectedDepth,
                onDepthSelected = { depth ->
                    onConfigChanged(
                        config.copy(
                            entryOverride = (config.entryOverride ?: EntryOverride()).copy(depth = depth)
                        )
                    )
                }
            )
        }

        // VARIABLE SELECTOR (for datasets with multiple variables)
        if (datasetType != null) {
            val variables = datasetType.availableVariables
            VariableSelector(
                variables = variables,
                selectedVariableId = config.selectedVariableId,
                onVariableSelected = { variable ->
                    onConfigChanged(config.copy(selectedVariableId = variable.id))
                }
            )
        }

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
