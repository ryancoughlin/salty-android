package com.example.saltyoffshore.ui.station.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.data.station.WaveForecast
import com.example.saltyoffshore.ui.chart.ChartColorScales
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Wave conditions bar chart.
 * Matches iOS WaveConditionsChart.
 */
@Composable
fun WaveConditionsChart(
    forecasts: List<WaveForecast.Forecast>,
    date: LocalDate,
    modifier: Modifier = Modifier
) {
    val filtered = remember(forecasts, date) {
        forecasts.filter {
            it.parsedTime.atZone(ZoneId.systemDefault()).toLocalDate() == date
        }.sortedBy { it.parsedTime }
    }

    if (filtered.isEmpty()) return

    val maxHeight = remember(filtered) {
        (filtered.maxOfOrNull { it.height } ?: 10.0) * 1.33
    }

    val now = remember { Instant.now() }
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val nowIndicatorColor = Color(0xFFFF6B35)
    val density = LocalDensity.current
    val hourFormatter = remember { DateTimeFormatter.ofPattern("ha").withZone(ZoneId.systemDefault()) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(vertical = 8.dp)
    ) {
        val barWidth = 28.dp.toPx()
        val topPadding = 32.dp.toPx()
        val bottomPadding = 44.dp.toPx()
        val chartHeight = size.height - topPadding - bottomPadding
        val totalWidth = size.width
        val spacing = if (filtered.size > 1) (totalWidth - barWidth * filtered.size) / (filtered.size + 1) else (totalWidth - barWidth) / 2

        filtered.forEachIndexed { index, forecast ->
            val x = spacing + index * (barWidth + spacing)
            val barHeight = ((forecast.height / maxHeight) * chartHeight).toFloat()
            val barTop = topPadding + chartHeight - barHeight
            val barColor = ChartColorScales.waveColor(forecast.height)

            // Now indicator
            val isNow = isClosestToNow(forecast.parsedTime, filtered.map { it.parsedTime }, now, date)
            if (isNow) {
                drawLine(
                    color = nowIndicatorColor,
                    start = Offset(x + barWidth / 2, topPadding),
                    end = Offset(x + barWidth / 2, topPadding + chartHeight),
                    strokeWidth = 2.dp.toPx()
                )
            }

            // Bar
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, barTop),
                size = Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
            )

            // Height label above bar
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = if (isNow) android.graphics.Color.rgb(255, 107, 53)
                        else onSurface.toArgb()
                    textSize = with(density) { 12.dp.toPx() }
                    textAlign = android.graphics.Paint.Align.CENTER
                    typeface = android.graphics.Typeface.MONOSPACE
                    isAntiAlias = true
                }
                drawText(
                    forecast.formattedHeight,
                    x + barWidth / 2,
                    barTop - 8.dp.toPx(),
                    paint
                )
            }

            // Direction arrow below bar
            drawArrow(
                center = Offset(x + barWidth / 2, topPadding + chartHeight + 16.dp.toPx()),
                degrees = forecast.direction.toFloat(),
                color = onSurfaceVariant,
                arrowSize = 10.dp.toPx()
            )

            // Hour label at bottom
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = onSurfaceVariant.toArgb()
                    textSize = with(density) { 10.dp.toPx() }
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                val label = hourFormatter.format(forecast.parsedTime).lowercase()
                drawText(label, x + barWidth / 2, size.height - 2.dp.toPx(), paint)
            }
        }
    }
}

private fun isClosestToNow(
    time: Instant, allTimes: List<Instant>, now: Instant, date: LocalDate
): Boolean {
    if (now.atZone(ZoneId.systemDefault()).toLocalDate() != date) return false
    val closest = allTimes.minByOrNull { kotlin.math.abs(it.epochSecond - now.epochSecond) }
    return closest == time
}

private fun DrawScope.drawArrow(center: Offset, degrees: Float, color: Color, arrowSize: Float) {
    rotate(degrees, pivot = center) {
        val path = Path().apply {
            moveTo(center.x, center.y - arrowSize / 2)
            lineTo(center.x - arrowSize / 3, center.y + arrowSize / 2)
            lineTo(center.x + arrowSize / 3, center.y + arrowSize / 2)
            close()
        }
        drawPath(path, color)
    }
}

private fun Color.toArgb(): Int {
    return android.graphics.Color.argb(
        (alpha * 255).toInt(),
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt()
    )
}
