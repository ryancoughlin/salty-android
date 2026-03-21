package com.example.saltyoffshore.data

/**
 * Filter mode for data visualization.
 * Matches iOS FilterMode enum exactly.
 */
enum class FilterMode {
    SQUASH,    // Remaps color scale to selected range
    HIDE_SHOW; // Shows/hides values outside range (transparent)

    val displayName: String
        get() = when (this) {
            HIDE_SHOW -> "Hide/Show"
            SQUASH -> "Squash"
        }
}
