package com.example.saltyoffshore.zarr

import com.mapbox.maps.ProjectedMeters

/**
 * Result from loading a Zarr slice - immutable data container.
 * Matches iOS `ZarrSliceData` exactly.
 */
data class ZarrSliceData(
    val floats: FloatArray,
    val width: Int,
    val height: Int,
    /** Unified bounds in Web Mercator (EPSG:3857) */
    val bounds: GridBounds
) {
    /** Southwest corner as ProjectedMeters (convenience accessor) */
    val sw: ProjectedMeters get() = bounds.sw

    /** Northeast corner as ProjectedMeters (convenience accessor) */
    val ne: ProjectedMeters get() = bounds.ne

    /**
     * Data value range computed from float values (ignoring NaN).
     * Used when the API doesn't provide a range for this variable.
     */
    val valueRange: ClosedFloatingPointRange<Float> by lazy {
        var minVal = Float.MAX_VALUE
        var maxVal = -Float.MAX_VALUE
        for (value in floats) {
            if (!value.isNaN()) {
                if (value < minVal) minVal = value
                if (value > maxVal) maxVal = value
            }
        }
        if (minVal <= maxVal) minVal..maxVal else 0f..1f
    }

    // Convenience constructor matching iOS
    constructor(
        floats: FloatArray,
        width: Int,
        height: Int,
        sw: ProjectedMeters,
        ne: ProjectedMeters
    ) : this(
        floats = floats,
        width = width,
        height = height,
        bounds = GridBounds.from(sw, ne)
    )

    // Required for data class with array
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ZarrSliceData) return false
        if (!floats.contentEquals(other.floats)) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (bounds != other.bounds) return false
        return true
    }

    override fun hashCode(): Int {
        var result = floats.contentHashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + bounds.hashCode()
        return result
    }
}
