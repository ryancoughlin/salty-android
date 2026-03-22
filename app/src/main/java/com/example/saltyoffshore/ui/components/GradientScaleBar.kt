package com.example.saltyoffshore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.data.Colorscale
import com.example.saltyoffshore.data.DatasetType
import com.example.saltyoffshore.data.DatasetUnit
import com.example.saltyoffshore.data.TemperatureUnits
import androidx.compose.material3.MaterialTheme
import com.example.saltyoffshore.ui.theme.SaltyType
import com.example.saltyoffshore.ui.theme.Spacing
import com.example.saltyoffshore.utils.GradientScaleUtil

/**
 * Matches iOS GradientScaleBar.swift exactly:
 * - 12pt monospaced fonts for min/max labels
 * - 6dp height gradient bar with 2dp corner radius
 * - Optional current value pointer (16x8dp rounded rect)
 * - Filter awareness: checkerboard behind with gradient clipped to filtered portion
 * - Unit conversion via DatasetUnit + TemperatureUnits
 * - Scale-aware position calculation via GradientScaleUtil
 */
@Composable
fun GradientScaleBar(
    min: Double,
    max: Double,
    colors: List<Color>,
    currentValue: Double? = null,
    filterRange: ClosedFloatingPointRange<Double>? = null,
    fullRange: ClosedFloatingPointRange<Double> = min..max,
    apiUnit: DatasetUnit = DatasetUnit.FAHRENHEIT,
    temperatureUnits: TemperatureUnits = TemperatureUnits.FAHRENHEIT,
    datasetType: DatasetType? = null,
    decimalPlaces: Int = 0,
    modifier: Modifier = Modifier
) {
    val labelColor = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
    ) {
        // Min label
        val displayMin = apiUnit.convertForDisplay(min, temperatureUnits)
        Text(
            text = GradientScaleUtil.formatValue(displayMin, decimalPlaces) +
                    apiUnit.displayUnitSuffix(temperatureUnits),
            style = SaltyType.mono(12).copy(color = labelColor)
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

            val isFiltered = filterRange != null &&
                    (filterRange.start != fullRange.start || filterRange.endInclusive != fullRange.endInclusive)

            if (isFiltered) {
                // Checkerboard behind the full bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(2.dp))
                ) {
                    CheckerboardPattern(
                        size = CheckerboardSize.SMALL,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Gradient clipped to filtered portion
                val filterStart = filterRange!!.start
                val filterEnd = filterRange.endInclusive
                val startFraction = GradientScaleUtil.calculatePosition(
                    filterStart, fullRange, datasetType
                ).toFloat().coerceIn(0f, 1f)
                val endFraction = GradientScaleUtil.calculatePosition(
                    filterEnd, fullRange, datasetType
                ).toFloat().coerceIn(0f, 1f)

                val leftDp = with(density) { (startFraction * widthPx).toDp() }
                val barWidth = with(density) { ((endFraction - startFraction) * widthPx).toDp() }

                if (barWidth > 0.dp) {
                    Box(
                        modifier = Modifier
                            .offset(x = leftDp)
                            .size(width = barWidth, height = 6.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Brush.horizontalGradient(colors))
                    )
                }
            } else {
                // Full gradient bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(
                            Brush.horizontalGradient(colors),
                            RoundedCornerShape(2.dp)
                        )
                )
            }

            // Current value pointer
            if (currentValue != null && max > min) {
                val ratio = GradientScaleUtil.calculatePosition(
                    currentValue, fullRange, datasetType
                ).toFloat().coerceIn(0f, 1f)
                val positionPx = ratio * widthPx
                val positionDp = with(density) { positionPx.toDp() }
                val pointerColor = colorAt(currentValue, min, max, colors)

                Box(
                    modifier = Modifier
                        .offset(x = positionDp - 8.dp)
                        .size(width = 16.dp, height = 8.dp)
                        .shadow(1.dp, RoundedCornerShape(4.dp))
                        .background(pointerColor, RoundedCornerShape(4.dp))
                        .border(1.dp, MaterialTheme.colorScheme.onPrimary, RoundedCornerShape(4.dp))
                )
            }
        }

        // Max label
        val displayMax = apiUnit.convertForDisplay(max, temperatureUnits)
        Text(
            text = GradientScaleUtil.formatValue(displayMax, decimalPlaces) +
                    apiUnit.displayUnitSuffix(temperatureUnits),
            style = SaltyType.mono(12).copy(color = labelColor)
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
    colorscale: Colorscale,
    currentValue: Double? = null,
    filterRange: ClosedFloatingPointRange<Double>? = null,
    fullRange: ClosedFloatingPointRange<Double> = min..max,
    apiUnit: DatasetUnit = DatasetUnit.FAHRENHEIT,
    temperatureUnits: TemperatureUnits = TemperatureUnits.FAHRENHEIT,
    datasetType: DatasetType? = null,
    decimalPlaces: Int = 0,
    modifier: Modifier = Modifier
) {
    val colors = colorscale.hexColors.map { hex ->
        Color(android.graphics.Color.parseColor(hex))
    }
    GradientScaleBar(
        min = min,
        max = max,
        colors = colors,
        currentValue = currentValue,
        filterRange = filterRange,
        fullRange = fullRange,
        apiUnit = apiUnit,
        temperatureUnits = temperatureUnits,
        datasetType = datasetType,
        decimalPlaces = decimalPlaces,
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
