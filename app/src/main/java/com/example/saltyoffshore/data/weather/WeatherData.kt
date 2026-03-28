package com.example.saltyoffshore.data.weather

import com.example.saltyoffshore.data.station.MarineUnits
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/**
 * Domain weather models. Matches iOS WeatherData.swift.
 */
data class WeatherData(
    val id: String = UUID.randomUUID().toString(),
    val location: WeatherLocation,
    val currentConditions: WeatherConditions,
    val forecast: List<WeatherConditions>,
    val waveForecast: List<WaveConditions>
) {
    val dayOverviews: List<DayOverview>
        get() {
            val days = ForecastDataFilter.fiveDayArray()
            return days.map { day ->
                DayOverview(
                    date = day,
                    windForecasts = forecast.filter {
                        it.time.atZone(ZoneId.systemDefault()).toLocalDate() == day
                    },
                    waveForecasts = waveForecast.filter {
                        it.time.atZone(ZoneId.systemDefault()).toLocalDate() == day
                    }
                )
            }.filter { it.windForecasts.isNotEmpty() || it.waveForecasts.isNotEmpty() }
        }
}

data class WeatherLocation(
    val latitude: Double,
    val longitude: Double
)

data class WeatherConditions(
    val id: String = UUID.randomUUID().toString(),
    val time: Instant,
    val wind: WindData,
    val weather: WeatherInfo,
    val precipitation: PrecipitationInfo,
    val temperature: TemperatureInfo,
    val uv: UVInfo,
    val atmospheric: AtmosphericInfo
) {
    val speed: Double get() = wind.speed
    val direction: Double get() = wind.direction
}

data class WindData(
    val speed: Double,      // mph
    val direction: Double,  // degrees
    val gust: Double        // mph
) {
    val speedFormatted: String get() = String.format("%.0f mph", speed)
    val gustFormatted: String get() = String.format("%.0f mph", gust)
    val directionCardinal: String get() = MarineUnits.cardinalDirection(direction)
}

data class WeatherInfo(
    val condition: String,
    val cloudCover: Double,
    val visibility: Double
)

data class PrecipitationInfo(
    val type: String,
    val rate: Double,
    val probability: Double?,
    val accumulation1hr: Double?
)

data class TemperatureInfo(
    val airTemp: Double,
    val dewPoint: Double
) {
    val airTempFormatted: String get() = "${airTemp.toInt()}°F"
    val dewPointFormatted: String get() = "${dewPoint.toInt()}°F"
}

data class UVInfo(
    val uvIndex: Double,
    val uvLevel: String
)

data class AtmosphericInfo(
    val pressure: Double,
    val humidity: Double
) {
    val pressureFormatted: String get() = String.format("%.2f inHg", pressure)
    val humidityFormatted: String get() = String.format("%.0f%%", humidity)
}
