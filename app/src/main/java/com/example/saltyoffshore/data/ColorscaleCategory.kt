package com.example.saltyoffshore.data

/**
 * Visual categorization for colorscales - used to organize picker UI
 */
enum class ColorscaleCategory(val displayName: String) {
    SINGLE_COLOR("Single Color"),  // Monochromatic - ideal for overlays
    NEUTRAL("Neutral"),            // Greyscale - ideal for overlays
    COLORFUL("Colored")            // Multi-hue - ideal for primary layers
}
