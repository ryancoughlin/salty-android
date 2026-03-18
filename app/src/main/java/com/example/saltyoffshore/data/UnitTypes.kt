package com.example.saltyoffshore.data

/**
 * Depth measurement units supported in the app.
 */
enum class DepthUnits(val rawValue: String, val displayName: String, val unitSuffix: String) {
    FEET("feet", "Feet", "'"),
    METERS("meters", "Meters", "m"),
    FATHOMS("fathoms", "Fathoms", "fm");

    /** Conversion factor from meters to this unit */
    val metersToUnitFactor: Double
        get() = when (this) {
            FEET -> 3.28084
            METERS -> 1.0
            FATHOMS -> 3.28084 / 6.0
        }

    companion object {
        fun fromRawValue(value: String?): DepthUnits? =
            entries.find { it.rawValue == value }
    }
}

/**
 * Distance measurement units supported in the app.
 */
enum class DistanceUnits(val rawValue: String, val displayName: String, val unitSuffix: String, val speedSuffix: String) {
    MILES("miles", "Miles", "mi", "mph"),
    NAUTICAL_MILES("nautical_miles", "Nautical Miles", "nm", "kts"),
    KILOMETERS("kilometers", "Kilometers", "km", "km/h");

    /** Conversion factor from meters to this unit */
    val metersToUnitFactor: Double
        get() = when (this) {
            MILES -> 0.000621371
            NAUTICAL_MILES -> 1.0 / 1852.0
            KILOMETERS -> 0.001
        }

    /** Convert nautical miles to user's preferred distance unit */
    fun convertDistance(fromNauticalMiles: Double): Double = when (this) {
        NAUTICAL_MILES -> fromNauticalMiles
        MILES -> fromNauticalMiles * 1.15078
        KILOMETERS -> fromNauticalMiles * 1.852
    }

    /** Convert knots to user's preferred speed unit */
    fun convertSpeed(fromKnots: Double): Double = when (this) {
        NAUTICAL_MILES -> fromKnots
        MILES -> fromKnots * 1.15078
        KILOMETERS -> fromKnots * 1.852
    }

    companion object {
        fun fromRawValue(value: String?): DistanceUnits? =
            entries.find { it.rawValue == value }
    }
}

/**
 * Speed measurement units supported in the app.
 */
enum class SpeedUnits(val rawValue: String, val displayName: String, val unitSuffix: String) {
    KNOTS("knots", "Knots", "kts"),
    METERS_PER_SECOND("meters_per_second", "m/s", "m/s"),
    MILES_PER_HOUR("miles_per_hour", "mph", "mph"),
    KILOMETERS_PER_HOUR("kilometers_per_hour", "km/h", "km/h");

    /** Conversion factor from m/s to this unit */
    val fromMpsConversionFactor: Float
        get() = when (this) {
            KNOTS -> 1.94384f
            METERS_PER_SECOND -> 1.0f
            MILES_PER_HOUR -> 2.23694f
            KILOMETERS_PER_HOUR -> 3.6f
        }

    companion object {
        fun fromRawValue(value: String?): SpeedUnits? =
            entries.find { it.rawValue == value }
    }
}

/**
 * Temperature measurement units supported in the app.
 */
enum class TemperatureUnits(val rawValue: String, val displayName: String, val unitSuffix: String) {
    FAHRENHEIT("fahrenheit", "Fahrenheit", "\u00B0F"),
    CELSIUS("celsius", "Celsius", "\u00B0C");

    /** Convert from Fahrenheit (API base unit) to this unit */
    fun convert(fahrenheit: Double): Double = when (this) {
        FAHRENHEIT -> fahrenheit
        CELSIUS -> (fahrenheit - 32) * 5.0 / 9.0
    }

    /** Convert from this unit back to Fahrenheit */
    fun toFahrenheit(value: Double): Double = when (this) {
        FAHRENHEIT -> value
        CELSIUS -> (value * 9.0 / 5.0) + 32
    }

    companion object {
        fun fromRawValue(value: String?): TemperatureUnits? =
            entries.find { it.rawValue == value }
    }
}

/**
 * Coordinate system preference for displaying and entering coordinates.
 */
enum class CoordinateSystem(val rawValue: String, val displayName: String, val description: String) {
    GPS("GPS (DMM)", "GPS", "Degrees & Decimal Minutes (42\u00B0 20.740\u2032 N, 070\u00B0 07.407\u2032 W)"),
    LORAN("LORAN-C", "LORAN-C", "Time Difference (TD) values for LORAN-C chains");

    companion object {
        fun fromRawValue(value: String?): CoordinateSystem? =
            entries.find { it.rawValue == value }
    }
}

/**
 * GPS coordinate display format preference.
 */
enum class GpsFormat(val rawValue: String, val displayName: String, val example: String) {
    DMM("dmm", "Degrees & Minutes", "42\u00B0 20.740\u2032 N"),
    DMS("dms", "Degrees, Minutes & Seconds", "42\u00B0 20\u2032 44\u2033 N"),
    DD("dd", "Decimal Degrees", "42.3456\u00B0 N");

    companion object {
        fun fromRawValue(value: String?): GpsFormat? =
            entries.find { it.rawValue == value }
    }
}

/**
 * Map theme options for different visual styles.
 */
enum class MapTheme(val rawValue: String, val displayName: String, val description: String) {
    LIGHT("light", "Light", "Bright ocean view for daytime fishing"),
    DARK("dark", "Dark", "Dark theme for night navigation"),
    SATELLITE("satellite", "Satellite", "Satellite imagery for terrain reference");

    companion object {
        /** Themes available in user settings (satellite excluded until feature complete) */
        val userSelectable: List<MapTheme> = listOf(LIGHT, DARK)

        fun fromRawValue(value: String?): MapTheme? =
            entries.find { it.rawValue == value }
    }
}
