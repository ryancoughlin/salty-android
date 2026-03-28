package com.example.saltyoffshore.data.sharelink

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Share link API models — exact match of iOS ShareLinkModels.swift wire format.
 *
 * iOS ref: Features/ShareLink/Types/ShareLinkModels.swift
 */

/**
 * Camera/viewport — center is [longitude, latitude] array to match iOS wire format.
 */
@Serializable
data class ShareLinkCameraView(
    val center: List<Double>,   // [longitude, latitude]
    val zoom: Double,
    val bearing: Double = 0.0,
    val pitch: Double = 0.0
) {
    val centerLongitude: Double get() = center.getOrElse(0) { 0.0 }
    val centerLatitude: Double get() = center.getOrElse(1) { 0.0 }

    companion object {
        fun from(longitude: Double, latitude: Double, zoom: Double, bearing: Double = 0.0, pitch: Double = 0.0) =
            ShareLinkCameraView(center = listOf(longitude, latitude), zoom = zoom, bearing = bearing, pitch = pitch)
    }
}

/**
 * Layer configuration — all fields from iOS ShareLinkDatasetConfig.
 */
@Serializable
data class ShareLinkDatasetConfig(
    @SerialName("dataset_id") val datasetId: String,
    @SerialName("colorscale_id") val colorscaleId: String? = null,
    @SerialName("custom_range") val customRange: List<Double>? = null, // [min, max]
    @SerialName("filter_mode") val filterMode: String = "hideShow",
    @SerialName("visual_enabled") val visualEnabled: Boolean = true,
    @SerialName("visual_opacity") val visualOpacity: Double = 1.0,
    @SerialName("contour_enabled") val contourEnabled: Boolean = false,
    @SerialName("contour_opacity") val contourOpacity: Double = 1.0,
    @SerialName("contour_color") val contourColor: String? = null,
    @SerialName("dynamic_contour_coloring") val dynamicContourColoring: Boolean? = null,
    @SerialName("arrows_enabled") val arrowsEnabled: Boolean? = null,
    @SerialName("arrows_opacity") val arrowsOpacity: Double? = null,
    @SerialName("breaks_enabled") val breaksEnabled: Boolean? = null,
    @SerialName("breaks_opacity") val breaksOpacity: Double? = null,
    @SerialName("numbers_enabled") val numbersEnabled: Boolean? = null,
    @SerialName("numbers_opacity") val numbersOpacity: Double? = null,
    @SerialName("particles_enabled") val particlesEnabled: Boolean? = null,
    @SerialName("selected_depth") val selectedDepth: Int? = null
)

/**
 * Overlay layer in share link.
 */
@Serializable
data class ShareLinkOverlayConfig(
    @SerialName("dataset_type") val datasetType: String,
    val config: ShareLinkDatasetConfig,
    @SerialName("entry_id") val entryId: String? = null,
    val depth: Int? = null
)

/**
 * Payload sent to POST /share-link/ — matches iOS exactly.
 * Note: no dataset_id or timestamp — those live in the entry.
 */
@Serializable
data class ShareLinkPayload(
    @SerialName("entry_id") val entryId: String,
    @SerialName("region_id") val regionId: String? = null,
    val view: ShareLinkCameraView? = null,
    @SerialName("primary_config") val primaryConfig: ShareLinkDatasetConfig? = null,
    val overlays: List<ShareLinkOverlayConfig>? = null
)

/**
 * Response from POST /share-link/.
 */
@Serializable
data class ShareLinkCreateResponse(
    val url: String,
    @SerialName("link_id") val linkId: String
)

/**
 * Nested entry in resolve response.
 */
@Serializable
data class ShareLinkEntryResponse(
    @SerialName("entry_id") val entryId: String,
    val timestamp: String
)

/**
 * Response from GET /share-link/{id} — nested structure matching iOS.
 */
@Serializable
data class ShareLinkResolveResponse(
    @SerialName("region_id") val regionId: String,
    @SerialName("dataset_id") val datasetId: String,
    @SerialName("dataset_name") val datasetName: String? = null,
    @SerialName("dataset_type") val datasetType: String? = null,
    @SerialName("region_bounds") val regionBounds: List<List<Double>>? = null,
    val view: ShareLinkCameraView? = null,
    val entry: ShareLinkEntryResponse,
    val payload: ShareLinkPayload
) {
    val timestamp: String get() = entry.timestamp
    val entryId: String get() = entry.entryId
    val primaryConfig: ShareLinkDatasetConfig? get() = payload.primaryConfig
    val overlays: List<ShareLinkOverlayConfig>? get() = payload.overlays
}

/**
 * Errors from share link operations — carries context like iOS.
 */
sealed class ShareLinkError : Exception() {
    data class NetworkError(override val cause: Throwable) : ShareLinkError() {
        override val message = "Check your internet connection"
    }
    data class EncodingError(override val cause: Throwable) : ShareLinkError() {
        override val message = "Failed to encode share link"
    }
    data class DecodingError(override val cause: Throwable) : ShareLinkError() {
        override val message = "Failed to decode share link"
    }
    data class ServerError(val statusCode: Int, val serverMessage: String?) : ShareLinkError() {
        override val message = "Server error ($statusCode)"
    }
    data object InvalidURL : ShareLinkError() {
        override val message = "Invalid share link URL"
    }
    data class UnknownError(override val cause: Throwable?) : ShareLinkError() {
        override val message = "Something went wrong"
    }

    val userFriendlyMessage: String get() = message ?: "Something went wrong"
}
