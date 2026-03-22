package com.example.saltyoffshore.ui.waypoint.chart

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

// MARK: - Chart Color Scales (Wind & Wave forecasts)

object ChartColorScales {

    // Wind speed color scale (0-40+ knots)
    // Light conditions show blues, strong winds show orange/red for danger
    private val windStops = listOf(
        ColorStop(0.0, Color(0xFFA8D5F2)),   // calm (light blue)
        ColorStop(5.0, Color(0xFF51B1E8)),    // light breeze
        ColorStop(8.0, Color(0xFF0077CC)),     // gentle breeze
        ColorStop(12.0, Color(0xFF0057A0)),    // moderate
        ColorStop(16.0, Color(0xFF003A6A)),    // fresh
        ColorStop(20.0, Color(0xFFFF8500)),    // strong (orange warning)
        ColorStop(25.0, Color(0xFFFF4500)),    // near gale
        ColorStop(30.0, Color(0xFFCC0000)),    // gale (red danger)
        ColorStop(35.0, Color(0xFF900A00)),    // storm
    )

    // Wave height color scale (0-12+ ft)
    // Calm cyans -> moderate purples -> rough pinks -> dangerous reds
    private val waveStops = listOf(
        ColorStop(0.0, Color(0xFF80DEEA)),    // glassy (visible cyan)
        ColorStop(1.0, Color(0xFF26C6DA)),     // small
        ColorStop(2.0, Color(0xFF00ACC1)),     // slight
        ColorStop(3.0, Color(0xFF0097A7)),     // moderate
        ColorStop(4.0, Color(0xFF673AB7)),     // rough (purple transition)
        ColorStop(5.0, Color(0xFF9C27B0)),     // very rough
        ColorStop(6.0, Color(0xFFE91E63)),     // high (pink/magenta)
        ColorStop(8.0, Color(0xFFF44336)),     // very high (red)
        ColorStop(10.0, Color(0xFFB71C1C)),    // phenomenal (dark red)
    )

    // SST High Contrast - 12-stop
    // Deep navy (coldest) -> blue -> cyan -> green -> yellow -> orange -> red -> brown (hottest)
    private val sstColors = listOf(
        Color(0xFF081D58), Color(0xFF21449B), Color(0xFF3883F6), Color(0xFF34D1DB),
        Color(0xFF0EFFC5), Color(0xFF7FF000), Color(0xFFEBF600), Color(0xFFFEC44F),
        Color(0xFFFA802F), Color(0xFFE6420E), Color(0xFFB3360B), Color(0xFF5E2206),
    )

    // Chlorophyll - 29 stops, log10 scale from 0.01 to 8.0 mg/m3
    // HIGH DENSITY in front detection zones (0.10-0.50 and 0.50-2.0)
    private val chlorophyllStops = listOf(
        // Ultra-clear Gulf Stream (0.01-0.05)
        ColorStop(0.01, Color(0xFFE040E0)),    // Bright magenta
        ColorStop(0.02, Color(0xFF9966CC)),    // Purple
        ColorStop(0.03, Color(0xFF6633CC)),    // Purple-blue
        ColorStop(0.05, Color(0xFF0D1F6D)),    // Deep indigo
        // Open ocean (0.05-0.10)
        ColorStop(0.07, Color(0xFF1E3A8A)),    // Deep blue
        ColorStop(0.10, Color(0xFF1E40AF)),    // Strong blue
        // Offshore front zone (0.10-0.50) - HIGH DENSITY for front detection
        ColorStop(0.12, Color(0xFF1E50C0)),    // Blue
        ColorStop(0.15, Color(0xFF2060D0)),    // Medium blue
        ColorStop(0.18, Color(0xFF2070E0)),    // Bright blue
        ColorStop(0.22, Color(0xFF2196F3)),    // Professional blue
        ColorStop(0.28, Color(0xFF42A5F5)),    // Light blue
        ColorStop(0.35, Color(0xFF64B5F6)),    // Sky blue
        ColorStop(0.42, Color(0xFF81D4FA)),    // Pale blue
        ColorStop(0.50, Color(0xFF00BCD4)),    // Cyan
        // Productive front zone (0.50-2.0) - HIGH DENSITY for front detection
        ColorStop(0.60, Color(0xFF00B5B8)),    // Cyan-teal
        ColorStop(0.70, Color(0xFF00ACC1)),    // Deep cyan
        ColorStop(0.80, Color(0xFF009FA8)),    // Teal-cyan
        ColorStop(0.90, Color(0xFF00938F)),    // Teal
        ColorStop(1.00, Color(0xFF00897B)),    // Teal-green
        ColorStop(1.20, Color(0xFF1E9E7A)),    // Green-teal
        ColorStop(1.50, Color(0xFF26A69A)),    // Medium teal
        ColorStop(1.80, Color(0xFF3BAF8A)),    // Teal-green
        ColorStop(2.00, Color(0xFF4CAF50)),    // Green
        // Coastal/bloom (2.0-8.0)
        ColorStop(3.00, Color(0xFF66BB6A)),    // Bright green
        ColorStop(4.00, Color(0xFF9CCC65)),    // Yellow-green
        ColorStop(5.00, Color(0xFFC0CA33)),    // Lime
        ColorStop(6.00, Color(0xFFFDD835)),    // Yellow
        ColorStop(7.00, Color(0xFFFFB300)),    // Amber
        ColorStop(8.00, Color(0xFFF57C00)),    // Deep orange
    )

