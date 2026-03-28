package com.example.saltyoffshore.data.sharelink

import com.example.saltyoffshore.config.AppConstants
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Share link API service matching iOS ShareLinkService.swift.
 * Creates and resolves shareable map configuration URLs.
 *
 * iOS ref: Services/ShareLinkService.swift
 */
object ShareLinkService {

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            })
        }
    }

    /**
     * Create a share link from a map configuration payload.
     * Returns the generated URL and link ID.
     */
    suspend fun createShareLink(payload: ShareLinkPayload): ShareLinkCreateResponse {
        return client.post("${AppConstants.apiBaseURL}/share-link/") {
            contentType(ContentType.Application.Json)
            setBody(payload)
        }.body()
    }

    /**
     * Resolve a share link ID to its full map configuration.
     */
    suspend fun resolveShareLink(linkId: String): ShareLinkResolveResponse {
        return client.get("${AppConstants.apiBaseURL}/share-link/$linkId").body()
    }
}
