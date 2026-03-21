package com.example.saltyoffshore.data

/**
 * Defines a selectable variable within a dataset type (e.g., Temperature vs Gradient).
 * Used for variable switching via QuickActionsBar chips.
 */
data class DatasetVariable(
    /** Unique identifier (e.g., "sst", "sst_gradient") */
    val id: String,

    /** Display name for UI chips (e.g., "Temperature", "Gradient") */
    val displayName: String,

    /** Zarr variable name to load (e.g., "sea_surface_temperature", "sst_gradient_magnitude") */
    val zarrVariableName: String,

    /** Key for API range lookup: entry.ranges[rangeKey] */
    val rangeKey: String,

    /** Whether this variable has PMTiles data for crosshair queries.
     * Primary variables (index 0) typically have PMTiles; gradients are Zarr-only. */
    val hasPMTilesData: Boolean,

    /** Whether this variable appears in UI pickers. */
    val isVisible: Boolean = true,

    /** Override colorscale when this variable is active. null = inherit from DatasetType. */
    val colorscale: Colorscale? = null,

    /** Override scale mode when this variable is active. null = inherit from DatasetType. */
    val scaleMode: ScaleMode? = null,

    /** Display unit for this variable (e.g., FAHRENHEIT, INVERSE_SECONDS) */
    val unit: DatasetUnit = DatasetUnit.DIMENSIONLESS,

    /** Decimal places for formatting values (e.g., 1 for temperature, 3 for rotation) */
    val decimalPlaces: Int = 1
) {
    /** Whether this is the primary/default variable */
    val isPrimary: Boolean get() = hasPMTilesData
}

// ScaleMode enum is defined in ScaleMode.kt
