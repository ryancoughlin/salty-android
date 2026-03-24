package com.example.saltyoffshore.ui.station

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.data.station.CurrentReading
import com.example.saltyoffshore.data.station.StationCurrents
import com.example.saltyoffshore.ui.station.charts.StationCurrentsChart
import com.example.saltyoffshore.ui.station.forecast.ForecastDaySelector
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Real-time currents section with day selector and depth chart.
 * Matches iOS StationCurrentsSection.
 */
@Composable
fun StationCurrentsSection(
    currents: StationCurrents?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    if (currents == null && !isLoading) return

    val historyByDay = remember(currents) {
        currents?.history
            ?.groupBy {
                Instant.parse(it.time).atZone(ZoneId.systemDefault()).toLocalDate()
            } ?: emptyMap()
    }

    val availableDays = remember(historyByDay) {
        historyByDay.keys.sorted()
    }

    var selectedDay by remember(availableDays) {
        mutableStateOf(availableDays.lastOrNull() ?: LocalDate.now())
    }

    val selectedDayHistory = remember(historyByDay, selectedDay) {
        historyByDay[selectedDay] ?: emptyList()
    }

    val binnedDepths = remember(selectedDayHistory) {
        val last = selectedDayHistory.lastOrNull() ?: return@remember emptyList()
        val allDepths = last.depths.map { it.depthFt }.sorted()
        if (allDepths.size <= 4) allDepths
        else listOf(
            allDepths[0],
            allDepths[allDepths.size / 3],
            allDepths[allDepths.size * 2 / 3],
            allDepths.last()
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Real-Time Currents",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                )
            } else if (currents != null && currents.history.isNotEmpty()) {
                // Day selector
                if (availableDays.size > 1) {
                    ForecastDaySelector(
                        selectedDay = selectedDay,
                        availableDays = availableDays,
                        onDaySelected = { selectedDay = it }
                    )
                }

                // Chart
                if (selectedDayHistory.isNotEmpty()) {
                    StationCurrentsChart(
                        history = selectedDayHistory,
                        selectedDay = selectedDay
                    )
                }

                // Depth legend
                if (binnedDepths.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        binnedDepths.forEachIndexed { index, depth ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(depthColor(index))
                                )
                                Text(
                                    "${depth}ft",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Updated timestamp
                currents.updatedAt?.let { timestamp ->
                    val ago = remember(timestamp) {
                        try {
                            val instant = Instant.parse(timestamp)
                            val minutes = ChronoUnit.MINUTES.between(instant, Instant.now())
                            when {
                                minutes < 1 -> "just now"
                                minutes < 60 -> "${minutes}m ago"
                                else -> "${minutes / 60}h ${minutes % 60}m ago"
                            }
                        } catch (_: Exception) { "" }
                    }
                    if (ago.isNotEmpty()) {
                        Text(
                            "Updated $ago",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                // Empty state
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Current data temporarily unavailable",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun depthColor(index: Int): Color {
    val opacity = (1.0f - index * 0.2f).coerceAtLeast(0.3f)
    return Color.White.copy(alpha = opacity)
}
