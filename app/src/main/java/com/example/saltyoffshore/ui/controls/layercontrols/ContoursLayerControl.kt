package com.example.saltyoffshore.ui.controls.layercontrols

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Contours layer control.
 * Opacity slider.
 * Matches iOS ContoursLayerControl.
 */
@Composable
fun ContoursLayerControl(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    opacity: Double,
    onOpacityChanged: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    LayerSection(
        title = "Contours",
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
