package com.example.saltyoffshore.ui.map.globallayers

import android.util.Log
import com.example.saltyoffshore.config.AppConstants
import com.example.saltyoffshore.config.MapLayers
import com.mapbox.bindgen.Value
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.maps.extension.style.expressions.dsl.generated.zoom
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.rasterLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.rasterSource

private const val TAG = "ShadedReliefLayer"

/**
 * Shaded relief raster layer - renders topographic shading for underwater terrain.
 * Read-only layer: visible at zoom 0-6 with tight fade at boundaries.
 *
 * Matches iOS ShadedReliefLayer.
 */
class ShadedReliefLayer(
    private val mapboxMap: MapboxMap
) {
    private val sourceId = MapLayers.Global.SHADED_RELIEF_SOURCE
    private val layerId = MapLayers.Global.SHADED_RELIEF_LAYER

    fun addToMap() {
        val style = mapboxMap.style
        if (style == null) {
            Log.w(TAG, "addToMap: style is null, cannot add layer")
            return
        }

        Log.d(TAG, "addToMap: URL=${AppConstants.shadedReliefTileURL}")

        // Add source
        if (!style.styleSourceExists(sourceId)) {
            Log.d(TAG, "Adding source: $sourceId")
            style.addSource(
                rasterSource(sourceId) {
                    tiles(listOf(AppConstants.shadedReliefTileURL))
                    minzoom(0)
                    maxzoom(6)
                }
            )
        } else {
            Log.d(TAG, "Source already exists: $sourceId")
        }

        // Add layer with zoom-based opacity fade
        if (!style.styleLayerExists(layerId)) {
            Log.d(TAG, "Adding layer: $layerId")
            style.addLayer(
                rasterLayer(layerId, sourceId) {
                    // Tight fade in/out at zoom boundaries (0-0.3 and 5.7-6)
                    rasterOpacity(
                        interpolate {
                            linear()
                            zoom()
                            stop(0.0) { literal(0.0) }
                            stop(0.3) { literal(1.0) }
                            stop(5.7) { literal(1.0) }
                            stop(6.0) { literal(0.0) }
                        }
                    )
                    rasterFadeDuration(300.0)
                    maxZoom(6.0)
                    // Note: slot removed temporarily to test - custom style may not have slots defined
                }
            )
            Log.d(TAG, "Layer added successfully")
        } else {
            Log.d(TAG, "Layer already exists: $layerId")
        }
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
