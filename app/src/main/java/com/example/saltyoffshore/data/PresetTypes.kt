package com.example.saltyoffshore.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

// MARK: - Core Preset Types

/**
 * Represents the type of range calculation for a preset.
 * iOS: enum PresetType
 */
sealed class PresetType {
    data class FixedRange(val min: Double, val max: Double) : PresetType()
    data object MicroBreak : PresetType()
    data class CurrentValueRange(val offset: Double) : PresetType()
}

/**
 * Represents a predefined range preset for quick filtering (works for all dataset types).
 * iOS: struct DatasetPreset
 */
data class DatasetPreset(
    val id: String,
    val label: String,
    val type: PresetType,
    val datasetType: DatasetType
) {
    /**
     * Calculate the range based on the preset type and current data.
     */
    fun calculateRange(
        currentValue: Double?,
        valueRange: ClosedFloatingPointRange<Double>
    ): ClosedFloatingPointRange<Double>? {
        return when (type) {
            is PresetType.FixedRange -> type.min..type.max

            is PresetType.MicroBreak -> {
                val value = currentValue ?: return null
                val offset = when (datasetType) {
                    DatasetType.SST -> 0.25 // 0.25 deg F for SST
                    DatasetType.CHLOROPHYLL -> 0.01 // 0.01 mg/m3 for chlorophyll
                    DatasetType.WATER_CLARITY -> 0.001 // 0.001 m^-1 for water clarity
                    else -> 0.01 // Default fallback
                }
                (value - offset)..(value + offset)
            }

            is PresetType.CurrentValueRange -> {
                val value = currentValue ?: return null
                (value - type.offset)..(value + type.offset)
            }
        }
    }

    companion object {
        // MARK: - SST Presets

        /** SST temperature break presets */
        val sstPresets: List<DatasetPreset> = listOf(
            DatasetPreset(
                id = "micro_break",
                label = "0.5 deg F Break",
                type = PresetType.MicroBreak,
                datasetType = DatasetType.SST
            ),
            DatasetPreset(
                id = "micro_break_1deg",
                label = "1 deg F Break",
                type = PresetType.CurrentValueRange(offset = 0.5),
                datasetType = DatasetType.SST
            )
        )

        // MARK: - MLD Presets

        /** MLD presets for filtering by mixed layer depth (meters) */
        val mldPresets: List<DatasetPreset> = listOf(
            DatasetPreset(
                id = "surface",
                label = "Surface",
                type = PresetType.FixedRange(min = 0.0, max = 15.0),
                datasetType = DatasetType.MLD
            ),
            DatasetPreset(
                id = "shallow",
                label = "Shallow",
                type = PresetType.FixedRange(min = 15.0, max = 30.0),
                datasetType = DatasetType.MLD
            ),
            DatasetPreset(
                id = "mid",
                label = "Mid",
                type = PresetType.FixedRange(min = 30.0, max = 60.0),
                datasetType = DatasetType.MLD
            ),
            DatasetPreset(
                id = "deep",
                label = "Deep",
                type = PresetType.FixedRange(min = 60.0, max = 100.0),
                datasetType = DatasetType.MLD
            ),
            DatasetPreset(
                id = "very_deep",
                label = "Very Deep",
                type = PresetType.FixedRange(min = 100.0, max = 200.0),
                datasetType = DatasetType.MLD
            )
        )
    }
}

// MARK: - Preset Configuration

/**
 * Centralized configuration for dataset presets - single source of truth.
 * iOS: struct PresetConfiguration
 */
