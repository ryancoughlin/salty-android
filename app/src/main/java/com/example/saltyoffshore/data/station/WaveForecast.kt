package com.example.saltyoffshore.data.station

import com.example.saltyoffshore.data.weather.WaveData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Station-specific wave forecast from /waves/{stationId}/forecast.
 * Matches iOS WaveForecast.
 */
@Serializable
data class WaveForecast(
    val station: Station,
    @SerialName("forecasts")
    private val _forecasts: List<Forecast>,
    @SerialName("model_run")
    val modelRun: String
) {
    val forecasts: List<Forecast>
        get() = _forecasts.sortedBy { it.time }

    @Serializable
    data class Station(
        val id: String,
        val name: String,
        val location: LocationGeometry,
        val type: String
    )

    @Serializable
    data class LocationGeometry(
        val type: String,
        val coordinates: List<Double>
    )

    @Serializable
    data class Forecast(
        val time: String,
        val height: Double,
        val period: Double,
        val direction: Double
    ) {
        val parsedTime: Instant get() = Instant.parse(time)

        val wave: WaveData get() = WaveData(height, period, direction)
        val formattedHeight: String get() = wave.heightFormatted
    }
}
