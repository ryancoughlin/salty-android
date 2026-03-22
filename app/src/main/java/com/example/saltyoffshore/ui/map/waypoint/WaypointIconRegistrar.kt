package com.example.saltyoffshore.ui.map.waypoint

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import com.example.saltyoffshore.data.waypoint.WaypointSymbol
import com.mapbox.maps.MapboxMap

/**
 * Registers waypoint symbol images into the Mapbox style.
 *
 * The custom Mapbox style already contains most symbol images (RedCircle, Marker-BluefinTuna, etc.).
 * This registrar checks for missing images and generates simple colored-circle fallbacks.
 *
 * Matches iOS pattern where symbols reference style images by name.
 */
object WaypointIconRegistrar {

    /**
     * Ensure all symbols used by the given waypoints are registered in the style.
     * No-op for images that already exist.
     */
    fun ensureRegistered(mapboxMap: MapboxMap, symbols: Set<WaypointSymbol>) {
        val style = mapboxMap.style ?: return

        for (symbol in symbols) {
            val imageName = symbol.imageName
            if (style.hasStyleImage(imageName)) continue

            // Generate a simple fallback circle bitmap
            val bitmap = generateFallbackBitmap(symbol)
            style.addImage(imageName, bitmap)
        }
    }

    /**
     * Generates a 48x48 colored circle as a fallback for missing style images.
     */
    private fun generateFallbackBitmap(symbol: WaypointSymbol): Bitmap {
        val size = 48
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fallbackColor(symbol)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2f, paint)

        // White border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2f, borderPaint)

        return bitmap
    }

    private fun fallbackColor(symbol: WaypointSymbol): Int {
        val name = symbol.rawValue.lowercase()
        return when {
            "red" in name -> android.graphics.Color.rgb(239, 68, 68)
            "blue" in name -> android.graphics.Color.rgb(59, 130, 246)
            "green" in name -> android.graphics.Color.rgb(16, 185, 129)
            "yellow" in name -> android.graphics.Color.rgb(245, 158, 11)
            else -> android.graphics.Color.rgb(107, 114, 128) // gray
        }
    }
}
