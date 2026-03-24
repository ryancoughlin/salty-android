package com.example.saltyoffshore.data.weather

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

/**
 * API response models for weather endpoints.
 * Matches iOS WeatherAPIModels.swift.
 */
@Serializable
data class WeatherAPIResponse(
    val latitude: Double,
    val longitude: Double,
    val summary: WeatherSummary? = null,
    @SerialName("current_conditions")
    val currentConditions: APICurrentConditions,
    val forecast: List<APIForecastConditions>,
    @SerialName("model_run")
    val modelRun: String = "",
    @SerialName("last_updated")
    val lastUpdated: String = ""
) {
    fun toWeatherData(waveConditions: List<WaveConditions> = emptyList()): WeatherData {
        return WeatherData(
            location = WeatherLocation(latitude, longitude),
            currentConditions = currentConditions.toWeatherConditions(),
            forecast = forecast.map { it.toWeatherConditions() },
            waveForecast = waveConditions
        )
    }

    @Serializable
    data class WeatherSummary(
        @SerialName("current_condition")
        val currentCondition: String? = null,
        @SerialName("temperature_trend")
        val temperatureTrend: String? = null,
        @SerialName("wind_trend")
        val windTrend: String? = null,
        @SerialName("precipitation_outlook")
        val precipitationOutlook: String? = null,
        @SerialName("notable_changes")
        val notableChanges: String? = null,
        @SerialName("dominant_pattern")
        val dominantPattern: String? = null
    )
}

@Serializable
data class APICurrentConditions(
    val time: String,
    val wind: APIWind,
    val weather: APIWeather? = null,
    val precipitation: APIPrecipitation? = null,
    val temperature: APITemperature? = null,
    val uv: APIUV? = null,
    val atmospheric: APIAtmospheric? = null
) {
    fun toWeatherConditions(): WeatherConditions = WeatherConditions(
        time = Instant.parse(time),
        wind = WindData(wind.speed, wind.direction, wind.gust),
        weather = weather?.let { WeatherInfo(it.condition, it.cloudCover, it.visibility) }
            ?: WeatherInfo("Unknown", 0.0, 10.0),
        precipitation = precipitation?.let {
            PrecipitationInfo(it.type, it.rate, it.probability, it.accumulation1hr)
        } ?: PrecipitationInfo("none", 0.0, null, null),
        temperature = temperature?.let { TemperatureInfo(it.airTemp, it.dewPoint) }
            ?: TemperatureInfo(0.0, 0.0),
        uv = uv?.let { UVInfo(it.uvIndex, it.uvLevel) } ?: UVInfo(0.0, "low"),
        atmospheric = atmospheric?.let { AtmosphericInfo(it.pressure, it.humidity) }
            ?: AtmosphericInfo(30.0, 50.0)
    )
}

@Serializable
data class APIForecastConditions(
    val time: String,
    val wind: APIWind,
    val weather: APIWeather? = null,
    val precipitation: APIPrecipitation? = null,
    val temperature: APITemperature? = null,
    val uv: APIUV? = null,
    val atmospheric: APIAtmospheric? = null
) {
    fun toWeatherConditions(): WeatherConditions = WeatherConditions(
        time = Instant.parse(time),
        wind = WindData(wind.speed, wind.direction, wind.gust),
        weather = weather?.let { WeatherInfo(it.condition, it.cloudCover, it.visibility) }
            ?: WeatherInfo("Unknown", 0.0, 10.0),
        precipitation = precipitation?.let {
            PrecipitationInfo(it.type, it.rate, it.probability, it.accumulation1hr)
        } ?: PrecipitationInfo("none", 0.0, null, null),
        temperature = temperature?.let { TemperatureInfo(it.airTemp, it.dewPoint) }
            ?: TemperatureInfo(0.0, 0.0),
        uv = uv?.let { UVInfo(it.uvIndex, it.uvLevel) } ?: UVInfo(0.0, "low"),
        atmospheric = atmospheric?.let { AtmosphericInfo(it.pressure, it.humidity) }
            ?: AtmosphericInfo(30.0, 50.0)
    )
}

@Serializable
data class APIWind(val speed: Double = 0.0, val direction: Double = 0.0, val gust: Double = 0.0)

@Serializable
data class APIWeather(
    val condition: String = "unknown",
    @SerialName("cloud_cover")
    val cloudCover: Double = 0.0,
    val visibility: Double = 0.0
)

@Serializable
data class APIPrecipitation(
    val type: String = "none",
    val rate: Double = 0.0,
    val probability: Double? = null,
    @SerialName("accumulation_1hr")
    val accumulation1hr: Double? = null
)

@Serializable
data class APITemperature(
    @SerialName("air_temp")
    val airTemp: Double = 0.0,
    @SerialName("dew_point")
    val dewPoint: Double = 0.0
)

@Serializable
data class APIUV(
    @SerialName("uv_index")
    val uvIndex: Double = 0.0,
    @SerialName("uv_level")
    val uvLevel: String = "low"
)

@Serializable
data class APIAtmospheric(val pressure: Double = 30.0, val humidity: Double = 50.0)

@Serializable
data class WaveAPIResponse(
    val latitude: Double,
    val longitude: Double,
    @SerialName("model_run")
    val modelRun: String? = null,
    @SerialName("last_updated")
    val lastUpdated: String? = null,
    val forecasts: List<WaveConditionsResponse>
)

@Serializable
data class WaveConditionsResponse(
    val time: String,
    val height: Double,
    val period: Double,
    val direction: Double
) {
    fun toWaveConditions(): WaveConditions = WaveConditions(
        time = Instant.parse(time),
        wave = WaveData(height, period, direction)
    )
}

@Serializable
data class WeatherSummaryResponse(
    val location: SummaryLocation,
    val summary: String
) {
    @Serializable
    data class SummaryLocation(
        val latitude: Double,
        val longitude: Double
    )
}
