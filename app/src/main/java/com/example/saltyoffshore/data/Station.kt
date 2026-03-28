package com.example.saltyoffshore.data

import com.mapbox.geojson.Point
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Offshore buoy station data.
 * Matches iOS Station struct.
 */
@Serializable
data class Station(
    val id: String,
    val name: String,
    val location: StationLocation,
    val type: String,
    val hasRealTimeData: Boolean = true,
    val owner: String = "NDBC",
    @SerialName("has_currents")
    val hasCurrents: Boolean = false
) {
    val coordinate: Point
        get() = Point.fromLngLat(location.coordinates[0], location.coordinates[1])
}

/**
 * GeoJSON Point location.
 * JSON: { "type": "Point", "coordinates": [lon, lat] }
 */
@Serializable
data class StationLocation(
    val type: String = "Point",
    val coordinates: List<Double>
) {
    val longitude: Double get() = coordinates[0]
    val latitude: Double get() = coordinates[1]
}
