package com.example.saltyoffshore.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import com.example.saltyoffshore.config.MapLayers
import com.example.saltyoffshore.data.RegionMetadata
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource

/**
 * Displays region bounds as a line outline on the map.
 * Matches iOS RegionOutlineView.swift exactly:
 * - Line width: 1.5
 * - Light theme: black
 * - Dark theme: white at 40%
 */
@Composable
fun RegionBoundsEffect(
    region: RegionMetadata?,
    isDarkTheme: Boolean = false
) {
    MapEffect(region?.id) { mapView ->
        val map = mapView.mapboxMap
        val style = map.style

        if (style == null) return@MapEffect

        // Remove existing layer/source if region is null
        if (region == null) {
            style.getLayer(MapLayers.Region.OUTLINE_LAYER)?.let {
                style.removeStyleLayer(MapLayers.Region.OUTLINE_LAYER)
            }
            style.getSource(MapLayers.Region.OUTLINE_SOURCE)?.let {
                style.removeStyleSource(MapLayers.Region.OUTLINE_SOURCE)
            }
            return@MapEffect
        }

        val polygon = createBoundsPolygon(region.bounds)
        val lineColor = if (isDarkTheme) "#66FFFFFF" else "#000000"

        // Check if source exists
        val existingSource = style.getSource(MapLayers.Region.OUTLINE_SOURCE)
        if (existingSource != null) {
            // Update existing source
            (existingSource as? GeoJsonSource)?.feature(polygon)
        } else {
            // Create new source and layer
            style.addSource(
                geoJsonSource(MapLayers.Region.OUTLINE_SOURCE) {
                    feature(polygon)
                }
            )

            style.addLayer(
                lineLayer(MapLayers.Region.OUTLINE_LAYER, MapLayers.Region.OUTLINE_SOURCE) {
                    lineWidth(1.5)
                    lineOpacity(1.0)
                    lineColor(lineColor)
                }
            )
        }
    }
}

/**
 * Creates a closed polygon from bounds [[minLon, minLat], [maxLon, maxLat]]
 * Returns 5 points: NW → NE → SE → SW → NW (closed loop)
 */
private fun createBoundsPolygon(bounds: List<List<Double>>): Feature {
    val minLon = bounds[0][0]
    val minLat = bounds[0][1]
    val maxLon = bounds[1][0]
    val maxLat = bounds[1][1]

    // NW → NE → SE → SW → NW (closed loop)
    val coordinates = listOf(
        Point.fromLngLat(minLon, maxLat), // NW
        Point.fromLngLat(maxLon, maxLat), // NE
        Point.fromLngLat(maxLon, minLat), // SE
        Point.fromLngLat(minLon, minLat), // SW
        Point.fromLngLat(minLon, maxLat)  // NW (close)
    )

    val polygon = Polygon.fromLngLats(listOf(coordinates))
    return Feature.fromGeometry(polygon)
}
