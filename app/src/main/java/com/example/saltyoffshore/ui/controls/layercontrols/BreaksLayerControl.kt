package com.example.saltyoffshore.ui.controls.layercontrols

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.saltyoffshore.data.DatasetRenderConfig

/**
 * Breaks layer control (thermal fronts).
 * Opacity slider.
 * Matches iOS BreaksLayerControl.
 */
@Composable
fun BreaksLayerControl(
    config: DatasetRenderConfig,
    onConfigChanged: (DatasetRenderConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    LayerSection(
        title = "Breaks",
        enabled = config.breaksEnabled,
        onToggle = { onConfigChanged(config.copy(breaksEnabled = it)) },
        modifier = modifier
    ) {
        LayerOpacityControl(
            opacity = config.breaksOpacity,
            onOpacityChanged = { onConfigChanged(config.copy(breaksOpacity = it)) },
            label = "Opacity"
        )
    }
}
