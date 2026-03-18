package com.example.saltyoffshore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saltyoffshore.data.Colorscale
import com.example.saltyoffshore.data.DatasetType

/**
 * Matches iOS GradientScaleBar.swift exactly:
 * - 12pt monospaced fonts for min/max labels
 * - 6dp height gradient bar with 4dp corner radius
 * - Optional current value pointer (16x8dp rounded rect)
 */
@Composable
fun GradientScaleBar(
    min: Double,
    max: Double,
    unit: String,
    colors: List<Color>,
    currentValue: Double? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Min label
        Text(
            text = "${min.toInt()}$unit",
            style = TextStyle(
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = Color.White
            )
        )

        // Gradient bar with optional pointer
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .height(14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            val density = LocalDensity.current
            val widthPx = constraints.maxWidth.toFloat()

            // The gradient bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(
                        Brush.horizontalGradient(colors),
                        RoundedCornerShape(4.dp)
                    )
            )

            // Current value pointer
            if (currentValue != null && max > min) {
                val ratio = ((currentValue - min) / (max - min)).coerceIn(0.0, 1.0)
                val positionPx = ratio * widthPx
                val positionDp = with(density) { positionPx.toFloat().toDp() }
                val pointerColor = colorAt(currentValue, min, max, colors)

                Box(
                    modifier = Modifier
                        .offset(x = positionDp - 8.dp)
                        .size(width = 16.dp, height = 8.dp)
                        .shadow(1.dp, RoundedCornerShape(4.dp))
                        .background(pointerColor, RoundedCornerShape(4.dp))
                        .border(1.dp, Color.White, RoundedCornerShape(4.dp))
                )
            }
        }

        // Max label
        Text(
            text = "${max.toInt()}$unit",
            style = TextStyle(
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = Color.White
            )
        )
    }
}

// Get color at specific value in gradient
private fun colorAt(value: Double, min: Double, max: Double, colors: List<Color>): Color {
    if (colors.isEmpty()) return Color.Gray
    if (colors.size == 1) return colors[0]

    val ratio = ((value - min) / (max - min)).coerceIn(0.0, 1.0)
    val position = ratio * (colors.size - 1)
    val index = position.toInt().coerceIn(0, colors.size - 2)
    val fraction = (position - index).toFloat()

    return lerp(colors[index], colors[index + 1], fraction)
}

private fun lerp(start: Color, end: Color, fraction: Float): Color {
    return Color(
        red = start.red + (end.red - start.red) * fraction,
        green = start.green + (end.green - start.green) * fraction,
        blue = start.blue + (end.blue - start.blue) * fraction,
        alpha = start.alpha + (end.alpha - start.alpha) * fraction
    )
}

/**
 * Overload that takes a Colorscale object directly.
 */
@Composable
fun GradientScaleBar(
    min: Double,
    max: Double,
    unit: String,
    colorscale: Colorscale,
    currentValue: Double? = null,
    modifier: Modifier = Modifier
) {
    val colors = colorscale.hexColors.map { hex ->
        Color(android.graphics.Color.parseColor(hex))
    }
    GradientScaleBar(
        min = min,
        max = max,
        unit = unit,
        colors = colors,
        currentValue = currentValue,
        modifier = modifier
    )
}

/**
 * Get color scale for dataset type using Colorscale system.
 */
fun getColorScale(datasetType: String): List<Color> {
    val type = DatasetType.fromRawValue(datasetType)
    val colorscale = type?.defaultColorscale ?: Colorscale.VIRIDIS
    return colorscale.hexColors.map { hex ->
        Color(android.graphics.Color.parseColor(hex))
    }
}

/**
 * Get Colorscale object for dataset type.
 */
fun getColorscale(datasetType: DatasetType): Colorscale {
    return datasetType.defaultColorscale
}
