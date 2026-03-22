package com.example.saltyoffshore.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saltyoffshore.data.CurrentValue
import com.example.saltyoffshore.data.DatasetConfiguration
import com.example.saltyoffshore.data.TemperatureUnits

/**
 * Crosshair visual with value display.
 * Matches iOS CrosshairView.
 */
@Composable
fun CrosshairView(
    primaryValue: CurrentValue,
    temperatureUnits: TemperatureUnits,
    modifier: Modifier = Modifier
) {
    val crosshairColor = MaterialTheme.colorScheme.onPrimary
    val shadowColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Crosshair lines
        Canvas(modifier = Modifier.size(32.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val lineLength = 12.dp.toPx()
            val gap = 4.dp.toPx()
            val strokeWidth = 2.dp.toPx()

            // Draw shadow first (offset slightly)
            val shadowOffset = 1.dp.toPx()

            // Horizontal lines with shadow
            // Left line shadow
            drawLine(
                color = shadowColor,
                start = Offset(center.x - gap - lineLength + shadowOffset, center.y + shadowOffset),
                end = Offset(center.x - gap + shadowOffset, center.y + shadowOffset),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            // Right line shadow
            drawLine(
                color = shadowColor,
                start = Offset(center.x + gap + shadowOffset, center.y + shadowOffset),
                end = Offset(center.x + gap + lineLength + shadowOffset, center.y + shadowOffset),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            // Top line shadow
            drawLine(
                color = shadowColor,
                start = Offset(center.x + shadowOffset, center.y - gap - lineLength + shadowOffset),
                end = Offset(center.x + shadowOffset, center.y - gap + shadowOffset),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            // Bottom line shadow
            drawLine(
                color = shadowColor,
                start = Offset(center.x + shadowOffset, center.y + gap + shadowOffset),
                end = Offset(center.x + shadowOffset, center.y + gap + lineLength + shadowOffset),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // Horizontal lines
            // Left line
            drawLine(
                color = crosshairColor,
                start = Offset(center.x - gap - lineLength, center.y),
                end = Offset(center.x - gap, center.y),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            // Right line
            drawLine(
                color = crosshairColor,
                start = Offset(center.x + gap, center.y),
                end = Offset(center.x + gap + lineLength, center.y),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // Vertical lines
            // Top line
            drawLine(
                color = crosshairColor,
                start = Offset(center.x, center.y - gap - lineLength),
                end = Offset(center.x, center.y - gap),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            // Bottom line
            drawLine(
                color = crosshairColor,
                start = Offset(center.x, center.y + gap),
                end = Offset(center.x, center.y + gap + lineLength),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }

        // Value label below crosshair
        AnimatedVisibility(
            visible = primaryValue.shouldShow,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(top = 40.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = formatValue(primaryValue, temperatureUnits),
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }
    }
}

private fun formatValue(currentValue: CurrentValue, temperatureUnits: TemperatureUnits): String {
    val value = currentValue.value ?: return "--"
    val apiUnit = currentValue.apiUnit ?: return "--"
    val datasetType = currentValue.datasetType ?: return "--"

    val config = DatasetConfiguration.forDatasetType(datasetType)
    val displayValue = apiUnit.convertForDisplay(value, temperatureUnits)
    val unitSuffix = apiUnit.displayUnitSuffix(temperatureUnits)

    return String.format("%.${config.decimalPlaces}f%s", displayValue, unitSuffix)
}
