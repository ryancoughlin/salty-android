package com.example.saltyoffshore.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegionMetadata(
    @SerialName("region_id") val id: String,
    val name: String,
    val bounds: List<List<Double>>,
    val datasets: List<Dataset> = emptyList(),
    @SerialName("cache_version") val cacheVersion: String? = null,
    @SerialName("last_updated") val lastUpdated: String? = null,
    val status: String = "active"
) {
    /** v2 API returns all datasets (entries loaded lazily per-dataset); show all datasets */
    val activeDatasets: List<Dataset>
        get() = datasets
}

@Serializable
data class Dataset(
    val id: String,
    val name: String,
    val type: String,
    @SerialName("source_type") val sourceType: String? = null,
    val composite: Boolean? = false,
    val metadata: DatasetMetadata? = null,
    val entries: List<TimeEntry>? = null,
    val beta: Boolean? = false,
    @SerialName("supports_depth_selection") val supportsDepthSelection: Boolean? = false,
    @SerialName("available_depths") val availableDepths: List<Int>? = null,
    @SerialName("zarr_url") val zarrUrl: String? = null,
    @SerialName("preview_url") val previewUrl: String? = null
) {
    /** Most recent entry by timestamp (matches iOS Dataset.mostRecentEntry) */
    val mostRecentEntry: TimeEntry?
        get() = entries?.maxByOrNull { it.timestamp }

    /** Single source of truth for depth UI: show picker when multiple depths available */
    val hasMultipleDepths: Boolean
        get() = (availableDepths?.size ?: 0) > 1

    /** Depths available for this dataset (defaults to surface) */
    val depths: List<Int>
        get() = availableDepths ?: listOf(0)

    /** Whether this dataset is a composite — dataset-level flag or any composite entry */
    val isCombinedDataset: Boolean
        get() = composite == true || entries?.any { it.isComposite } == true

    /** All unique sensors across entries (matches iOS Dataset.uniqueSensors) */
    val uniqueSensors: List<String>
        get() = entries?.mapNotNull { it.sourceMetadata?.sensor }?.distinct() ?: emptyList()

    // MARK: - Layer Availability (matches iOS Dataset capabilities)

    private val datasetType: DatasetType?
        get() = DatasetType.fromRawValue(type)

    /** Whether this dataset supports visual layer (COG raster) */
    val hasVisualLayer: Boolean
        get() = true // All datasets have visual layer

    /** Whether this dataset supports contour lines */
    val hasContours: Boolean
        get() = true // Most datasets have contours

    /** Whether this dataset supports breaks (thermal fronts) */
    val hasBreaks: Boolean
        get() = datasetType?.supportsFronts ?: false

    /** Whether this dataset supports arrows (currents) */
    val hasArrows: Boolean
        get() = datasetType == DatasetType.CURRENTS

    /** Whether this dataset supports particle animation (currents) */
    val hasParticles: Boolean
        get() = datasetType == DatasetType.CURRENTS

    /** Whether this dataset supports numbers layer */
    val hasNumbers: Boolean
        get() = datasetType != DatasetType.WATER_TYPE
}

@Serializable
data class DatasetMetadata(
    @SerialName("cloud_free") val cloudFree: Boolean? = true,
    val frequency: String? = null,
    val resolution: String? = null,
    val description: String? = null,
    val lag: String? = null,
    val sensor: String? = null,
    @SerialName("high_resolution") val highResolution: Boolean? = false,
    @SerialName("processing_delay") val processingDelay: String? = null,
    @SerialName("time_of_day") val timeOfDay: String? = null,
    @SerialName("temporal_coverage") val temporalCoverage: String? = null
)

@Serializable
data class TimeEntry(
    @SerialName("entry_id") val id: String,
    val timestamp: String,
    val depth: Int = 0,
    val layers: LayerData,
    val ranges: Map<String, DataRange>? = null,
    @SerialName("data_coverage_percentage") val dataCoveragePercentage: Double? = null,
    @SerialName("source_dataset") val sourceDataset: String? = null,
    @SerialName("source_metadata") val sourceMetadata: SourceMetadata? = null,
    @SerialName("preview_url") val previewUrl: String? = null,
    val composite: Boolean? = false
) {
    /** Whether this entry is a composite dataset */
    val isComposite: Boolean get() = composite ?: false

    /** Whether this entry is surface data (depth = 0) */
    val isSurface: Boolean get() = depth == 0

    /** Whether this entry is subsurface data (depth > 0) */
    val isSubsurface: Boolean get() = depth > 0
}

@Serializable
data class LayerData(
    val cog: String? = null,
    val image: String? = null,
    val pmtiles: PMTilesSource? = null,
    val features: String? = null
)

@Serializable
data class DataRange(
    val min: Double? = null,
    val max: Double? = null,
    val unit: String
)

@Serializable
data class PMTilesSource(
    val url: String,
    val layers: List<String> = emptyList(),
    val source: String,
    @SerialName("file_url") val fileUrl: String? = null
)

@Serializable
data class SourceMetadata(
    @SerialName("dataset_id") val datasetId: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    val sensor: String? = null,
    @SerialName("temporal_coverage") val temporalCoverage: String? = null,
    val resolution: String? = null,
    val lag: String? = null,
    val description: String? = null,
    @SerialName("cloud_free") val cloudFree: Boolean? = null,
    @SerialName("time_of_day") val timeOfDay: String? = null,
    @SerialName("high_resolution") val highResolution: Boolean? = null,
    @SerialName("processing_delay") val processingDelay: String? = null
)
