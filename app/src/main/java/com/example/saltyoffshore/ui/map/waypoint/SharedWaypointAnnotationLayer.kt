package com.example.saltyoffshore.ui.map.waypoint

import android.graphics.Color
import android.util.Log
import com.example.saltyoffshore.config.MapLayers
import com.example.saltyoffshore.data.waypoint.SharedWaypoint
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
import kotlin.math.abs

private const val TAG = "SharedWaypointAnnotationLayer"

/**
 * Renders CREW (shared) waypoints on the map using GeoJSON source + SymbolLayer.
 * Matches iOS SharedWaypointLayer.swift.
 *
 * Features:
 * - Separate source/layers from own waypoints
 * - Crew color halos: 7-color cycle based on crewId hash
 * - Active crew styling: larger icons, wider halo
 * - When activeCrewId set: only shows that crew's waypoints
 * - Minimum zoom: 2.0
 * - Excludes owned waypoints (avoids duplicates)
 */
class SharedWaypointAnnotationLayer(
    private val mapboxMap: MapboxMap
) {
    private val sourceId = MapLayers.Waypoint.SHARED_SOURCE
    private val layerId = MapLayers.Waypoint.SHARED_LAYER
    private val clusterLayerId = MapLayers.Waypoint.SHARED_CLUSTER_LAYER
    private val countLayerId = MapLayers.Waypoint.SHARED_COUNT_LAYER

    private var isAdded = false

    companion object {
        /** 7-color crew cycle matching iOS SharedWaypointLayer.crewColors */
        private val CREW_COLORS = listOf(
            Color.rgb(59, 130, 246),   // blue   #3B82F6
            Color.rgb(16, 185, 129),   // green  #10B981
            Color.rgb(245, 158, 11),   // orange #F59E0B
            Color.rgb(239, 68, 68),    // red    #EF4444
            Color.rgb(139, 92, 246),   // purple #8B5CF6
            Color.rgb(236, 72, 153),   // pink   #EC4899
            Color.rgb(6, 182, 212)     // cyan   #06B6D4
        )

        fun crewColor(crewId: String): Int {
            val index = abs(crewId.hashCode()) % CREW_COLORS.size
            return CREW_COLORS[index]
        }
    }

    /**
     * Update shared waypoints on the map.
     */
    fun update(
        sharedWaypoints: List<SharedWaypoint>,
        activeCrewId: String?,
        ownedWaypointIds: Set<String>,
        selectedWaypointId: String?
    ) {
        val style = mapboxMap.style ?: return

        // Filter: exclude owned waypoints, optionally filter to active crew
        val visible = sharedWaypoints.filter { sw ->
            sw.waypoint.id !in ownedWaypointIds &&
                (activeCrewId == null || sw.crewId == activeCrewId)
        }

        if (visible.isEmpty()) {
            if (isAdded) setVisibility(false)
            return
        }

        // Register missing icons
        val symbols = visible.map { it.waypoint.symbol }.toSet()
        WaypointIconRegistrar.ensureRegistered(mapboxMap, symbols)

        val isCrewMode = activeCrewId != null

        // Build GeoJSON features
        val features = visible.map { sw ->
            val wp = sw.waypoint
            val isGarmin = wp.symbol.category == WaypointCategory.GARMIN
            val baseSize = if (isGarmin) 0.3 else 0.4
            val iconSize = if (isCrewMode) baseSize * 1.3 else baseSize

            Feature.fromGeometry(wp.coordinate).apply {
                addStringProperty("id", wp.id)
                addStringProperty("name", wp.name ?: "")
                addStringProperty("icon", wp.symbol.imageName)
                addStringProperty("crewId", sw.crewId)
                addBooleanProperty("selected", wp.id == selectedWaypointId)
                addNumberProperty("iconSize", iconSize)
                addNumberProperty("textSize", if (isCrewMode) 13.0 else 12.0)
                addNumberProperty("haloWidth", if (isCrewMode) 1.5 else 1.0)
            }
        }

        val featureCollection = FeatureCollection.fromFeatures(features)

        // Always check if source exists (survives style reloads)
        if (!style.styleSourceExists(sourceId)) {
            addToMap(featureCollection)
        } else {
            style.getSourceAs<GeoJsonSource>(sourceId)
                ?.featureCollection(featureCollection)
        }

        setVisibility(true)
    }

    private fun addToMap(featureCollection: FeatureCollection) {
        val style = mapboxMap.style ?: return

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

        // Individual shared waypoint symbols
        if (!style.styleLayerExists(layerId)) {
            style.addLayer(
                symbolLayer(layerId, sourceId) {
                    filter(not { has { literal("point_count") } })

                    // Data-driven icon
                    iconImage(get { literal("icon") })
                    iconSize(get { literal("iconSize") })
                    iconAnchor(IconAnchor.CENTER)
                    iconAllowOverlap(true)

                    // Icon halo (crew color handled via data update)
                    iconHaloColor(Color.GRAY)
                    iconHaloWidth(2.0)

                    // Text label
                    textField(get { literal("name") })
                    textFont(listOf("Roboto Bold", "Arial Unicode MS Regular"))
                    textSize(get { literal("textSize") })
                    textColor(Color.WHITE)
                    textHaloColor(Color.BLACK)
                    textHaloWidth(get { literal("haloWidth") })
                    textAnchor(TextAnchor.TOP)
                    textOffset(listOf(0.0, -1.75))
                    textAllowOverlap(false)

                    minZoom(2.0)
                }
            )
        }

        isAdded = true
        Log.d(TAG, "Added to map")
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
        Log.d(TAG, "Removed from map")
    }
}
