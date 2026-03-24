package com.example.saltyoffshore.data.station

import com.example.saltyoffshore.config.AppConstants
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Fetches station observation data.
 * Matches iOS StationObservationService.
 */
object StationObservationService {

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    suspend fun fetchObservation(stationId: String): StationObservation {
        return client.get("${AppConstants.apiBaseURL}/stations/$stationId/observations").body()
    }
}
