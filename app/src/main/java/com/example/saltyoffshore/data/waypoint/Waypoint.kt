package com.example.saltyoffshore.data.waypoint

import com.mapbox.geojson.Point
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Waypoint(
    val id: String,
    var name: String? = null,
    var notes: String? = null,
    var symbol: WaypointSymbol = WaypointSymbol.BLUE_CIRCLE,
    var latitude: Double,
    var longitude: Double,
    @SerialName("created_at") val createdAt: String,
    @SerialName("track_id") var trackId: String? = null,
    @SerialName("depth_feet") var depthFeet: Double? = null
) {
    val coordinate: Point get() = Point.fromLngLat(longitude, latitude)
}
