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
    onEnabledChanged: (Boolean) -> Unit,
    opacity: Float,
    onOpacityChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    LayerSection(
        title = "Breaks",
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
