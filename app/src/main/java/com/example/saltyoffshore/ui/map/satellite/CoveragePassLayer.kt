package com.example.saltyoffshore.ui.map.satellite

import android.graphics.Color
import android.util.Log
import com.example.saltyoffshore.data.satellite.PassStatus
import com.example.saltyoffshore.data.satellite.RegionalPass
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.dsl.generated.get
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.maps.extension.style.expressions.dsl.generated.match
import com.mapbox.maps.extension.style.layers.properties.generated.TextAnchor

private const val TAG = "CoveragePassLayer"

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

private const val COLOR_SUCCESS = "#4CAF50"
private const val COLOR_RUNNING = "#9E9E9E"
private const val COLOR_UNAVAILABLE = "#424242"

/**
 * Renders coverage pass layers onto the Mapbox style.
 * Called from SatelliteLayers on style load and on data changes.
 *
 * iOS ref: CoveragePassLayer.swift
 */
fun renderCoverageLayers(
    style: Style,
    passes: List<RegionalPass>,
    selectedId: String?
) {
    Log.d(TAG, "Rendering coverage layers: ${passes.size} passes, selected=$selectedId")

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
