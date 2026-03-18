package com.example.saltyoffshore.ui.map.globallayers

import com.example.saltyoffshore.config.AppConstants
import com.example.saltyoffshore.config.MapLayers
import com.mapbox.bindgen.Value
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.rasterLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.rasterSource

/**
 * Shaded bathymetry raster layer - renders hillshaded underwater terrain.
 * Matches iOS ShadedBathymetryLayer.
 */
class ShadedBathymetryLayer(
    private val mapboxMap: MapboxMap,
    private var opacity: Double = 1.0
) {
    private val sourceId = MapLayers.Global.SHADED_BATHYMETRY_SOURCE
    private val layerId = MapLayers.Global.SHADED_BATHYMETRY_LAYER

    fun addToMap() {
        val style = mapboxMap.style ?: return

        // Add source
        if (!style.styleSourceExists(sourceId)) {
            style.addSource(
                rasterSource(sourceId) {
                    tiles(listOf(AppConstants.shadedBathymetryTileURL))
                    minzoom(4)
                    maxzoom(14)
                }
            )
        }

        // Add layer
        if (!style.styleLayerExists(layerId)) {
            style.addLayer(
                rasterLayer(layerId, sourceId) {
                    rasterOpacity(opacity)
                    rasterFadeDuration(300.0)
                    slot("bottom")
                }
            )
        }
    }

    fun updateOpacity(newOpacity: Double) {
        opacity = newOpacity
        mapboxMap.style?.setStyleLayerProperty(layerId, "raster-opacity", Value.valueOf(newOpacity))
    }

    fun removeFromMap() {
        val style = mapboxMap.style ?: return
        if (style.styleLayerExists(layerId)) {
            style.removeStyleLayer(layerId)
        }
        if (style.styleSourceExists(sourceId)) {
            style.removeStyleSource(sourceId)
        }
    }
}
