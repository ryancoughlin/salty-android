package com.example.saltyoffshore.ui.map.globallayers

import android.graphics.Color
import com.example.saltyoffshore.config.AppConstants
import com.example.saltyoffshore.config.MapLayers
import com.mapbox.bindgen.Value
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.fillLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.vectorSource

/**
 * Shipping lanes layer with fill and line styling.
 * Matches iOS ShippingLanesLayer.
 */
class ShippingLanesLayer(
    private val mapboxMap: MapboxMap,
    private var opacity: Double = 0.4
) {
    private val sourceId = MapLayers.Global.SHIPPING_LANES_SOURCE
    private val sourceLayerName = "shippinglanes"

    fun addToMap() {
        val style = mapboxMap.style ?: return

        // Add vector source
        if (!style.styleSourceExists(sourceId)) {
            style.addSource(
                vectorSource(sourceId) {
                    tiles(listOf(AppConstants.shippingLanesTileURL))
                }
            )
        }

        // Fill layer for subtle background
        if (!style.styleLayerExists(MapLayers.Global.SHIPPING_LANES_FILL_LAYER)) {
            style.addLayer(
                fillLayer(MapLayers.Global.SHIPPING_LANES_FILL_LAYER, sourceId) {
                    sourceLayer(sourceLayerName)
                    fillColor(Color.BLACK)
                    fillOpacity(opacity * 0.6)
                }
            )
        }

        // Line layer
        if (!style.styleLayerExists(MapLayers.Global.SHIPPING_LANES_LINES_LAYER)) {
            style.addLayer(
                lineLayer(MapLayers.Global.SHIPPING_LANES_LINES_LAYER, sourceId) {
                    sourceLayer(sourceLayerName)
                    lineColor(Color.BLACK)
                    lineWidth(0.8)
                    lineOpacity(opacity)
                }
            )
        }
    }

    fun updateOpacity(newOpacity: Double) {
        opacity = newOpacity
        val style = mapboxMap.style ?: return
        style.setStyleLayerProperty(MapLayers.Global.SHIPPING_LANES_FILL_LAYER, "fill-opacity", Value.valueOf(newOpacity * 0.6))
        style.setStyleLayerProperty(MapLayers.Global.SHIPPING_LANES_LINES_LAYER, "line-opacity", Value.valueOf(newOpacity))
    }

    fun removeFromMap() {
        val style = mapboxMap.style ?: return

        listOf(
            MapLayers.Global.SHIPPING_LANES_FILL_LAYER,
            MapLayers.Global.SHIPPING_LANES_LINES_LAYER
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
