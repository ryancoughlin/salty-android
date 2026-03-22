package com.example.saltyoffshore.ui.waypoint.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.data.waypoint.WaveForecastEntry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Bar chart for wave height with color-coded bars (cyan -> purple -> red).
 * Structurally identical to WindForecastChart but uses waveColor scale
 * and shows height + period annotations.
 *
 * iOS ref: WaypointWeatherView.WaveForecastChart
 */
@Composable
fun WaveForecastChart(
    forecasts: List<WaveForecastEntry>,
    modifier: Modifier = Modifier
) {
    if (forecasts.isEmpty()) return

    val density = LocalDensity.current
    val barWidthPx = with(density) { ChartConstants.BAR_WIDTH.toPx() }
    val totalWidth = ChartConstants.BAR_WIDTH * forecasts.size
    val cornerRadiusPx = with(density) { ChartConstants.BAR_CORNER_RADIUS.toPx() }

    // Y range: 0 to max * 1.33 (iOS: maxHeight = max * 1.33)
    val maxHeight = remember(forecasts) {
        val max = forecasts.maxOfOrNull { it.height } ?: 5.0
        max * 1.33
    }

    // "Now" indicator: closest forecast to current time, only if today
    val zone = ZoneId.systemDefault()
    val nowIndex = remember(forecasts) {
        val today = LocalDate.now(zone)
        val firstDate = Instant.parse(forecasts.first().time).atZone(zone).toLocalDate()
        if (firstDate != today) -1
        else {
            val now = Instant.now()
            forecasts.indices.minByOrNull { i ->
                kotlin.math.abs(Instant.parse(forecasts[i].time).epochSecond - now.epochSecond)
            } ?: -1
        }
    }

    // Pre-compute time labels
    val timeFmt = remember { DateTimeFormatter.ofPattern("ha") }
    val timeLabels = remember(forecasts) {
        forecasts.map { entry ->
            Instant.parse(entry.time).atZone(zone).format(timeFmt).lowercase()
        }
    }

    val textPaint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.MONOSPACE
        }
    }

    val accentColor = ChartConstants.ACCENT_COLOR
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(
        modifier = modifier
            .height(ChartConstants.CHART_HEIGHT_FORECAST)
            .width(totalWidth)
            .horizontalScroll(rememberScrollState())
    ) {
        val chartH = size.height
        val topZone = chartH * 0.30f   // annotations + "Now" pill
        val bottomZone = chartH * 0.15f // direction arrow + time label
        val plotH = chartH - topZone - bottomZone
        val plotTop = topZone

        val primaryColor = onSurface.toArgb()
        val tertiaryColor = onSurfaceVariant.copy(alpha = 0.5f).toArgb()

        // 1. Draw "Now" rule line
        if (nowIndex >= 0) {
            val cx = nowIndex * barWidthPx + barWidthPx / 2
            drawLine(
                color = accentColor.copy(alpha = 0.8f),
                start = Offset(cx, topZone * 0.4f),
                end = Offset(cx, chartH - bottomZone),
                strokeWidth = 2.dp.toPx()
            )
        }

        // 2. Draw bars
        forecasts.forEachIndexed { i, entry ->
            val barH = if (maxHeight > 0) (entry.height / maxHeight * plotH).toFloat() else 0f
            val x = i * barWidthPx + 2.dp.toPx()
            val w = barWidthPx - 4.dp.toPx()

            drawRoundRect(
                color = ChartColorScales.waveColor(entry.height),
                topLeft = Offset(x, plotTop + plotH - barH),
                size = Size(w, barH),
                cornerRadius = CornerRadius(cornerRadiusPx)
            )
        }

        // 3. Draw annotations
        forecasts.forEachIndexed { i, entry ->
            val cx = i * barWidthPx + barWidthPx / 2

            // -- "Now" pill --
            if (i == nowIndex) {
                drawNowPill(cx, topZone * 0.10f, accentColor, barWidthPx, onSurface.toArgb())
            }

            // -- Height label --
            val barH = if (maxHeight > 0) (entry.height / maxHeight * plotH).toFloat() else 0f
            val barTop = plotTop + plotH - barH
            val labelY = if (i == nowIndex) barTop - 18.dp.toPx() else barTop - 6.dp.toPx()

            textPaint.textSize = 12.dp.toPx()
            textPaint.color = primaryColor
            drawContext.canvas.nativeCanvas.drawText(
                formatHeight(entry.height),
                cx,
                labelY,
                textPaint
            )

            // -- Period label --
            textPaint.textSize = 10.dp.toPx()
            textPaint.color = tertiaryColor
            drawContext.canvas.nativeCanvas.drawText(
                "${entry.period.toInt()}s",
                cx,
                labelY + 12.dp.toPx(),
                textPaint
            )

            // -- Direction arrow below bar --
            drawDirectionArrow(
                cx = cx,
                cy = chartH - bottomZone + 10.dp.toPx(),
                degrees = entry.direction.toFloat(),
                color = onSurfaceVariant.copy(alpha = 0.5f),
                arrowSize = 5.dp.toPx()
            )

            // -- Time label at very bottom --
            textPaint.textSize = 10.dp.toPx()
            textPaint.color = tertiaryColor
            drawContext.canvas.nativeCanvas.drawText(
                timeLabels[i],
                cx,
                chartH - 2.dp.toPx(),
                textPaint
            )
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

/**
 * Draw a "Now" pill indicator.
 * iOS ref: ChartTimeIndicator.nowIndicatorView
 */
private fun DrawScope.drawNowPill(
    cx: Float,
    y: Float,
    color: Color,
    barWidth: Float,
    textColor: Int
) {
    val pillW = barWidth * 0.9f
    val pillH = 16.dp.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(cx - pillW / 2, y),
        size = Size(pillW, pillH),
        cornerRadius = CornerRadius(4.dp.toPx())
    )
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
        textAlign = android.graphics.Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        textSize = 11.dp.toPx()
        this.color = textColor
    }
    drawContext.canvas.nativeCanvas.drawText(
        "Now",
        cx,
        y + pillH * 0.75f,
        paint
    )
}

/**
 * Draw a small directional arrow (triangle) rotated by [degrees].
 * iOS ref: Image(systemName: "arrowshape.up.fill").rotationEffect
 */
private fun DrawScope.drawDirectionArrow(
    cx: Float,
    cy: Float,
    degrees: Float,
    color: Color,
    arrowSize: Float
) {
    rotate(degrees = degrees, pivot = Offset(cx, cy)) {
        val path = Path().apply {
            moveTo(cx, cy - arrowSize)         // tip (top)
            lineTo(cx - arrowSize * 0.6f, cy + arrowSize * 0.5f) // bottom-left
            lineTo(cx + arrowSize * 0.6f, cy + arrowSize * 0.5f) // bottom-right
            close()
        }
        drawPath(path, color)
    }
}

/** Format wave height: "4.2ft" or "1ft" for whole numbers. iOS: forecast.formattedHeight */
private fun formatHeight(height: Double): String {
    return if (height == height.toLong().toDouble()) {
        "${height.toInt()}ft"
    } else {
        "${"%.1f".format(height)}ft"
    }
}
