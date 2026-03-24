package com.example.saltyoffshore.ui.station.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.data.weather.WeatherConditions

/**
 * Compact wind sparkline for station hero section.
 * Matches iOS WindSparkLineChart.
 */
@Composable
fun WindSparkLineChart(
    forecasts: List<WeatherConditions>,
    modifier: Modifier = Modifier
) {
    if (forecasts.size < 2) return

    val sorted = remember(forecasts) { forecasts.sortedBy { it.time } }
    val maxSpeed = remember(sorted) { (sorted.maxOfOrNull { it.speed } ?: 10.0) * 1.1 }
    val minSpeed = remember(sorted) { 0.0 }
    val lineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
    ) {
        val padding = 4.dp.toPx()
        val chartWidth = size.width - padding * 2
        val chartHeight = size.height - padding * 2

        val path = Path()
        sorted.forEachIndexed { index, condition ->
            val x = padding + (index.toFloat() / (sorted.size - 1)) * chartWidth
            val y = padding + chartHeight * (1f - ((condition.speed - minSpeed) / (maxSpeed - minSpeed)).toFloat())

            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(path, lineColor, style = Stroke(width = 1.5.dp.toPx()))
    }
}
