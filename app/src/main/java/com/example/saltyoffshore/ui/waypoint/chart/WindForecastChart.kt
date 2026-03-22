package com.example.saltyoffshore.ui.waypoint.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saltyoffshore.data.waypoint.WeatherForecastEntry
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Bar chart showing wind speed over time with color-coded bars, speed/gust
 * annotations, direction arrows, and a "Now" indicator.
 *
 * iOS ref: WindForecastChart in WaypointWeatherView.swift
 *
 * Uses Canvas drawing for full control over per-bar coloring, annotations,
 * and direction arrows -- the same visual as iOS Swift Charts but without
 * needing a chart library that supports per-bar custom colors.
 */
@Composable
fun WindForecastChart(
    forecasts: List<WeatherForecastEntry>,
    sunriseHour: Int? = null,
    sunsetHour: Int? = null,
    modifier: Modifier = Modifier
) {
    if (forecasts.isEmpty()) return

    val zone = ZoneId.systemDefault()
    val hourFmt = remember { DateTimeFormatter.ofPattern("ha", Locale.getDefault()) }

    // Parse times once
    val parsed = remember(forecasts) {
        forecasts.map { entry ->
            val zdt = Instant.parse(entry.time).atZone(zone)
            ParsedWindEntry(
                zdt = zdt,
                speed = entry.wind.speed,
                gust = entry.wind.gust,
                direction = entry.wind.direction,
                hourLabel = zdt.format(hourFmt).lowercase(Locale.getDefault()),
                color = ChartColorScales.windColor(entry.wind.speed)
            )
        }
    }

    // Find "now" index: closest forecast to current time (only if chart day is today)
    val nowIndex = remember(parsed) {
        findNowIndex(parsed)
    }

    val maxSpeed = remember(parsed) {
        val peak = parsed.maxOf { maxOf(it.speed, it.gust) }
        peak * 1.33
    }

    val density = LocalDensity.current
    val barWidthPx = with(density) { ChartConstants.BAR_WIDTH.toPx() }
    val gapPx = with(density) { 8.dp.toPx() }
    val cellWidth = barWidthPx + gapPx

    // Total width for horizontal scrolling
    val totalWidth = with(density) { (cellWidth * parsed.size + gapPx).toDp() }

    val scrollState = rememberScrollState()

    val annotationHeight = 60.dp  // space above bars for speed/gust/Now
    val arrowHeight = 20.dp       // space below bars for direction arrow
    val labelHeight = 16.dp       // space below arrow for hour label
    val barAreaHeight = ChartConstants.CHART_HEIGHT_FORECAST

    val surface = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier.horizontalScroll(scrollState)) {
        // Annotation row (speed, gust, "Now" badge)
        Row(modifier = Modifier.width(totalWidth).height(annotationHeight)) {
            parsed.forEachIndexed { index, entry ->
                Box(
                    modifier = Modifier
                        .width(with(density) { cellWidth.toDp() })
                        .height(annotationHeight),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (index == nowIndex) {
                            NowBadge()
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                        Text(
                            text = "${entry.speed.toInt()}",
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = FontFamily.Monospace,
                            color = onSurface
                        )
                        Text(
                            text = "${entry.gust.toInt()}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }

        // Bar area (Canvas)
        Canvas(
            modifier = Modifier
                .width(totalWidth)
                .height(barAreaHeight)
        ) {
            val chartH = size.height

            // Day/night background
            if (sunriseHour != null && sunsetHour != null) {
                drawNightZones(parsed, cellWidth, chartH, sunriseHour, sunsetHour, surface)
            }

            // "Now" vertical rule
            if (nowIndex != null) {
                val cx = nowIndex * cellWidth + cellWidth / 2f
                drawLine(
                    color = ChartConstants.ACCENT_COLOR.copy(alpha = 0.8f),
                    start = Offset(cx, 0f),
                    end = Offset(cx, chartH),
                    strokeWidth = with(density) { 2.dp.toPx() }
                )
            }

            // Bars
            val cornerPx = with(density) { ChartConstants.BAR_CORNER_RADIUS.toPx() }
            parsed.forEachIndexed { index, entry ->
                val x = index * cellWidth + gapPx / 2f
                val fraction = (entry.speed / maxSpeed).coerceIn(0.0, 1.0)
                val barH = (fraction * chartH).toFloat()

                drawRoundRect(
                    color = entry.color,
                    topLeft = Offset(x, chartH - barH),
                    size = Size(barWidthPx, barH),
                    cornerRadius = CornerRadius(cornerPx, cornerPx)
                )
            }
        }

        // Direction arrows row
        Row(modifier = Modifier.width(totalWidth).height(arrowHeight)) {
            parsed.forEachIndexed { _, entry ->
                Box(
                    modifier = Modifier
                        .width(with(density) { cellWidth.toDp() })
                        .height(arrowHeight),
                    contentAlignment = Alignment.Center
                ) {
                    DirectionArrow(
                        degrees = entry.direction,
                        color = onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }

        // Hour labels row
        Row(modifier = Modifier.width(totalWidth).height(labelHeight)) {
            parsed.forEach { entry ->
                Text(
                    text = entry.hourLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    fontFamily = FontFamily.Monospace,
                    color = onSurfaceVariant.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(with(density) { cellWidth.toDp() })
                )
            }
        }
    }
}

// ── "Now" Badge ────────────────────────────────────────────────────────────

private val NowBadgeColor = Color(0xFFFFCC33)

@Composable
private fun NowBadge() {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = NowBadgeColor
    ) {
        Text(
            text = "Now",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

// ── Direction Arrow ────────────────────────────────────────────────────────

@Composable
private fun DirectionArrow(degrees: Double, color: Color) {
    val arrowSize = 10.dp
    Canvas(modifier = Modifier.width(arrowSize).height(arrowSize)) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = size.width / 2.5f

        rotate(degrees.toFloat(), pivot = Offset(cx, cy)) {
            // Simple upward-pointing triangle
            val path = Path().apply {
                moveTo(cx, cy - r)          // tip
                lineTo(cx - r * 0.5f, cy + r * 0.5f)  // bottom-left
                lineTo(cx + r * 0.5f, cy + r * 0.5f)  // bottom-right
                close()
            }
            drawPath(path, color)
        }
    }
}

// ── Night Zones ────────────────────────────────────────────────────────────

private fun DrawScope.drawNightZones(
    entries: List<ParsedWindEntry>,
    cellWidth: Float,
    chartHeight: Float,
    sunriseHour: Int,
    sunsetHour: Int,
    surfaceColor: Color
) {
    val nightColor = surfaceColor.copy(alpha = ChartConstants.NIGHT_OVERLAY_OPACITY)

    // Walk entries, batch consecutive night hours into rectangles
    var nightStart: Float? = null
    entries.forEachIndexed { index, entry ->
        val hour = entry.zdt.hour
        val isNight = hour < sunriseHour || hour >= sunsetHour
        val x = index * cellWidth

        if (isNight && nightStart == null) {
            nightStart = x
        }
        if (!isNight && nightStart != null) {
            drawRect(nightColor, Offset(nightStart!!, 0f), Size(x - nightStart!!, chartHeight))
            nightStart = null
        }
    }
    // Close trailing night zone
    if (nightStart != null) {
        val endX = entries.size * cellWidth
        drawRect(nightColor, Offset(nightStart!!, 0f), Size(endX - nightStart!!, chartHeight))
    }
}

// ── Helpers ────────────────────────────────────────────────────────────────

private data class ParsedWindEntry(
    val zdt: ZonedDateTime,
    val speed: Double,
    val gust: Double,
    val direction: Double,
    val hourLabel: String,
    val color: Color
)

/**
 * Find the index of the forecast closest to "now", but only if the forecasts
 * include today. Returns null if chart day is not today.
 *
 * iOS ref: ChartTimeIndicator.shouldShowNowIndicator
 */
private fun findNowIndex(entries: List<ParsedWindEntry>): Int? {
    if (entries.isEmpty()) return null

    val today = java.time.LocalDate.now()
    val chartDay = entries.first().zdt.toLocalDate()
    if (chartDay != today) return null

    val now = Instant.now()
    return entries.indices.minByOrNull { i ->
        kotlin.math.abs(entries[i].zdt.toInstant().toEpochMilli() - now.toEpochMilli())
    }
}