data class PresetConfiguration(
    val datasetType: DatasetType,
    val staticPresets: List<DatasetPreset> = emptyList(),
    val supportsDynamicPresets: Boolean = false,
    val dynamicBuilder: ((COGBandStatistics) -> List<DatasetPreset>)? = null
) {
    companion object {
        /** All preset configurations by dataset type */
        val configurations: Map<DatasetType, PresetConfiguration> = mapOf(
            DatasetType.SST to PresetConfiguration(
                datasetType = DatasetType.SST,
                staticPresets = DatasetPreset.sstPresets,
                supportsDynamicPresets = true,
                dynamicBuilder = DynamicPresetBuilder::buildSSTPresets
            ),
            DatasetType.MLD to PresetConfiguration(
                datasetType = DatasetType.MLD,
                staticPresets = DatasetPreset.mldPresets,
                supportsDynamicPresets = false,
                dynamicBuilder = null
            ),
            DatasetType.CHLOROPHYLL to PresetConfiguration(
                datasetType = DatasetType.CHLOROPHYLL,
                staticPresets = emptyList(),
                supportsDynamicPresets = true,
                dynamicBuilder = null // Loads stats only, no presets
            ),
            DatasetType.CURRENTS to PresetConfiguration(
                datasetType = DatasetType.CURRENTS,
                staticPresets = emptyList(),
                supportsDynamicPresets = true,
                dynamicBuilder = DynamicPresetBuilder::buildCurrentsPresets
            ),
            DatasetType.SALINITY to PresetConfiguration(
                datasetType = DatasetType.SALINITY,
                staticPresets = emptyList(),
                supportsDynamicPresets = true,
                dynamicBuilder = DynamicPresetBuilder::buildSalinityPresets
            ),
            DatasetType.FSLE to PresetConfiguration(
                datasetType = DatasetType.FSLE,
                staticPresets = emptyList(),
                supportsDynamicPresets = true,
                dynamicBuilder = null // Loads stats only, no presets
            )
        )

        /** Check if a dataset type supports presets */
        fun supportsPresets(datasetType: DatasetType): Boolean {
            return configurations.containsKey(datasetType)
        }

        /** Get configuration for a dataset type */
        fun configuration(datasetType: DatasetType): PresetConfiguration? {
            return configurations[datasetType]
        }
    }
}

// MARK: - COG Band Statistics

/**
 * Statistics for a COG band, used for dynamic preset generation.
 * iOS: struct COGBandStatistics
 */
@Serializable
data class COGBandStatistics(
    val min: Double,
    val max: Double,
    val mean: Double,
    val std: Double,
    val median: Double? = null,
    val majority: Double? = null,
    val minority: Double? = null,
    @SerialName("percentile_2") val percentile2: Double,
    @SerialName("percentile_98") val percentile98: Double,
    val count: Double? = null,
    val sum: Double? = null,
    val unique: Double? = null,
    @SerialName("valid_percent") val validPercent: Double? = null,
    @SerialName("masked_pixels") val maskedPixels: Double? = null,
    @SerialName("valid_pixels") val validPixels: Double? = null
)

// MARK: - Dynamic Preset Builder

/**
 * Builds dynamic presets from COG statistics.
 * iOS: enum DynamicPresetBuilder
 */
object DynamicPresetBuilder {

    /** Build SST dynamic presets from COG statistics */
    fun buildSSTPresets(stats: COGBandStatistics): List<DatasetPreset> {
        return listOfNotNull(
            primeZonePreset(stats),
            warmWaterPreset(stats),
            temperatureBreaksPreset(stats)
        )
    }

    /** Build currents dynamic presets from COG statistics */
    fun buildCurrentsPresets(stats: COGBandStatistics): List<DatasetPreset> {
        return listOfNotNull(
            currentEdgesPreset(stats),
            slackCurrentPreset(stats),
            moderateCurrentPreset(stats),
            strongCurrentPreset(stats)
        )
    }

    /** Build salinity dynamic presets from COG statistics */
    fun buildSalinityPresets(stats: COGBandStatistics): List<DatasetPreset> {
        return listOfNotNull(
            salinityEdgesPreset(stats),
            blueWaterPreset(stats),
            transitionZonePreset(stats),
            riverPlumePreset(stats)
        )
    }

    // MARK: - SST Preset Builders

    private fun primeZonePreset(stats: COGBandStatistics): DatasetPreset? {
        if (!stats.mean.isFinite() || !stats.std.isFinite()) return null

        val lowerBound = stats.mean - stats.std
        val upperBound = stats.mean + stats.std
        val meanTemp = round(stats.mean).toInt()
        val range = round(stats.std * 2).toInt()

        return DatasetPreset(
            id = "dynamic_prime_zone",
            label = "Prime Zone (${meanTemp}+/-${range / 2} deg F)",
            type = PresetType.FixedRange(min = lowerBound, max = upperBound),
            datasetType = DatasetType.SST
        )
    }

