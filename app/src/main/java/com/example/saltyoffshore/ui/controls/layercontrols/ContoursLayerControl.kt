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
    onEnabledChanged: (Boolean) -> Unit,
    opacity: Float,
    onOpacityChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    LayerSection(
        title = "Contours",
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
