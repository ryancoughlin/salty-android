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
        onEnabledChanged = { onConfigChanged(config.copy(numbersEnabled = it)) },
        modifier = modifier
    ) {
        LayerOpacityControl(
            opacity = config.numbersOpacity.toFloat(),
            onOpacityChanged = { onConfigChanged(config.copy(numbersOpacity = it.toDouble())) },
            label = "Opacity"
        )
    }
}
