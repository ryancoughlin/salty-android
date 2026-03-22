package com.example.saltyoffshore.config

import androidx.compose.ui.unit.dp
import kotlin.math.pow

/**
 * Crosshair positioning and query configuration.
 * Matches iOS CrosshairConstants exactly.
 */
object CrosshairConstants {
    val yOffset = (-40).dp

    private const val BASE_SIZE = 20.0

    /**
     * Query box size scaled by zoom level.
     * Higher zoom → grid points spread further on screen → bigger box.
     */
    fun queryBoxSize(zoomLevel: Double): Double {
        val zoomOffset = zoomLevel - 7.0
        val scaled = BASE_SIZE * 1.5.pow(zoomOffset)
        return scaled.coerceIn(16.0, 80.0)
    }

    const val QUERY_THROTTLE_MS = 150L

    const val SCALE_BAR_MIN_ZOOM = 8.0
}
