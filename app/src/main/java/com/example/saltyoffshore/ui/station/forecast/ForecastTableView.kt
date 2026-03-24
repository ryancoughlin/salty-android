package com.example.saltyoffshore.ui.station.forecast

import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saltyoffshore.data.weather.WeatherConditions
import com.example.saltyoffshore.data.weather.WaveConditions
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Forecast table view combining wind + wave data.
 * Matches iOS ForecastTableView.
 */
@Composable
fun ForecastTableView(
    windForecasts: List<WeatherConditions>,
    waveForecasts: List<WaveConditions>,
    modifier: Modifier = Modifier
) {
    val combined = remember(windForecasts, waveForecasts) {
        val waveByTime = waveForecasts.associateBy { it.time }
        windForecasts.map { wind ->
            Triple(wind.time, wind, waveByTime[wind.time])
        }
    }

    val timeFormatter = remember {
        DateTimeFormatter.ofPattern("ha").withZone(ZoneId.systemDefault())
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TableHeader("Time", Modifier.width(48.dp))
            TableHeader("Wind", Modifier.width(56.dp))
            TableHeader("Gust", Modifier.width(48.dp))
            TableHeader("Waves", Modifier.width(48.dp))
            TableHeader("Per", Modifier.width(40.dp))
            TableHeader("Dir", Modifier.width(40.dp))
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        combined.forEach { (time, wind, wave) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    timeFormatter.format(time).lowercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(48.dp)
                )
                Row(
                    modifier = Modifier.width(56.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        String.format("%.0f", wind.speed),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "↑",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.rotate(wind.direction.toFloat())
                    )
                }
                Text(
                    String.format("%.0f", wind.wind.gust),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(48.dp)
                )
                Text(
                    wave?.wave?.heightFormatted ?: "-",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.width(48.dp)
                )
                Text(
                    wave?.wave?.periodFormatted ?: "-",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(40.dp)
                )
                Text(
                    wave?.wave?.directionCardinal ?: "-",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(40.dp)
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun TableHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}
