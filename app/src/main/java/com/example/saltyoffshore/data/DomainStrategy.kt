package com.example.saltyoffshore.data

/**
 * How the color scale domain (min/max) is determined for a dataset.
 * Single source of truth — ZarrManager reads this to compute the range
 * that flows to the shader and GradientScaleBar.
 *
 * Two paths:
 * - Fixed: hardcoded range, ignores data values (chlorophyll)
 * - PercentileClipped: aggregate from API entry ranges, clip outliers (everything else)
 *
 * Result is always a ClosedFloatingPointRange<Float> stored on every frame's metadata.
 */
sealed class DomainStrategy {

    /**
     * Use a fixed domain regardless of data values.
     * Example: Chlorophyll always maps 0.01...8.0 mg/m³.
     */
    data class Fixed(val range: ClosedFloatingPointRange<Float>) : DomainStrategy()

    /**
     * Aggregate entry-level min/max across all frames at a depth,
     * then clip to percentiles to reduce outlier influence.
     *
     * @param minPercentile Floor percentile for entry minimums (e.g. 10 = P10)
     * @param maxPercentile Ceiling percentile for entry maximums (e.g. 75 = P75)
     */
    data class PercentileClipped(
        val minPercentile: Int,
        val maxPercentile: Int
    ) : DomainStrategy()

    /**
     * Compute the aggregate domain from entry-level min/max arrays.
     * API always provides ranges, so this should always return a value.
     */
    fun aggregateRange(entryMins: List<Float>, entryMaxes: List<Float>): ClosedFloatingPointRange<Float>? {
        return when (this) {
            is Fixed -> range

            is PercentileClipped -> {
                if (entryMins.size < 2) {
                    // Single entry — use its range directly (no clipping possible)
                    val first = entryMins.firstOrNull()
                    val last = entryMaxes.firstOrNull()
                    if (first != null && last != null && first < last) {
                        first..last
                    } else {
                        null
                    }
                } else {
                    val sortedMins = entryMins.sorted()
                    val sortedMaxes = entryMaxes.sorted()
                    val n = sortedMins.size

                    val clippedMin = sortedMins[minOf(n * minPercentile / 100, n - 1)]
                    val clippedMax = sortedMaxes[minOf(n * maxPercentile / 100, n - 1)]

                    if (clippedMin < clippedMax) clippedMin..clippedMax else null
                }
            }
        }
    }

    companion object {
        /**
         * Default strategy: P10 floor / P75 ceiling.
         * Tightens the domain so warm water reaches the red zone instead of
         * compressing into orange from afternoon solar heating outliers.
         */
        val Default = PercentileClipped(minPercentile = 10, maxPercentile = 75)
    }
}
