package com.example.saltyoffshore.ui.station.forecast

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.data.weather.DayOverview
import com.example.saltyoffshore.data.weather.ForecastViewMode
import com.example.saltyoffshore.data.weather.WeatherData
import com.example.saltyoffshore.ui.station.charts.WaveConditionsChart
import com.example.saltyoffshore.ui.station.charts.WindConditionsChart

/**
 * Day selector + chart or table content.
 * Matches iOS VerticalDayForecastView.
 */
@Composable
fun VerticalDayForecastView(
    weatherData: WeatherData,
    viewMode: ForecastViewMode,
    modifier: Modifier = Modifier
) {
    val dayOverviews = remember(weatherData) { weatherData.dayOverviews }
    val availableDays = remember(dayOverviews) { dayOverviews.map { it.date } }

    var selectedDay by remember(availableDays) {
        mutableStateOf(availableDays.firstOrNull() ?: java.time.LocalDate.now())
    }

    val selectedOverview = remember(dayOverviews, selectedDay) {
        dayOverviews.firstOrNull { it.date == selectedDay }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (availableDays.size > 1) {
            ForecastDaySelector(
                selectedDay = selectedDay,
                availableDays = availableDays,
                onDaySelected = { selectedDay = it }
            )
        }

        Log.d("VerticalDayForecast", "selectedDay=$selectedDay, selectedOverview=${selectedOverview != null}, availableDays=$availableDays")
        selectedOverview?.let { overview ->
            Log.d("VerticalDayForecast", "Day ${overview.date}: wind=${overview.windForecasts.size}, wave=${overview.waveForecasts.size}")
            if (viewMode == ForecastViewMode.CHART) {
                // Wave chart
                if (overview.waveForecasts.isNotEmpty()) {
                    Text("Waves", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    // Convert WaveConditions to WaveForecast.Forecast for the chart
                    // The chart expects WaveForecast.Forecast but we have WaveConditions
                    // Use WindConditionsChart pattern instead — both take domain models
                    WaveConditionsChartFromConditions(overview.waveForecasts, overview.date)
                }

                // Wind chart
                if (overview.windForecasts.isNotEmpty()) {
                    Text("Wind", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    WindConditionsChart(forecasts = weatherData.forecast, date = overview.date)
                }
            } else {
                ForecastTableView(
                    windForecasts = overview.windForecasts,
                    waveForecasts = overview.waveForecasts
                )
            }
        }
    }
}

/**
 * Adapter: renders WaveConditionsChart from WaveConditions (domain model).
 * Converts to the format the canvas chart expects.
 */
@Composable
private fun WaveConditionsChartFromConditions(
    waveConditions: List<com.example.saltyoffshore.data.weather.WaveConditions>,
    date: java.time.LocalDate
) {
    // Convert WaveConditions to WaveForecast.Forecast-like data for canvas rendering
    // Reuse the same canvas logic by creating minimal forecast-like objects
    val forecasts = remember(waveConditions) {
        waveConditions.map { wc ->
            com.example.saltyoffshore.data.station.WaveForecast.Forecast(
                time = wc.time.toString(),
                height = wc.height,
                period = wc.period,
                direction = wc.direction
            )
        }
    }
    WaveConditionsChart(forecasts = forecasts, date = date)
}
