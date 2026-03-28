package com.example.saltyoffshore.data

import com.example.saltyoffshore.config.AppConstants
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object SaltyApi {

    internal val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    suspend fun getRegions(): RegionsResponse {
        return client.get("${AppConstants.apiBaseURL}/regions").body()
    }

    suspend fun fetchRegion(regionId: String): RegionMetadata {
        return client.get("${AppConstants.apiBaseURL}/v2/region/$regionId").body()
    }

    /**
     * Fetch entries for a single dataset on demand.
     * Returns a Dataset with populated entries list.
     *
     * iOS ref: OceanDataService.fetchDatasetEntries(regionId:datasetId:)
     */
    suspend fun fetchDatasetEntries(regionId: String, datasetId: String): Dataset {
        return client.get("${AppConstants.apiBaseURL}/v2/region/$regionId/dataset/$datasetId").body()
    }
}
