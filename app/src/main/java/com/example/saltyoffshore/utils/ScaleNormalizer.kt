package com.example.saltyoffshore.utils

import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Normalizes raw float values to [0, 1] for color mapping.
 * Must stay in sync with Metal ScaleNormalization.h.
 *
 * Port of iOS ScaleNormalizer.swift — verbatim math.
 */
interface ScaleNormalizer {
    /** Map raw value → [0, 1]. Returns null for noData (NaN or -9999). */
    fun normalize(value: Float): Float?

    /** Map [0, 1] back to raw domain (legend ticks, crosshair readout). */
    fun denormalize(t: Float): Float
}

/** Shared fill-value check — NaN or -9999. */
fun isNoData(v: Float): Boolean = v.isNaN() || v == -9999f

// ── Linear ───────────────────────────────────────────────────────────────────
// normalize: clamp((v - min) / (max - min), 0, 1)

class LinearNormalizer(
    private val min: Float,
    private val max: Float
) : ScaleNormalizer {

    override fun normalize(value: Float): Float? {
        if (isNoData(value)) return null
        val range = max - min
        if (range <= 0) return 0.5f
        return min(max((value - min) / range, 0f), 1f)
    }

    override fun denormalize(t: Float): Float {
        return min + t * (max - min)
    }
}

// ── Log10 ────────────────────────────────────────────────────────────────────
// normalize: clamp((log10(v) - log10(min)) / (log10(max) - log10(min)), 0, 1)
// Clamps input to 1e-6 before log.

class Log10Normalizer(
    private val min: Float,
    private val max: Float
) : ScaleNormalizer {

    override fun normalize(value: Float): Float? {
        if (isNoData(value)) return null
        val clamped = max(value, 1e-6f)
        val logVal = log10(clamped)
        val logMin = log10(max(min, 1e-6f))
        val logMax = log10(max(max, 1e-6f))
        val logRange = logMax - logMin
        if (logRange <= 0) return 0.5f
        return min(max((logVal - logMin) / logRange, 0f), 1f)
    }

    override fun denormalize(t: Float): Float {
        val logMin = log10(max(min, 1e-6f))
        val logMax = log10(max(max, 1e-6f))
        return 10f.pow(logMin + t * (logMax - logMin))
    }
}

// ── Sqrt ─────────────────────────────────────────────────────────────────────
// normalize: clamp(sqrt((v - min) / (max - min)), 0, 1)

class SqrtNormalizer(
    private val min: Float,
    private val max: Float
) : ScaleNormalizer {

    override fun normalize(value: Float): Float? {
        if (isNoData(value)) return null
        val range = max - min
        if (range <= 0) return 0.5f
        val linear = min(max((value - min) / range, 0f), 1f)
        return sqrt(linear)
    }

    override fun denormalize(t: Float): Float {
        return min + (t * t) * (max - min)
    }
}

// ── Divergent ────────────────────────────────────────────────────────────────
// Values below center map [min, center] → [0.0, 0.5].
// Values above center map [center, max] → [0.5, 1.0].
// Each side independent — asymmetric domains pin center at exactly 0.5.

class DivergentNormalizer(
    private val min: Float,
    private val max: Float,
    private val center: Float = 0f
) : ScaleNormalizer {

    override fun normalize(value: Float): Float? {
        if (isNoData(value)) return null
        return if (value <= center) {
            val range = center - min
            if (range <= 0) return 0.5f
            val t = (value - min) / range
            min(max(t * 0.5f, 0f), 0.5f)
        } else {
            val range = max - center
            if (range <= 0) return 0.5f
            val t = (value - center) / range
            min(max(0.5f + t * 0.5f, 0.5f), 1f)
        }
    }

    override fun denormalize(t: Float): Float {
        return if (t <= 0.5f) {
            min + (t / 0.5f) * (center - min)
        } else {
            center + ((t - 0.5f) / 0.5f) * (max - center)
        }
    }
}
