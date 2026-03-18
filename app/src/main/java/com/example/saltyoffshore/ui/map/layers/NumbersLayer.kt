package com.example.saltyoffshore.ui.map.layers

import android.graphics.Color
import com.example.saltyoffshore.data.DatasetType
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.style.expressions.dsl.generated.format
import com.mapbox.maps.extension.style.expressions.dsl.generated.get
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.maps.extension.style.expressions.dsl.generated.numberFormat
import com.mapbox.maps.extension.style.expressions.dsl.generated.rgb
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.SymbolPlacement
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.vectorSource
import com.mapbox.bindgen.Value

/**
 * Renders numeric values from PMTiles data layer.
 * Matches iOS NumbersLayer exactly.
 *
 * - Fixed spacing: 200px between symbols
 * - Visible at zoom 7+
 * - Black text with white halo
 * - Number formatting based on dataset decimal places
 */
class NumbersLayer(
    private val mapboxMap: MapboxMap,
    private val sourceId: String,
    private val layerId: String,
    private val datasetType: DatasetType,
    private val pmtilesURL: String,
    private val sourceLayer: String = "data",
    private var opacity: Double = 1.0
) {
    companion object {
        private const val SYMBOL_SPACING = 200.0
        private const val MIN_ZOOM = 7.0
        private const val MAX_ZOOM = 12L

        fun sourceId(regionId: String) = "numbers-source-$regionId"
        fun layerId(regionId: String) = "numbers-layer-$regionId"
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

            // Add symbol layer
            if (!style.styleLayerExists(layerId)) {
                val decimalPlaces = datasetType.numberDecimalPlaces

                style.addLayer(
                    symbolLayer(layerId, sourceId) {
                        sourceLayer(sourceLayer)

                        // Number-formatted text field
                        textField(
                            numberFormat(get { literal(datasetType.dataField) }) {
                                minFractionDigits(decimalPlaces)
                                maxFractionDigits(decimalPlaces)
                            }
                        )

                        textSize(12.0)
                        textColor(rgb(0.0, 0.0, 0.0)) // Black
                        textHaloColor(rgb(255.0, 255.0, 255.0)) // White
                        textHaloWidth(2.0)
                        textFont(listOf("League Mono Regular"))
                        textOpacity(opacity)

                        symbolSpacing(SYMBOL_SPACING)
                        textAllowOverlap(false)
                        textIgnorePlacement(false)
                        symbolPlacement(SymbolPlacement.POINT)

                        minZoom(MIN_ZOOM)
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
}
