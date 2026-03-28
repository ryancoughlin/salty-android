package com.example.saltyoffshore.data

/**
 * Filter mode for data visualization.
 * Matches iOS FilterMode enum exactly.
 */
enum class FilterMode(val displayName: String, val rawValue: String) {
    SQUASH("Squash", "squash"),
    HIDE_SHOW("Hide/Show", "hideShow")
}
