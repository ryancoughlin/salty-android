package com.example.saltyoffshore.zarr

import android.graphics.Color
import com.example.saltyoffshore.data.Colorscale
import kotlin.math.log10
import kotlin.math.max

/**
 * Shared colormap texture creation for GPU shaders.
 * Creates 256×1 RGBA8 LUT textures for shader sampling.
 *
 * Matches iOS `ColormapTextureFactory` exactly.
 */
object ColormapTextureFactory {

    /**
     * How color stops are distributed across the 256-pixel texture.
     *
     * Most colorscales use [Uniform] — stops evenly spaced. Chlorophyll uses
     * [Log10] to match TiTiler's `create_log10_positioned_colormap`, where each
     * stop is placed at its log10(value) position in the palette.
     */
    sealed class StopDistribution {
        /** Evenly spaced stops (default for linear, sqrt, diverging scales) */
        data object Uniform : StopDistribution()

        /**
         * Stops positioned at log10(value) within the given domain.
         * Each stop value maps to `(log10(value) - log10(domain.min)) / (log10(domain.max) - log10(domain.min))`.
         */
        data class Log10(
            val stops: List<Float>,
            val domain: ClosedFloatingPointRange<Float>
        ) : StopDistribution()
    }

    /**
     * Create 256×1 RGBA8 colormap texture bytes from Colorscale.
     *
     * @param colorscale Source colorscale with interpolation stops
     * @param distribution How stops are positioned in the texture (default: uniform)
     * @return 256×4 = 1024 bytes of RGBA pixel data for shader sampling
     */
    fun createTextureBytes(
        colorscale: Colorscale,
        distribution: StopDistribution = StopDistribution.Uniform
    ): ByteArray {
        if (colorscale.colors.isEmpty()) {
            return ByteArray(256 * 4) { 255.toByte() } // White fallback
        }

        val width = 256
        val pixels = ByteArray(width * 4)

        when (distribution) {
            is StopDistribution.Uniform -> fillUniform(pixels, colorscale.colors, width)
            is StopDistribution.Log10 -> fillLog10(
                pixels, colorscale.colors,
                distribution.stops, distribution.domain, width
            )
        }

        return pixels
    }

    // MARK: - Distribution Strategies

    /**
     * Evenly space color stops across the texture.
     */
    private fun fillUniform(pixels: ByteArray, colors: List<Int>, width: Int) {
        for (i in 0 until width) {
            val t = i.toFloat() / (width - 1).toFloat()
            val colorIndex = t * (colors.size - 1)
            val lowIndex = colorIndex.toInt()
            val highIndex = minOf(lowIndex + 1, colors.size - 1)
            val f = colorIndex - lowIndex

            val low = colors[lowIndex]
            val high = colors[highIndex]

            pixels[i * 4 + 0] = lerp(Color.red(low), Color.red(high), f).toByte()
            pixels[i * 4 + 1] = lerp(Color.green(low), Color.green(high), f).toByte()
            pixels[i * 4 + 2] = lerp(Color.blue(low), Color.blue(high), f).toByte()
            pixels[i * 4 + 3] = 255.toByte()
        }
    }

    /**
     * Position color stops at their log10(value) locations in the texture.
     * Matches TiTiler's `create_log10_positioned_colormap` — each stop is placed
     * at `(log10(stopValue) - logMin) / (logMax - logMin)` in the 256-entry palette.
     */
    private fun fillLog10(
        pixels: ByteArray,
        colors: List<Int>,
        stops: List<Float>,
        domain: ClosedFloatingPointRange<Float>,
        width: Int
    ) {
        val logMin = log10(max(domain.start, 1e-10f).toDouble())
        val logMax = log10(max(domain.endInclusive, 1e-10f).toDouble())
        val logRange = logMax - logMin

        if (logRange <= 0 || stops.size != colors.size) {
            fillUniform(pixels, colors, width)
            return
        }

        // Compute normalized [0,1] position for each stop in log space
        val positions = stops.map { value ->
            val logVal = log10(max(value, 1e-10f).toDouble())
            ((logVal - logMin) / logRange).toFloat()
        }

        // For each pixel, find surrounding stops and interpolate
        for (i in 0 until width) {
            val t = i.toFloat() / (width - 1).toFloat()

            // Find the two stops that bracket this position
            var upperIdx = positions.size - 1
            for (j in positions.indices) {
                if (positions[j] >= t) {
                    upperIdx = j
                    break
                }
            }
            val lowerIdx = maxOf(upperIdx - 1, 0)

            val low = colors[lowerIdx]
            val high = colors[upperIdx]

            val segmentStart = positions[lowerIdx]
            val segmentEnd = positions[upperIdx]
            val segmentRange = segmentEnd - segmentStart
            val f = if (segmentRange > 0) (t - segmentStart) / segmentRange else 0f

            pixels[i * 4 + 0] = lerp(Color.red(low), Color.red(high), f).toByte()
            pixels[i * 4 + 1] = lerp(Color.green(low), Color.green(high), f).toByte()
            pixels[i * 4 + 2] = lerp(Color.blue(low), Color.blue(high), f).toByte()
            pixels[i * 4 + 3] = 255.toByte()
        }
    }

    private fun lerp(a: Int, b: Int, t: Float): Int {
        return ((1 - t) * a + t * b).toInt().coerceIn(0, 255)
    }
}
