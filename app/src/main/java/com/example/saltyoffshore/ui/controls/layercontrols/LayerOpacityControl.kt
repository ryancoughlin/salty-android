package com.example.saltyoffshore.ui.controls.layercontrols

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.ui.theme.SaltyColors

/**
 * Reusable opacity slider control.
 * Used across all layer types (visual, contours, breaks, arrows).
 * Matches iOS LayerOpacityControl exactly.
 *
 * Layout: label (caption, 54dp) + Slider (0.0-1.0, step 0.05) + percentage text (caption, 36dp, trailing).
 */
@Composable
fun LayerOpacityControl(
    opacity: Float,
    onOpacityChanged: (Float) -> Unit,
    label: String = "Opacity",
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = SaltyColors.textPrimary,
            modifier = Modifier.width(54.dp)
        )

        Slider(
            value = opacity,
            onValueChange = onOpacityChanged,
            valueRange = 0f..1f,
            steps = 19, // 0.05 increments
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.outline
            )
        )

        Text(
            text = "${(opacity * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = SaltyColors.textPrimary,
            textAlign = TextAlign.End,
            modifier = Modifier.width(36.dp)
        )
    }
}
