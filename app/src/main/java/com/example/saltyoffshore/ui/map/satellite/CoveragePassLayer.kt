package com.example.saltyoffshore.ui.map.satellite

import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import com.example.saltyoffshore.data.satellite.GeoJSONPolygon
import com.example.saltyoffshore.data.satellite.PassStatus
import com.example.saltyoffshore.data.satellite.RegionalPass
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.maps.MapView
import com.mapbox.maps.extension.style.expressions.dsl.generated.get
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.maps.extension.style.expressions.dsl.generated.match
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.circleLayer
import com.mapbox.maps.extension.style.layers.generated.fillLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.TextAnchor
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.plugin.gestures.gestures

// Source/Layer IDs — match iOS CoveragePassLayer
private const val SELECTED_SOURCE = "coverage-selected-source"
private const val SELECTED_FILL = "coverage-selected-fill"
private const val SELECTED_OUTLINE = "coverage-selected-outline"
private const val SELECTED_LABEL_SOURCE = "coverage-selected-label-source"
private const val SELECTED_LABEL_LAYER = "coverage-selected-label"

private const val PINS_SOURCE = "coverage-pins-source"
private const val PINS_CIRCLE = "coverage-pins-circle"
private const val PINS_LABEL = "coverage-pins-label"

val COVERAGE_PASS_LAYER_IDS = listOf(
    SELECTED_FILL, SELECTED_OUTLINE, SELECTED_LABEL_LAYER,
    PINS_CIRCLE, PINS_LABEL
)

val COVERAGE_PASS_SOURCE_IDS = listOf(
    SELECTED_SOURCE, SELECTED_LABEL_SOURCE, PINS_SOURCE
)

// Status colors — green for success, gray for running, dark for unavailable
private const val COLOR_SUCCESS = "#4CAF50"
private const val COLOR_RUNNING = "#9E9E9E"
private const val COLOR_UNAVAILABLE = "#424242"

/**
 * Coverage mode map layers.
 * Selected pass shows full polygon, unselected passes show tappable pins.
 *
 * iOS ref: CoveragePassLayer.swift
 */
@Composable
fun CoveragePassLayerEffect(
    mapView: MapView,
    passes: List<RegionalPass>,
    selectedId: String?,
    onPassTap: (String) -> Unit
) {
    LaunchedEffect(passes, selectedId) {
        val style = mapView.mapboxMap.style ?: return@LaunchedEffect

        val selectedPass = passes.firstOrNull { it.id == selectedId }
        val unselectedPasses = passes.filter { it.id != selectedId }

        // SELECTED PASS: Full polygon with label
        val selectedFeatures = selectedPass?.let {
            listOfNotNull(it.geometry.toMapboxFeature(it.id))
        } ?: emptyList()

        addOrUpdateSource(style, SELECTED_SOURCE, selectedFeatures)
        addFillLayerIfNeeded(style, SELECTED_FILL, SELECTED_SOURCE) {
            fillColor(Color.WHITE)
            fillOpacity(0.45)
        }
        addLineLayerIfNeeded(style, SELECTED_OUTLINE, SELECTED_SOURCE) {
            lineColor(Color.WHITE)
            lineWidth(2.5)
            lineOpacity(1.0)
        }

        // Selected label
        val labelFeatures = selectedPass?.let { pass ->
            val (lat, lon) = pass.center
            val label = "${pass.name}\n${pass.shortDateTime}"
            listOf(
                Feature.fromGeometry(Point.fromLngLat(lon, lat)).apply {
                    addStringProperty("label", label)
                }
            )
        } ?: emptyList()

        addOrUpdateSource(style, SELECTED_LABEL_SOURCE, labelFeatures)
        addSymbolLayerIfNeeded(style, SELECTED_LABEL_LAYER, SELECTED_LABEL_SOURCE) {
            textField(get { literal("label") })
            textSize(14.0)
            textColor(Color.WHITE)
            textOpacity(1.0)
            textHaloColor(Color.argb(230, 0, 0, 0))
            textHaloWidth(1.5)
            textFont(listOf("DIN Pro Medium"))
            textAnchor(TextAnchor.CENTER)
            textAllowOverlap(true)
        }

        // UNSELECTED PASSES: Circle pins + labels
        val pinFeatures = unselectedPasses.map { pass ->
            val (lat, lon) = pass.center
            val statusStr = when (pass.status) {
                PassStatus.SUCCESS -> "success"
                PassStatus.RUNNING -> "running"
                else -> "unavailable"
            }
            Feature.fromGeometry(Point.fromLngLat(lon, lat)).apply {
                addStringProperty("id", pass.id)
                addStringProperty("status", statusStr)
                addStringProperty("time_label", pass.timeLocal)
            }
        }

        addOrUpdateSource(style, PINS_SOURCE, pinFeatures)
        addCircleLayerIfNeeded(style, PINS_CIRCLE, PINS_SOURCE) {
            circleRadius(6.0)
            circleColor(
                match {
                    get { literal("status") }
                    literal("success"); literal(COLOR_SUCCESS)
                    literal("running"); literal(COLOR_RUNNING)
                    literal(COLOR_UNAVAILABLE)
                }
            )
            circleStrokeWidth(1.5)
            circleStrokeColor(Color.WHITE)
        }
        addSymbolLayerIfNeeded(style, PINS_LABEL, PINS_SOURCE) {
            textField(get { literal("time_label") })
            textSize(10.0)
            textColor(Color.WHITE)
            textHaloColor(Color.BLACK)
            textHaloWidth(1.0)
            textOffset(listOf(0.0, 1.2))
            textAnchor(TextAnchor.TOP)
            textAllowOverlap(false)
        }
    }

    // Click handler for pin taps
    DisposableEffect(Unit) {
        val clickListener = com.mapbox.maps.plugin.gestures.OnMapClickListener { point ->
            val mapboxMap = mapView.mapboxMap
            val screenPoint = mapboxMap.pixelForCoordinate(point)
            val queryGeometry = com.mapbox.maps.RenderedQueryGeometry(screenPoint)
            val options = com.mapbox.maps.RenderedQueryOptions(listOf(PINS_CIRCLE), null)

            mapboxMap.queryRenderedFeatures(queryGeometry, options) { result ->
                val features = result.value ?: return@queryRenderedFeatures
                val tappedId = features.firstOrNull()
                    ?.queriedFeature
                    ?.feature
                    ?.getStringProperty("id")
                if (tappedId != null) {
                    onPassTap(tappedId)
                }
            }
            false
        }

        val gesturesPlugin = mapView.gestures
        gesturesPlugin.addOnMapClickListener(clickListener)

        onDispose {
            gesturesPlugin.removeOnMapClickListener(clickListener)
        }
    }
}

// MARK: - GeoJSON Conversion

private fun GeoJSONPolygon.toMapboxFeature(id: String): Feature? {
    val rings = coordinates.map { ring ->
        ring.map { coord -> Point.fromLngLat(coord[0], coord[1]) }
    }
    if (rings.isEmpty()) return null
    return Feature.fromGeometry(Polygon.fromLngLats(rings)).apply {
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

private fun addCircleLayerIfNeeded(
    style: com.mapbox.maps.Style,
    layerId: String,
    sourceId: String,
    block: com.mapbox.maps.extension.style.layers.generated.CircleLayerDsl.() -> Unit
) {
    if (!style.styleLayerExists(layerId)) {
        style.addLayer(circleLayer(layerId, sourceId, block))
    }
}
