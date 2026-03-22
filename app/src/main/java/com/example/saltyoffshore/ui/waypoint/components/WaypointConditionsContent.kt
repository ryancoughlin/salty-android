package com.example.saltyoffshore.ui.waypoint.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.data.waypoint.ConditionHistoryResult
import com.example.saltyoffshore.data.waypoint.ConditionResult
import com.example.saltyoffshore.data.waypoint.ConditionsHistoryResponse
import com.example.saltyoffshore.data.waypoint.ConditionsResponse
import com.example.saltyoffshore.ui.waypoint.chart.ChartColorScales
import com.example.saltyoffshore.ui.waypoint.chart.ConditionDataPoint
import com.example.saltyoffshore.ui.waypoint.chart.WaypointConditionChart
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * UI state for the Conditions tab.
 * iOS ref: WaypointConditionsViewModel state properties
 */
data class ConditionsUiState(
    val isLoading: Boolean = true,
    val response: ConditionsResponse? = null,
    val historyResponse: ConditionsHistoryResponse? = null,
    val error: String? = null
) {
    val availableConditions: List<ConditionResult>
        get() = response?.results?.filter { it.hasData } ?: emptyList()

    val hasNoData: Boolean
        get() {
            val results = response?.results ?: return false
            return results.all { !it.hasData }
        }

    val sunriseSunsetString: String?
        get() = response?.sunriseSunsetString

    /** Map of condition type -> history result for quick lookup. */
    val historyByType: Map<String, ConditionHistoryResult>
        get() = historyResponse?.results?.associateBy { it.type } ?: emptyMap()
}

/**
 * Conditions tab content inside WaypointDetailSheet.
 *
 * iOS ref: WaypointConditionsView
 * Shows a forecast summary hero, condition rows with label+value,
 * 7-day history charts per condition, sunrise/sunset info, and moon phase.
 */
@Composable
fun WaypointConditionsContent(
    conditionsState: ConditionsUiState,
    summaryText: String?,
    summaryLoading: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        // ── Forecast Summary Hero ───────────────────────────────────────
        ForecastSummaryHero(
            summaryText = summaryText,
            isLoading = summaryLoading
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── Conditions Body ─────────────────────────────────────────────
        when {
            conditionsState.isLoading -> {
                // Shimmer placeholders
                repeat(4) {
                    ShimmerRow()
                    if (it < 3) Spacer(modifier = Modifier.height(12.dp))
                }
            }

            conditionsState.error != null -> {
                Text(
                    text = "Could not load conditions",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                )
            }

            conditionsState.hasNoData -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                ) {
                    Text(
                        text = "No conditions available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "This area may be cloudy or out of satellite coverage",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                val historyByType = conditionsState.historyByType

                // Condition rows with inline history charts
                conditionsState.availableConditions.forEach { result ->
                    WaypointConditionRow(
                        result = result,
                        historyResult = historyByType[result.type]
                    )
                }

                // Sunrise / Sunset
                conditionsState.sunriseSunsetString?.let { ss ->
                    InfoRow(label = "Sunrise / Sunset", value = ss)
                }

                // Moon phase
                conditionsState.response?.moon?.let { moon ->
                    InfoRow(
                        label = "Moon Phase",
                        value = "${moon.phaseName} (${moon.illumination}%)",
                        leadingIcon = { Icon(Icons.Default.DarkMode, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }
        }
    }
}

// ── Forecast Summary Hero ───────────────────────────────────────────────────

/**
 * Hero text showing the weather summary at the top of the Conditions tab.
 * iOS ref: ForecastSummaryHero
 */
@Composable
private fun ForecastSummaryHero(
    summaryText: String?,
    isLoading: Boolean
) {
    if (isLoading) {
        // Skeleton lines
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ShimmerBar(modifier = Modifier.fillMaxWidth().height(20.dp))
            ShimmerBar(modifier = Modifier.width(180.dp).height(20.dp))
        }
    } else if (summaryText != null) {
        Text(
            text = summaryText,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ── Condition Row ───────────────────────────────────────────────────────────

private val dateLabelFmt = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())

/**
 * Single condition row: label on left, formatted value on right,
 * with optional 7-day history chart below.
 * iOS ref: WaypointConditionRow
 */
@Composable
private fun WaypointConditionRow(
    result: ConditionResult,
    historyResult: ConditionHistoryResult?
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = result.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = result.relativeTimeString,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            Text(
                text = result.condition,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // 7-day history chart (if data available)
        historyResult?.let { history ->
            val dataPoints = remember(history) {
                history.dataPoints.mapNotNull { point ->
                    val value = point.numericValue ?: return@mapNotNull null
                    val dateLabel = try {
                        LocalDate.parse(point.date).format(dateLabelFmt)
                    } catch (_: Exception) {
                        point.date.takeLast(5) // fallback
                    }
                    ConditionDataPoint(date = dateLabel, value = value)
                }
            }

            if (dataPoints.size >= 2) {
                val valueRange = remember(dataPoints) {
                    val values = dataPoints.map { it.value }
                    values.min()..values.max()
                }

                WaypointConditionChart(
                    dataPoints = dataPoints,
                    colorForValue = { value ->
                        colorForConditionType(result.type, value, valueRange)
                    },
                    valueFormatter = { value ->
                        formatConditionValue(result.type, value)
                    },
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }

        HorizontalDivider()
    }
}

// ── Color Scale Mapping ─────────────────────────────────────────────────────

/**
 * Map condition type to the appropriate color scale.
 * iOS ref: ConditionResult color mapping in WaypointConditionChart
 */
private fun colorForConditionType(
    type: String,
    value: Double,
    range: ClosedRange<Double>
): Color {
    return when (type) {
        "sst" -> ChartColorScales.sstColor(value, range)
        "chlorophyll" -> ChartColorScales.chlorophyllColor(value)
        "salinity" -> ChartColorScales.salinityColor(value, range)
        "mld" -> ChartColorScales.mldColor(value, range)
        "currents" -> ChartColorScales.currentsColor(value, range)
        else -> ChartColorScales.sstColor(value, range) // sensible fallback
    }
}

/**
 * Format a condition value for chart annotation labels.
 * iOS ref: ConditionResult.formattedValue
 */
private fun formatConditionValue(type: String, value: Double): String {
    return when (type) {
        "sst" -> "${"%.1f".format(value)}\u00B0"
        "chlorophyll" -> "${"%.2f".format(value)}"
        "salinity" -> "${"%.1f".format(value)}"
        "mld" -> "${value.toInt()}m"
        "currents" -> "${"%.1f".format(value)}kt"
        "dissolved_oxygen" -> "${"%.1f".format(value)}"
        else -> "${"%.1f".format(value)}"
    }
}

// ── Info Row ────────────────────────────────────────────────────────────────

/**
 * Generic info row with label, optional leading icon, and value on right.
 * iOS ref: InfoRow / MoonPhaseInfoRow
 */
@Composable
private fun InfoRow(
    label: String,
    value: String,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                leadingIcon?.invoke()
                if (leadingIcon != null) Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        HorizontalDivider()
    }
}

// ── Shimmer Helpers ─────────────────────────────────────────────────────────

@Composable
private fun ShimmerRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        ShimmerBar(modifier = Modifier.width(100.dp).height(16.dp))
        ShimmerBar(modifier = Modifier.width(80.dp).height(16.dp))
    }
}

@Composable
private fun ShimmerBar(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val brush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
        ),
        start = Offset(translateAnim - 200f, 0f),
        end = Offset(translateAnim, 0f)
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(brush)
    )
}
