package com.example.saltyoffshore.ui.map.layers

import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.style.expressions.dsl.generated.get
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.maps.extension.style.expressions.dsl.generated.log10
import com.mapbox.maps.extension.style.expressions.dsl.generated.rgb
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.SymbolPlacement
import com.mapbox.maps.extension.style.layers.properties.generated.TextRotationAlignment
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.vectorSource
import com.mapbox.bindgen.Value
import kotlin.math.log10 as mathLog10
import kotlin.math.max

/**
 * Color utility for currents speed visualization.
 * Uses logarithmic interpolation for better visual distribution.
 */
object CurrentsColorUtil {
    // Color stops matching iOS
    private val COLOR_CALM = "#FFFFFF"
    private val COLOR_LIGHT = "#B3B3B3"
    private val COLOR_MODERATE = "#90E0EF"
    private val COLOR_STRONG = "#38BEF8"
    private val COLOR_VERY_STRONG = "#FFD60A"
    private val COLOR_MAXIMUM = "#E63946"

    fun getLogColorExpression(minSpeed: Double, maxSpeed: Double): Expression {
        val safeMin = max(minSpeed, 0.01)
        val safeMax = max(maxSpeed, safeMin + 0.01)

        val logMin = mathLog10(safeMin)
        val logMax = mathLog10(safeMax)
        val range = logMax - logMin

        return interpolate {
            linear()
            log10 { get { literal("speed") } }
            stop {
                literal(logMin)
                rgb(255.0, 255.0, 255.0)
            }
            stop {
                literal(logMin + range * 0.2)
                rgb(179.0, 179.0, 179.0)
            }
            stop {
                literal(logMin + range * 0.4)
                rgb(144.0, 224.0, 239.0)
            }
            stop {
                literal(logMin + range * 0.6)
                rgb(56.0, 190.0, 248.0)
            }
            stop {
                literal(logMin + range * 0.8)
                rgb(255.0, 214.0, 10.0)
            }
            stop {
                literal(logMax)
                rgb(230.0, 57.0, 70.0)
            }
        }
    }
}

/**
 * Currents arrows layer.
 * Renders rotated arrow glyphs showing ocean current direction and speed.
 */
class CurrentsLayer(
    private val mapboxMap: MapboxMap,
    private var pmtilesURL: String,
    private val sourceLayer: String = "data",
    private var opacity: Double = 1.0,
    private val regionId: String,
    private var speedRange: ClosedFloatingPointRange<Double>
) {
    private val sourceId = "currents-source-$regionId"
    private val layerId = "currents-layer-$regionId"

    fun addToMap() {
        mapboxMap.style?.let { style ->
            // Vector source
            if (!style.styleSourceExists(sourceId)) {
                style.addSource(
                    vectorSource(sourceId) {
                        tiles(listOf(pmtilesURL))
                        maxzoom(12)
                    }
                )
            }

            // Symbol layer with rotated arrows
            if (!style.styleLayerExists(layerId)) {
                val safeMin = max(speedRange.start, 0.01)
                val safeMax = max(speedRange.endInclusive, 0.02)

                style.addLayer(
                    symbolLayer(layerId, sourceId) {
                        sourceLayer(sourceLayer)

                        // Arrow glyph
                        textField("↑")

                        // Rotation from data
                        textRotate(get { literal("direction") })
                        textRotationAlignment(TextRotationAlignment.MAP)
                        textKeepUpright(false)

                        // Size based on log-interpolated speed
                        textSize(
                            interpolate {
                                linear()
                                log10 { get { literal("speed") } }
                                stop {
                                    literal(mathLog10(safeMin))
                                    literal(4.0)
                                }
                                stop {
                                    literal(mathLog10(safeMax))
                                    literal(24.0)
                                }
                            }
                        )

                        // Prevent overlap
                        textAllowOverlap(false)
                        symbolSortKey(get { literal("speed") })

                        // Dynamic color ramp
                        textColor(
                            CurrentsColorUtil.getLogColorExpression(
                                speedRange.start,
                                speedRange.endInclusive
                            )
                        )

                        // Halo for visibility
                        textHaloColor(rgb(0.0, 0.0, 0.0))
                        textHaloWidth(1.0)
                        textOpacity(opacity)

                        symbolPlacement(SymbolPlacement.POINT)
                    }
                )
            }
        }
    }

    fun updateOpacity(newOpacity: Double) {
        opacity = newOpacity
        mapboxMap.style?.setStyleLayerProperty(
            layerId,
            "text-opacity",
            Value.valueOf(newOpacity)
        )
    }

    fun updateSpeedRange(newRange: ClosedFloatingPointRange<Double>) {
        speedRange = newRange
        // Would need to rebuild the layer to update color expression
    }

    fun removeFromMap() {
        mapboxMap.style?.let { style ->
            if (style.styleLayerExists(layerId)) {
                style.removeStyleLayer(layerId)
            }
            if (style.styleSourceExists(sourceId)) {
                style.removeStyleSource(sourceId)
            }
        }
    }

    companion object {
        fun sourceId(regionId: String) = "currents-source-$regionId"
        fun layerId(regionId: String) = "currents-layer-$regionId"
    }
}
