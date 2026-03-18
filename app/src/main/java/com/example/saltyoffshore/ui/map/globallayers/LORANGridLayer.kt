package com.example.saltyoffshore.ui.map.globallayers

import android.graphics.Color
import com.example.saltyoffshore.config.MapLayers
import com.example.saltyoffshore.data.LoranRegionConfig
import com.mapbox.bindgen.Value
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.style.expressions.dsl.generated.eq
import com.mapbox.maps.extension.style.expressions.dsl.generated.get
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.maps.extension.style.expressions.dsl.generated.mod
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.SymbolPlacement
import com.mapbox.maps.extension.style.layers.properties.generated.TextPitchAlignment
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.vectorSource

/**
 * LORAN-C grid lines with TD (Time Difference) values.
 * Matches iOS LORANGridLayer.
 */
class LORANGridLayer(
    private val mapboxMap: MapboxMap,
    private var opacity: Double = 0.2,
    private val loranConfig: LoranRegionConfig?
) {
    private val sourceId = MapLayers.Global.LORAN_GRID_SOURCE

    fun addToMap() {
        val style = mapboxMap.style ?: return
        val config = loranConfig ?: return

        // Add vector source from Mapbox tileset
        if (!style.styleSourceExists(sourceId)) {
            style.addSource(
                vectorSource(sourceId) {
                    url(config.mapboxSourceId)
                }
            )
        }

        // Major LORAN lines (every 100 TD) - visible at all zoom levels
        if (!style.styleLayerExists(MapLayers.Global.LORAN_GRID_MAJOR_LAYER)) {
            style.addLayer(
                lineLayer(MapLayers.Global.LORAN_GRID_MAJOR_LAYER, sourceId) {
                    sourceLayer(config.sourceLayerName)
                    filter(eq { mod { get { literal("td") }; literal(100) }; literal(0) })
                    lineColor(Color.BLACK)
                    lineWidth(1.0)
                    lineOpacity(opacity)
                    slot("top")
                }
            )
        }

        // Minor LORAN lines (every 50 TD) - visible at higher zoom levels
        if (!style.styleLayerExists(MapLayers.Global.LORAN_GRID_MINOR_LAYER)) {
            style.addLayer(
                lineLayer(MapLayers.Global.LORAN_GRID_MINOR_LAYER, sourceId) {
                    sourceLayer(config.sourceLayerName)
                    filter(eq { mod { get { literal("td") }; literal(50) }; literal(0) })
                    lineColor(Color.BLACK)
                    lineWidth(1.0)
                    lineOpacity(opacity)
                    minZoom(8.0)
                    slot("top")
                }
            )
        }

        // Major TD labels (every 100 TD) - visible at medium zoom
        if (!style.styleLayerExists(MapLayers.Global.LORAN_GRID_LABELS_MAJOR_LAYER)) {
            style.addLayer(
                symbolLayer(MapLayers.Global.LORAN_GRID_LABELS_MAJOR_LAYER, sourceId) {
                    sourceLayer(config.sourceLayerName)
                    filter(eq { mod { get { literal("td") }; literal(100) }; literal(0) })
                    textField(get { literal("td") })
                    textSize(9.0)
                    textFont(listOf("Roboto Condensed"))
                    textColor(Color.BLACK)
                    textOpacity(1.0)
                    textHaloColor(Color.WHITE)
                    textHaloWidth(0.8)
                    symbolPlacement(SymbolPlacement.LINE)
                    textAllowOverlap(false)
                    textIgnorePlacement(false)
                    textPitchAlignment(TextPitchAlignment.VIEWPORT)
                    symbolSpacing(250.0)
                    minZoom(6.0)
                    slot("middle")
                }
            )
        }

        // Minor TD labels (every 50 TD) - visible at high zoom
        if (!style.styleLayerExists(MapLayers.Global.LORAN_GRID_LABELS_MINOR_LAYER)) {
            style.addLayer(
                symbolLayer(MapLayers.Global.LORAN_GRID_LABELS_MINOR_LAYER, sourceId) {
                    sourceLayer(config.sourceLayerName)
                    filter(eq { mod { get { literal("td") }; literal(50) }; literal(0) })
                    textField(get { literal("td") })
                    textSize(9.0)
                    textFont(listOf("Roboto Condensed"))
                    textColor(Color.BLACK)
                    textOpacity(1.0)
                    textHaloColor(Color.WHITE)
                    textHaloWidth(0.5)
                    symbolPlacement(SymbolPlacement.LINE)
                    textAllowOverlap(false)
                    textIgnorePlacement(false)
                    textPitchAlignment(TextPitchAlignment.VIEWPORT)
                    symbolSpacing(150.0)
                    minZoom(8.0)
                    slot("middle")
                }
            )
        }
    }

    fun updateOpacity(newOpacity: Double) {
        opacity = newOpacity
        val style = mapboxMap.style ?: return
        style.setStyleLayerProperty(MapLayers.Global.LORAN_GRID_MAJOR_LAYER, "line-opacity", Value.valueOf(newOpacity))
        style.setStyleLayerProperty(MapLayers.Global.LORAN_GRID_MINOR_LAYER, "line-opacity", Value.valueOf(newOpacity))
    }

    fun removeFromMap() {
        val style = mapboxMap.style ?: return

        listOf(
            MapLayers.Global.LORAN_GRID_MAJOR_LAYER,
            MapLayers.Global.LORAN_GRID_MINOR_LAYER,
            MapLayers.Global.LORAN_GRID_LABELS_MAJOR_LAYER,
            MapLayers.Global.LORAN_GRID_LABELS_MINOR_LAYER
        ).forEach { layerId ->
            if (style.styleLayerExists(layerId)) {
                style.removeStyleLayer(layerId)
            }
        }

        if (style.styleSourceExists(sourceId)) {
            style.removeStyleSource(sourceId)
        }
    }
}
