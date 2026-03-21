package com.example.saltyoffshore.ui.controls.layercontrols

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Numbers layer control - displays dataset values as text on map.
 * Opacity slider.
 * Matches iOS NumbersLayerControl.
 */
@Composable
fun NumbersLayerControl(
    enabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
    opacity: Float,
    onOpacityChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    LayerSection(
        title = "Numbers",
        enabled = enabled,
        onEnabledChanged = onEnabledChanged,
        modifier = modifier
    ) {
        LayerOpacityControl(
            opacity = opacity,
            onOpacityChanged = onOpacityChanged
        )
    }
}