    private fun warmWaterPreset(stats: COGBandStatistics): DatasetPreset? {
        if (!stats.percentile98.isFinite() || !stats.mean.isFinite()) return null

        val threshold = round(stats.percentile98).toInt()
        return DatasetPreset(
            id = "dynamic_warm_water",
            label = "Warm Water (${threshold} deg F)",
            type = PresetType.FixedRange(min = stats.mean, max = stats.percentile98),
            datasetType = DatasetType.SST
        )
    }

    private fun temperatureBreaksPreset(stats: COGBandStatistics): DatasetPreset? {
        if (!stats.mean.isFinite() || !stats.std.isFinite()) return null
        if (!stats.percentile2.isFinite() || !stats.percentile98.isFinite()) return null

        // Calculate optimal break range using enhanced algorithm
        val breakRange = calculateOptimalBreakRange(stats)

        val lowerTemp = round(breakRange.start).toInt()
        val upperTemp = round(breakRange.endInclusive).toInt()

        return DatasetPreset(
            id = "dynamic_temperature_breaks",
            label = "Thermal Fronts (${lowerTemp}-${upperTemp} deg F)",
            type = PresetType.FixedRange(min = breakRange.start, max = breakRange.endInclusive),
            datasetType = DatasetType.SST
        )
    }

    /**
     * Calculate optimal temperature break range for highlighting strong fronts.
     */
    private fun calculateOptimalBreakRange(stats: COGBandStatistics): ClosedFloatingPointRange<Double> {
        // Step 1: Clip extremes (ignore clouds/outliers)
        val safeMin = stats.percentile2
        val safeMax = stats.percentile98

        // Step 2: Detect break spread using standard deviation
        val stdDev = stats.std

        // Step 3: Determine target break spread (2-6 deg F range)
        // - Low std (<2 deg F): water is flat, use minimum 2 deg F
        // - Medium std (2-6 deg F): use std as proxy for break strength
        // - High std (>6 deg F): cap at 6 deg F to avoid too broad
        val targetSpread = min(max(stdDev * 0.5, 2.0), 6.0)

        // Step 4: Center around median for strongest breaks
        val center = stats.median ?: stats.mean
        val halfSpread = targetSpread / 2.0

        val lowerBound = max(center - halfSpread, safeMin)
        val upperBound = min(center + halfSpread, safeMax)

        // Ensure we have a meaningful range
        if (upperBound - lowerBound < 2.0) {
            // Fallback to a reasonable 4 deg F range around median
            return (center - 2.0)..(center + 2.0)
        }

        return lowerBound..upperBound
    }

    // MARK: - Currents Preset Builders

    /** Slack Currents (bottom 2% - calm areas for drifting) */
    private fun slackCurrentPreset(stats: COGBandStatistics): DatasetPreset? {
        if (!stats.percentile2.isFinite() || !stats.min.isFinite()) return null

        val threshold = String.format("%.1f", stats.percentile2)
        return DatasetPreset(
            id = "dynamic_slack_current",
            label = "Slack Water (<${threshold} kts)",
            type = PresetType.FixedRange(min = stats.min, max = stats.percentile2),
            datasetType = DatasetType.CURRENTS
        )
    }

    /** Moderate Currents (mean +/- 1 std - ideal fishing speed) */
    private fun moderateCurrentPreset(stats: COGBandStatistics): DatasetPreset? {
        if (!stats.mean.isFinite() || !stats.std.isFinite()) return null

        val lowerBound = stats.mean - stats.std
        val upperBound = stats.mean + stats.std
        val meanSpeed = String.format("%.1f", stats.mean)
        val halfRange = String.format("%.1f", stats.std)

        return DatasetPreset(
            id = "dynamic_moderate_current",
            label = "Moderate Current (${meanSpeed}+/-${halfRange} kts)",
            type = PresetType.FixedRange(min = lowerBound, max = upperBound),
            datasetType = DatasetType.CURRENTS
        )
    }

    /** Strong Currents (top 2% - rips and upwelling zones) */
    private fun strongCurrentPreset(stats: COGBandStatistics): DatasetPreset? {
        if (!stats.percentile98.isFinite() || !stats.max.isFinite()) return null

        val threshold = String.format("%.1f", stats.percentile98)
        return DatasetPreset(
            id = "dynamic_strong_current",
            label = "Strong Current (>${threshold} kts)",
            type = PresetType.FixedRange(min = stats.percentile98, max = stats.max),
            datasetType = DatasetType.CURRENTS
        )
    }

