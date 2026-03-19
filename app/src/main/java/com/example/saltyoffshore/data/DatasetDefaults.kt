package com.example.saltyoffshore.data

/**
 * Centralized defaults for dataset rendering.
 * Used by both primary datasets and overlays.
 * Matches iOS DatasetDefaults exactly.
 */
data class DatasetDefaults(
    val contourEnabled: Boolean,
    val contourOpacity: Double,
    val visualOpacity: Double,
    val arrowsEnabled: Boolean,
    val arrowsOpacity: Double,
    val particlesEnabled: Boolean,
    val numbersEnabled: Boolean,
    val numbersOpacity: Double
) {
    companion object {
        val standard = DatasetDefaults(
            contourEnabled = false,
            contourOpacity = 1.0,
            visualOpacity = 1.0,
            arrowsEnabled = false,
            arrowsOpacity = 1.0,
            particlesEnabled = false,
            numbersEnabled = false,
            numbersOpacity = 1.0
        )
    }
}
