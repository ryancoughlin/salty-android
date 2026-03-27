package com.example.saltyoffshore.ui.map.satellite

import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.example.saltyoffshore.data.satellite.GeoJSONPolygon
import com.example.saltyoffshore.data.satellite.SatelliteTrack
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.maps.MapView
import com.mapbox.maps.extension.style.expressions.dsl.generated.get
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.fillLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.TextAnchor
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs

// Source/Layer IDs — match iOS SatelliteTrackLayer
private const val UNSELECTED_SOURCE = "sat-unselected-source"
private const val UNSELECTED_OUTLINE = "sat-unselected-outline"

private const val TRAIL_SOURCE = "sat-trail-source"
private const val TRAIL_FILL = "sat-trail-fill"
private const val TRAIL_OUTLINE = "sat-trail-outline"

private const val SELECTED_SOURCE = "sat-selected-source"
private const val SELECTED_FILL = "sat-selected-fill"
private const val SELECTED_GLOW = "sat-selected-glow"
private const val SELECTED_OUTLINE = "sat-selected-outline"

private const val LABEL_SOURCE = "sat-label-source"
private const val LABEL_LAYER = "sat-labels"

private const val TRAIL_LABEL_SOURCE = "sat-trail-label-source"
private const val TRAIL_LABEL_LAYER = "sat-trail-labels"

val SATELLITE_TRACK_LAYER_IDS = listOf(
    UNSELECTED_OUTLINE, TRAIL_FILL, TRAIL_OUTLINE,
    SELECTED_FILL, SELECTED_GLOW, SELECTED_OUTLINE,
    LABEL_LAYER, TRAIL_LABEL_LAYER
)

val SATELLITE_TRACK_SOURCE_IDS = listOf(
    UNSELECTED_SOURCE, TRAIL_SOURCE, SELECTED_SOURCE,
    LABEL_SOURCE, TRAIL_LABEL_SOURCE
)

/**
 * Satellite tracker mode map layers.
 * Shows selected satellite with trail, unselected as faint outlines.
 *
 * iOS ref: SatelliteTrackLayer.swift
 */
