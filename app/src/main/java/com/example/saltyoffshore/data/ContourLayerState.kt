package com.example.saltyoffshore.data

import android.graphics.Color

/**
 * State for contour line rendering.
 * Mirrors iOS ContourLayerState exactly.
 */
data class ContourLayerState(
    val color: Int,
    val opacity: Double,
    val valueRange: ClosedFloatingPointRange<Double>,
    val datasetType: DatasetType,
    val dynamicColoring: Boolean,
    val sourceLayer: String?,
    val sourceId: String,
    val layerId: String
) {
    val fieldName: String
        get() = datasetType.contourFieldName

    companion object {
        // SSH 3-color constants (Eddy layer)
        val EDDY_NEGATIVE_COLOR = Color.rgb(0, 76, 140)    // #004a8c - Blue (cyclonic)
        val EDDY_NEUTRAL_COLOR = Color.BLACK               // Black (convergence)
        val EDDY_POSITIVE_COLOR = Color.rgb(204, 0, 0)     // #cc0000 - Red (anticyclonic)

        fun default(
            datasetType: DatasetType,
            sourceId: String,
            layerId: String
        ) = ContourLayerState(
            color = Color.BLACK,
            opacity = 1.0,
            valueRange = 0.0..100.0,
            datasetType = datasetType,
            dynamicColoring = false,
            sourceLayer = "contours",
            sourceId = sourceId,
            layerId = layerId
        )
    }
}
