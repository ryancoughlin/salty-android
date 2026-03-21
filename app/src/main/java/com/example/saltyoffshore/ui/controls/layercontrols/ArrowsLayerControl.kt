package com.example.saltyoffshore.ui.controls.layercontrols

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.saltyoffshore.data.DatasetRenderConfig

/**
 * Arrows layer control (currents).
 * Opacity slider.
 * Matches iOS ArrowsLayerControl.
 */
@Composable
fun ArrowsLayerControl(
    config: DatasetRenderConfig,
    onConfigChanged: (DatasetRenderConfig) -> Unit,
    title: String = "Arrows",
    modifier: Modifier = Modifier
) {
    LayerSection(
        title = title,
        enabled = config.arrowsEnabled,
        onToggle = { onConfigChanged(config.copy(arrowsEnabled = it)) },
        modifier = modifier
    ) {
        LayerOpacityControl(
            opacity = config.arrowsOpacity,
            onOpacityChanged = { onConfigChanged(config.copy(arrowsOpacity = it)) },
            label = "Opacity"
        )
    }
}
