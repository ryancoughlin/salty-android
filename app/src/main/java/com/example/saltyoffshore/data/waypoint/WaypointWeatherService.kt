package com.example.saltyoffshore.data.waypoint

import com.example.saltyoffshore.config.AppConstants
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Service for fetching weather and wave forecasts at waypoint locations.
 *
 * iOS ref: WaypointConditionsService (weather/wave portion)
 * Endpoints:
 *   GET /weather/forecast?latitude={lat}&longitude={lon}
 *   GET /waves/forecast?latitude={lat}&longitude={lon}
 *   GET /weather/summary?latitude={lat}&longitude={lon}
 */
object WaypointWeatherService {

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    /** Fetch 7-day weather forecast. */
    suspend fun fetchWeather(lat: Double, lon: Double): WeatherResponse {
        return client.get("${AppConstants.apiBaseURL}/weather/forecast") {
            parameter("latitude", lat)
            parameter("longitude", lon)
        }.body()
    }

    /** Fetch 7-day wave forecast. */
    suspend fun fetchWaves(lat: Double, lon: Double): WaveResponse {
        return client.get("${AppConstants.apiBaseURL}/waves/forecast") {
            parameter("latitude", lat)
            parameter("longitude", lon)
        }.body()
    }

    /** Fetch plain-text weather summary. */
    suspend fun fetchSummary(lat: Double, lon: Double): WeatherSummaryResponse {
        return client.get("${AppConstants.apiBaseURL}/weather/summary") {
            parameter("latitude", lat)
            parameter("longitude", lon)
        }.body()
    }
}

// ── Weather API Response Models ─────────────────────────────────────────────

/**
 * Response from /weather/forecast.
 * iOS ref: WeatherAPIResponse
 */
@Serializable
data class WeatherResponse(
    val latitude: Double,
    val longitude: Double,
    val summary: WeatherSummary? = null,
    @SerialName("current_conditions") val currentConditions: WeatherForecastEntry,
    val forecast: List<WeatherForecastEntry>,
    @SerialName("model_run") val modelRun: String,
    @SerialName("last_updated") val lastUpdated: String
)

@Serializable
data class WeatherSummary(
    @SerialName("current_condition") val currentCondition: String? = null,
    @SerialName("temperature_trend") val temperatureTrend: String? = null,
    @SerialName("wind_trend") val windTrend: String? = null,
    @SerialName("precipitation_outlook") val precipitationOutlook: String? = null,
    @SerialName("notable_changes") val notableChanges: String? = null,
    @SerialName("dominant_pattern") val dominantPattern: String? = null
)

@Serializable
data class WeatherForecastEntry(
    val time: String,
    val wind: Wind,
    val weather: Weather? = null,
    val precipitation: Precipitation? = null,
    val temperature: Temperature? = null,
    val uv: UV? = null,
    val atmospheric: Atmospheric? = null
)

@Serializable
data class Wind(
    val speed: Double,
    val direction: Double,
    val gust: Double
)

@Serializable
data class Weather(
    val condition: String,
    @SerialName("cloud_cover") val cloudCover: Double,
    val visibility: Double
)

@Serializable
data class Precipitation(
    val type: String,
    val rate: Double,
    val probability: Double? = null,
    @SerialName("accumulation_1hr") val accumulation1hr: Double? = null
)

@Serializable
data class Temperature(
    @SerialName("air_temp") val airTemp: Double,
    @SerialName("dew_point") val dewPoint: Double
)

@Serializable
data class UV(
    @SerialName("uv_index") val uvIndex: Double,
    @SerialName("uv_level") val uvLevel: String
)

@Serializable
data class Atmospheric(
    val pressure: Double,
    val humidity: Double
)

// ── Wave API Response Models ────────────────────────────────────────────────

/**
 * Response from /waves/forecast.
 * iOS ref: WaveAPIResponse
 */
@Serializable
data class WaveResponse(
    val latitude: Double,
    val longitude: Double,
    val forecasts: List<WaveForecastEntry>,
    @SerialName("model_run") val modelRun: String? = null,
    @SerialName("last_updated") val lastUpdated: String? = null
)

@Serializable
data class WaveForecastEntry(
    val time: String,
    val height: Double,
    val period: Double,
    val direction: Double
)

// ── Weather Summary Response ────────────────────────────────────────────────

/**
 * Response from /weather/summary.
 * iOS ref: WeatherSummaryResponse
 */
@Serializable
data class WeatherSummaryResponse(
    val location: SummaryLocation,
    val summary: String
)

@Serializable
data class SummaryLocation(
    val latitude: Double,
    val longitude: Double
)
