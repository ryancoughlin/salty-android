package com.example.saltyoffshore.ui.controls.layercontrols

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Arrows layer control (currents).
 * Opacity slider.
 * Matches iOS ArrowsLayerControl.
 */
@Composable
fun ArrowsLayerControl(
    enabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
    opacity: Float,
    onOpacityChanged: (Float) -> Unit,
    title: String = "Arrows",
    modifier: Modifier = Modifier
) {
    LayerSection(
        title = title,
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
