package com.example.saltyoffshore.data

import androidx.compose.runtime.Stable

/**
 * Universal dataset rendering configuration.
 * Used by primary dataset AND overlays - same primitive.
 * Matches iOS DatasetRenderConfig exactly.
 */
@Stable
data class DatasetRenderConfig(
    val datasetId: String,

    // SHARED CONTROLS
    val colorscale: Colorscale? = null,
    val customRange: ClosedFloatingPointRange<Double>? = null,

    // VISUAL LAYER
    val visualEnabled: Boolean = true,
    val visualOpacity: Double = 1.0,

    // CONTOUR LAYER
    val contourEnabled: Boolean = false,
    val contourOpacity: Double = 1.0,
    val contourColor: Long = 0xFF000000,
    val dynamicContourColoring: Boolean = false,

    // ENTRY OVERRIDE (manual time/depth selection for any overlay)
    val entryOverride: EntryOverride? = null,

    // ARROW LAYER (currents-specific)
    val arrowsEnabled: Boolean = false,
    val arrowsOpacity: Double = 1.0,

    // PARTICLES LAYER (currents-specific)
    val particlesEnabled: Boolean = false,

    // BREAKS LAYER (vector visualization)
    val breaksEnabled: Boolean = false,
    val breaksOpacity: Double = 1.0,

    // NUMBERS LAYER
    val numbersEnabled: Boolean = false,
    val numbersOpacity: Double = 1.0,

    // PRESET SUPPORT
    val selectedPreset: DatasetPreset? = null,
    val cogStatistics: Any? = null,

    // FILTER MODE
    val filterMode: FilterMode = FilterMode.SQUASH,

    // VARIABLE SELECTION (for datasets with multiple variables like SST + Gradient)
    val selectedVariableId: String? = null
) {
    /**
     * Create immutable snapshot for MapContent rendering.
     * Pass dataRange from entry.ranges when calling.
     */
    fun snapshot(
        dataRange: ClosedFloatingPointRange<Double> = 0.0..1.0,
        resamplingMethod: String = "bilinear",
        selectedBreakId: String? = null
    ): DatasetRenderingSnapshot {
        val effectiveRange = customRange ?: dataRange

        return DatasetRenderingSnapshot(
            visualEnabled = visualEnabled,
            visualOpacity = visualOpacity,
            breaksEnabled = breaksEnabled,
            breaksOpacity = breaksOpacity,
            selectedBreakId = selectedBreakId,
            contourEnabled = contourEnabled,
            contourOpacity = contourOpacity,
            arrowsEnabled = arrowsEnabled,
            arrowsOpacity = arrowsOpacity,
            particlesEnabled = particlesEnabled,
            numbersEnabled = numbersEnabled,
            numbersOpacity = numbersOpacity,
            isFilterActive = customRange != null,
            filterMin = effectiveRange.start,
            filterMax = effectiveRange.endInclusive,
            filterMode = filterMode,
            selectedColorscale = colorscale,
            selectedPreset = selectedPreset,
            dataMin = dataRange.start,
            dataMax = dataRange.endInclusive,
            resamplingMethod = resamplingMethod
        )
    }

    /**
     * Get the selected variable for a dataset.
     * Returns primary variable if none selected or selection invalid.
     * iOS ref: DatasetRenderConfig.selectedVariable(for:)
     */
    fun selectedVariable(dataset: Dataset): DatasetVariable {
        val datasetType = DatasetType.fromRawValue(dataset.type) ?: return DatasetType.SST.primaryVariable
        return selectedVariableId?.let { id ->
            datasetType.availableVariables.find { it.id == id }
        } ?: datasetType.primaryVariable
    }

    /**
     * Active range: user filter if set, otherwise data range from API.
     * Single source of truth for what range to use for visualization.
     */
    fun activeRange(dataRange: ClosedFloatingPointRange<Double>?): ClosedFloatingPointRange<Double>? {
        if (customRange != null) return customRange
        return dataRange
    }

    /**
     * Clear filter - reset to use entry data.
     */
    fun clearFilter(): DatasetRenderConfig = copy(
        customRange = null,
        selectedPreset = null,
        colorscale = null
    )

    companion object {
        /**
         * Create default config for primary dataset (uses squash mode for color detail).
         */
        fun primaryDefaults(datasetType: DatasetType, datasetId: String): DatasetRenderConfig {
            val d = datasetType.defaults
            return DatasetRenderConfig(
                datasetId = datasetId,
                colorscale = null,
                customRange = null,
                visualEnabled = true,
                visualOpacity = d.visualOpacity,
                contourEnabled = d.contourEnabled,
                contourOpacity = d.contourOpacity,
                contourColor = 0xFF000000,
                dynamicContourColoring = false,
                entryOverride = null,
                arrowsEnabled = d.arrowsEnabled,
                arrowsOpacity = d.arrowsOpacity,
                particlesEnabled = d.particlesEnabled,
                breaksEnabled = false,
                breaksOpacity = 1.0,
                numbersEnabled = false,
                numbersOpacity = 1.0,
                selectedPreset = null,
                cogStatistics = null,
                filterMode = FilterMode.SQUASH,
                selectedVariableId = null
            )
        }

        /**
         * Create default config for overlay dataset (uses hideShow mode for transparency).
         */
        fun overlayDefaults(datasetType: DatasetType, datasetId: String): DatasetRenderConfig {
            val d = datasetType.defaults
            return DatasetRenderConfig(
                datasetId = datasetId,
                colorscale = null,
                customRange = null,
                visualEnabled = true,
                visualOpacity = 0.7,
                contourEnabled = d.contourEnabled,
                contourOpacity = d.contourOpacity,
                contourColor = 0xFF000000,
                dynamicContourColoring = false,
                entryOverride = null,
                arrowsEnabled = d.arrowsEnabled,
                arrowsOpacity = d.arrowsOpacity,
                particlesEnabled = d.particlesEnabled,
                breaksEnabled = false,
                breaksOpacity = 1.0,
                numbersEnabled = false,
                numbersOpacity = 1.0,
                selectedPreset = null,
                cogStatistics = null,
                filterMode = FilterMode.HIDE_SHOW,
                selectedVariableId = null
            )
        }
    }
}
