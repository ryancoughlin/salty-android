package com.example.saltyoffshore.data.waypoint

import com.example.saltyoffshore.data.coordinate.CoordinateAxisValues
import com.example.saltyoffshore.data.coordinate.CoordinateFormatter
import com.example.saltyoffshore.data.coordinate.GPSFormat
import com.mapbox.geojson.Point

data class WaypointFormState(
    val name: String = "",
    val notes: String = "",
    val symbol: WaypointSymbol = WaypointSymbol.DOT,
    val latitudeValues: CoordinateAxisValues = CoordinateAxisValues(segments = emptyList(), direction = ""),
    val longitudeValues: CoordinateAxisValues = CoordinateAxisValues(segments = emptyList(), direction = ""),
    val canonicalCoordinate: Point? = null
) {
    fun setFromWaypoint(waypoint: Waypoint, format: GPSFormat): WaypointFormState {
        val coord = Point.fromLngLat(waypoint.longitude, waypoint.latitude)
        return copy(
            name = waypoint.name ?: "",
            notes = waypoint.notes ?: "",
            symbol = waypoint.symbol
        ).setFieldsFromCoordinate(coord, format)
    }

    fun parseCoordinate(format: GPSFormat): Point? {
        val lat = CoordinateFormatter.parse(latitudeValues, format, isLatitude = true) ?: return null
        val lon = CoordinateFormatter.parse(longitudeValues, format, isLatitude = false) ?: return null
        return Point.fromLngLat(lon, lat)
    }

    fun isCoordinateValid(format: GPSFormat): Boolean =
        parseCoordinate(format) != null

    fun buildWaypoint(from: Waypoint, format: GPSFormat): Waypoint? {
        val coord = canonicalCoordinate ?: parseCoordinate(format) ?: return null
        return from.copy(
            name = name.ifEmpty { null },
            notes = notes.ifEmpty { null },
            symbol = symbol,
            latitude = coord.latitude(),
            longitude = coord.longitude()
        )
    }

    fun updateCanonicalCoordinate(format: GPSFormat): WaypointFormState =
        copy(canonicalCoordinate = parseCoordinate(format))

    fun setFieldsFromCoordinate(coord: Point, format: GPSFormat): WaypointFormState {
        val latValues = CoordinateFormatter.valuesFromCoordinate(coord.latitude(), format, isLatitude = true)
        val lonValues = CoordinateFormatter.valuesFromCoordinate(coord.longitude(), format, isLatitude = false)
        return copy(
            canonicalCoordinate = coord,
            latitudeValues = latValues,
            longitudeValues = lonValues
        )
    }
}
