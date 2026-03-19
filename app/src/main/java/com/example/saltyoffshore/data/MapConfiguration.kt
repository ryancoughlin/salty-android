package com.example.saltyoffshore.data

import kotlinx.serialization.Serializable

/**
 * Portable map state - the "save file format" for a complete map view.
 *
 * Source-agnostic: can be created from share links, saved maps, crew pushes, etc.
 * Apply via ViewModel.apply() to restore full map state.
 *
 * ## Lego Block Order (applied sequentially):
 * 1. Region
 * 2. Dataset + Entry
 * 3. Primary Config (render settings)
 * 4. Overlays (additional datasets)
 * 5. Camera (viewport position)
 */
@Serializable
data class MapConfiguration(
    // Block 1: Region
    val regionId: String,

    // Block 2: Dataset + Entry
    val datasetId: String,
    val timestamp: String,  // ISO 8601 - resolved to TimeEntry at apply time
    val entryId: String? = null,  // UUID from API - used for direct share link generation

    // Block 3: Primary Config
    val primaryConfig: LayerConfig? = null,

    // Block 4: Overlays
    val overlays: List<OverlayConfig>? = null,

    // Block 5: Camera
    val camera: CameraConfig? = null
) {
    /**
     * Short display name for the primary dataset (e.g., "SST", "CUR").
     */
    val datasetShortName: String
        get() = DatasetType.fromRawValue(datasetId)?.shortName
            ?: datasetId.take(3).uppercase()

    /**
     * Render settings for a single dataset layer.
     * Maps 1:1 with DatasetRenderConfig but uses JSON-safe primitives.
     */
    @Serializable
    data class LayerConfig(
        val datasetId: String,
        val colorscaleId: String? = null,
        val customRangeMin: Double? = null,
        val customRangeMax: Double? = null,
        val filterMode: String,  // "hideShow" | "squash"

        // Visual layer
        val visualEnabled: Boolean,
        val visualOpacity: Double,

        // Contour layer
        val contourEnabled: Boolean,
        val contourOpacity: Double,
        val contourColorHex: String? = null,
        val dynamicContourColoring: Boolean? = null,

        // Arrows (currents)
        val arrowsEnabled: Boolean? = null,
        val arrowsOpacity: Double? = null,

        // Particles (currents)
        val particlesEnabled: Boolean? = null,

        // Breaks (FSLE)
        val breaksEnabled: Boolean? = null,
        val breaksOpacity: Double? = null,

        // Numbers
        val numbersEnabled: Boolean? = null,
        val numbersOpacity: Double? = null,

        // Depth (dissolved oxygen)
        val selectedDepth: Int? = null
    ) {
        /**
         * Short display name for the dataset (e.g., "SST", "CUR").
         * Uses canonical DatasetType.shortName when possible.
         */
        val datasetShortName: String
            get() = DatasetType.fromRawValue(datasetId)?.shortName
                ?: datasetId.take(3).uppercase()

        /**
         * Human-readable names of active layer toggles (e.g., ["Contour", "Arrows"]).
         */
        val activeLayerNames: List<String>
            get() = buildList {
                if (contourEnabled) add("Contour")
                if (arrowsEnabled == true) add("Arrows")
                if (particlesEnabled == true) add("Particles")
                if (numbersEnabled == true) add("Numbers")
                if (breaksEnabled == true) add("Breaks")
            }

        /**
         * Resolve customRange from min/max doubles.
         */
        fun resolveCustomRange(): ClosedFloatingPointRange<Double>? {
            val min = customRangeMin ?: return null
            val max = customRangeMax ?: return null
            return min..max
        }

        /**
         * Returns a copy with the specified depth.
         */
        fun withDepth(depth: Int): LayerConfig = copy(selectedDepth = depth)
    }

    /**
     * An overlay dataset with its own render config and optional entry override.
     */
    @Serializable
    data class OverlayConfig(
        val datasetType: String,  // DatasetType rawValue
        val config: LayerConfig,
        val entryId: String? = null,  // SST time sync override
        val depth: Int? = null  // Dissolved oxygen depth
    ) {
        /**
         * Short display name for the overlay dataset (e.g., "CHL", "CUR").
         */
        val datasetShortName: String
            get() = DatasetType.fromRawValue(datasetType)?.shortName
                ?: datasetType.take(3).uppercase()
    }

    /**
     * Camera position for map viewport.
     */
    @Serializable
    data class CameraConfig(
        val centerLongitude: Double,
        val centerLatitude: Double,
        val zoom: Double,
        val bearing: Double,
        val pitch: Double
    )
}

/**
 * Errors that can occur when applying a MapConfiguration.
 */
sealed class MapConfigurationError : Exception() {
    data class RegionNotLoaded(val id: String) : MapConfigurationError() {
        override val message: String = "Region '$id' could not be loaded"
    }

    data class DatasetNotFound(val id: String) : MapConfigurationError() {
        override val message: String = "Dataset '$id' not found in region"
    }
}
