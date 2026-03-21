package com.example.saltyoffshore.data

/**
 * Filter mode for data visualization.
 * Matches iOS FilterMode enum exactly.
 */
enum class FilterMode(val displayName: String) {
    SQUASH("Squash"),       // Remaps color scale to selected range
    HIDE_SHOW("Hide/Show")  // Shows/hides values outside range (transparent)
}
