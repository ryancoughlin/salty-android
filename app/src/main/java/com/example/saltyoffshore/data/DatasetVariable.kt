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

/**
 * Defines how raw data values map to the 0-1 colormap range.
 * Used by both TiTiler (COG) and Zarr shader.
 */
enum class ScaleMode(val value: Int) {
    /** Linear: value maps directly to colormap position.
     * Use for: SST, MLD, Salinity, FSLE, Dissolved O₂ */
    LINEAR(0),

    /** Logarithmic: log10(value) maps to colormap position.
     * Use for: Chlorophyll (0.01-10 mg/m³), Currents (0.1-3 kt), Phytoplankton.
     * Gives more colorscale resolution to small values. */
    LOGARITHMIC(1),

    /** Diverging: Zero-centered, symmetric around midpoint.
     * Use for: SSH/Eddys (-50cm to +50cm, zero = white/neutral).
     * Maps: [-maxAbs, +maxAbs] → [0, 1] with zero at 0.5 */
    DIVERGING(2),

    /** Square root: sqrt of linear position — expands low values, compresses high.
     * Unassigned to datasets. Available for future testing. */
    SQRT(3)
}
