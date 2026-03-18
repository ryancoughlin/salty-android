package com.example.saltyoffshore.ui.controls.layercontrols

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Breaks layer control (thermal fronts).
 * Opacity slider.
 * Matches iOS BreaksLayerControl.
 */
@Composable
fun BreaksLayerControl(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    opacity: Double,
    onOpacityChanged: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    LayerSection(
        title = "Breaks",
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
