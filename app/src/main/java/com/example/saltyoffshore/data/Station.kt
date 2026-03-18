package com.example.saltyoffshore.data

import com.mapbox.geojson.Point
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
    val hasRealTimeData: Boolean,
    val owner: String
) {
    val coordinate: Point
        get() = Point.fromLngLat(location.longitude, location.latitude)
}

@Serializable
data class StationLocation(
    val latitude: Double,
    val longitude: Double
)
