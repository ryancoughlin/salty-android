package com.example.saltyoffshore.ui.map.layers

import android.graphics.Color
import com.example.saltyoffshore.data.ContourLayerState
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.style.expressions.dsl.generated.all
import com.mapbox.maps.extension.style.expressions.dsl.generated.color
import com.mapbox.maps.extension.style.expressions.dsl.generated.eq
import com.mapbox.maps.extension.style.expressions.dsl.generated.get
import com.mapbox.maps.extension.style.expressions.dsl.generated.gte
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.maps.extension.style.expressions.dsl.generated.lte
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.layers.properties.generated.SymbolPlacement
import com.mapbox.bindgen.Value

/**
 * SST/Temperature contour layer.
 * Renders major (1°) and minor (0.5°) isolines with labels.
 *
 * Layer structure:
 * - {layerId}-major: Major contour lines (is_major = true)
 * - {layerId}-decimal: Minor contour lines (is_major = false)
 * - {layerId}-labels-major: Labels for major lines
 * - {layerId}-labels-minor: Labels for minor lines
 */
class ContourLayer(
    private val mapboxMap: MapboxMap,
    private val state: ContourLayerState
) {
    fun addToMap() {
        mapboxMap.style?.let { style ->
            val rangeFilter = buildRangeFilter()
            val lineColor = color(state.color)

            // Major contour lines (is_major = true)
            if (!style.styleLayerExists("${state.layerId}-major")) {
                style.addLayer(
                    lineLayer("${state.layerId}-major", state.sourceId) {
                        state.sourceLayer?.let { sourceLayer(it) }
                        filter(
                            all {
                                eq { get("is_major"); literal(true) }
                                rangeFilter
                            }
                        )
                        lineColor(lineColor)
                        lineWidth(1.5)
                        lineOpacity(state.opacity)
                        lineCap(LineCap.ROUND)
                        lineJoin(LineJoin.ROUND)
                    }
                )
            }

            // Minor/decimal contour lines (is_major = false)
            if (!style.styleLayerExists("${state.layerId}-decimal")) {
                style.addLayer(
                    lineLayer("${state.layerId}-decimal", state.sourceId) {
                        state.sourceLayer?.let { sourceLayer(it) }
                        filter(
                            all {
                                eq { get("is_major"); literal(false) }
                                rangeFilter
                            }
                        )
                        lineColor(lineColor)
                        lineWidth(1.0)
                        lineOpacity(state.opacity * 0.7)
                        lineCap(LineCap.ROUND)
                        lineJoin(LineJoin.ROUND)
                        minZoom(7.0)
                    }
                )
            }

            // Labels for major lines
            if (!style.styleLayerExists("${state.layerId}-labels-major")) {
                style.addLayer(
                    symbolLayer("${state.layerId}-labels-major", state.sourceId) {
                        state.sourceLayer?.let { sourceLayer(it) }
                        filter(
                            all {
                                eq { get("is_major"); literal(true) }
                                rangeFilter
                            }
                        )
                        textColor(color(Color.BLACK))
                        textHaloColor(color(Color.WHITE))
                        textHaloWidth(2.0)
                        textFont(listOf("League Mono Regular"))
                        symbolPlacement(SymbolPlacement.LINE)
                        textField(get(state.datasetType.contourLabel))
                        textSize(12.0)
                        textMaxAngle(45.0)
                        textAllowOverlap(false)
                        textIgnorePlacement(false)
                        symbolSpacing(170.0)
                        textPadding(2.0)
                        textOpacity(state.opacity)
                        minZoom(7.0)
                    }
                )
            }

            // Labels for minor lines
            if (!style.styleLayerExists("${state.layerId}-labels-minor")) {
                style.addLayer(
                    symbolLayer("${state.layerId}-labels-minor", state.sourceId) {
                        state.sourceLayer?.let { sourceLayer(it) }
                        filter(
                            all {
                                eq { get("is_major"); literal(false) }
                                rangeFilter
                            }
                        )
                        textColor(color(Color.BLACK))
                        textHaloColor(color(Color.WHITE))
                        textHaloWidth(1.5)
                        textFont(listOf("League Mono Regular"))
                        symbolPlacement(SymbolPlacement.LINE)
                        textField(get(state.datasetType.contourLabel))
                        textSize(10.0)
                        textMaxAngle(45.0)
                        textAllowOverlap(false)
                        textIgnorePlacement(false)
                        symbolSpacing(170.0)
                        textPadding(2.0)
                        textOpacity(state.opacity * 0.8)
                        minZoom(9.0)
                    }
                )
            }
        }
    }

    private fun buildRangeFilter(): Expression {
        return all {
            gte { get(state.fieldName); literal(state.valueRange.start) }
            lte { get(state.fieldName); literal(state.valueRange.endInclusive) }
        }
    }

    fun updateOpacity(newOpacity: Double) {
        mapboxMap.style?.let { style ->
            style.setStyleLayerProperty("${state.layerId}-major", "line-opacity", Value.valueOf(newOpacity))
            style.setStyleLayerProperty("${state.layerId}-decimal", "line-opacity", Value.valueOf(newOpacity * 0.7))
            style.setStyleLayerProperty("${state.layerId}-labels-major", "text-opacity", Value.valueOf(newOpacity))
            style.setStyleLayerProperty("${state.layerId}-labels-minor", "text-opacity", Value.valueOf(newOpacity * 0.8))
        }
    }

    fun removeFromMap() {
        mapboxMap.style?.let { style ->
            listOf("-major", "-decimal", "-labels-major", "-labels-minor").forEach { suffix ->
                val id = "${state.layerId}$suffix"
                if (style.styleLayerExists(id)) {
                    style.removeStyleLayer(id)
                }
            }
        }
    }

    companion object {
        fun layerId(regionId: String) = "contour-layer-$regionId"
    }
}
