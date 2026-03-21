package com.example.saltyoffshore.ui.controls.layercontrols

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.saltyoffshore.data.DatasetRenderConfig

/**
 * Visual layer control.
 * Opacity slider.
 * Matches iOS VisualLayerControl.
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
        onToggle = { onConfigChanged(config.copy(visualEnabled = it)) },
        modifier = modifier
    ) {
        LayerOpacityControl(
            opacity = config.visualOpacity,
            onOpacityChanged = { onConfigChanged(config.copy(visualOpacity = it)) },
            label = "Opacity"
        )
    }
}
