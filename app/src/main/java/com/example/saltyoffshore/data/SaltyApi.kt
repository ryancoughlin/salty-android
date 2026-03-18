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

    private val client = HttpClient(OkHttp) {
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
        return client.get("${AppConstants.apiBaseURL}/region/$regionId").body()
    }
}
