package com.example.saltyoffshore.ui.controls.layercontrols

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Visual layer control.
 * Opacity + colorscale picker.
 * Matches iOS VisualLayerControl.
 */
@Composable
fun VisualLayerControl(
    enabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
    opacity: Float,
    onOpacityChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    LayerSection(
        title = "Visual",
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
