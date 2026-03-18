package com.example.saltyoffshore.ui.map.layers

import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.rasterLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.rasterSource
import com.mapbox.bindgen.Value

/**
 * COG (Cloud Optimized GeoTIFF) raster layer for visual ocean data.
 * Renders SST, Chlorophyll, MLD heat maps.
 *
 * Usage:
 * - sourceId format: "cog-source-{regionId}"
 * - layerId format: "cog-layer-{regionId}"
 * - cogURL pattern: "{baseURL}/cog/{region}/{dataset}/{timestamp}/{z}/{x}/{y}.png"
 */
class COGVisualLayer(
    private val mapboxMap: MapboxMap,
    private val sourceId: String,
    private val layerId: String,
    private var cogURL: String,
    private var opacity: Double = 1.0
) {
    /**
     * Render layer with new tile URL and opacity.
     * Creates source/layer if needed, updates if exists.
     */
    fun render(tileURL: String, newOpacity: Double) {
        cogURL = tileURL
        opacity = newOpacity

        mapboxMap.style?.let { style ->
            if (!style.styleSourceExists(sourceId)) {
                style.addSource(
                    rasterSource(sourceId) {
                        tiles(listOf(cogURL))
                        minzoom(0)
                        maxzoom(16)
                    }
                )
            } else {
                style.setStyleSourceProperty(
                    sourceId,
                    "tiles",
                    Value.valueOf(listOf(Value.valueOf(cogURL)))
                )
            }

            if (!style.styleLayerExists(layerId)) {
                style.addLayer(
                    rasterLayer(layerId, sourceId) {
                        rasterOpacity(opacity)
                    }
                )
            } else {
                style.setStyleLayerProperty(
                    layerId,
                    "raster-opacity",
                    Value.valueOf(opacity)
                )
            }
        }
    }

    fun addToMap() {
        mapboxMap.style?.let { style ->
            // Add raster source
            if (!style.styleSourceExists(sourceId)) {
                style.addSource(
                    rasterSource(sourceId) {
                        tiles(listOf(cogURL))
                        minzoom(0)
                        maxzoom(16)
                    }
                )
            }

            // Add raster layer
            if (!style.styleLayerExists(layerId)) {
                style.addLayer(
                    rasterLayer(layerId, sourceId) {
                        rasterOpacity(opacity)
                    }
                )
            }
        }
    }

    fun updateTileURL(newURL: String) {
        cogURL = newURL
        mapboxMap.style?.setStyleSourceProperty(
            sourceId,
            "tiles",
            Value.valueOf(listOf(Value.valueOf(newURL)))
        )
    }

    fun updateOpacity(newOpacity: Double) {
        opacity = newOpacity
        mapboxMap.style?.setStyleLayerProperty(
            layerId,
            "raster-opacity",
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

    companion object {
        fun sourceId(regionId: String) = "cog-source-$regionId"
        fun layerId(regionId: String) = "cog-layer-$regionId"
    }
}
