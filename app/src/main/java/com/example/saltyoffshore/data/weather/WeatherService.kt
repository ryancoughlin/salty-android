package com.example.saltyoffshore.data.weather

import android.util.Log
import com.example.saltyoffshore.config.AppConstants
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Weather and wave forecast service.
 * Matches iOS OceanDataService + WaypointConditionsService weather endpoints.
 */
object WeatherService {

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    /** Fetch weather forecast for a coordinate. */
    suspend fun fetchWeather(lat: Double, lon: Double): WeatherAPIResponse {
        return client.get("${AppConstants.apiBaseURL}/weather/forecast") {
            parameter("latitude", lat)
            parameter("longitude", lon)
        }.body()
    }

    /** Fetch wave forecast for a coordinate. */
    suspend fun fetchWaves(lat: Double, lon: Double): WaveAPIResponse {
        return client.get("${AppConstants.apiBaseURL}/waves/forecast") {
            parameter("latitude", lat)
            parameter("longitude", lon)
        }.body()
    }

    /** Fetch weather summary text for a coordinate. */
    suspend fun fetchWeatherSummary(lat: Double, lon: Double): WeatherSummaryResponse {
        return client.get("${AppConstants.apiBaseURL}/weather/summary") {
            parameter("latitude", lat)
            parameter("longitude", lon)
        }.body()
    }

    /** Convenience: fetch weather + waves and combine into domain model. */
    suspend fun fetchWeatherData(lat: Double, lon: Double): WeatherData {
        Log.d(TAG, "fetchWeatherData: Calling /weather/forecast and /waves/forecast for ($lat, $lon)")
        val weatherResponse = fetchWeather(lat, lon)
        Log.d(TAG, "fetchWeatherData: Weather API returned ${weatherResponse.forecast.size} forecast entries, modelRun=${weatherResponse.modelRun}")
        val waveResponse = fetchWaves(lat, lon)
        Log.d(TAG, "fetchWeatherData: Wave API returned ${waveResponse.forecasts.size} wave entries")
        val waveConditions = waveResponse.forecasts.map { it.toWaveConditions() }
        return weatherResponse.toWeatherData(waveConditions)
    }

    private const val TAG = "WeatherService"
}
