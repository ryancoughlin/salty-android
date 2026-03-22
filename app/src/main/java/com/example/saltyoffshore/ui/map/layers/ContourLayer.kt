package com.example.saltyoffshore.ui.map.layers

import android.graphics.Color
import android.util.Log
import com.example.saltyoffshore.data.ContourLayerState
import com.example.saltyoffshore.data.DatasetType
import com.mapbox.bindgen.Value
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.dsl.generated.all
import com.mapbox.maps.extension.style.expressions.dsl.generated.color
import com.mapbox.maps.extension.style.expressions.dsl.generated.eq
import com.mapbox.maps.extension.style.expressions.dsl.generated.get
import com.mapbox.maps.extension.style.expressions.dsl.generated.gte
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.maps.extension.style.expressions.dsl.generated.lte
import com.mapbox.maps.extension.style.expressions.dsl.generated.neq
import com.mapbox.maps.extension.style.expressions.dsl.generated.rgb
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.layers.properties.generated.SymbolPlacement
import com.mapbox.maps.extension.style.layers.properties.generated.TextTransform

private const val TAG = "ContourLayer"

/**
 * Contour layer renderer — dispatches to per-dataset-type rendering.
 * Matches iOS ContourRenderer routing exactly:
 * - SST → StandardContourLayer (major/decimal lines + labels)
 * - SSH → SSHContourLayer (strength-based + labels)
 * - MLD → MLDContourLayer (single line + labels)
 * - Salinity/FSLE → SalinityContourLayer (single line + labels)
 * - DissolvedOxygen/Chlorophyll → DissolvedOxygenContourLayer (major/decimal + labels)
 * - Phytoplankton → PhytoplanktonContourLayer (major/minor + labels)
 */
