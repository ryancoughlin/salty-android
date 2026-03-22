package com.example.saltyoffshore.ui.measurement

import androidx.compose.runtime.Composable
import com.example.saltyoffshore.data.DistanceUnits
import com.example.saltyoffshore.data.measurement.MapMeasurement
import com.example.saltyoffshore.data.measurement.formatDistance
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.circleLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSource

private const val LINE_SOURCE = "measurement-line-source"
private const val LINE_LAYER = "measurement-line-layer"
private const val POINT_SOURCE = "measurement-point-source"
private const val POINT_LAYER = "measurement-point-layer"
private const val LABEL_SOURCE = "measurement-label-source"
private const val LABEL_LAYER = "measurement-label-layer"

/**
 * Renders measurement lines, vertex circles, and distance labels on the map.
 * Port of iOS MeasurementLayer.swift.
 */
@Composable
fun MeasurementMapEffect(
    measurements: List<MapMeasurement>,
    distanceUnits: DistanceUnits
) {
    MapEffect(measurements, distanceUnits) { mapView ->
        val style = mapView.mapboxMap.style ?: return@MapEffect

        ensureMeasurementLayers(style)

        // Build line features
        val lineFeatures = measurements.filter { it.hasSegments }.map { measurement ->
            Feature.fromGeometry(LineString.fromLngLats(measurement.coordinates)).apply {
                addStringProperty("id", measurement.id)
            }
        }

        // Build point features
        val pointFeatures = measurements.flatMap { measurement ->
            measurement.points.map { point ->
                Feature.fromGeometry(point.coordinate).apply {
                    addStringProperty("id", point.id)
                }
            }
        }

        // Build label features at segment midpoints
        val labelFeatures = measurements.flatMap { measurement ->
            measurement.segments.map { segment ->
                Feature.fromGeometry(segment.midpoint).apply {
                    addStringProperty("distance", formatDistance(segment.distanceMeters, distanceUnits))
                    addStringProperty("id", segment.id)
                }
            }
        }

        // Update sources
        (style.getSource(LINE_SOURCE) as? GeoJsonSource)
            ?.featureCollection(FeatureCollection.fromFeatures(lineFeatures))
        (style.getSource(POINT_SOURCE) as? GeoJsonSource)
            ?.featureCollection(FeatureCollection.fromFeatures(pointFeatures))
        (style.getSource(LABEL_SOURCE) as? GeoJsonSource)
            ?.featureCollection(FeatureCollection.fromFeatures(labelFeatures))
    }
}

/**
 * Idempotently adds the three GeoJSON sources and layers for measurement rendering.
 */
private fun ensureMeasurementLayers(style: Style) {
    if (style.styleSourceExists(LINE_SOURCE)) return

    // --- Sources ---
    style.addSource(geoJsonSource(LINE_SOURCE) {
        featureCollection(FeatureCollection.fromFeatures(emptyList()))
    })
    style.addSource(geoJsonSource(POINT_SOURCE) {
        featureCollection(FeatureCollection.fromFeatures(emptyList()))
    })
    style.addSource(geoJsonSource(LABEL_SOURCE) {
        featureCollection(FeatureCollection.fromFeatures(emptyList()))
    })

    // --- Line layer ---
    style.addLayer(lineLayer(LINE_LAYER, LINE_SOURCE) {
        lineColor("#000000")
        lineWidth(3.0)
        lineOpacity(0.9)
        lineCap(LineCap.ROUND)
        lineJoin(LineJoin.ROUND)
    })

    // --- Point layer ---
    style.addLayer(circleLayer(POINT_LAYER, POINT_SOURCE) {
        circleRadius(8.0)
        circleColor("#FFFFFF")
        circleStrokeColor("#000000")
        circleStrokeWidth(2.5)
    })

    // --- Label layer ---
    style.addLayer(symbolLayer(LABEL_LAYER, LABEL_SOURCE) {
        textField(Expression.get("distance"))
        textSize(12.0)
        textColor("#FFFFFF")
        textHaloColor("#000000")
        textHaloWidth(1.5)
        textAllowOverlap(true)
    })
}
