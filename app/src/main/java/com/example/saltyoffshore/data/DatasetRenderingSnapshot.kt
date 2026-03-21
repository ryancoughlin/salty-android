package com.example.saltyoffshore.data

/**
 * Snapshot of all layer visibility/opacity states for a dataset.
 * Mirrors iOS DatasetRenderingSnapshot exactly.
 */
data class DatasetRenderingSnapshot(
    // Visual Layer (COG raster)
    val visualEnabled: Boolean = true,
    val visualOpacity: Double = 1.0,

    // Breaks Layer (thermal fronts)
    val breaksEnabled: Boolean = false,
    val breaksOpacity: Double = 1.0,
    val selectedBreakId: String? = null,

    // Contour Layer
    val contourEnabled: Boolean = true,
    val contourOpacity: Double = 1.0,

    // Arrows Layer (Currents only)
    val arrowsEnabled: Boolean = true,
    val arrowsOpacity: Double = 1.0,

    // Particles Layer (Currents only)
    val particlesEnabled: Boolean = false,

    // Numbers Layer
    val numbersEnabled: Boolean = false,
    val numbersOpacity: Double = 1.0,

    // Filter State
    val isFilterActive: Boolean = false,
    val filterMin: Double = 0.0,
    val filterMax: Double = 100.0,
    val filterMode: FilterMode = FilterMode.SQUASH,

    // Data Range
    val dataMin: Double = 0.0,
    val dataMax: Double = 100.0,

    // Colormap
    val selectedColorscale: Colorscale? = null,
    val selectedPreset: DatasetPreset? = null,
    val resamplingMethod: String = "bilinear"
) {
    val renderRange: ClosedFloatingPointRange<Double>
        get() = if (isFilterActive) filterMin..filterMax else dataMin..dataMax

    val contourRange: ClosedFloatingPointRange<Double>
        get() = dataMin..dataMax

    val contourFilterRange: ClosedFloatingPointRange<Double>?
        get() = if (isFilterActive) filterMin..filterMax else null

    companion object {
        fun default() = DatasetRenderingSnapshot()
    }
}
