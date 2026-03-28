package com.example.saltyoffshore.ui.station

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.data.weather.ForecastViewMode
import com.example.saltyoffshore.data.weather.WeatherData
import com.example.saltyoffshore.ui.station.forecast.ForecastToggleView
import com.example.saltyoffshore.ui.station.forecast.VerticalDayForecastView

/**
 * Forecast section container: header + toggle + day content.
 * Matches iOS StationForecastView.
 */
@Composable
fun StationForecastView(
    weatherData: WeatherData?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    var viewMode by remember { mutableStateOf(ForecastViewMode.CHART) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Forecast",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        ForecastToggleView(
            viewMode = viewMode,
            onModeChanged = { viewMode = it }
        )

        if (weatherData != null) {
            Log.d("StationForecastView", "weatherData present: forecast=${weatherData.forecast.size}, waves=${weatherData.waveForecast.size}, dayOverviews=${weatherData.dayOverviews.size}")
            VerticalDayForecastView(
                weatherData = weatherData,
                viewMode = viewMode
            )
        } else if (isLoading) {
            Log.d("StationForecastView", "weatherData is null, isLoading=true")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }
    }
}
