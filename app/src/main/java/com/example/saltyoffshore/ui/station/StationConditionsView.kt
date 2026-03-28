package com.example.saltyoffshore.ui.station

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saltyoffshore.data.station.MarineUnits
import com.example.saltyoffshore.data.station.ObservationData

@Composable
fun StationConditionsView(
    observation: ObservationData?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Conditions",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (observation != null) {
            ConditionsGrid(observation)

            // Last updated row
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                if (observation.dataAge.isStale) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Stale data",
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(12.dp)
                    )
                }
                val textColor = if (observation.dataAge.isStale) Color(0xFFFBBF24)
                    else MaterialTheme.colorScheme.onSurfaceVariant

                Text("Last updated", style = MaterialTheme.typography.labelSmall, color = textColor)
                Text("·", style = MaterialTheme.typography.labelSmall, color = textColor)
                Text(
                    observation.dataAge.formattedTimeAgo,
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor
                )
            }
        } else if (isLoading) {
            // Placeholder skeleton
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            )
        } else {
            Text(
                "No observation data available",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
    }
}

@Composable
private fun ConditionsGrid(observation: ObservationData) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Row 1: Wave Height | Period | Wind Speed
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ConditionItem(
                value = observation.wave.formattedHeight ?: "-",
                label = "Wave Height",
                modifier = Modifier.weight(1f)
            )
            ConditionItem(
                value = observation.wave.formattedPeriod ?: "-",
                label = "Period",
                modifier = Modifier.weight(1f)
            )
            // Wind with direction arrow
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = observation.wind.formattedWindSpeed ?: "-",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    observation.wind.direction?.let { dir ->
                        Text(
                            text = "↑",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.rotate(dir.toFloat())
                        )
                    }
                }
                Text(
                    "Wind Speed",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Row 2: Water Temp | Air Temp
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ConditionItem(
                value = observation.met.waterTemp?.let { MarineUnits.formatTemperature(it) } ?: "-",
                label = "Water Temp",
                modifier = Modifier.width(120.dp)
            )
            ConditionItem(
                value = observation.met.airTemp?.let { MarineUnits.formatTemperature(it) } ?: "-",
                label = "Air Temp",
                modifier = Modifier.width(120.dp)
            )
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun ConditionItem(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
