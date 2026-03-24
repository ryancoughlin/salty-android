package com.example.saltyoffshore.ui.station

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.ui.station.charts.WindSparkLineChart
import com.example.saltyoffshore.viewmodel.StationDetailViewModel

/**
 * Station detail sheet — hero section + data section.
 * Matches iOS StationDetailsView.
 */
@Composable
fun StationDetailsView(
    stationId: String,
    viewModel: StationDetailViewModel,
    modifier: Modifier = Modifier
) {
    // Load data when stationId changes
    LaunchedEffect(stationId) {
        viewModel.loadData(stationId)
    }

    val observation = viewModel.observation(stationId)
    val weather = viewModel.weather(stationId)
    val currents = viewModel.currents(stationId)
    val truncatedForecasts = viewModel.truncatedForecasts(stationId)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        // MARK: - Hero Section
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(top = 24.dp)
        ) {
            // Station header: name + ID
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = observation?.name ?: "Station Name",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stationId,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Summary text
            StationSummaryView(
                summaryText = viewModel.summaryText(stationId)
            )

            // Wind sparkline (5-day trend)
            if (truncatedForecasts.isNotEmpty()) {
                WindSparkLineChart(forecasts = truncatedForecasts)
            }
        }

        Spacer(Modifier.height(32.dp))

        // MARK: - Data Section
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            // Current conditions
            StationConditionsView(
                observation = observation?.observations,
                isLoading = viewModel.isObservationLoading(stationId)
            )

            // Real-time currents (only if station has currents data)
            StationCurrentsSection(
                currents = currents,
                isLoading = viewModel.isLoading
            )

            // 5-day forecast (charts + table)
            StationForecastView(
                weatherData = weather,
                isLoading = viewModel.isWeatherLoading(stationId)
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}