class ContourLayer(
    private val mapboxMap: MapboxMap,
    private val state: ContourLayerState
) {
    private val createdLayerIds = mutableListOf<String>()

    fun addToMap() {
        val style = mapboxMap.style ?: return
        when (state.datasetType) {
            DatasetType.SST -> addStandardContour(style)
            DatasetType.SEA_SURFACE_HEIGHT -> addSSHContour(style)
            DatasetType.MLD -> addSimpleContour(style, spacing = 125.0, padding = 5.0)
            DatasetType.SALINITY, DatasetType.FSLE -> addSimpleContour(style, spacing = 125.0, padding = 5.0)
            DatasetType.DISSOLVED_OXYGEN, DatasetType.CHLOROPHYLL -> addDissolvedOxygenContour(style)
            DatasetType.PHYTOPLANKTON -> addPhytoplanktonContour(style)
            else -> addStandardContour(style)
        }
        Log.d(TAG, "Added ${createdLayerIds.size} contour layers for ${state.datasetType}")
    }

    // MARK: - Standard Contour (SST)
    // iOS: StandardContourLayer — major/decimal lines + labels

    private fun addStandardContour(style: Style) {
        val rangeFilter = buildRangeFilter()
        val lineColor = dynamicColorExpression()

        addLineLayerIfNeeded(style, "${state.layerId}-major") {
            state.sourceLayer?.let { sourceLayer(it) }
            filter(all { eq { get("is_major"); literal(true) }; rangeFilter })
            lineColor(lineColor)
            lineWidth(1.5)
            lineOpacity(state.opacity)
            lineCap(LineCap.ROUND)
            lineJoin(LineJoin.ROUND)
        }

        addLineLayerIfNeeded(style, "${state.layerId}-decimal") {
            state.sourceLayer?.let { sourceLayer(it) }
            filter(all { eq { get("is_major"); literal(false) }; rangeFilter })
            lineColor(lineColor)
            lineWidth(1.0)
            lineOpacity(state.opacity * 0.7)
            lineCap(LineCap.ROUND)
            lineJoin(LineJoin.ROUND)
            minZoom(7.0)
        }

        addSymbolLayerIfNeeded(style, "${state.layerId}-labels-major") {
            state.sourceLayer?.let { sourceLayer(it) }
            filter(all { eq { get("is_major"); literal(true) }; rangeFilter })
            textColor(color(Color.BLACK))
            textHaloColor(color(Color.WHITE))
            textHaloWidth(2.0)
            textFont(listOf("League Mono Regular"))
            symbolPlacement(SymbolPlacement.LINE)
            textField(get(state.datasetType.contourLabel))
            textSize(12.0)
            textMaxAngle(45.0)
            textAllowOverlap(false)
            textIgnorePlacement(false)
            symbolSpacing(170.0)
            textPadding(2.0)
            textOpacity(state.opacity)
            minZoom(7.0)
        }

        addSymbolLayerIfNeeded(style, "${state.layerId}-labels-minor") {
            state.sourceLayer?.let { sourceLayer(it) }
            filter(all { eq { get("is_major"); literal(false) }; rangeFilter })
            textColor(color(Color.BLACK))
            textHaloColor(color(Color.WHITE))
            textHaloWidth(1.5)
            textFont(listOf("League Mono Regular"))
            symbolPlacement(SymbolPlacement.LINE)
            textField(get(state.datasetType.contourLabel))
            textSize(10.0)
            textMaxAngle(45.0)
            textAllowOverlap(false)
            textIgnorePlacement(false)
            symbolSpacing(170.0)
            textPadding(2.0)
            textOpacity(state.opacity * 0.8)
            minZoom(9.0)
        }
    }

    // MARK: - SSH Contour
    // iOS: SSHContourLayer — strength-based lines + labels

    private fun addSSHContour(style: Style) {
        val rangeFilter = buildRangeFilter()
        val valueField = state.fieldName
        val sshColor = sshDynamicColorExpression()

        // Neutral line (SSH = 0) — always visible
        addLineLayerIfNeeded(style, "${state.layerId}-neutral-line") {
            state.sourceLayer?.let { sourceLayer(it) }
            filter(all { eq { get(valueField); literal(0) }; rangeFilter })
            lineWidth(2.5)
            lineOpacity(1.0)
            lineColor(color(Color.BLACK))
        }

        // Weak strength lines (dashed)
        addLineLayerIfNeeded(style, "${state.layerId}-weak") {
            state.sourceLayer?.let { sourceLayer(it) }
            filter(all { eq { get("strength"); literal("weak") }; rangeFilter })
            lineDasharray(listOf(8.0, 4.0))
            lineWidth(1.75)
            lineOpacity(state.opacity)
            lineColor(sshColor)
            minZoom(6.0)
        }

        // Moderate strength lines (dashed)
        addLineLayerIfNeeded(style, "${state.layerId}-moderate") {
            state.sourceLayer?.let { sourceLayer(it) }
            filter(all { eq { get("strength"); literal("moderate") }; rangeFilter })
            lineDasharray(listOf(12.0, 6.0))
            lineWidth(2.0)
            lineOpacity(state.opacity)
            lineColor(sshColor)
            minZoom(4.0)
        }

        // Strong strength lines (solid)
        addLineLayerIfNeeded(style, "${state.layerId}-strong") {
            state.sourceLayer?.let { sourceLayer(it) }
            filter(all { eq { get("strength"); literal("strong") }; rangeFilter })
            lineWidth(2.25)
            lineOpacity(state.opacity)
            lineColor(sshColor)
            minZoom(2.0)
        }

        // Zero line labels — always visible
        addSymbolLayerIfNeeded(style, "${state.layerId}-zero-labels") {
            state.sourceLayer?.let { sourceLayer(it) }
            filter(eq { get(valueField); literal(0) })
            textColor(color(Color.BLACK))
            textHaloColor(color(Color.WHITE))
            textHaloWidth(1.5)
            textFont(listOf("League Mono Regular"))
            symbolPlacement(SymbolPlacement.LINE_CENTER)
            textField(get(valueField))
            textSize(14.0)
            textTransform(TextTransform.UPPERCASE)
            textMaxAngle(45.0)
            symbolSpacing(50.0)
            textPadding(5.0)
            textOpacity(state.opacity)
        }

        // Strength labels (strong, moderate, weak)
        addSSHStrengthLabels(style, "strong", minZoom = 2.0)
        addSSHStrengthLabels(style, "moderate", minZoom = 4.0)
        addSSHStrengthLabels(style, "weak", minZoom = 6.0)
    }

    private fun addSSHStrengthLabels(style: Style, strength: String, minZoom: Double) {
        val valueField = state.fieldName
        addSymbolLayerIfNeeded(style, "${state.layerId}-labels-$strength") {
            state.sourceLayer?.let { sourceLayer(it) }
            filter(all {
                eq { get("strength"); literal(strength) }
                neq { get(valueField); literal(0) }
            })
            textColor(color(Color.BLACK))
            textHaloColor(color(Color.WHITE))
            textHaloWidth(1.5)
            textFont(listOf("League Mono Regular"))
            symbolPlacement(SymbolPlacement.LINE_CENTER)
            textField(get(valueField))
            textSize(14.0)
            textTransform(TextTransform.UPPERCASE)
            textMaxAngle(45.0)
            textAllowOverlap(false)
            symbolSpacing(100.0)
            textPadding(5.0)
            textOpacity(state.opacity)
            minZoom(minZoom)
        }
    }

    // MARK: - Simple Contour (MLD, Salinity, FSLE)
    // iOS: MLDContourLayer / SalinityContourLayer — single line + labels

    private fun addSimpleContour(style: Style, spacing: Double, padding: Double) {
        val rangeFilter = buildRangeFilter()
        val lineColor = dynamicColorExpression()

        addLineLayerIfNeeded(style, state.layerId) {
            state.sourceLayer?.let { sourceLayer(it) }
            filter(rangeFilter)
            lineColor(lineColor)
            lineWidth(2.0)
            lineOpacity(state.opacity)
            lineCap(LineCap.ROUND)
            lineJoin(LineJoin.ROUND)
        }

        addSymbolLayerIfNeeded(style, "${state.layerId}-labels") {
            state.sourceLayer?.let { sourceLayer(it) }
            filter(rangeFilter)
            textColor(color(Color.BLACK))
            textHaloColor(color(Color.WHITE))
            textHaloWidth(1.5)
            textFont(listOf("League Mono Regular"))
            symbolPlacement(SymbolPlacement.LINE)
            textField(get(state.datasetType.contourLabel))
            textSize(12.0)
            textMaxAngle(45.0)
            textAllowOverlap(false)
            symbolSpacing(spacing)
            textPadding(padding)
            textOpacity(state.opacity)
        }
    }

    // MARK: - Dissolved Oxygen / Chlorophyll Contour
    // iOS: DissolvedOxygenContourLayer — major/decimal lines + labels (wider lines)

    private fun addDissolvedOxygenContour(style: Style) {
        val rangeFilter = buildRangeFilter()
        val lineColor = dynamicColorExpression()

        addLineLayerIfNeeded(style, "${state.layerId}-major") {
            state.sourceLayer?.let { sourceLayer(it) }
            filter(all { eq { get("is_major"); literal(true) }; rangeFilter })
            lineColor(lineColor)
            lineWidth(2.5)
            lineOpacity(state.opacity)
            lineCap(LineCap.ROUND)
            lineJoin(LineJoin.ROUND)
        }

        addLineLayerIfNeeded(style, "${state.layerId}-decimal") {
            state.sourceLayer?.let { sourceLayer(it) }
            filter(all { eq { get("is_major"); literal(false) }; rangeFilter })
            lineColor(lineColor)
            lineWidth(1.5)
            lineOpacity(state.opacity * 0.7)
            lineCap(LineCap.ROUND)
            lineJoin(LineJoin.ROUND)
            minZoom(7.0)
        }

        addSymbolLayerIfNeeded(style, "${state.layerId}-labels-major") {
            state.sourceLayer?.let { sourceLayer(it) }
            filter(all { eq { get("is_major"); literal(true) }; rangeFilter })
            textColor(color(Color.BLACK))
            textHaloColor(color(Color.WHITE))
            textHaloWidth(2.0)
            textFont(listOf("League Mono Regular"))
            symbolPlacement(SymbolPlacement.LINE)
            textField(get(state.datasetType.contourLabel))
            textSize(12.0)
            textMaxAngle(45.0)
            textAllowOverlap(false)
            symbolSpacing(150.0)
            textPadding(8.0)
            textOpacity(state.opacity)
            minZoom(7.0)
        }

        addSymbolLayerIfNeeded(style, "${state.layerId}-labels-minor") {
            state.sourceLayer?.let { sourceLayer(it) }
            filter(all { eq { get("is_major"); literal(false) }; rangeFilter })
            textColor(color(Color.BLACK))
            textHaloColor(color(Color.WHITE))
            textHaloWidth(1.5)
            textFont(listOf("League Mono Regular"))
            symbolPlacement(SymbolPlacement.LINE)
            textField(get(state.datasetType.contourLabel))
            textSize(10.0)
            textMaxAngle(45.0)
            textAllowOverlap(false)
            symbolSpacing(200.0)
            textPadding(6.0)
            textOpacity(state.opacity * 0.8)
            minZoom(9.0)
        }
    }

    // MARK: - Phytoplankton Contour
    // iOS: PhytoplanktonContourLayer — major/minor with phyc_label field

    private fun addPhytoplanktonContour(style: Style) {
        val rangeFilter = buildRangeFilter()
        val lineColor = dynamicColorExpression()

        addLineLayerIfNeeded(style, "${state.layerId}-major") {
            state.sourceLayer?.let { sourceLayer(it) }
            filter(all { eq { get("is_major"); literal(true) }; rangeFilter })
            lineColor(lineColor)
            lineWidth(2.5)
            lineOpacity(state.opacity)
            lineCap(LineCap.ROUND)
            lineJoin(LineJoin.ROUND)
        }

        addLineLayerIfNeeded(style, "${state.layerId}-minor") {
            state.sourceLayer?.let { sourceLayer(it) }
            filter(all { eq { get("is_major"); literal(false) }; rangeFilter })
            lineColor(lineColor)
            lineWidth(1.5)
            lineOpacity(state.opacity * 0.7)
            lineCap(LineCap.ROUND)
            lineJoin(LineJoin.ROUND)
            minZoom(7.0)
        }

        addSymbolLayerIfNeeded(style, "${state.layerId}-labels-major") {
            state.sourceLayer?.let { sourceLayer(it) }
            filter(all { eq { get("is_major"); literal(true) }; rangeFilter })
            textColor(color(Color.BLACK))
            textHaloColor(color(Color.WHITE))
            textHaloWidth(2.0)
            textFont(listOf("League Mono Regular"))
            symbolPlacement(SymbolPlacement.LINE)
            textField(get("phyc_label"))
            textSize(12.0)
            textMaxAngle(45.0)
            textAllowOverlap(false)
            symbolSpacing(150.0)
            textPadding(8.0)
            textOpacity(state.opacity)
            minZoom(7.0)
        }

        addSymbolLayerIfNeeded(style, "${state.layerId}-labels-minor") {
            state.sourceLayer?.let { sourceLayer(it) }
            filter(all { eq { get("is_major"); literal(false) }; rangeFilter })
            textColor(color(Color.BLACK))
            textHaloColor(color(Color.WHITE))
            textHaloWidth(1.5)
            textFont(listOf("League Mono Regular"))
            symbolPlacement(SymbolPlacement.LINE)
            textField(get("phyc_label"))
            textSize(10.0)
            textMaxAngle(45.0)
            textAllowOverlap(false)
            symbolSpacing(200.0)
            textPadding(6.0)
            textOpacity(state.opacity * 0.8)
            minZoom(9.0)
        }
    }

    // MARK: - Expression Builders

    private fun buildRangeFilter(): Expression {
        return all {
            gte { get(state.fieldName); literal(state.valueRange.start) }
            lte { get(state.fieldName); literal(state.valueRange.endInclusive) }
        }
    }

    private fun dynamicColorExpression(): Expression = color(state.color)

    /**
     * SSH 3-color interpolation (blue → black → red).
     * iOS: always uses this for SSH regardless of dynamicColoring toggle.
     */
    private fun sshDynamicColorExpression(): Expression {
        return interpolate {
            linear()
            get { literal(state.fieldName) }
            stop(-1.0) { rgb(0.0, 76.0, 140.0) }  // #004a8c Blue (cyclonic)
            stop(0.0) { rgb(0.0, 0.0, 0.0) }       // Black (convergence)
            stop(1.0) { rgb(204.0, 0.0, 0.0) }      // #cc0000 Red (anticyclonic)
        }
    }

    // MARK: - Layer Helpers

    private fun addLineLayerIfNeeded(style: Style, id: String, block: com.mapbox.maps.extension.style.layers.generated.LineLayerDsl.() -> Unit) {
        if (!style.styleLayerExists(id)) {
            style.addLayer(lineLayer(id, state.sourceId, block))
            createdLayerIds.add(id)
        }
    }

    private fun addSymbolLayerIfNeeded(style: Style, id: String, block: com.mapbox.maps.extension.style.layers.generated.SymbolLayerDsl.() -> Unit) {
        if (!style.styleLayerExists(id)) {
            style.addLayer(symbolLayer(id, state.sourceId, block))
            createdLayerIds.add(id)
        }
    }

    // MARK: - Updates

    fun updateOpacity(newOpacity: Double) {
        val style = mapboxMap.style ?: return
        for (id in createdLayerIds) {
            if (!style.styleLayerExists(id)) continue
            if (id.contains("labels") || id.contains("zero-labels")) {
                val multiplier = if (id.endsWith("-minor")) 0.8 else 1.0
                style.setStyleLayerProperty(id, "text-opacity", Value.valueOf(newOpacity * multiplier))
            } else {
                val multiplier = when {
                    id.endsWith("-decimal") || id.endsWith("-minor") -> 0.7
                    else -> 1.0
                }
                style.setStyleLayerProperty(id, "line-opacity", Value.valueOf(newOpacity * multiplier))
            }
        }
    }

    fun removeFromMap() {
        val style = mapboxMap.style ?: return
        for (id in createdLayerIds) {
            if (style.styleLayerExists(id)) {
                style.removeStyleLayer(id)
            }
        }
        createdLayerIds.clear()
    }

    companion object {
        fun layerId(regionId: String) = "contour-layer-$regionId"
    }
}
