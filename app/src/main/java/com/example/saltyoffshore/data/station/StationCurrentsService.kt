package com.example.saltyoffshore.data.station

import com.example.saltyoffshore.config.AppConstants
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Fetches station currents data.
 * Returns null on 404 (station has no currents sensor).
 * Matches iOS StationCurrentsService.
 */
object StationCurrentsService {

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    suspend fun fetchCurrents(stationId: String): StationCurrents? {
        val response: HttpResponse = client.get(
            "${AppConstants.apiBaseURL}/stations/$stationId/currents"
        )
        // 404 means station has no currents sensor — return null, not error
        if (response.status.value == 404) return null
        return response.body()
    }
}
