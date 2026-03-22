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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.data.waypoint.WeatherForecastEntry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Composite Canvas chart: precipitation bars (blue, 25% opacity) + temperature
 * line (orange) overlay. Matches iOS WeatherForecastChart in WaypointWeatherView.
 *
 * iOS ref: WaypointWeatherView.WeatherForecastChart
 */
@Composable
fun WeatherForecastChart(
    forecasts: List<WeatherForecastEntry>,
    modifier: Modifier = Modifier
) {
    if (forecasts.isEmpty()) return

    val density = LocalDensity.current
    val barWidthPx = with(density) { ChartConstants.BAR_WIDTH.toPx() }
    val totalWidth = ChartConstants.BAR_WIDTH * forecasts.size
    val cornerRadiusPx = with(density) { ChartConstants.BAR_CORNER_RADIUS.toPx() }

    // Pre-compute ranges
    val temps = remember(forecasts) { forecasts.mapNotNull { it.temperature?.airTemp } }
    val tempMin = remember(temps) { (temps.minOrNull() ?: 60.0) - 5.0 }
    val tempMax = remember(temps) { (temps.maxOrNull() ?: 90.0) + 5.0 }
    val tempRange = tempMax - tempMin

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

    // Text paint (monospace)
    val textPaintPrimary = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.MONOSPACE
        }
    }

    val tempColor = Color(0xFFFF8040)       // orange-red
    val tempLineColor = Color(0x66FF8040)   // 40% opacity
    val precipBarColor = Color(0x400000FF)  // blue 25% opacity
    val accentColor = ChartConstants.ACCENT_COLOR

    Canvas(
        modifier = modifier
            .height(ChartConstants.CHART_HEIGHT_FORECAST)
            .width(totalWidth)
            .horizontalScroll(rememberScrollState())
    ) {
        val chartH = size.height
        // Reserve top/bottom zones for annotations
        val topZone = chartH * 0.30f   // labels + "Now" pill
        val bottomZone = chartH * 0.12f // weather icon + time label
        val plotH = chartH - topZone - bottomZone
        val plotTop = topZone

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

        // 2. Draw precipitation bars
        forecasts.forEachIndexed { i, entry ->
            val prob = entry.precipitation?.probability ?: 0.0
            if (prob > 0) {
                val barH = (prob / 100.0 * 0.5 * plotH).toFloat()
                val x = i * barWidthPx + 2.dp.toPx()
                val w = barWidthPx - 4.dp.toPx()
                drawRoundRect(
                    color = precipBarColor,
                    topLeft = Offset(x, plotTop + plotH - barH),
                    size = Size(w, barH),
                    cornerRadius = CornerRadius(cornerRadiusPx)
                )
            }
        }

        // 3. Draw temperature line + dots
        if (tempRange > 0) {
            val path = Path()
            val points = mutableListOf<Offset>()

            forecasts.forEachIndexed { i, entry ->
                val temp = entry.temperature?.airTemp ?: return@forEachIndexed
                val cx = i * barWidthPx + barWidthPx / 2
                val normalized = (temp - tempMin) / tempRange
                // Map to 20%-80% of plot area (leaving room for annotations)
                val y = plotTop + plotH * (1.0 - (normalized * 0.6 + 0.2)).toFloat()
                val point = Offset(cx, y)
                points.add(point)
                if (points.size == 1) path.moveTo(cx, y)
                else path.lineTo(cx, y)
            }

            // Line
            drawPath(
                path = path,
                color = tempLineColor,
                style = Stroke(width = 2.dp.toPx())
            )

            // Dots
            points.forEachIndexed { i, pt ->
                drawCircle(
                    color = tempColor,
                    radius = 3.dp.toPx(),
                    center = pt
                )
            }
        }

        // 4. Draw annotations (temperature + precip labels above, weather icon + time below)
        val primaryColor = android.graphics.Color.WHITE
        val tertiaryColor = android.graphics.Color.argb(128, 255, 255, 255)

        forecasts.forEachIndexed { i, entry ->
            val cx = i * barWidthPx + barWidthPx / 2

            // -- "Now" pill --
            if (i == nowIndex) {
                drawNowPill(cx, topZone * 0.10f, accentColor, barWidthPx)
            }

            // -- Temperature label above point --
            val temp = entry.temperature?.airTemp
            if (temp != null) {
                val normalized = (temp - tempMin) / tempRange
                val pointY = plotTop + plotH * (1.0 - (normalized * 0.6 + 0.2)).toFloat()
                val labelY = if (i == nowIndex) pointY - 18.dp.toPx() else pointY - 8.dp.toPx()

                textPaintPrimary.textSize = 12.dp.toPx()
                textPaintPrimary.color = primaryColor
                drawContext.canvas.nativeCanvas.drawText(
                    "${temp.toInt()}\u00B0",
                    cx,
                    labelY,
                    textPaintPrimary
                )
            }

            // -- Precipitation label --
            val prob = entry.precipitation?.probability ?: 0.0
            if (prob > 10) {
                val tempLabelY = if (temp != null) {
                    val normalized = (temp - tempMin) / tempRange
                    val pointY = plotTop + plotH * (1.0 - (normalized * 0.6 + 0.2)).toFloat()
                    if (i == nowIndex) pointY - 30.dp.toPx() else pointY - 20.dp.toPx()
                } else {
                    plotTop - 10.dp.toPx()
                }

                textPaintPrimary.textSize = 10.dp.toPx()
                textPaintPrimary.color = tertiaryColor
                drawContext.canvas.nativeCanvas.drawText(
                    "${prob.toInt()}%",
                    cx,
                    tempLabelY,
                    textPaintPrimary
                )
            }

            // -- Weather condition icon (text abbreviation below chart) --
            val condition = entry.weather?.condition
            if (condition != null) {
                val iconY = chartH - bottomZone + 14.dp.toPx()
                textPaintPrimary.textSize = 10.dp.toPx()
                textPaintPrimary.color = tertiaryColor
                drawContext.canvas.nativeCanvas.drawText(
                    weatherConditionAbbrev(condition),
                    cx,
                    iconY,
                    textPaintPrimary
                )
            }

            // -- Time label at very bottom --
            textPaintPrimary.textSize = 10.dp.toPx()
            textPaintPrimary.color = tertiaryColor
            drawContext.canvas.nativeCanvas.drawText(
                timeLabels[i],
                cx,
                chartH - 2.dp.toPx(),
                textPaintPrimary
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
    barWidth: Float
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
        this.color = android.graphics.Color.BLACK
    }
    drawContext.canvas.nativeCanvas.drawText(
        "Now",
        cx,
        y + pillH * 0.75f,
        paint
    )
}

/**
 * Map weather condition string to a short abbreviation for the chart.
 * iOS ref: weatherIconName() — uses SF Symbols; we use text abbreviations
 * since Android Canvas can't render Material icons easily.
 */
private fun weatherConditionAbbrev(condition: String): String {
    val c = condition.lowercase()
    return when {
        c.contains("clear") || c.contains("sunny") -> "\u2600" // ☀
        c.contains("partly") && c.contains("cloud") -> "\u26C5" // ⛅
        c.contains("rain") && c.contains("thunder") -> "\u26C8" // ⛈
        c.contains("thunder") -> "\u26A1" // ⚡
        c.contains("rain") -> "\uD83C\uDF27" // 🌧 (using unicode)
        c.contains("drizzle") -> "\uD83C\uDF26" // 🌦
        c.contains("snow") -> "\u2744" // ❄
        c.contains("fog") || c.contains("mist") -> "\uD83C\uDF2B" // 🌫
        else -> "\u2601" // ☁
    }
}
