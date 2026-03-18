package com.example.saltyoffshore.ui.map.layers

import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.style.expressions.dsl.generated.concat
import com.mapbox.maps.extension.style.expressions.dsl.generated.get
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.maps.extension.style.expressions.dsl.generated.match
import com.mapbox.maps.extension.style.expressions.dsl.generated.product
import com.mapbox.maps.extension.style.expressions.dsl.generated.rgb
import com.mapbox.maps.extension.style.expressions.dsl.generated.round
import com.mapbox.maps.extension.style.expressions.dsl.generated.subtract
import com.mapbox.maps.extension.style.expressions.dsl.generated.toString
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.layers.properties.generated.SymbolPlacement
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.vectorSource
import com.mapbox.bindgen.Value

/**
 * Temperature break visualization for offshore fishermen.
 * Renders break lines with strength-based styling and labels.
 * Matches iOS BreaksVectorLayer exactly.
 *
 * GeoJSON properties:
 * - strength: "weak", "moderate", "strong", "very_strong"
 * - cold_temp_f: temperature on cold side
 * - warm_temp_f: temperature on warm side
 * - temp_change_f: degrees difference across the break
 * - warm_side: "offshore", "inshore", "north", "south", "east", "west"
 */
class BreaksVectorLayer(
    private val mapboxMap: MapboxMap,
    private val sourceId: String,
    private val layerId: String,
    private val pmtilesURL: String,
    private val sourceLayer: String = "breaks",
    private var opacity: Double = 1.0,
    private val selectedBreakId: String? = null
) {
    companion object {
        // Widths by strength (thicker = stronger break = better fishing)
        private const val WIDTH_VERY_STRONG = 6.0
        private const val WIDTH_STRONG = 4.5
        private const val WIDTH_MODERATE = 3.0
        private const val WIDTH_WEAK = 2.0

        // Heat scale colors: stronger = more red (hotter fishing spots)
        private const val HEX_VERY_STRONG = "#F22619"  // Deep red
        private const val HEX_STRONG = "#FF5919"       // Orange-red
        private const val HEX_MODERATE = "#FF8C26"     // Orange
        private const val HEX_WEAK = "#FFB340"         // Gold

        private const val MAX_ZOOM = 12L
        private const val MIN_ZOOM = 5.0

        fun sourceId(regionId: String) = "breaks-source-$regionId"
        fun layerId(regionId: String) = "breaks-layer-$regionId"
    }

    fun addToMap() {
        mapboxMap.style?.let { style ->
            // Add vector source if needed
            if (!style.styleSourceExists(sourceId)) {
                style.addSource(
                    vectorSource(sourceId) {
                        tiles(listOf(pmtilesURL))
                        maxzoom(MAX_ZOOM)
                    }
                )
            }

            // 1. Break line glow - soft halo for depth
            if (!style.styleLayerExists("$layerId-glow")) {
                style.addLayer(
                    lineLayer("$layerId-glow", sourceId) {
                        sourceLayer(sourceLayer)
                        lineWidth(glowWidthExpression())
                        lineColor(lineColorExpression())
                        lineOpacity(opacity * 0.5)
                        lineBlur(5.0)
                    }
                )
            }

            // 2. Break line - main visualization with strength-based color
            if (!style.styleLayerExists("$layerId-line")) {
                style.addLayer(
                    lineLayer("$layerId-line", sourceId) {
                        sourceLayer(sourceLayer)
                        lineWidth(lineWidthExpression())
                        lineColor(lineColorExpression())
                        lineOpacity(lineOpacityExpression())
                        lineCap(LineCap.ROUND)
                        lineJoin(LineJoin.ROUND)
                    }
                )
            }

            // 3. Temperature range label - "75°F → 82°F"
            if (!style.styleLayerExists("$layerId-label")) {
                style.addLayer(
                    symbolLayer("$layerId-label", sourceId) {
                        sourceLayer(sourceLayer)
                        textField(tempRangeLabelExpression())
                        textFont(listOf("DIN Pro Bold", "Arial Unicode MS Bold"))
                        textSize(labelSizeExpression())
                        textColor(rgb(255.0, 255.0, 255.0)) // White
                        textHaloColor(rgb(0.0, 0.0, 0.0)) // Black
                        textHaloWidth(2.0)
                        textHaloBlur(0.5)
                        symbolPlacement(SymbolPlacement.LINE)
                        textAllowOverlap(false)
                        textIgnorePlacement(false)
                        minZoom(MIN_ZOOM)
                    }
                )
            }

            // 4. Selection highlight (if break is selected)
            if (selectedBreakId != null) {
                // Selection glow
                if (!style.styleLayerExists("$layerId-sel-glow")) {
                    style.addLayer(
                        lineLayer("$layerId-sel-glow", sourceId) {
                            sourceLayer(sourceLayer)
                            filter(
                                com.mapbox.maps.extension.style.expressions.dsl.generated.eq {
                                    get { literal("id") }
                                    literal(selectedBreakId)
                                }
                            )
                            lineWidth(14.0)
                            lineColor(rgb(255.0, 255.0, 255.0)) // White
                            lineOpacity(0.6)
                            lineBlur(4.0)
                        }
                    )
                }

                // Selection outline
                if (!style.styleLayerExists("$layerId-sel-line")) {
                    style.addLayer(
                        lineLayer("$layerId-sel-line", sourceId) {
                            sourceLayer(sourceLayer)
                            filter(
                                com.mapbox.maps.extension.style.expressions.dsl.generated.eq {
                                    get { literal("id") }
                                    literal(selectedBreakId)
                                }
                            )
                            lineWidth(4.0)
                            lineColor(rgb(255.0, 255.0, 255.0)) // White
                            lineOpacity(1.0)
                        }
                    )
                }
            }
        }
    }

    // Color expression based on strength
    private fun lineColorExpression() = match {
        get { literal("strength") }
        literal("very_strong")
        literal(HEX_VERY_STRONG)
        literal("strong")
        literal(HEX_STRONG)
        literal("moderate")
        literal(HEX_MODERATE)
        literal("weak")
        literal(HEX_WEAK)
        literal(HEX_MODERATE) // default
    }

    // Line width expression based on strength
    private fun lineWidthExpression() = match {
        get { literal("strength") }
        literal("very_strong")
        literal(WIDTH_VERY_STRONG)
        literal("strong")
        literal(WIDTH_STRONG)
        literal("moderate")
        literal(WIDTH_MODERATE)
        literal("weak")
        literal(WIDTH_WEAK)
        literal(WIDTH_MODERATE) // default
    }

    // Glow width expression (line width + halo)
    private fun glowWidthExpression() = match {
        get { literal("strength") }
        literal("very_strong")
        literal(WIDTH_VERY_STRONG + 12.0)
        literal("strong")
        literal(WIDTH_STRONG + 10.0)
        literal("moderate")
        literal(WIDTH_MODERATE + 8.0)
        literal("weak")
        literal(WIDTH_WEAK + 6.0)
        literal(WIDTH_MODERATE + 8.0) // default
    }

    // Opacity expression based on strength
    private fun lineOpacityExpression() = match {
        get { literal("strength") }
        literal("very_strong")
        literal(opacity)
        literal("strong")
        literal(opacity * 0.95)
        literal("moderate")
        literal(opacity * 0.85)
        literal("weak")
        literal(opacity * 0.7)
        literal(opacity * 0.85) // default
    }

    // Temperature range label: "75°F → 82°F" (Fahrenheit)
    private fun tempRangeLabelExpression() = concat {
        toString { round { get { literal("cold_temp_f") } } }
        literal("°F → ")
        toString { round { get { literal("warm_temp_f") } } }
        literal("°F")
    }

    // Label size based on strength
    private fun labelSizeExpression() = match {
        get { literal("strength") }
        literal("very_strong")
        literal(16.0)
        literal("strong")
        literal(14.0)
        literal("moderate")
        literal(12.0)
        literal("weak")
        literal(10.0)
        literal(12.0) // default
    }

    fun updateOpacity(newOpacity: Double) {
        opacity = newOpacity
        mapboxMap.style?.let { style ->
            style.setStyleLayerProperty("$layerId-glow", "line-opacity", Value.valueOf(newOpacity * 0.5))
            // Line opacity is strength-dependent, so we need to update each value
            style.setStyleLayerProperty("$layerId-line", "line-opacity", Value.valueOf(newOpacity))
            style.setStyleLayerProperty("$layerId-label", "text-opacity", Value.valueOf(newOpacity))
        }
    }

    fun removeFromMap() {
        mapboxMap.style?.let { style ->
            listOf("-glow", "-line", "-label", "-sel-glow", "-sel-line").forEach { suffix ->
                val id = "$layerId$suffix"
                if (style.styleLayerExists(id)) {
                    style.removeStyleLayer(id)
                }
            }
            if (style.styleSourceExists(sourceId)) {
                style.removeStyleSource(sourceId)
            }
        }
    }
}
