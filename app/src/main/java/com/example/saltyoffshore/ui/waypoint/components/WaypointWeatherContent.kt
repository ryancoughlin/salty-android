package com.example.saltyoffshore.ui.waypoint.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saltyoffshore.data.waypoint.WaveForecastEntry
import com.example.saltyoffshore.data.waypoint.WeatherForecastEntry
import com.example.saltyoffshore.data.waypoint.WeatherResponse
import com.example.saltyoffshore.data.waypoint.WaveResponse
import com.example.saltyoffshore.ui.waypoint.chart.WaveForecastChart
import com.example.saltyoffshore.ui.waypoint.chart.WeatherForecastChart
import com.example.saltyoffshore.ui.waypoint.chart.WindForecastChart
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * UI state for the Weather tab.
 * iOS ref: WaypointWeatherViewModel state properties
 */
data class WeatherUiState(
    val isLoading: Boolean = true,
    val weatherResponse: WeatherResponse? = null,
    val waveResponse: WaveResponse? = null,
    val summaryText: String? = null,
    val error: String? = null,
    val sunriseHour: Int? = null,
    val sunsetHour: Int? = null
) {
    val hasData: Boolean get() = weatherResponse != null
}

/**
 * Weather tab content inside WaypointDetailSheet.
 *
 * iOS ref: WaypointWeatherView
 * Displays forecast grouped by day with Wind, Weather, and Wave charts
 * per day section.
 */
@Composable
fun WaypointWeatherContent(
    weatherState: WeatherUiState
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        when {
            weatherState.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 32.dp)
                        .size(32.dp),
                    strokeWidth = 2.dp
                )
            }

            weatherState.error != null -> {
                Text(
                    text = "Could not load forecast",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                )
            }

            !weatherState.hasData -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                ) {
                    Text(
                        text = "No forecast available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Weather data is not available for this location",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                // Summary hero
                weatherState.summaryText?.let { summary ->
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                val dayGroups = remember(weatherState.weatherResponse, weatherState.waveResponse) {
                    groupByDay(weatherState.weatherResponse!!, weatherState.waveResponse)
                }

                dayGroups.forEachIndexed { index, group ->
                    DayChartSection(
                        group = group,
                        isFirst = index == 0,
                        sunriseHour = weatherState.sunriseHour,
                        sunsetHour = weatherState.sunsetHour
                    )
                }
            }
        }
    }
}

// ── Day Grouping ────────────────────────────────────────────────────────────

private data class DayGroup(
    val date: LocalDate,
    val label: String,
    val windEntries: List<WeatherForecastEntry>,
    val weatherEntries: List<WeatherForecastEntry>,
    val waveEntries: List<WaveForecastEntry>
)

private fun groupByDay(
    weather: WeatherResponse,
    waves: WaveResponse?
): List<DayGroup> {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val tomorrow = today.plusDays(1)
    val monthDayFmt = DateTimeFormatter.ofPattern("MMMM d", Locale.getDefault())

    // Group weather forecasts by local date
    val weatherByDay = weather.forecast.groupBy { entry ->
        Instant.parse(entry.time).atZone(zone).toLocalDate()
    }

    // Group wave forecasts by local date
    val waveByDay = waves?.forecasts?.groupBy { entry ->
        Instant.parse(entry.time).atZone(zone).toLocalDate()
    } ?: emptyMap()

    val allDates = (weatherByDay.keys + waveByDay.keys).distinct().sorted()

    return allDates.map { date ->
        val label = when (date) {
            today -> "Today, ${date.format(monthDayFmt)}"
            tomorrow -> "Tomorrow, ${date.format(monthDayFmt)}"
            else -> "${date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())}, ${date.format(monthDayFmt)}"
        }
        val dayWeather = weatherByDay[date] ?: emptyList()
        DayGroup(
            date = date,
            label = label,
            windEntries = dayWeather,
            weatherEntries = dayWeather,
            waveEntries = waveByDay[date] ?: emptyList()
        )
    }
}

// ── Day Chart Section ───────────────────────────────────────────────────────

@Composable
private fun DayChartSection(
    group: DayGroup,
    isFirst: Boolean,
    sunriseHour: Int?,
    sunsetHour: Int?
) {
    val sectionHeaderStyle = MaterialTheme.typography.labelSmall.copy(
        letterSpacing = 0.5.sp
    )
    val sectionHeaderColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = Modifier.fillMaxWidth()) {
        // Day header
        Text(
            text = group.label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(
                top = if (isFirst) 0.dp else 20.dp,
                bottom = 12.dp
            )
        )

        // ── WIND section ────────────────────────────────────────────────
        if (group.windEntries.isNotEmpty()) {
            Text(
                text = "WIND",
                style = sectionHeaderStyle,
                color = sectionHeaderColor,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            WindForecastChart(
                forecasts = group.windEntries,
                sunriseHour = sunriseHour,
                sunsetHour = sunsetHour
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── WEATHER section ─────────────────────────────────────────────
        if (group.weatherEntries.isNotEmpty()) {
            Text(
                text = "WEATHER",
                style = sectionHeaderStyle,
                color = sectionHeaderColor,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            WeatherForecastChart(
                forecasts = group.weatherEntries
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── WAVES section (only if wave data exists) ────────────────────
        if (group.waveEntries.isNotEmpty()) {
            Text(
                text = "WAVES",
                style = sectionHeaderStyle,
                color = sectionHeaderColor,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            WaveForecastChart(
                forecasts = group.waveEntries
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        HorizontalDivider()
    }
}
