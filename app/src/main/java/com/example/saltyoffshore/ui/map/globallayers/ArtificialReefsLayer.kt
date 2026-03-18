package com.example.saltyoffshore.ui.map.globallayers

import android.graphics.Color
import com.example.saltyoffshore.config.MapLayers
import com.mapbox.bindgen.Value
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.style.expressions.dsl.generated.coalesce
import com.mapbox.maps.extension.style.expressions.dsl.generated.get
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.maps.extension.style.expressions.dsl.generated.zoom
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.extension.style.layers.properties.generated.TextAnchor
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.vectorSource

/**
 * Artificial Reefs layer - point data showing reef locations with labels.
 *
 * Visual styling:
 * - Custom marker icon
 * - White text labels with dark halo for readability
 *
 * Matches iOS ArtificialReefsLayer.
 */
class ArtificialReefsLayer(
    private val mapboxMap: MapboxMap,
    private var opacity: Double = 1.0
) {
    private val sourceId = MapLayers.Global.ARTIFICIAL_REEFS_SOURCE
    private val sourceLayerName = "artificial_reefs"

    companion object {
        const val MARKER_IMAGE_ID = "Marker-Artificial-Reef"
    }

    fun addToMap() {
        val style = mapboxMap.style ?: return

        // Add vector source from Mapbox tileset
        if (!style.styleSourceExists(sourceId)) {
            style.addSource(
                vectorSource(sourceId) {
                    url("mapbox://snowcast.j9wrfvce")
                }
            )
        }

        // Symbol layer with custom marker icon and labels
        if (!style.styleLayerExists(MapLayers.Global.ARTIFICIAL_REEFS_LAYER)) {
            style.addLayer(
                symbolLayer(MapLayers.Global.ARTIFICIAL_REEFS_LAYER, sourceId) {
                    sourceLayer(sourceLayerName)
                    // Icon
                    iconImage(MARKER_IMAGE_ID)
                    iconSize(iconSizeExpression())
                    iconAnchor(IconAnchor.BOTTOM)
                    iconAllowOverlap(true)
                    iconOpacity(opacity)
                    // Label
                    textField(labelExpression())
                    textFont(listOf("Roboto Condensed Bold", "Arial Unicode MS Regular"))
                    textSize(labelSizeExpression())
                    textColor(Color.WHITE)
                    textHaloColor(Color.BLACK)
                    textHaloWidth(1.2)
                    textAnchor(TextAnchor.TOP)
                    textOffset(listOf(0.0, 0.2))
                    textOpacity(opacity)
                    textAllowOverlap(false)
                    minZoom(5.0)
                }
            )
        }
    }

    private fun iconSizeExpression(): Expression {
        return interpolate {
            linear()
            zoom()
            stop(5.0) { literal(0.25) }
            stop(8.0) { literal(0.32) }
            stop(12.0) { literal(0.4) }
        }
    }

    private fun labelExpression(): Expression {
        return coalesce {
            get { literal("reefName") }
            literal("Reef")
        }
    }

    private fun labelSizeExpression(): Expression {
        return interpolate {
            linear()
            zoom()
            stop(5.0) { literal(10.0) }
            stop(8.0) { literal(11.0) }
            stop(12.0) { literal(12.0) }
            stop(14.0) { literal(14.0) }
        }
    }

    fun updateOpacity(newOpacity: Double) {
        opacity = newOpacity
        val style = mapboxMap.style ?: return
        style.setStyleLayerProperty(MapLayers.Global.ARTIFICIAL_REEFS_LAYER, "icon-opacity", Value.valueOf(newOpacity))
        style.setStyleLayerProperty(MapLayers.Global.ARTIFICIAL_REEFS_LAYER, "text-opacity", Value.valueOf(newOpacity))
    }

    fun removeFromMap() {
        val style = mapboxMap.style ?: return

        if (style.styleLayerExists(MapLayers.Global.ARTIFICIAL_REEFS_LAYER)) {
            style.removeStyleLayer(MapLayers.Global.ARTIFICIAL_REEFS_LAYER)
        }

        if (style.styleSourceExists(sourceId)) {
            style.removeStyleSource(sourceId)
        }
    }
}
