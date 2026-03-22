package com.example.saltyoffshore.utils

import com.example.saltyoffshore.data.DatasetType
import com.example.saltyoffshore.data.renderingConfig
import com.example.saltyoffshore.data.scaleMode
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToLong

/**
 * Gradient scale calculations matching ScaleNormalization.h (GPU shader) exactly.
 * Delegates to ScaleNormalizer — single source of normalization math.
 *
 * Port of iOS GradientScaleUtil.swift — verbatim.
 */
object GradientScaleUtil {

    // ── Position Calculation ─────────────────────────────────────────────

    /** Calculate normalized position (0-1) for colormap lookup. */
    fun calculatePosition(
        value: Double,
        valueRange: ClosedFloatingPointRange<Double>,
        datasetType: DatasetType?
    ): Double {
        val scaleMode = datasetType?.scaleMode
            ?: com.example.saltyoffshore.data.ScaleMode.LINEAR
        return calculatePosition(value, valueRange, scaleMode)
    }

    fun calculatePosition(
        value: Double,
        valueRange: ClosedFloatingPointRange<Double>,
        scaleMode: com.example.saltyoffshore.data.ScaleMode
    ): Double {
        val lo = valueRange.start
        val hi = valueRange.endInclusive
        if (hi <= lo) return 0.5
        val normalizer = scaleMode.normalizer(lo.toFloat(), hi.toFloat())
        return (normalizer.normalize(value.toFloat()) ?: 0.5f).toDouble()
    }

    // ── Inverse Position (position → value) ──────────────────────────────

    /** Convert normalized position (0-1) back to data value. Used for drag handles. */
    fun calculateValue(
        position: Double,
        valueRange: ClosedFloatingPointRange<Double>,
        datasetType: DatasetType?
    ): Double {
        val scaleMode = datasetType?.scaleMode
            ?: com.example.saltyoffshore.data.ScaleMode.LINEAR
        return calculateValue(position, valueRange, scaleMode)
    }

    fun calculateValue(
        position: Double,
        valueRange: ClosedFloatingPointRange<Double>,
        scaleMode: com.example.saltyoffshore.data.ScaleMode
    ): Double {
        val lo = valueRange.start
        val hi = valueRange.endInclusive
        if (hi <= lo) return lo
        val t = position.coerceIn(0.0, 1.0).toFloat()
        val normalizer = scaleMode.normalizer(lo.toFloat(), hi.toFloat())
        return normalizer.denormalize(t).toDouble()
    }

    // ── Symmetric Range ──────────────────────────────────────────────────

    /** Calculate symmetric range for diverging colormaps (zero-centered). */
    fun calculateSymmetricRange(
        range: ClosedFloatingPointRange<Double>
    ): ClosedFloatingPointRange<Double> {
        val maxAbs = max(abs(range.start), abs(range.endInclusive))
        return -maxAbs..maxAbs
    }

    // ── Formatting ───────────────────────────────────────────────────────

    fun formatValue(value: Double, decimalPlaces: Int): String {
        return String.format("%.${decimalPlaces}f", value)
    }

    fun formatValue(value: Double, datasetType: DatasetType): String {
        return String.format("%.${datasetType.numberDecimalPlaces}f", value)
    }

    // ── Snapping ─────────────────────────────────────────────────────────

    /** Snap value to dataset-specific increments for clean filter UI.
     * Uses renderingConfig.snapIncrement — null means no snapping. */
    fun snapValue(value: Double, datasetType: DatasetType?): Double {
        val increment = datasetType?.renderingConfig?.snapIncrement ?: return value
        return (value / increment).roundToLong() * increment
    }
}
