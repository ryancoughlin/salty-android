package com.example.saltyoffshore.ui.map.waypoint

import android.content.Context
import android.graphics.Color
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import com.example.saltyoffshore.config.MapLayers
import com.example.saltyoffshore.data.waypoint.Waypoint
import com.example.saltyoffshore.data.waypoint.WaypointCategory
import com.example.saltyoffshore.data.waypoint.WaypointSymbol
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.style.expressions.dsl.generated.get
import com.mapbox.maps.extension.style.expressions.dsl.generated.has
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.maps.extension.style.expressions.dsl.generated.not
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.circleLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.extension.style.layers.properties.generated.TextAnchor
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs

private const val TAG = "WaypointLayer"

/**
 * Waypoint layer matching iOS WaypointLayer.swift.
 *
 * Styling (data-driven via GeoJSON feature properties):
 * - iconImage: from "icon" property (drawable name)
 * - iconSize: from "iconSize" property (0.24 Garmin / 0.4 other)
 * - textField: from "name" property
 * - textOffset: (0, -1.75) - text above icon
 * - minZoom: 4.0
 *
 * TODO: Add clustering (clusterRadius=30, minPoints=6, maxZoom=11)
 */
class WaypointAnnotationLayer(
    private val mapboxMap: MapboxMap,
    private val context: Context
) {
    private val sourceId = MapLayers.Waypoint.OWN_SOURCE
    private val layerId = MapLayers.Waypoint.OWN_LAYER
    private val clusterLayerId = MapLayers.Waypoint.OWN_CLUSTER_LAYER
    private val countLayerId = MapLayers.Waypoint.OWN_COUNT_LAYER

    fun update(waypoints: List<Waypoint>) {
        Log.d(TAG, "update() ${waypoints.size} waypoints")
        val style = mapboxMap.style ?: run {
            Log.e(TAG, "Style is null!")
            return
        }

        if (waypoints.isEmpty()) {
            removeFromMap()
            return
        }

        // Register icon images from drawable resources
        registerIcons(waypoints.map { it.symbol }.toSet())

        // Build GeoJSON features with icon size based on category (matches iOS)
        val features = waypoints.map { wp ->
            val iconSize = if (wp.symbol.category == WaypointCategory.GARMIN) 0.24 else 0.4
            Feature.fromGeometry(wp.coordinate).apply {
                addStringProperty("id", wp.id)
                addStringProperty("name", wp.name ?: "")
                addStringProperty("icon", wp.symbol.drawableName)
                addNumberProperty("iconSize", iconSize)
            }
        }

        val featureCollection = FeatureCollection.fromFeatures(features)

        if (style.styleSourceExists(sourceId)) {
            style.getSourceAs<GeoJsonSource>(sourceId)?.featureCollection(featureCollection)
        } else {
            addToMap(featureCollection)
        }
    }

    private fun registerIcons(symbols: Set<WaypointSymbol>) {
        val style = mapboxMap.style ?: return

        for (symbol in symbols) {
            val iconName = symbol.drawableName
            if (style.hasStyleImage(iconName)) continue

            val resId = context.resources.getIdentifier(iconName, "drawable", context.packageName)
            if (resId == 0) {
                Log.w(TAG, "Drawable not found: $iconName")
                continue
            }

            val drawable = context.getDrawable(resId) ?: continue
            style.addImage(iconName, drawable.toBitmap())
        }
    }

    private fun addToMap(featureCollection: FeatureCollection) {
        val style = mapboxMap.style ?: return

        // GeoJSON source with clustering (matches iOS WaypointLayer)
        style.addSource(
            geoJsonSource(sourceId) {
                featureCollection(featureCollection)
                cluster(true)
                clusterRadius(30)
                clusterMaxZoom(11)
                clusterMinPoints(6)
            }
        )

        // Cluster circles (black, radius 12)
        style.addLayer(
            circleLayer(clusterLayerId, sourceId) {
                filter(has { literal("point_count") })
                circleRadius(12.0)
                circleColor(Color.BLACK)
            }
        )

        // Cluster count labels (white, size 9)
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

        // Individual waypoint markers (filter out clustered points)
        style.addLayer(
            symbolLayer(layerId, sourceId) {
                filter(not { has { literal("point_count") } })

                // Icon - data-driven from feature properties
                iconImage(iconImageExpression())
                iconSize(iconSizeExpression())
                iconAnchor(IconAnchor.CENTER)
                iconAllowOverlap(true)

                // Text label - data-driven
                textField(textFieldExpression())
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
        Log.d(TAG, "Layers added with clustering. Source: ${style.styleSourceExists(sourceId)}")
    }

    fun removeFromMap() {
        val style = mapboxMap.style ?: return
        listOf(layerId, clusterLayerId, countLayerId).forEach { id ->
            if (style.styleLayerExists(id)) style.removeStyleLayer(id)
        }
        if (style.styleSourceExists(sourceId)) style.removeStyleSource(sourceId)
    }

    /** Data-driven icon size from feature property */
    private fun iconSizeExpression(): Expression = get { literal("iconSize") }

    /** Data-driven icon image from feature property */
    private fun iconImageExpression(): Expression = get { literal("icon") }

    /** Data-driven text from feature property */
    private fun textFieldExpression(): Expression = get { literal("name") }
}

/**
 * Maps WaypointSymbol to Android drawable resource name (lowercase with underscores).
 */
val WaypointSymbol.drawableName: String
    get() = when (this) {
        WaypointSymbol.RED_CIRCLE -> "redcircle"
        WaypointSymbol.YELLOW_CIRCLE -> "yellowcircle"
        WaypointSymbol.BLUE_CIRCLE -> "bluecircle"
        WaypointSymbol.GREEN_CIRCLE -> "greencircle"
        WaypointSymbol.RED_FLAG -> "redflag"
        WaypointSymbol.YELLOW_FLAG -> "yellowflag"
        WaypointSymbol.BLUE_FLAG -> "blueflag"
        WaypointSymbol.GREEN_FLAG -> "greenflag"
        WaypointSymbol.RED_SQUARE -> "redsquare"
        WaypointSymbol.YELLOW_SQUARE -> "yellowsquare"
        WaypointSymbol.BLUE_SQUARE -> "bluesquare"
        WaypointSymbol.GREEN_SQUARE -> "greensquare"
        WaypointSymbol.RED_TRIANGLE -> "redtriangle"
        WaypointSymbol.YELLOW_TRIANGLE -> "yellowtriangle"
        WaypointSymbol.BLUE_TRIANGLE -> "bluetriangle"
        WaypointSymbol.GREEN_TRIANGLE -> "greentriangle"
        WaypointSymbol.DOT -> "dot"
        WaypointSymbol.FISHING_AREA_1 -> "fishing_area_1"
        WaypointSymbol.FISHING_AREA_2 -> "fishing_area_2"
        WaypointSymbol.FISHING_AREA_3 -> "fishing_area_3"
        WaypointSymbol.FISHING_AREA_4 -> "fishing_area_4"
        WaypointSymbol.FISHING_AREA_5 -> "fishing_area_5"
        WaypointSymbol.FISHING_AREA_6 -> "fishing_area_6"
        WaypointSymbol.FISHING_AREA_7 -> "fishing_area_7"
        WaypointSymbol.FISHING_AREA_8 -> "fishing_area_8"
        WaypointSymbol.FISHING_AREA_9 -> "fishing_area_9"
        WaypointSymbol.BLUEFIN_TUNA -> "marker_bluefintuna"
        WaypointSymbol.YELLOWFIN_TUNA -> "marker_yellowfintuna"
        WaypointSymbol.BLACKFIN_TUNA -> "marker_blackfintuna"
        WaypointSymbol.BIG_EYE -> "marker_bigeye"
        WaypointSymbol.ALBACORE -> "marker_albacore"
        WaypointSymbol.BILLFISH -> "marker_billfish"
        WaypointSymbol.SAILFISH -> "marker_sailfish"
        WaypointSymbol.BLACK_MARLIN -> "marker_blackmarlin"
        WaypointSymbol.BLUE_MARLIN -> "marker_bluemarlin"
        WaypointSymbol.STRIPED_MARLIN -> "marker_stripedmarlin"
        WaypointSymbol.GROUPER -> "marker_grouper"
        WaypointSymbol.MAHI -> "marker_mahi"
        WaypointSymbol.WAHOO -> "marker_wahoo"
        WaypointSymbol.SEAMOUNT -> "marker_seamount"
        WaypointSymbol.SAND -> "marker_sand"
        WaypointSymbol.CORAL_REEF -> "marker_coral"
        WaypointSymbol.FAD -> "marker_fad"
        WaypointSymbol.DROP_OFF -> "marker_dropoff"
        WaypointSymbol.SHIPWRECK -> "marker_shipwreck"
        WaypointSymbol.OIL_PLATFORM -> "marker_oilplatform"
        WaypointSymbol.ANCHOR -> "marker_anchor"
        WaypointSymbol.BIRDS -> "marker_birds"
        WaypointSymbol.CHLOROPHYLL -> "marker_chlorophyll"
        WaypointSymbol.FLAG -> "marker_flag"
        WaypointSymbol.CAUTION -> "marker_caution"
        WaypointSymbol.DIVE -> "marker_diveflag"
    }
