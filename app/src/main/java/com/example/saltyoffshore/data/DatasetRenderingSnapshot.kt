package com.example.saltyoffshore.data

/**
 * Snapshot of all layer visibility/opacity states for a dataset.
 * Immutable snapshot for MapContent rendering (prevents data races).
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
    val contourEnabled: Boolean = false,
    val contourOpacity: Double = 1.0,

    // Arrows Layer (Currents only)
    val arrowsEnabled: Boolean = false,
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
    val selectedColorscale: Colorscale? = null,

    // Preset Support
    val selectedPreset: DatasetPreset? = null,

    // Data Range
    val dataMin: Double = 0.0,
    val dataMax: Double = 100.0,

    // Resampling
    val resamplingMethod: ResamplingMethod = ResamplingMethod.BILINEAR
) {
    val renderRange: ClosedFloatingPointRange<Double>
        get() = if (isFilterActive) filterMin..filterMax else dataMin..dataMax

    /** Contour filter range: hideShow mode uses filter range, squash uses full data range */
    val contourFilterRange: ClosedFloatingPointRange<Double>
        get() = if (isFilterActive && filterMode == FilterMode.HIDE_SHOW) filterMin..filterMax
                else dataMin..dataMax

    /** Contour coloring range: squash uses filter range, hideShow uses full data range */
    val contourRange: ClosedFloatingPointRange<Double>
        get() = if (isFilterActive && filterMode == FilterMode.SQUASH) filterMin..filterMax
                else dataMin..dataMax

    companion object {
        fun default() = DatasetRenderingSnapshot()
    }
}
