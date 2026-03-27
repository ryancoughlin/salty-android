package com.example.saltyoffshore.data.satellite

import android.util.Log
import com.example.saltyoffshore.config.AppConstants
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import kotlinx.serialization.json.Json

/**
 * Fetches satellite data from API.
 * Two base paths: /satellites (tracker + coverage) and /region (pass predictions).
 *
 * iOS ref: SatelliteService (actor)
 */
object SatelliteService {

    private const val TAG = "SatelliteService"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Fetch global satellite tracks (where are satellites now?).
     * GET /satellites/swaths
     */
    suspend fun fetchTracks(client: HttpClient): TrackerResponse {
        Log.d(TAG, "Fetching satellite tracks (global)")
        val response: String = client.get("${AppConstants.apiBaseURL}/satellites/swaths") {
            header("Cache-Control", "no-cache, no-store")
            header("Pragma", "no-cache")
        }.body()
        return json.decodeFromString(response)
    }

    /**
     * Fetch regional coverage (what data is over my water?).
     * GET /satellites/coverage?region_id={regionId}
     */
    suspend fun fetchCoverage(client: HttpClient, regionId: String): CoverageResponse {
        Log.d(TAG, "Fetching coverage for region: $regionId")
        val response: String = client.get("${AppConstants.apiBaseURL}/satellites/coverage") {
            parameter("region_id", regionId)
            header("Cache-Control", "no-cache, no-store")
            header("Pragma", "no-cache")
        }.body()
        return json.decodeFromString(response)
    }

    /**
     * Fetch satellite coverage predictions for a region.
     * GET /region/{regionId}/satellite-coverage
     */
    suspend fun fetchSatelliteCoverage(client: HttpClient, regionId: String): SatelliteCoverage {
        Log.d(TAG, "Fetching satellite coverage for region: $regionId")
        val response: String = client.get("${AppConstants.apiBaseURL}/region/$regionId/satellite-coverage") {
            header("Cache-Control", "no-cache, no-store")
            header("Pragma", "no-cache")
        }.body()
        return json.decodeFromString(response)
    }
}
