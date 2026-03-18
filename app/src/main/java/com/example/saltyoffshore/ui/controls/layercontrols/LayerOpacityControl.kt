package com.example.saltyoffshore.ui.controls.layercontrols

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Reusable opacity slider control.
 * Used across all layer types (visual, contours, breaks, arrows).
 * Matches iOS LayerOpacityControl exactly.
 */
@Composable
fun LayerOpacityControl(
    opacity: Double,
    onOpacityChanged: (Double) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Black
        )

        Spacer(modifier = Modifier.width(8.dp))

        Slider(
            value = opacity.toFloat(),
            onValueChange = { onOpacityChanged(it.toDouble()) },
            valueRange = 0f..1f,
            steps = 19, // 5% increments
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = Color.Black,
                activeTrackColor = Color.Black,
                inactiveTrackColor = Color.LightGray
            )
        )

        Text(
            text = "${(opacity * 100).toInt()}%",
            fontSize = 12.sp,
            color = Color.Black,
            modifier = Modifier.width(36.dp)
        )
    }
}
