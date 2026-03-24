package com.example.saltyoffshore.data.weather

/**
 * Toggle between chart and table forecast views.
 * Matches iOS ForecastViewMode.
 */
enum class ForecastViewMode {
    CHART,
    TABLE;

    val label: String
        get() = name.lowercase().replaceFirstChar { it.uppercase() }
}
