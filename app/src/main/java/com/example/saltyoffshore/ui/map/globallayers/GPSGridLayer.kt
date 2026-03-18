package com.example.saltyoffshore.ui.map.globallayers

import android.graphics.Color
import com.example.saltyoffshore.config.AppConstants
import com.example.saltyoffshore.config.MapLayers
import com.mapbox.bindgen.Value
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.style.expressions.dsl.generated.eq
import com.mapbox.maps.extension.style.expressions.dsl.generated.get
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.SymbolPlacement
import com.mapbox.maps.extension.style.layers.properties.generated.TextPitchAlignment
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.vectorSource

/**
 * GPS coordinate grid lines with dynamic spacing based on zoom level.
 * Matches iOS GPSGridLayer.
 */
class GPSGridLayer(
    private val mapboxMap: MapboxMap,
    private var opacity: Double = 0.2
) {
    private val sourceId = MapLayers.Global.GPS_GRID_SOURCE
    private val sourceLayerName = "gps_grid"

    fun addToMap() {
        val style = mapboxMap.style ?: return

        // Add vector source
        if (!style.styleSourceExists(sourceId)) {
            style.addSource(
                vectorSource(sourceId) {
                    tiles(listOf(AppConstants.gpsGridURL))
                    maxzoom(8)
                }
            )
        }

        // Coarse Grid Lines (1.0 degree) - Zoomed Out
        if (!style.styleLayerExists(MapLayers.Global.GPS_GRID_COARSE_LINES_LAYER)) {
            style.addLayer(
                lineLayer(MapLayers.Global.GPS_GRID_COARSE_LINES_LAYER, sourceId) {
                    sourceLayer(sourceLayerName)
                    lineColor(Color.BLACK)
                    lineWidth(1.0)
                    lineOpacity(opacity)
                    minZoom(3.0)
                    maxZoom(8.0)
                    slot("top")
                }
            )
        }

        // Coarse Grid Labels
        if (!style.styleLayerExists(MapLayers.Global.GPS_GRID_COARSE_LABELS_LAYER)) {
            style.addLayer(
                symbolLayer(MapLayers.Global.GPS_GRID_COARSE_LABELS_LAYER, sourceId) {
                    sourceLayer(sourceLayerName)
                    filter(eq { get { literal("grid_type") }; literal("coarse") })
                    textField(get { literal("coordinate_label") })
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

        // Fine Grid Lines (0.5 degree) - Zoomed In
        if (!style.styleLayerExists(MapLayers.Global.GPS_GRID_FINE_LINES_LAYER)) {
            style.addLayer(
                lineLayer(MapLayers.Global.GPS_GRID_FINE_LINES_LAYER, sourceId) {
                    sourceLayer(sourceLayerName)
                    lineColor(Color.BLACK)
                    lineWidth(1.0)
                    lineOpacity(opacity)
                    minZoom(7.0)
                    maxZoom(15.0)
                    slot("top")
                }
            )
        }

        // Fine Grid Labels
        if (!style.styleLayerExists(MapLayers.Global.GPS_GRID_FINE_LABELS_LAYER)) {
            style.addLayer(
                symbolLayer(MapLayers.Global.GPS_GRID_FINE_LABELS_LAYER, sourceId) {
                    sourceLayer(sourceLayerName)
                    filter(eq { get { literal("grid_type") }; literal("fine") })
                    textField(get { literal("coordinate_label") })
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
                    minZoom(6.0)
                    slot("middle")
                }
            )
        }
    }

    fun updateOpacity(newOpacity: Double) {
        opacity = newOpacity
        val style = mapboxMap.style ?: return
        style.setStyleLayerProperty(MapLayers.Global.GPS_GRID_COARSE_LINES_LAYER, "line-opacity", Value.valueOf(newOpacity))
        style.setStyleLayerProperty(MapLayers.Global.GPS_GRID_FINE_LINES_LAYER, "line-opacity", Value.valueOf(newOpacity))
    }

    fun removeFromMap() {
        val style = mapboxMap.style ?: return

        listOf(
            MapLayers.Global.GPS_GRID_COARSE_LINES_LAYER,
            MapLayers.Global.GPS_GRID_COARSE_LABELS_LAYER,
            MapLayers.Global.GPS_GRID_FINE_LINES_LAYER,
            MapLayers.Global.GPS_GRID_FINE_LABELS_LAYER
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
