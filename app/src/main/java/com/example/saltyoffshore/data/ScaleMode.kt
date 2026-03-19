package com.example.saltyoffshore.data

/**
 * Defines how raw data values map to the 0-1 colormap range.
 * Used by both TiTiler (COG) and Zarr shader.
 *
 * Matches iOS ScaleMode enum exactly - rawValue passed to Metal shaders.
 */
enum class ScaleMode(val rawValue: Int) {
    /**
     * Linear: value maps directly to colormap position.
     * Use for: SST, MLD, Salinity, FSLE, Dissolved O₂
     */
    LINEAR(0),

    /**
     * Logarithmic: log10(value) maps to colormap position.
     * Use for: Chlorophyll (0.01-10 mg/m³), Currents (0.1-3 kt), Phytoplankton.
     * Gives more colorscale resolution to small values.
     */
    LOGARITHMIC(1),

    /**
     * Diverging: Zero-centered, symmetric around midpoint.
     * Use for: SSH/Eddys (-50cm to +50cm, zero = white/neutral).
     * Maps: [-maxAbs, +maxAbs] → [0, 1] with zero at 0.5.
     */
    DIVERGING(2),

    /**
     * Square root: sqrt of linear position — expands low values, compresses high.
     * Currently used for Currents.
     */
    SQRT(3)
}
