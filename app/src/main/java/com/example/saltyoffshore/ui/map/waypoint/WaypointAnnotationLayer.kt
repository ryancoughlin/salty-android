package com.example.saltyoffshore.ui.map.waypoint

import android.graphics.Color
import android.util.Log
import com.example.saltyoffshore.config.MapLayers
import com.example.saltyoffshore.data.waypoint.Waypoint
import com.example.saltyoffshore.data.waypoint.WaypointCategory
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.style.expressions.dsl.generated.get
import com.mapbox.maps.extension.style.expressions.dsl.generated.has
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.maps.extension.style.expressions.dsl.generated.not
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.circleLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.extension.style.layers.properties.generated.TextAnchor
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs

private const val TAG = "WaypointAnnotationLayer"

/**
 * Renders OWN waypoints on the map using GeoJSON source + SymbolLayer.
 * Matches iOS WaypointLayer.swift.
 *
 * Features:
 * - Data-driven icon images from waypoint symbol
 * - Text labels above icons
 * - Clustering: minPoints=6, clusterRadius=30, clusterMaxZoom=11
 * - Minimum zoom: 4.0
 * - Hidden when activeCrewId is set (crew focus mode)
 */
class WaypointAnnotationLayer(
    private val mapboxMap: MapboxMap
) {
    private val sourceId = MapLayers.Waypoint.OWN_SOURCE
    private val layerId = MapLayers.Waypoint.OWN_LAYER
    private val clusterLayerId = MapLayers.Waypoint.OWN_CLUSTER_LAYER
    private val countLayerId = MapLayers.Waypoint.OWN_COUNT_LAYER

    private var lastWaypointIds: Set<String> = emptySet()
    private var isAdded = false

    /**
     * Update waypoints on the map. Creates source/layers on first call,
     * updates GeoJSON data on subsequent calls.
     */
    fun update(
        waypoints: List<Waypoint>,
        selectedWaypointId: String?,
        activeCrewId: String?
    ) {
        val style = mapboxMap.style ?: return

        // Hide own waypoints in crew focus mode (matches iOS)
        val visible = activeCrewId == null && waypoints.isNotEmpty()

        if (!visible) {
            if (isAdded) setVisibility(false)
            return
        }

        // Register any missing icon images
        val symbols = waypoints.map { it.symbol }.toSet()
        WaypointIconRegistrar.ensureRegistered(mapboxMap, symbols)

        // Build GeoJSON features
        val features = waypoints.map { wp ->
            Feature.fromGeometry(wp.coordinate).apply {
                addStringProperty("id", wp.id)
                addStringProperty("name", wp.name ?: "")
                addStringProperty("icon", wp.symbol.imageName)
                addBooleanProperty("selected", wp.id == selectedWaypointId)
                addNumberProperty(
                    "iconSize",
                    if (wp.symbol.category == WaypointCategory.GARMIN) 0.24 else 0.4
                )
                addNumberProperty(
                    "selectedIconSize",
                    if (wp.symbol.category == WaypointCategory.GARMIN) 0.32 else 0.52
                )
            }
        }

        val featureCollection = FeatureCollection.fromFeatures(features)
        val waypointIds = waypoints.map { it.id }.toSet()

        if (!isAdded) {
            addToMap(featureCollection)
        } else {
            // Update GeoJSON data
            style.getSourceAs<GeoJsonSource>(sourceId)
                ?.featureCollection(featureCollection)
        }

        if (isAdded) setVisibility(true)
        lastWaypointIds = waypointIds
    }

    private fun addToMap(featureCollection: FeatureCollection) {
        val style = mapboxMap.style ?: return

        try {
            // Source with clustering
            if (!style.styleSourceExists(sourceId)) {
                style.addSource(
                    geoJsonSource(sourceId) {
                        featureCollection(featureCollection)
                        cluster(true)
                        clusterRadius(30)
                        clusterMaxZoom(11)
                        clusterMinPoints(6)
                    }
                )
            }

            // Cluster circles
            if (!style.styleLayerExists(clusterLayerId)) {
                style.addLayer(
                    circleLayer(clusterLayerId, sourceId) {
                        filter(has { literal("point_count") })
                        circleRadius(12.0)
                        circleColor(Color.BLACK)
                    }
                )
            }

            // Cluster count labels
            if (!style.styleLayerExists(countLayerId)) {
                style.addLayer(
                    symbolLayer(countLayerId, sourceId) {
                        filter(has { literal("point_count") })
                        textField(get { literal("point_count") })
                        textSize(9.0)
                        textColor(Color.WHITE)
                        textAllowOverlap(true)
                        textIgnorePlacement(true)
                    }
                )
            }

            // Individual waypoint symbols
            if (!style.styleLayerExists(layerId)) {
                style.addLayer(
                    symbolLayer(layerId, sourceId) {
                        filter(not { has { literal("point_count") } })

                        // Data-driven icon from "icon" property
                        iconImage(get { literal("icon") })
                        iconSize(literal(0.3))
                        iconAnchor(IconAnchor.CENTER)
                        iconAllowOverlap(true)

                        // Text label
                        textField(get { literal("name") })
                        textFont(listOf("Roboto Bold", "Arial Unicode MS Regular"))
                        textSize(12.0)
                        textColor(Color.WHITE)
                        textHaloColor(Color.BLACK)
                        textHaloWidth(1.0)
                        textAnchor(TextAnchor.TOP)
                        textOffset(listOf(0.0, -1.75))
                        textAllowOverlap(false)

                        minZoom(4.0)
                    }
                )
            }

            isAdded = true
            Log.d(TAG, "Added to map with ${featureCollection.features()?.size ?: 0} features")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add waypoint layer: ${e.message}", e)
        }
    }

    private fun setVisibility(visible: Boolean) {
        val style = mapboxMap.style ?: return
        val visibility = if (visible) "visible" else "none"

        listOf(layerId, clusterLayerId, countLayerId).forEach { id ->
            if (style.styleLayerExists(id)) {
                style.setStyleLayerProperty(
                    id, "visibility",
                    com.mapbox.bindgen.Value.valueOf(visibility)
                )
            }
        }
    }

    fun removeFromMap() {
        val style = mapboxMap.style ?: return

        listOf(layerId, countLayerId, clusterLayerId).forEach { id ->
            if (style.styleLayerExists(id)) {
                style.removeStyleLayer(id)
            }
        }

        if (style.styleSourceExists(sourceId)) {
            style.removeStyleSource(sourceId)
        }

        isAdded = false
        lastWaypointIds = emptySet()
        Log.d(TAG, "Removed from map")
    }
}