@Composable
fun SatelliteTrackLayerEffect(
    mapView: MapView,
    tracks: List<SatelliteTrack>,
    selectedId: String?
) {
    LaunchedEffect(tracks, selectedId) {
        val style = mapView.mapboxMap.style ?: return@LaunchedEffect

        val selectedTrack = tracks.firstOrNull { it.id == selectedId }

        // 1. UNSELECTED SATELLITES — faint outlines
        val unselectedFeatures = tracks
            .filter { it.id != selectedId }
            .mapNotNull { it.current.geometry.toMapboxFeature(it.id) }

        addOrUpdateSource(style, UNSELECTED_SOURCE, unselectedFeatures)
        addLineLayerIfNeeded(style, UNSELECTED_OUTLINE, UNSELECTED_SOURCE) {
            lineColor(Color.WHITE)
            lineWidth(1.5)
            lineOpacity(0.5)
        }

        // 2. SELECTED TRAIL — fading segments
        val trailFeatures = selectedTrack?.trail?.mapIndexedNotNull { index, segment ->
            segment.geometry.toMapboxFeature(segment.id)?.apply {
                addNumberProperty("index", index)
            }
        } ?: emptyList()

        addOrUpdateSource(style, TRAIL_SOURCE, trailFeatures)
        addFillLayerIfNeeded(style, TRAIL_FILL, TRAIL_SOURCE) {
            fillColor(Color.WHITE)
            fillOpacity(
                interpolate {
                    linear()
                    get { literal("index") }
                    stop { literal(0); literal(0.5) }
                    stop { literal(9); literal(0.15) }
                }
            )
        }
        addLineLayerIfNeeded(style, TRAIL_OUTLINE, TRAIL_SOURCE) {
            lineColor(Color.WHITE)
            lineWidth(2.0)
            lineOpacity(
                interpolate {
                    linear()
                    get { literal("index") }
                    stop { literal(0); literal(0.9) }
                    stop { literal(9); literal(0.3) }
                }
            )
        }

        // 3. SELECTED CURRENT — prominent polygon
        val selectedFeatures = selectedTrack?.let {
            listOfNotNull(it.current.geometry.toMapboxFeature(it.id))
        } ?: emptyList()

        addOrUpdateSource(style, SELECTED_SOURCE, selectedFeatures)
        addFillLayerIfNeeded(style, SELECTED_FILL, SELECTED_SOURCE) {
            fillColor(Color.WHITE)
            fillOpacity(0.65)
        }
        addLineLayerIfNeeded(style, SELECTED_GLOW, SELECTED_SOURCE) {
            lineColor(Color.YELLOW)
            lineWidth(12.0)
            lineOpacity(0.6)
            lineBlur(8.0)
        }
        addLineLayerIfNeeded(style, SELECTED_OUTLINE, SELECTED_SOURCE) {
            lineColor(Color.WHITE)
            lineWidth(3.0)
            lineOpacity(1.0)
        }

        // 4. LABELS — satellite names at polygon centers
        val labelFeatures = tracks.map { track ->
            val isSelected = track.id == selectedId
            val (lat, lon) = track.center
            val label = "${track.name}\n${track.current.shortDateTime}"
            Feature.fromGeometry(Point.fromLngLat(lon, lat)).apply {
                addStringProperty("id", track.id)
                addStringProperty("label", label)
                addNumberProperty("selected_num", if (isSelected) 1.0 else 0.0)
            }
        }

        addOrUpdateSource(style, LABEL_SOURCE, labelFeatures)
        addSymbolLayerIfNeeded(style, LABEL_LAYER, LABEL_SOURCE) {
            textField(get { literal("label") })
            textSize(
                interpolate {
                    linear()
                    get { literal("selected_num") }
                    stop { literal(0); literal(11.0) }
                    stop { literal(1); literal(14.0) }
                }
            )
            textColor(Color.WHITE)
            textOpacity(1.0)
            textHaloColor(Color.argb(230, 0, 0, 0))
            textHaloWidth(1.5)
            textFont(listOf("Roboto Condensed"))
            textAnchor(TextAnchor.CENTER)
            textAllowOverlap(true)
        }

        // 5. TRAIL LABELS — timestamps at trail segment centers
        val trailLabelFeatures = selectedTrack?.trail?.mapIndexedNotNull { index, segment ->
            val (lat, lon) = segment.geometry.center
            Feature.fromGeometry(Point.fromLngLat(lon, lat)).apply {
                addStringProperty("id", segment.id)
                addStringProperty("label", segment.timeLocal)
                addNumberProperty("index", index)
            }
        } ?: emptyList()

        addOrUpdateSource(style, TRAIL_LABEL_SOURCE, trailLabelFeatures)
        addSymbolLayerIfNeeded(style, TRAIL_LABEL_LAYER, TRAIL_LABEL_SOURCE) {
            textField(get { literal("label") })
            textSize(11.0)
            textColor(Color.WHITE)
            textOpacity(1.0)
            textHaloColor(Color.argb(204, 0, 0, 0))
            textHaloWidth(1.0)
            textFont(listOf("Roboto Condensed"))
            textAnchor(TextAnchor.CENTER)
            textAllowOverlap(false)
        }
    }
}

// MARK: - GeoJSON Conversion

/** Convert API polygon to Mapbox Feature */
private fun GeoJSONPolygon.toMapboxFeature(id: String): Feature? {
    val rings = coordinates.map { ring ->
        ring.map { coord ->
            Point.fromLngLat(coord[0], coord[1])
        }
    }
    if (rings.isEmpty()) return null
    val polygon = Polygon.fromLngLats(rings)
    return Feature.fromGeometry(polygon).apply {
        addStringProperty("id", id)
    }
}

// MARK: - Style Helpers

private fun addOrUpdateSource(style: com.mapbox.maps.Style, sourceId: String, features: List<Feature>) {
    val collection = FeatureCollection.fromFeatures(features)
    if (style.styleSourceExists(sourceId)) {
        style.getSourceAs<GeoJsonSource>(sourceId)?.featureCollection(collection)
    } else {
        style.addSource(geoJsonSource(sourceId) { featureCollection(collection) })
    }
}

private fun addFillLayerIfNeeded(
    style: com.mapbox.maps.Style,
    layerId: String,
    sourceId: String,
    block: com.mapbox.maps.extension.style.layers.generated.FillLayerDsl.() -> Unit
) {
    if (!style.styleLayerExists(layerId)) {
        style.addLayer(fillLayer(layerId, sourceId, block))
    }
}

private fun addLineLayerIfNeeded(
    style: com.mapbox.maps.Style,
    layerId: String,
    sourceId: String,
    block: com.mapbox.maps.extension.style.layers.generated.LineLayerDsl.() -> Unit
) {
    if (!style.styleLayerExists(layerId)) {
        style.addLayer(lineLayer(layerId, sourceId, block))
    }
}

private fun addSymbolLayerIfNeeded(
    style: com.mapbox.maps.Style,
    layerId: String,
    sourceId: String,
    block: com.mapbox.maps.extension.style.layers.generated.SymbolLayerDsl.() -> Unit
) {
    if (!style.styleLayerExists(layerId)) {
        style.addLayer(symbolLayer(layerId, sourceId, block))
    }
}
