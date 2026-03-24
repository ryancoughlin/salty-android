package com.example.saltyoffshore.ui.chart

import androidx.compose.ui.graphics.Color

/**
 * Color scales for wave/wind chart bars.
 * Matches iOS ChartColorScales.
 */
object ChartColorScales {

    // 9 stops from calm (blue) to extreme (red)
    private val windColors = listOf(
        Color(0xFF3B82F6), // 0 kt - blue
        Color(0xFF06B6D4), // 5 kt - cyan
        Color(0xFF10B981), // 10 kt - green
        Color(0xFF84CC16), // 15 kt - lime
        Color(0xFFFBBF24), // 20 kt - yellow
        Color(0xFFF59E0B), // 25 kt - amber
        Color(0xFFEF4444), // 30 kt - red
        Color(0xFFDC2626), // 35 kt - dark red
        Color(0xFF991B1B)  // 40+ kt - crimson
    )

    private val waveColors = listOf(
        Color(0xFF3B82F6), // 0 ft - blue
        Color(0xFF06B6D4), // 1.5 ft - cyan
        Color(0xFF10B981), // 3 ft - green
        Color(0xFF84CC16), // 4.5 ft - lime
        Color(0xFFFBBF24), // 6 ft - yellow
        Color(0xFFF59E0B), // 7.5 ft - amber
        Color(0xFFEF4444), // 9 ft - red
        Color(0xFFDC2626), // 10.5 ft - dark red
        Color(0xFF991B1B)  // 12+ ft - crimson
    )

    fun windColor(speed: Double): Color = interpolate(windColors, speed, 0.0, 40.0)

    fun waveColor(height: Double): Color = interpolate(waveColors, height, 0.0, 12.0)

    private fun interpolate(colors: List<Color>, value: Double, min: Double, max: Double): Color {
        val normalized = ((value - min) / (max - min)).coerceIn(0.0, 1.0)
        val scaledIndex = normalized * (colors.size - 1)
        val lowerIndex = scaledIndex.toInt().coerceIn(0, colors.size - 2)
        val fraction = (scaledIndex - lowerIndex).toFloat()

        val c1 = colors[lowerIndex]
        val c2 = colors[lowerIndex + 1]
        return Color(
            red = c1.red + (c2.red - c1.red) * fraction,
            green = c1.green + (c2.green - c1.green) * fraction,
            blue = c1.blue + (c2.blue - c1.blue) * fraction,
            alpha = 1f
        )
    }
}
