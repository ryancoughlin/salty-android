package com.example.saltyoffshore.ui.controls.layercontrols

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.data.DatasetRenderConfig

/**
 * Contours layer control.
 * Opacity + color picker + optional dynamic coloring toggle.
 * Matches iOS ContoursLayerControl exactly.
 */
@Composable
fun ContoursLayerControl(
    config: DatasetRenderConfig,
    onConfigChanged: (DatasetRenderConfig) -> Unit,
    showDynamicColoring: Boolean = false,
    modifier: Modifier = Modifier
) {
    LayerSection(
        title = "Contours",
        enabled = config.contourEnabled,
        onEnabledChanged = { onConfigChanged(config.copy(contourEnabled = it)) },
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LayerOpacityControl(
                opacity = config.contourOpacity.toFloat(),
                onOpacityChanged = { onConfigChanged(config.copy(contourOpacity = it.toDouble())) },
                label = "Opacity"
            )

            // Colors row: label + color circle + dynamic toggle
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Colors",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.width(54.dp)
                )

                // Color picker circle (only when dynamic coloring is off)
                if (!config.dynamicContourColoring) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(config.contourColor))
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            .clickable { /* TODO: Open color picker */ }
                    )
                }

                if (showDynamicColoring) {
                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "Dynamic Colors",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Switch(
                        checked = config.dynamicContourColoring,
                        onCheckedChange = { onConfigChanged(config.copy(dynamicContourColoring = it)) },
                        modifier = Modifier.scale(0.8f),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    )
                }
            }
        }
    }
}
