package com.example.saltyoffshore.ui.controls.layercontrols

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.data.Colorscale
import com.example.saltyoffshore.data.DatasetRenderConfig

/**
 * Visual layer control.
 * Opacity slider + colorscale picker.
 * Matches iOS VisualLayerControl.
 *
 * iOS ref: Views/DatasetControls/Components/LayerControls/VisualLayerControl.swift
 */
@Composable
fun VisualLayerControl(
    config: DatasetRenderConfig,
    onConfigChanged: (DatasetRenderConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    LayerSection(
        title = "Visual",
        enabled = config.visualEnabled,
        onEnabledChanged = { onConfigChanged(config.copy(visualEnabled = it)) },
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LayerOpacityControl(
                opacity = config.visualOpacity.toFloat(),
                onOpacityChanged = { onConfigChanged(config.copy(visualOpacity = it.toDouble())) },
                label = "Opacity"
            )

            ColorscalePicker(
                colorscales = Colorscale.ALL,
                selected = config.colorscale,
                onSelect = { onConfigChanged(config.copy(colorscale = it)) }
            )
        }
    }
}
