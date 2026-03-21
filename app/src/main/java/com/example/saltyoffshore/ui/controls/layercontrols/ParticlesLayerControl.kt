package com.example.saltyoffshore.ui.controls.layercontrols

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.saltyoffshore.data.DatasetRenderConfig

/**
 * Animated particles layer control (currents only).
 * Toggle only, no opacity.
 * Matches iOS ParticlesLayerControl.
 */
@Composable
fun ParticlesLayerControl(
    config: DatasetRenderConfig,
    onConfigChanged: (DatasetRenderConfig) -> Unit,
    title: String = "Particles",
    modifier: Modifier = Modifier
) {
    LayerSection(
        title = title,
        enabled = config.particlesEnabled,
        onEnabledChanged = { onConfigChanged(config.copy(particlesEnabled = it)) },
        modifier = modifier
    ) {
        // No content — toggle only
    }
}
