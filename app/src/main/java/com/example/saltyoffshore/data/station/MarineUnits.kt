package com.example.saltyoffshore.data.station

/**
 * Marine unit conversions matching iOS MarineUnits.
 */
object MarineUnits {

    // --- Conversions ---

    fun metersToFeet(meters: Double): Double = meters * 3.28084

    fun metersPerSecondToMph(mps: Double): Double = mps * 2.23694

    fun metersPerSecondToKnots(mps: Double): Double = mps * 1.94384

    fun celsiusToFahrenheit(celsius: Double): Double = celsius * 9.0 / 5.0 + 32.0

    fun metersToNauticalMiles(meters: Double): Double = meters / 1852.0

    fun metersToMiles(meters: Double): Double = meters / 1609.344

    fun metersToKilometers(meters: Double): Double = meters / 1000.0

    // --- Formatting ---

    /** Formats temperature from Celsius to Fahrenheit with degree symbol. */
    fun formatTemperature(celsius: Double): String {
        val fahrenheit = celsiusToFahrenheit(celsius)
        return "${fahrenheit.toInt()}°F"
    }

    fun minimalWindSpeed(mps: Double): String =
        String.format("%.0f", metersPerSecondToKnots(mps))

    fun minimalWaveHeight(meters: Double): String =
        String.format("%.1f", metersToFeet(meters))

    /** Cardinal direction from degrees. */
    fun cardinalDirection(degrees: Double): String {
        val normalized = ((degrees % 360) + 360) % 360
        val directions = listOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
            "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
        val index = ((normalized + 11.25) / 22.5).toInt() % 16
        return directions[index]
    }
}
