package com.example.saltyoffshore.ui.map.globallayers

import android.graphics.Color
import com.example.saltyoffshore.config.AppConstants
import com.example.saltyoffshore.config.MapLayers
import com.example.saltyoffshore.data.DepthUnits
import com.mapbox.bindgen.Value
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.style.expressions.dsl.generated.concat
import com.mapbox.maps.extension.style.expressions.dsl.generated.get
import com.mapbox.maps.extension.style.expressions.dsl.generated.has
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.maps.extension.style.expressions.dsl.generated.numberFormat
import com.mapbox.maps.extension.style.expressions.dsl.generated.toString
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.layers.generated.SymbolLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.layers.properties.generated.SymbolPlacement
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.vectorSource

/**
 * Bathymetry contours from regional vector tile source.
 * z5-z12, layer "bathymetry", fields: depth_m, fathom_label, is_100_fathom, is_marked_line.
 *
 * Matches iOS BathymetryLayer.
 */
class BathymetryLayer(
    private val mapboxMap: MapboxMap,
    private val depthUnits: DepthUnits = DepthUnits.FATHOMS,
    private var opacity: Double = 0.2
) {
    private val sourceId = MapLayers.Global.BATHYMETRY_SOURCE
    private val sourceLayerName = "bathymetry"

    companion object {
        private const val SYMBOL_SPACING = 180.0
    }

    fun addToMap() {
        val style = mapboxMap.style ?: return

        // Add vector source
        if (!style.styleSourceExists(sourceId)) {
            style.addSource(
                vectorSource(sourceId) {
                    tiles(listOf(AppConstants.bathymetryContoursURL))
                    minzoom(5)
                    maxzoom(12)
                }
            )
        }

        // Regular contour lines
        if (!style.styleLayerExists(MapLayers.Global.BATHYMETRY_LINES_LAYER)) {
            style.addLayer(
                lineLayer(MapLayers.Global.BATHYMETRY_LINES_LAYER, sourceId) {
                    sourceLayer(sourceLayerName)
                    lineColor(Color.BLACK)
                    lineWidth(1.0)
                    lineOpacity(opacity)
                    minZoom(5.0)
                    slot("middle")
                }
            )
        }

        // Depth labels along regular contour lines
        if (!style.styleLayerExists(MapLayers.Global.BATHYMETRY_LABELS_LAYER)) {
            style.addLayer(
                symbolLayer(MapLayers.Global.BATHYMETRY_LABELS_LAYER, sourceId) {
                    sourceLayer(sourceLayerName)
                    filter(has { literal("depth_m") })
                    symbolPlacement(SymbolPlacement.LINE)
                    symbolSpacing(SYMBOL_SPACING)
                    textFont(listOf("Roboto Condensed Bold"))
                    textField(depthLabelExpression())
                    textSize(11.0)
                    textColor(Color.BLACK)
                    textHaloColor(Color.WHITE)
                    textHaloWidth(1.3)
                    textOpacity(1.0)
                    minZoom(5.0)
                    slot("middle")
                }
            )
        }

        // Marked fathom lines (100 FATHOM, etc.) - thicker stroke
        if (!style.styleLayerExists(MapLayers.Global.BATHYMETRY_MARKED_LINES_LAYER)) {
            style.addLayer(
                lineLayer(MapLayers.Global.BATHYMETRY_MARKED_LINES_LAYER, sourceId) {
                    sourceLayer(sourceLayerName)
                    filter(has { literal("fathom_label") })
                    lineColor(Color.BLACK)
                    lineWidth(2.5)
                    lineOpacity(opacity)
                    minZoom(5.0)
                    slot("middle")
                }
            )
        }

        // Marked fathom labels
        if (!style.styleLayerExists(MapLayers.Global.BATHYMETRY_MARKED_LABELS_LAYER)) {
            style.addLayer(
                symbolLayer(MapLayers.Global.BATHYMETRY_MARKED_LABELS_LAYER, sourceId) {
                    sourceLayer(sourceLayerName)
                    filter(has { literal("fathom_label") })
                    symbolPlacement(SymbolPlacement.LINE)
                    symbolSpacing(SYMBOL_SPACING)
                    textFont(listOf("Roboto Condensed Bold"))
                    textField(
                        concat {
                            toString { get { literal("fathom_label") } }
                            literal(" FATHOM")
                        }
                    )
                    textSize(12.0)
                    textColor(Color.BLACK)
                    textHaloColor(Color.WHITE)
                    textHaloWidth(1.5)
                    textOpacity(1.0)
                    textAllowOverlap(true)
                    textIgnorePlacement(false)
                    minZoom(5.0)
                    slot("middle")
                }
            )
        }
    }

    private fun depthLabelExpression(): Expression {
        val factor = depthUnits.metersToUnitFactor
        val suffix = depthUnits.unitSuffix

        // Build the depth conversion expression: abs(depth_m * factor)
        val depthExpr = Expression.abs(
            Expression.product(
                Expression.get("depth_m"),
                Expression.literal(factor)
            )
        )

        return concat {
            numberFormat(depthExpr) {
                minFractionDigits(0)
                maxFractionDigits(0)
            }
            literal(suffix)
        }
    }

    fun updateOpacity(newOpacity: Double) {
        opacity = newOpacity
        val style = mapboxMap.style ?: return
        style.setStyleLayerProperty(MapLayers.Global.BATHYMETRY_LINES_LAYER, "line-opacity", Value.valueOf(newOpacity))
        style.setStyleLayerProperty(MapLayers.Global.BATHYMETRY_MARKED_LINES_LAYER, "line-opacity", Value.valueOf(newOpacity))
    }

    fun removeFromMap() {
        val style = mapboxMap.style ?: return

        listOf(
            MapLayers.Global.BATHYMETRY_LINES_LAYER,
            MapLayers.Global.BATHYMETRY_LABELS_LAYER,
            MapLayers.Global.BATHYMETRY_MARKED_LINES_LAYER,
            MapLayers.Global.BATHYMETRY_MARKED_LABELS_LAYER
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
