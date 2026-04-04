package com.example.saltyoffshore.data

/**
 * How the color scale domain (min/max) is determined for a dataset.
 * Single source of truth — ZarrManager reads this to compute the range
 * that flows to the shader and GradientScaleBar.
 *
 * Two paths:
 * - Fixed: hardcoded range, ignores data values (chlorophyll)
 * - Aggregated: absolute min/max from API entry ranges (everything else)
 *
 * Result is always a ClosedFloatingPointRange<Float> stored on every frame's metadata.
 *
 * iOS ref: RenderingConfig.swift — DomainStrategy enum
 */
sealed class DomainStrategy {

    /**
     * Use a fixed domain regardless of data values.
     * Example: Chlorophyll always maps 0.01...8.0 mg/m³.
     */
    data class Fixed(val range: ClosedFloatingPointRange<Float>) : DomainStrategy()

    /**
     * Aggregate entry-level min/max across all frames at a depth.
     * Uses the absolute min and max — no percentile clipping.
     */
    data object Aggregated : DomainStrategy()

    /**
     * Compute the aggregate domain from entry-level min/max arrays.
     * API always provides ranges, so this should always return a value.
     */
    fun aggregateRange(entryMins: List<Float>, entryMaxes: List<Float>): ClosedFloatingPointRange<Float>? {
        return when (this) {
            is Fixed -> range

            is Aggregated -> {
                val min = entryMins.minOrNull()
                val max = entryMaxes.maxOrNull()
                if (min != null && max != null && min < max) min..max else null
            }
        }
    }

    companion object {
        val Default = Aggregated
    }
}