    // Salinity/Flow scale - smooth flowing transition from cool to warm
    private val flowColors = listOf(
        Color(0xFF0A0D3A), Color(0xFF0D1F6D), Color(0xFF12328F), Color(0xFF1746B1),
        Color(0xFF1F7BBF), Color(0xFF22A6C5), Color(0xFF27C8B8), Color(0xFF3FDF9B),
        Color(0xFF87F27A), Color(0xFFC9F560), Color(0xFFF7F060),
    )

    // MLD/Cascade scale - cool to warm with brightened professional tones
    private val cascadeColors = listOf(
        Color(0xFF2D2D6B), Color(0xFF1E4DB8), Color(0xFF2196F3), Color(0xFF03A9F4),
        Color(0xFF00BCD4), Color(0xFF009688), Color(0xFF4CAF50), Color(0xFF8BC34A),
        Color(0xFFCDDC39), Color(0xFFFFC107), Color(0xFFFF9800), Color(0xFFF44336),
    )

    // Currents scale - purple to red, 8 stops evenly distributed
    private val currentsColors = listOf(
        Color(0xFF1A0033), Color(0xFF4A148C), Color(0xFF1976D2), Color(0xFF00BCD4),
        Color(0xFF26A69A), Color(0xFFFFCA28), Color(0xFFFF7043), Color(0xFFE53935),
    )

    // MARK: - Public API

    fun windColor(speed: Double): Color = interpolate(speed, windStops)

    fun waveColor(height: Double): Color = interpolate(height, waveStops)

    fun sstColor(temp: Double, range: ClosedRange<Double>): Color =
        interpolateNormalized(temp, range, sstColors)

    fun chlorophyllColor(value: Double): Color = interpolate(value, chlorophyllStops)

    fun salinityColor(value: Double, range: ClosedRange<Double>): Color =
        interpolateNormalized(value, range, flowColors)

    fun mldColor(depth: Double, range: ClosedRange<Double>): Color =
        interpolateNormalized(depth, range, cascadeColors)

    fun currentsColor(speed: Double, range: ClosedRange<Double>): Color =
        interpolateNormalized(speed, range, currentsColors)

    // MARK: - Interpolation

    private data class ColorStop(val value: Double, val color: Color)

    /** Interpolate between explicit value stops (wind, wave, chlorophyll). */
    private fun interpolate(value: Double, stops: List<ColorStop>): Color {
        if (stops.isEmpty()) return Color.Gray

        // Clamp to range
        val clamped = value.coerceIn(stops.first().value, stops.last().value)

        // Find bounding stops
        for (i in 0 until stops.size - 1) {
            val lo = stops[i]
            val hi = stops[i + 1]
            if (clamped <= hi.value) {
                val fraction = ((clamped - lo.value) / (hi.value - lo.value)).toFloat()
                return lerp(lo.color, hi.color, fraction)
            }
        }
        return stops.last().color
    }

    /** Interpolate using evenly-spaced color array over a dynamic range (SST, salinity, MLD, currents). */
    private fun interpolateNormalized(
        value: Double,
        range: ClosedRange<Double>,
        colors: List<Color>,
    ): Color {
        if (colors.isEmpty()) return Color.Gray

        val clamped = value.coerceIn(range.start, range.endInclusive)
        val position = (clamped - range.start) / (range.endInclusive - range.start)

        val maxIndex = colors.size - 1
        val exactIndex = position * maxIndex
        val lowerIndex = exactIndex.toInt().coerceAtMost(maxIndex)
        val upperIndex = (lowerIndex + 1).coerceAtMost(maxIndex)
        val fraction = (exactIndex - lowerIndex).toFloat()

        return lerp(colors[lowerIndex], colors[upperIndex], fraction)
    }
}
