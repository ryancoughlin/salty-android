package com.example.saltyoffshore.data

/**
 * Controls how raster data is interpolated between pixels.
 * Matches iOS ResamplingMethod exactly.
 */
enum class ResamplingMethod(val rawValue: String, val displayName: String) {
    NEAREST("nearest", "No Blending"),
    BILINEAR("bilinear", "Soft Transitions");

    companion object {
        val default = BILINEAR
    }
}
