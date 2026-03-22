package com.example.saltyoffshore.data.measurement

import com.mapbox.geojson.Point
import com.mapbox.turf.TurfMeasurement
import com.mapbox.turf.TurfConstants
import com.example.saltyoffshore.data.DistanceUnits
import java.util.UUID

data class MeasurementPoint(
    val id: String = UUID.randomUUID().toString(),
    val coordinate: Point
)

data class MeasurementSegment(val start: Point, val end: Point) {

    val id: String
        get() = "${start.longitude()}_${start.latitude()}_${end.longitude()}_${end.latitude()}"

    val distanceMeters: Double
        get() = TurfMeasurement.distance(start, end, TurfConstants.UNIT_KILOMETERS) * 1000.0

    val midpoint: Point
        get() = TurfMeasurement.midpoint(start, end)

    val bearingDegrees: Double
        get() {
            val bearing = TurfMeasurement.bearing(start, end)
            return (bearing + 360) % 360
        }
}

data class MapMeasurement(
    val id: String = UUID.randomUUID().toString(),
    val points: List<MeasurementPoint> = emptyList()
) {

    val segments: List<MeasurementSegment>
        get() = points.zipWithNext { a, b -> MeasurementSegment(a.coordinate, b.coordinate) }

    val totalDistanceMeters: Double
        get() = segments.sumOf { it.distanceMeters }

    val coordinates: List<Point>
        get() = points.map { it.coordinate }

    val hasSegments: Boolean
        get() = points.size >= 2
}

fun formatDistance(meters: Double, units: DistanceUnits): String {
    val converted = meters * units.metersToUnitFactor
    return "%.1f %s".format(converted, units.unitSuffix)
}
