package com.example.saltyoffshore.data.sharelink

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Share link API models matching iOS ShareLinkModels.swift.
 *
 * iOS ref: Features/ShareLink/Types/ShareLinkModels.swift
 */

/**
 * Payload sent to POST /share-link/ to create a shareable link.
 */
@Serializable
data class ShareLinkPayload(
    @SerialName("region_id") val regionId: String,
    @SerialName("dataset_id") val datasetId: String,
    @SerialName("entry_id") val entryId: String? = null,
    val timestamp: String,
    val view: ShareLinkView? = null,
    @SerialName("primary_config") val primaryConfig: ShareLinkLayerConfig? = null,
    val overlays: List<ShareLinkOverlayConfig>? = null
)

/**
 * Camera/viewport configuration for share link.
 */
@Serializable
data class ShareLinkView(
    @SerialName("center_longitude") val centerLongitude: Double,
    @SerialName("center_latitude") val centerLatitude: Double,
    val zoom: Double,
    val bearing: Double = 0.0,
    val pitch: Double = 0.0
)

/**
 * Layer configuration for share link - JSON-safe primitives only.
 */
@Serializable
data class ShareLinkLayerConfig(
    @SerialName("dataset_id") val datasetId: String,
    @SerialName("colorscale_id") val colorscaleId: String? = null,
    @SerialName("custom_range_min") val customRangeMin: Double? = null,
    @SerialName("custom_range_max") val customRangeMax: Double? = null,
    @SerialName("filter_mode") val filterMode: String = "hideShow",
    @SerialName("visual_enabled") val visualEnabled: Boolean = true,
    @SerialName("visual_opacity") val visualOpacity: Double = 1.0,
    @SerialName("contour_enabled") val contourEnabled: Boolean = false,
    @SerialName("contour_opacity") val contourOpacity: Double = 1.0,
    @SerialName("selected_depth") val selectedDepth: Int? = null
)

/**
 * Overlay layer in share link.
 */
@Serializable
data class ShareLinkOverlayConfig(
    @SerialName("dataset_type") val datasetType: String,
    val config: ShareLinkLayerConfig,
    @SerialName("entry_id") val entryId: String? = null,
    val depth: Int? = null
)

/**
 * Response from POST /share-link/ with the generated URL.
 */
@Serializable
data class ShareLinkCreateResponse(
    val url: String,
    @SerialName("link_id") val linkId: String
)

/**
 * Response from GET /share-link/{id} with full resolved data.
 */
@Serializable
data class ShareLinkResolveResponse(
    val id: String,
    @SerialName("region_id") val regionId: String,
    @SerialName("dataset_id") val datasetId: String,
    @SerialName("entry_id") val entryId: String? = null,
    val timestamp: String? = null,
    val view: ShareLinkView? = null,
    @SerialName("primary_config") val primaryConfig: ShareLinkLayerConfig? = null,
    val overlays: List<ShareLinkOverlayConfig>? = null
)

/**
 * Errors from share link operations.
 */
sealed class ShareLinkError : Exception() {
    data object NetworkError : ShareLinkError() {
        override val message = "Check your internet connection"
    }
    data object EncodingError : ShareLinkError() {
        override val message = "Failed to encode share link"
    }
    data object ServerError : ShareLinkError() {
        override val message = "Server error creating share link"
    }
    data object NotFound : ShareLinkError() {
        override val message = "Share link not found"
    }
}
