package com.example.saltyoffshore.data

import com.mapbox.geojson.Point
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.cos

/**
 * Offshore fishing tournament with boundary rules.
 * Matches iOS Tournament struct.
 */
@Serializable
data class Tournament(
    val id: String,
    val name: String,
    @SerialName("authority_year")
    val authorityYear: Int,
    val notes: String,
    @SerialName("boundary_type")
    val boundaryType: String,
    @SerialName("boundary_distance")
    val boundaryDistance: Double,
    @SerialName("boundary_unit")
    val boundaryUnit: String,
    @SerialName("anchor_name")
    val anchorName: String,
    @SerialName("anchor_lat")
    val anchorLat: Double? = null,
    @SerialName("anchor_lon")
    val anchorLon: Double? = null,
    @SerialName("exclusion_zones")
    val exclusionZones: List<ExclusionZone>? = null
) {
    /** Boundary center coordinate */
    val boundaryCenter: Point?
        get() {
            val lat = anchorLat ?: return null
            val lon = anchorLon ?: return null
            return Point.fromLngLat(lon, lat)
        }

    /** Boundary radius in meters */
    val boundaryRadius: Double?
        get() {
            if (boundaryDistance <= 0) return null
            return when (boundaryUnit.lowercase()) {
                "nm", "nmi" -> boundaryDistance * 1852.0
                "mi", "miles" -> boundaryDistance * 1609.34
                "km" -> boundaryDistance * 1000.0
                else -> boundaryDistance * 1852.0
            }
        }

    val displayName: String
        get() = if (authorityYear > 0) "$name ($authorityYear)" else name

    val shortName: String
        get() {
            val components = name.split(" ")
            return if (components.size > 3) components.take(3).joinToString(" ") else name
        }

    val hasValidBoundary: Boolean
        get() = boundaryType == "radius" && boundaryRadius != null && boundaryCenter != null
}

/**
 * Restricted area within a tournament boundary where fishing is prohibited.
 */
@Serializable
data class ExclusionZone(
    val name: String,
    val minLat: Double,
    val maxLat: Double,
    val minLon: Double,
    val maxLon: Double
) {
    /** Polygon coordinates for the exclusion zone */
    val coordinates: List<Point>
        get() = listOf(
            Point.fromLngLat(minLon, minLat),
            Point.fromLngLat(minLon, maxLat),
            Point.fromLngLat(maxLon, maxLat),
            Point.fromLngLat(maxLon, minLat),
            Point.fromLngLat(minLon, minLat) // Close the polygon
        )

    val center: Point
        get() = Point.fromLngLat(
            (minLon + maxLon) / 2,
            (minLat + maxLat) / 2
        )
}
