package com.example.saltyoffshore.zarr

/**
 * Metadata for a frame (lightweight, all kept in memory).
 * Matches iOS `ZarrFrameMetadata` exactly.
 */
data class ZarrFrameMetadata(
    val entryId: String,
    val zarrIndex: Int?,  // null when API entry has no Zarr match
    /**
     * Color scale domain for this frame. Computed by DomainStrategy at load time —
     * either fixed (chlorophyll) or aggregated min/max (everything else).
     */
    val dataRange: ClosedFloatingPointRange<Float>,
    /** Entry timestamp (Unix seconds). Used for manifest retry when zarrIndex is null. */
    val timestamp: Long
) {
    val hasZarrData: Boolean get() = zarrIndex != null
}

/**
 * Immutable context for loading Zarr data.
 *
 * This is configuration/metadata — it never changes after creation.
 * Mutable state (like current depth) belongs in the Manager that owns this context.
 *
 * Matches iOS `ZarrDatasetContext` exactly.
 */
data class ZarrDatasetContext(
    val zarrUrl: String,
    /** Pre-fetched metadata + coordinates — shared by all frame loads. */
    val preparedDataset: ZarrReader.PreparedDataset,
    /**
     * Frame metadata grouped by depth — built once from all entries.
     * Surface-only datasets have one key: [0].
     */
    val depthFrames: Map<Int, DepthFrames>,
    /** Available depths from API (e.g., [0, 10, 25, 50, 100]) */
    val availableDepths: List<Int>
) {
    /**
     * Container for frames at a specific depth level.
     */
    data class DepthFrames(
        val frames: List<ZarrFrameMetadata>,
        val index: Map<String, Int>  // entryId → frame index
    )

    // MARK: - Queries (no mutable state)

    /** Get frames for a specific depth */
    fun frames(depthMeters: Int): List<ZarrFrameMetadata> {
        return depthFrames[depthMeters]?.frames ?: emptyList()
    }

    /** Get entry ID lookup for a specific depth */
    fun entryIdToIndex(depthMeters: Int): Map<String, Int> {
        return depthFrames[depthMeters]?.index ?: emptyMap()
    }

    /** Convert depth in meters to Zarr array index */
    fun depthIndex(depthMeters: Int): Int {
        return availableDepths.indexOf(depthMeters).takeIf { it >= 0 } ?: 0
    }

    /** Default depth (first available, or 0) */
    val defaultDepth: Int
        get() = availableDepths.firstOrNull() ?: 0
}
