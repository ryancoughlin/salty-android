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
    onToggle: (Boolean) -> Unit,
    opacity: Double,
    onOpacityChanged: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    LayerSection(
        title = "Numbers",
        enabled = enabled,
        onToggle = onToggle,
        modifier = modifier
    ) {
        LayerOpacityControl(
            opacity = opacity,
            onOpacityChanged = onOpacityChanged,
            label = "Opacity"
        )
    }
}
