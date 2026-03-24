package com.example.saltyoffshore.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Loads station list from bundled JSON file.
 * Matches iOS StationService.
 */
object StationListService {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchStations(context: Context): List<Station> = withContext(Dispatchers.IO) {
        val jsonString = context.assets.open("ndbcstations.json")
            .bufferedReader()
            .use { it.readText() }
        json.decodeFromString<List<Station>>(jsonString)
    }
}
