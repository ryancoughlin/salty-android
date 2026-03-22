package com.example.saltyoffshore.ui.waypoint.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Data point for the condition history chart.
 * iOS ref: HistoryDataPoint (subset used for charting)
 */
data class ConditionDataPoint(
    val date: String,   // "Mar 15", "Mar 16"
    val value: Double
)

/**
 * Compact 7-day bar chart for waypoint condition history.
 * Each bar grows from the domain baseline (data min), with a color-accent cap.
 *
 * iOS ref: WaypointConditionChart
 *
 * @param dataPoints    Daily values to chart
 * @param colorForValue Maps a data value to its colorscale color
 * @param valueFormatter Formats a value for the annotation label (e.g. "68.2F")
 */
@Composable
fun WaypointConditionChart(
    dataPoints: List<ConditionDataPoint>,
    colorForValue: (Double) -> Color,
    valueFormatter: (Double) -> String,
    modifier: Modifier = Modifier
) {
    if (dataPoints.isEmpty()) return

    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant

    // Y domain: min..max with 25% padding above (matches iOS yDomain)
    val values = dataPoints.map { it.value }
    val dataMin = values.min()
    val dataMax = values.max()
    val range = dataMax - dataMin
    val topPadding = if (range > 0) range * 0.25 else dataMax * 0.1
    val domainMin = dataMin
    val domainMax = dataMax + topPadding
    val domainRange = domainMax - domainMin

    Column(modifier) {
        // Value annotations above bars
        Row(Modifier.fillMaxWidth()) {
            dataPoints.forEach { point ->
                Text(
                    text = valueFormatter(point.value),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    color = textColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(2.dp))

        // Bar chart canvas
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(ChartConstants.CHART_HEIGHT_HISTORY)
        ) {
            val barSpacing = size.width / dataPoints.size
            val barWidth = barSpacing * 0.85f
            val capHeight = 3.dp.toPx()
            val cornerPx = 2.dp.toPx()

            dataPoints.forEachIndexed { index, point ->
                val x = index * barSpacing + (barSpacing - barWidth) / 2

                // Bar height proportional to domain (grows from baseline)
                val normalized = if (domainRange > 0) {
                    (point.value - domainMin) / domainRange
                } else {
                    0.5
                }
                val barHeight = (normalized * size.height).toFloat()
                val barTop = size.height - barHeight

                // Base bar (surfaceVariant) with rounded top corners
                drawRoundRect(
                    color = surfaceVariant,
                    topLeft = Offset(x, barTop),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(cornerPx)
                )

                // Color accent cap (top 3dp, rounded top corners only)
                val capPath = Path().apply {
                    val left = x
                    val right = x + barWidth
                    val top = barTop
                    val bottom = barTop + capHeight

                    // Start bottom-left (square corner)
                    moveTo(left, bottom)
                    // Left edge up
                    lineTo(left, top + cornerPx)
                    // Top-left rounded corner
                    cubicTo(left, top, left + cornerPx, top, left + cornerPx, top)
                    // Top edge
                    lineTo(right - cornerPx, top)
                    // Top-right rounded corner
                    cubicTo(right, top, right, top + cornerPx, right, top + cornerPx)
                    // Right edge down (square corner)
                    lineTo(right, bottom)
                    close()
                }
                drawPath(
                    path = capPath,
                    color = colorForValue(point.value)
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // Date labels below bars
        Row(Modifier.fillMaxWidth()) {
            dataPoints.forEach { point ->
                Text(
                    text = point.date,
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
