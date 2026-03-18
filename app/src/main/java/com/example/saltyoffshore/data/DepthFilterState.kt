package com.example.saltyoffshore.data

/**
 * State management for subsurface depth filtering.
 * Manages depth selection for datasets that support multiple depth levels.
 */
data class DepthFilterState(
    /** Currently selected depth in meters (0 = surface) */
    val selectedDepth: Int = 0,
    /** Available depths for current dataset */
    val availableDepths: List<Int> = listOf(0)
) {
    /** Whether depth selection UI should be shown (derived from availableDepths) */
    val hasSelection: Boolean
        get() = availableDepths.size > 1

    /** Get display label for depth */
    fun displayLabel(depth: Int): String {
        return if (depth == 0) "Surface" else "${depth}m"
    }

    /** Get short label for depth (for compact UI) */
    fun shortLabel(depth: Int): String {
        return "${depth}m"
    }

    /** Reset to surface */
    fun resetToSurface(): DepthFilterState {
        return copy(selectedDepth = 0)
    }
}
