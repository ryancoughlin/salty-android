package com.example.saltyoffshore.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.saltyoffshore.data.satellite.CoverageSummary
import com.example.saltyoffshore.data.satellite.RegionalPass
import com.example.saltyoffshore.data.satellite.SatelliteCoverage
import com.example.saltyoffshore.data.satellite.SatelliteService
import com.example.saltyoffshore.data.satellite.SatelliteTrack
import io.ktor.client.HttpClient
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Unified store for all satellite data.
 *
 * Store Pattern: View -> Store -> Service
 * - Holds all satellite state (tracks, passes, predictions)
 * - View orchestrates loading via LaunchedEffect
 * - Service is stateless I/O
 *
 * iOS ref: SatelliteStore (@Observable class)
 */
class SatelliteStore(private val client: HttpClient) {

    // MARK: - State

    /** Global satellite tracks (tracker mode) */
    var tracks by mutableStateOf<List<SatelliteTrack>>(emptyList())
        private set

    /** Regional passes — recent satellite passes (coverage mode) */
    var passes by mutableStateOf<List<RegionalPass>>(emptyList())
        private set

    /** Coverage summary stats */
    var summary by mutableStateOf<CoverageSummary?>(null)
        private set

    /** Pass predictions — upcoming satellites (coverage mode) */
    var predictions by mutableStateOf<SatelliteCoverage?>(null)
        private set

    /** Loading state */
    var isLoading by mutableStateOf(false)
        private set

    /** Last error message */
    var error by mutableStateOf<String?>(null)
        private set

    // MARK: - Actions

    /** Load global satellite tracks */
    suspend fun loadTracks() {
        isLoading = true
        error = null
        try {
            val response = SatelliteService.fetchTracks(client)
            tracks = response.satellites
            Log.d(TAG, "Loaded ${tracks.size} satellite tracks")
        } catch (e: Exception) {
            error = e.message
            tracks = emptyList()
            Log.e(TAG, "Failed to load tracks: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    /** Load regional coverage data (passes + predictions in parallel) */
    suspend fun loadCoverage(regionId: String) = coroutineScope {
        isLoading = true
        error = null
        try {
            val passesDeferred = async { SatelliteService.fetchCoverage(client, regionId) }
            val predictionsDeferred = async { SatelliteService.fetchSatelliteCoverage(client, regionId) }

            // Passes are required — failure is an error
            try {
                val coverageResponse = passesDeferred.await()
                summary = coverageResponse.summary
                passes = coverageResponse.passes
                Log.d(TAG, "Loaded ${passes.size} regional passes")
            } catch (e: Exception) {
                error = e.message
                summary = null
                passes = emptyList()
                Log.e(TAG, "Failed to load passes: ${e.message}")
            }

            // Predictions are supplemental — log but don't fail overall load
            try {
                predictions = predictionsDeferred.await()
                Log.d(TAG, "Loaded predictions for $regionId")
            } catch (e: Exception) {
                predictions = null
                Log.w(TAG, "Failed to load predictions: ${e.message}")
            }
        } finally {
            isLoading = false
        }
    }

    /** Clear all data */
    fun clear() {
        tracks = emptyList()
        passes = emptyList()
        summary = null
        predictions = null
        error = null
    }

    companion object {
        private const val TAG = "SatelliteStore"
    }
}