    /** Current Edges (gradients - where fish congregate) */
    private fun currentEdgesPreset(stats: COGBandStatistics): DatasetPreset? {
        if (!stats.std.isFinite() || !stats.mean.isFinite()) return null

        // Focus on moderate current range where gradients are strongest
        val halfStd = stats.std * 0.5
        val lowerBound = stats.mean - halfStd
        val upperBound = stats.mean + halfStd

        val lowerSpeed = String.format("%.1f", lowerBound)
        val upperSpeed = String.format("%.1f", upperBound)

        return DatasetPreset(
            id = "dynamic_current_edges",
            label = "Current Edges (${lowerSpeed}-${upperSpeed} kts)",
            type = PresetType.FixedRange(min = lowerBound, max = upperBound),
            datasetType = DatasetType.CURRENTS
        )
    }

    // MARK: - Salinity Preset Builders

    /**
     * River Plume (bottom 2% - freshwater influence from rivers like Mississippi, coastal runoff).
     * Baitfish concentrate at freshwater/saltwater interfaces.
     */
    private fun riverPlumePreset(stats: COGBandStatistics): DatasetPreset? {
        if (!stats.percentile2.isFinite() || !stats.min.isFinite()) return null

        val threshold = String.format("%.1f", stats.percentile2)
        return DatasetPreset(
            id = "dynamic_river_plume",
            label = "River Plume (<${threshold} PSU)",
            type = PresetType.FixedRange(min = stats.min, max = stats.percentile2),
            datasetType = DatasetType.SALINITY
        )
    }

    /**
     * Transition Zone (mean +/- 1 std - mixing zone between coastal and offshore).
     * Active feeding zone where species from both environments overlap.
     */
    private fun transitionZonePreset(stats: COGBandStatistics): DatasetPreset? {
        if (!stats.mean.isFinite() || !stats.std.isFinite()) return null

        val lowerBound = stats.mean - stats.std
        val upperBound = stats.mean + stats.std
        val meanSalinity = String.format("%.1f", stats.mean)
        val halfRange = String.format("%.1f", stats.std)

        return DatasetPreset(
            id = "dynamic_transition_zone",
            label = "Transition (${meanSalinity}+/-${halfRange} PSU)",
            type = PresetType.FixedRange(min = lowerBound, max = upperBound),
            datasetType = DatasetType.SALINITY
        )
    }

    /**
     * Blue Water (top 2% - high salinity offshore water, Gulf Stream influence).
     * Clean offshore water where pelagics (marlin, tuna, wahoo) prefer.
     */
    private fun blueWaterPreset(stats: COGBandStatistics): DatasetPreset? {
        if (!stats.percentile98.isFinite() || !stats.max.isFinite()) return null

        val threshold = String.format("%.1f", stats.percentile98)
        return DatasetPreset(
            id = "dynamic_blue_water",
            label = "Blue Water (>${threshold} PSU)",
            type = PresetType.FixedRange(min = stats.percentile98, max = stats.max),
            datasetType = DatasetType.SALINITY
        )
    }

    /**
     * Salinity Edges (gradient zones where fish congregate).
     * Similar to temperature breaks - discontinuities concentrate bait and predators.
     */
    private fun salinityEdgesPreset(stats: COGBandStatistics): DatasetPreset? {
        if (!stats.std.isFinite() || !stats.mean.isFinite()) return null

        // Focus on narrow band around median where gradients are sharpest
        val halfStd = stats.std * 0.5
        val lowerBound = stats.mean - halfStd
        val upperBound = stats.mean + halfStd

        val lowerSalinity = String.format("%.1f", lowerBound)
        val upperSalinity = String.format("%.1f", upperBound)

        return DatasetPreset(
            id = "dynamic_salinity_edges",
            label = "Salinity Edges (${lowerSalinity}-${upperSalinity} PSU)",
            type = PresetType.FixedRange(min = lowerBound, max = upperBound),
            datasetType = DatasetType.SALINITY
        )
    }
}
