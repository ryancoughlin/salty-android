package com.example.saltyoffshore.ui.controls.layercontrols

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.saltyoffshore.data.DatasetRenderConfig

/**
 * Numbers layer control - displays dataset values as text on map.
 * Opacity slider.
 * Matches iOS NumbersLayerControl.
 */
@Composable
fun NumbersLayerControl(
    config: DatasetRenderConfig,
    onConfigChanged: (DatasetRenderConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    LayerSection(
        title = "Numbers",
        enabled = config.numbersEnabled,
        onToggle = { onConfigChanged(config.copy(numbersEnabled = it)) },
        modifier = modifier
    ) {
        LayerOpacityControl(
            opacity = config.numbersOpacity,
            onOpacityChanged = { onConfigChanged(config.copy(numbersOpacity = it)) },
            label = "Opacity"
        )
    }
}
