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
    onToggle: (Boolean) -> Unit,
    opacity: Double,
    onOpacityChanged: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    LayerSection(
        title = "Visual",
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
