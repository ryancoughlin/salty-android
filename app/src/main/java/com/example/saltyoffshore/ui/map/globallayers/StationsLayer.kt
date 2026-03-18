package com.example.saltyoffshore.ui.map.globallayers

import android.graphics.Color
import com.example.saltyoffshore.config.MapLayers
import com.example.saltyoffshore.data.Station
import com.mapbox.bindgen.Value
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.style.expressions.dsl.generated.get
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.circleLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs

/**
 * Offshore station markers with clustering.
 * Matches iOS StationsLayer.
 */
class StationsLayer(
    private val mapboxMap: MapboxMap,
    private var opacity: Double = 1.0
) {
    private val sourceId = MapLayers.Global.STATIONS_SOURCE

    companion object {
        const val MARKER_IMAGE_ID = "Marker-Station"
    }

    fun addToMap(stations: List<Station>) {
        val style = mapboxMap.style ?: return

        // Create GeoJSON from stations
        val features = stations.map { station ->
            Feature.fromGeometry(station.coordinate).apply {
                addStringProperty("id", station.id)
                addStringProperty("name", station.name)
                addStringProperty("type", station.type)
            }
        }

        // Add or update source
        if (!style.styleSourceExists(sourceId)) {
            style.addSource(
                geoJsonSource(sourceId) {
                    featureCollection(FeatureCollection.fromFeatures(features))
                    cluster(true)
                    clusterRadius(100)
                    clusterMaxZoom(14)
                    clusterMinPoints(4)
                }
            )
        } else {
            // Update existing source
            style.getSourceAs<GeoJsonSource>(sourceId)?.featureCollection(
                FeatureCollection.fromFeatures(features)
            )
        }

        // Cluster circles
        if (!style.styleLayerExists(MapLayers.Global.STATIONS_CLUSTER_LAYER)) {
            style.addLayer(
                circleLayer(MapLayers.Global.STATIONS_CLUSTER_LAYER, sourceId) {
                    filter(literal(listOf("has", "point_count")))
                    circleRadius(8.0)
                    circleColor(Color.BLACK)
                    circleOpacity(opacity)
                }
            )
        }

        // Cluster count labels
        if (!style.styleLayerExists(MapLayers.Global.STATIONS_COUNT_LAYER)) {
            style.addLayer(
                symbolLayer(MapLayers.Global.STATIONS_COUNT_LAYER, sourceId) {
                    filter(literal(listOf("has", "point_count")))
                    textField(get { literal("point_count") })
                    textSize(8.0)
                    textColor(Color.WHITE)
                    textAllowOverlap(true)
                    textIgnorePlacement(true)
                    textOpacity(opacity)
                }
            )
        }

        // Individual station markers
        if (!style.styleLayerExists(MapLayers.Global.STATIONS_LAYER)) {
            style.addLayer(
                symbolLayer(MapLayers.Global.STATIONS_LAYER, sourceId) {
                    filter(literal(listOf("!", listOf("has", "point_count"))))
                    iconImage(MARKER_IMAGE_ID)
                    iconSize(0.14)
                    iconAnchor(IconAnchor.BOTTOM)
                    iconAllowOverlap(true)
                    iconOpacity(opacity)
                }
            )
        }
    }

    fun updateStations(stations: List<Station>) {
        val style = mapboxMap.style ?: return
        val features = stations.map { station ->
            Feature.fromGeometry(station.coordinate).apply {
                addStringProperty("id", station.id)
                addStringProperty("name", station.name)
                addStringProperty("type", station.type)
            }
        }

        style.getSourceAs<GeoJsonSource>(sourceId)?.featureCollection(
            FeatureCollection.fromFeatures(features)
        )
    }

    fun updateOpacity(newOpacity: Double) {
        opacity = newOpacity
        val style = mapboxMap.style ?: return
        style.setStyleLayerProperty(MapLayers.Global.STATIONS_CLUSTER_LAYER, "circle-opacity", Value.valueOf(newOpacity))
        style.setStyleLayerProperty(MapLayers.Global.STATIONS_COUNT_LAYER, "text-opacity", Value.valueOf(newOpacity))
        style.setStyleLayerProperty(MapLayers.Global.STATIONS_LAYER, "icon-opacity", Value.valueOf(newOpacity))
    }

    fun removeFromMap() {
        val style = mapboxMap.style ?: return

        listOf(
            MapLayers.Global.STATIONS_CLUSTER_LAYER,
            MapLayers.Global.STATIONS_COUNT_LAYER,
            MapLayers.Global.STATIONS_LAYER
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
