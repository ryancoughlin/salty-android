package com.example.saltyoffshore.ui.map.globallayers

import android.graphics.Color
import com.example.saltyoffshore.config.AppConstants
import com.example.saltyoffshore.config.MapLayers
import com.mapbox.bindgen.Value
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.style.expressions.dsl.generated.concat
import com.mapbox.maps.extension.style.expressions.dsl.generated.eq
import com.mapbox.maps.extension.style.expressions.dsl.generated.get
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.maps.extension.style.expressions.dsl.generated.match
import com.mapbox.maps.extension.style.expressions.dsl.generated.zoom
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.fillLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.TextTransform
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.vectorSource

/**
 * Marine Protected Areas layer with color-coded fishing restrictions.
 *
 * Visual hierarchy:
 * 1. Fill color indicates restriction severity
 * 2. Colored border with severity-based width
 * 3. Labels showing site name and restriction status
 *
 * Colors:
 * - Red: Fishing prohibited
 * - Orange: Partial prohibition
 * - Amber: Restricted fishing
 * - Green: No restrictions
 * - Gray: Unknown
 *
 * Matches iOS MarineProtectedAreasLayer.
 */
class MarineProtectedAreasLayer(
    private val mapboxMap: MapboxMap,
    private var opacity: Double = 0.7
) {
    private val sourceId = MapLayers.Global.MPA_SOURCE
    private val sourceLayerName = "marine_protected_areas"

    // Hex colors for Mapbox expressions
    private val hexProhibited = "#D32F2F"      // Red - full prohibition
    private val hexPartialBan = "#E65100"      // Orange - partial ban
    private val hexRestricted = "#FFC107"      // Amber - restricted
    private val hexAllowed = "#4CAF50"         // Green - no restrictions
    private val hexUnknown = "#9E9E9E"         // Gray - unknown

    // Border colors (darker variants)
    private val hexProhibitedBorder = "#B71C1C"
    private val hexPartialBanBorder = "#BF360C"
    private val hexAllowedBorder = "#2E7D32"
    private val hexDefaultBorder = "#5D4037"

    fun addToMap() {
        val style = mapboxMap.style ?: return

        // Add vector source
        if (!style.styleSourceExists(sourceId)) {
            style.addSource(
                vectorSource(sourceId) {
                    tiles(listOf(AppConstants.marineProtectedAreasTileURL))
                    maxzoom(16)
                }
            )
        }

        // 1. Base fill layer with restriction-based colors
        if (!style.styleLayerExists(MapLayers.Global.MPA_FILL_LAYER)) {
            style.addLayer(
                fillLayer(MapLayers.Global.MPA_FILL_LAYER, sourceId) {
                    sourceLayer(sourceLayerName)
                    fillColor(fillColorExpression())
                    fillOpacity(opacity * 0.30)
                }
            )
        }

        // 2. Outline layer with severity-based colors and widths
        if (!style.styleLayerExists(MapLayers.Global.MPA_OUTLINE_LAYER)) {
            style.addLayer(
                lineLayer(MapLayers.Global.MPA_OUTLINE_LAYER, sourceId) {
                    sourceLayer(sourceLayerName)
                    lineColor(lineColorExpression())
                    lineWidth(lineWidthExpression())
                    lineOpacity(opacity)
                }
            )
        }

        // 3. Labels showing site name and restriction badge
        if (!style.styleLayerExists(MapLayers.Global.MPA_LABELS_LAYER)) {
            style.addLayer(
                symbolLayer(MapLayers.Global.MPA_LABELS_LAYER, sourceId) {
                    sourceLayer(sourceLayerName)
                    textField(labelExpression())
                    textFont(listOf("Roboto Condensed Bold"))
                    textSize(labelSizeExpression())
                    textColor(Color.WHITE)
                    textHaloColor(labelHaloColorExpression())
                    textHaloWidth(1.5)
                    textTransform(TextTransform.UPPERCASE)
                    textOpacity(opacity)
                    minZoom(7.0)
                }
            )
        }
    }

    private fun fillColorExpression(): Expression {
        return match {
            get { literal("Fish_Rstr") }
            literal("Commercial and Recreational Fishing Prohibited"); literal(hexProhibited)
            literal("Commercial Fishing Prohibited"); literal(hexPartialBan)
            literal("Commercial Fishing Prohibited and Recreational Fishing Restricted"); literal(hexPartialBan)
            literal("Recreational Fishing Prohibited"); literal(hexPartialBan)
            literal("Commercial Fishing Restricted and Recreational Fishing Prohibited"); literal(hexPartialBan)
            literal("Commercial and Recreational Fishing Restricted"); literal(hexRestricted)
            literal("Commercial Fishing Restricted"); literal(hexRestricted)
            literal("Recreational Fishing Restricted"); literal(hexRestricted)
            literal("No Site Restrictions"); literal(hexAllowed)
            literal(hexUnknown)
        }
    }

    private fun lineColorExpression(): Expression {
        return match {
            get { literal("Fish_Rstr") }
            literal("Commercial and Recreational Fishing Prohibited"); literal(hexProhibitedBorder)
            literal("Commercial Fishing Prohibited"); literal(hexPartialBanBorder)
            literal("Recreational Fishing Prohibited"); literal(hexPartialBanBorder)
            literal("No Site Restrictions"); literal(hexAllowedBorder)
            literal(hexDefaultBorder)
        }
    }

    private fun lineWidthExpression(): Expression {
        return match {
            get { literal("Fish_Rstr") }
            literal("Commercial and Recreational Fishing Prohibited"); literal(2.5)
            literal("No Site Restrictions"); literal(1.0)
            literal(1.5)
        }
    }

    private fun labelExpression(): Expression {
        return concat {
            get { literal("Site_Name") }
            literal("\n")
            restrictionBadgeExpression()
        }
    }

    private fun restrictionBadgeExpression(): Expression {
        return match {
            get { literal("Fish_Rstr") }
            literal("Commercial and Recreational Fishing Prohibited"); literal("NO FISHING")
            literal("Commercial Fishing Prohibited"); literal("NO COMMERCIAL")
            literal("Recreational Fishing Prohibited"); literal("NO RECREATIONAL")
            literal("Commercial Fishing Prohibited and Recreational Fishing Restricted"); literal("NO COMMERCIAL")
            literal("Commercial Fishing Restricted and Recreational Fishing Prohibited"); literal("NO RECREATIONAL")
            literal("Commercial and Recreational Fishing Restricted"); literal("RESTRICTED")
            literal("Commercial Fishing Restricted"); literal("RESTRICTED")
            literal("Recreational Fishing Restricted"); literal("RESTRICTED")
            literal("No Site Restrictions"); literal("")
            literal("CHECK RULES")
        }
    }

    private fun labelSizeExpression(): Expression {
        return interpolate {
            linear()
            zoom()
            stop(7.0) { literal(10.0) }
            stop(10.0) { literal(12.0) }
            stop(14.0) { literal(14.0) }
        }
    }

    private fun labelHaloColorExpression(): Expression {
        return match {
            get { literal("Fish_Rstr") }
            literal("Commercial and Recreational Fishing Prohibited"); literal(hexProhibitedBorder)
            literal("Commercial Fishing Prohibited"); literal(hexPartialBanBorder)
            literal("Recreational Fishing Prohibited"); literal(hexPartialBanBorder)
            literal("No Site Restrictions"); literal(hexAllowedBorder)
            literal(hexDefaultBorder)
        }
    }

    fun updateOpacity(newOpacity: Double) {
        opacity = newOpacity
        val style = mapboxMap.style ?: return
        style.setStyleLayerProperty(MapLayers.Global.MPA_FILL_LAYER, "fill-opacity", Value.valueOf(newOpacity * 0.30))
        style.setStyleLayerProperty(MapLayers.Global.MPA_OUTLINE_LAYER, "line-opacity", Value.valueOf(newOpacity))
        style.setStyleLayerProperty(MapLayers.Global.MPA_LABELS_LAYER, "text-opacity", Value.valueOf(newOpacity))
    }

    fun removeFromMap() {
        val style = mapboxMap.style ?: return

        listOf(
            MapLayers.Global.MPA_FILL_LAYER,
            MapLayers.Global.MPA_OUTLINE_LAYER,
            MapLayers.Global.MPA_LABELS_LAYER
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
