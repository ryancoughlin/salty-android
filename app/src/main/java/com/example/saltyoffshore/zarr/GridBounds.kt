package com.example.saltyoffshore.zarr

import com.mapbox.maps.ProjectedMeters
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Unified bounds representation for Zarr grid data in Web Mercator (EPSG:3857).
 *
 * Why this exists:
 * - ZarrSliceData, PreparedDataset, and velocity grids all need bounds
 * - Using one type eliminates confusion about which properties to use
 * - Clear naming: "sw" = southwest corner, "ne" = northeast corner
 *
 * Matches iOS `GridBounds` exactly.
 */
data class GridBounds(
    /** Southwest corner easting in meters (EPSG:3857) */
    val swEasting: Double,
    /** Southwest corner northing in meters (EPSG:3857) */
    val swNorthing: Double,
    /** Northeast corner easting in meters (EPSG:3857) */
    val neEasting: Double,
    /** Northeast corner northing in meters (EPSG:3857) */
    val neNorthing: Double
) {
    // MARK: - Convenience Accessors

    /** Southwest corner as ProjectedMeters (for Mapbox APIs) */
    val sw: ProjectedMeters
        get() = ProjectedMeters(swNorthing, swEasting)

    /** Northeast corner as ProjectedMeters (for Mapbox APIs) */
    val ne: ProjectedMeters
        get() = ProjectedMeters(neNorthing, neEasting)

    /** Width in meters */
    val width: Double
        get() = neEasting - swEasting

    /** Height in meters */
    val height: Double
        get() = neNorthing - swNorthing

    // MARK: - Factory Methods

    companion object {
        /**
         * Create bounds from ProjectedMeters corners.
         */
        fun from(sw: ProjectedMeters, ne: ProjectedMeters): GridBounds = GridBounds(
            swEasting = sw.easting,
            swNorthing = sw.northing,
            neEasting = ne.easting,
            neNorthing = ne.northing
        )

        /**
         * Create bounds from coordinate arrays, expanding by half a pixel to cover full extent.
         *
         * Zarr coordinates mark pixel centers. This method adds half-pixel padding so
         * the rendered texture covers the full spatial extent.
         *
         * @param x0 First x coordinate
         * @param x1 Last x coordinate
         * @param y0 First y coordinate
         * @param y1 Last y coordinate
         * @param width Grid width in pixels
         * @param height Grid height in pixels
         */
        fun pixelEdgeBounds(
            x0: Double, x1: Double,
            y0: Double, y1: Double,
            width: Int, height: Int
        ): GridBounds {
            val pixelWidth = abs(x1 - x0) / (width - 1).toDouble()
            val pixelHeight = abs(y1 - y0) / (height - 1).toDouble()
            val halfPx = pixelWidth / 2.0
            val halfPy = pixelHeight / 2.0

            val minX = min(x0, x1) - halfPx
            val maxX = max(x0, x1) + halfPx
            val minY = min(y0, y1) - halfPy
            val maxY = max(y0, y1) + halfPy

            return GridBounds(
                swEasting = minX,
                swNorthing = minY,
                neEasting = maxX,
                neNorthing = maxY
            )
        }
    }
}
