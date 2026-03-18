package com.example.saltyoffshore.config

import androidx.compose.ui.unit.dp

/**
 * Crosshair positioning and query configuration.
 * Matches iOS CrosshairConstants exactly.
 */
object CrosshairConstants {
    /**
     * Crosshair Y offset from screen center (negative = above center).
     * iOS uses -40 points.
     */
    val yOffset = (-40).dp

    /**
     * Query box size at various zoom levels.
     * Larger box at lower zoom, smaller at higher zoom for precision.
     */
    fun queryBoxSize(zoom: Double): Int {
        return when {
            zoom < 5 -> 384
            zoom < 6 -> 192
            zoom < 7 -> 96
            zoom < 8 -> 48
            else -> 24
        }
    }

    /**
     * Throttle delay for crosshair queries (ms).
     */
    const val QUERY_THROTTLE_MS = 350L

    /**
     * Minimum zoom to show scale bar.
     */
    const val SCALE_BAR_MIN_ZOOM = 8.0
}
