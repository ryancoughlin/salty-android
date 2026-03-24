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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.data.station.CurrentReading
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Currents line chart — speed by depth over time.
 * Matches iOS StationCurrentsChart.
 */
@Composable
fun StationCurrentsChart(
    history: List<CurrentReading>,
    selectedDay: LocalDate,
    modifier: Modifier = Modifier
) {
    if (history.isEmpty()) return

    val binnedDepths = remember(history) {
        val allDepths = history.flatMap { it.depths.map { d -> d.depthFt } }.toSet().sorted()
        if (allDepths.size <= 4) allDepths
        else listOf(
            allDepths[0],
            allDepths[allDepths.size / 3],
            allDepths[allDepths.size * 2 / 3],
            allDepths.last()
        )
    }

    data class ChartPoint(val timeEpoch: Long, val depthFt: Int, val speedKnots: Double)

    val chartData = remember(history, binnedDepths) {
        val validDepths = binnedDepths.toSet()
        history.flatMap { reading ->
            val epoch = Instant.parse(reading.time).epochSecond
            reading.depths
                .filter { it.depthFt in validDepths && it.speedKnots != null }
                .map { ChartPoint(epoch, it.depthFt, it.speedKnots!!) }
        }
    }

    val maxSpeed = remember(chartData) {
        (chartData.maxOfOrNull { it.speedKnots } ?: 2.0) * 1.2
    }

    val dayStart = remember(selectedDay) {
        selectedDay.atStartOfDay(ZoneId.systemDefault()).toInstant().epochSecond
    }
    val dayEnd = dayStart + 86400

    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val density = LocalDensity.current
    val hourFormatter = remember { DateTimeFormatter.ofPattern("ha").withZone(ZoneId.systemDefault()) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
    ) {
        val leftPadding = 0f
        val rightPadding = 40.dp.toPx()
        val topPadding = 8.dp.toPx()
        val bottomPadding = 24.dp.toPx()
        val chartWidth = size.width - leftPadding - rightPadding
        val chartHeight = size.height - topPadding - bottomPadding

        fun xForTime(epoch: Long): Float {
            val fraction = (epoch - dayStart).toFloat() / (dayEnd - dayStart).toFloat()
            return leftPadding + fraction * chartWidth
        }

        fun yForSpeed(speed: Double): Float {
            val fraction = (speed / maxSpeed).toFloat().coerceIn(0f, 1f)
            return topPadding + chartHeight * (1f - fraction)
        }

        // X-axis labels (6-hour intervals)
        for (h in 0..4) {
            val epoch = dayStart + h * 6 * 3600L
            val x = xForTime(epoch)
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = onSurfaceVariant.toArgb()
                    textSize = with(density) { 10.dp.toPx() }
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                val label = hourFormatter.format(Instant.ofEpochSecond(epoch)).lowercase()
                drawText(label, x, size.height, paint)
            }
        }

        // Y-axis labels
        val ySteps = 3
        for (i in 0..ySteps) {
            val speed = maxSpeed * i / ySteps
            val y = yForSpeed(speed)
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = onSurfaceVariant.toArgb()
                    textSize = with(density) { 10.dp.toPx() }
                    textAlign = android.graphics.Paint.Align.LEFT
                    isAntiAlias = true
                }
                drawText(String.format("%.1f", speed), size.width - rightPadding + 4.dp.toPx(), y + 4.dp.toPx(), paint)
            }
        }

        // Draw lines per depth
        binnedDepths.forEachIndexed { depthIndex, depthFt ->
            val points = chartData
                .filter { it.depthFt == depthFt }
                .sortedBy { it.timeEpoch }

            if (points.size < 2) return@forEachIndexed

            val lineColor = if (depthIndex == 0) Color.White
            else Color.White.copy(alpha = (0.6f - depthIndex * 0.15f).coerceAtLeast(0.3f))
            val lineWidth = if (depthIndex == 0) 2.dp.toPx() else 1.dp.toPx()

            val path = Path()
            points.forEachIndexed { i, point ->
                val x = xForTime(point.timeEpoch)
                val y = yForSpeed(point.speedKnots)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            drawPath(path, lineColor, style = Stroke(width = lineWidth))
        }

        // Now indicator
        val now = Instant.now().epochSecond
        if (now in dayStart..dayEnd) {
            val nowX = xForTime(now)
            drawLine(
                color = Color(0xFFFF6B35),
                start = Offset(nowX, topPadding),
                end = Offset(nowX, topPadding + chartHeight),
                strokeWidth = 2.dp.toPx()
            )
        }
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
