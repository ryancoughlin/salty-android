package com.example.saltyoffshore.ui.map.layers

import com.mapbox.geojson.Feature
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.RenderedQueryGeometry
import com.mapbox.maps.RenderedQueryOptions
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.circleLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.vectorSource
import com.mapbox.bindgen.Value

/**
 * Invisible layer for crosshair feature queries.
 * Used to get ocean data values at a point.
 *
 * Notes:
 * - PMTiles URL pattern: "pmtiles://{baseURL}/{region}/{dataset}/{timestamp}.pmtiles"
 * - Layer must have ID "data-layer" for crosshair query manager compatibility
 * - Circle is invisible (opacity 0) - only exists for feature intersection queries
 */
class DataQueryLayer(
    private val mapboxMap: MapboxMap,
    private val regionId: String,
    private var pmtilesURL: String,
    private val sourceLayer: String = "data"
) {
    private val sourceId = "data-source-$regionId"
    private val layerId = "data-layer" // Must stay "data-layer" for query compatibility

    fun addToMap() {
        mapboxMap.style?.let { style ->
            // Vector source from PMTiles
            if (!style.styleSourceExists(sourceId)) {
                style.addSource(
                    vectorSource(sourceId) {
                        tiles(listOf(pmtilesURL))
                        maxzoom(8)
                    }
                )
            }

            // Invisible circle layer for feature queries
            if (!style.styleLayerExists(layerId)) {
                style.addLayer(
                    circleLayer(layerId, sourceId) {
                        sourceLayer(sourceLayer)
                        circleRadius(2.0)
                        circleOpacity(0.0) // Invisible - only for queries
                    }
                )
            }
        }
    }

    fun updatePMTilesURL(newURL: String) {
        pmtilesURL = newURL
        mapboxMap.style?.setStyleSourceProperty(
            sourceId,
            "tiles",
            Value.valueOf(listOf(Value.valueOf(newURL)))
        )
    }

    fun queryFeatureAtPoint(screenPoint: ScreenCoordinate, callback: (Feature?) -> Unit) {
        val queryOptions = RenderedQueryOptions(
            listOf(layerId),
            null
        )

        mapboxMap.queryRenderedFeatures(
            RenderedQueryGeometry(screenPoint),
            queryOptions
        ) { expected ->
            val feature = expected.value?.firstOrNull()?.queriedFeature?.feature
            callback(feature)
        }
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
        const val LAYER_ID = "data-layer"
        fun sourceId(regionId: String) = "data-source-$regionId"
    }
}
