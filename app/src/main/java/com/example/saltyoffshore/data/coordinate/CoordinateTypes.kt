package com.example.saltyoffshore.data.coordinate

import android.text.InputType

// MARK: - Coordinate Axis

/// Which coordinate axis a row or field belongs to
enum class CoordinateAxis {
    LATITUDE,
    LONGITUDE
}

// MARK: - Coordinate Field (Focus Target)

/// Unified focus field for coordinate input -- replaces format-specific cases
sealed class CoordinateField {
    data class Segment(val axis: CoordinateAxis, val index: Int) : CoordinateField()
}

// MARK: - Segment Configuration

/// Describes one numeric input segment (degrees, minutes, seconds, or decimal degrees)
data class CoordinateSegmentConfig(
    val unit: String,
    val placeholder: String,
    val maxLength: Int,
    val keyboardType: Int // Android InputType constant
)

// MARK: - Axis Configuration

/// Describes the full input layout for one coordinate axis (latitude or longitude)
data class CoordinateAxisConfig(
    val label: String,
    val segments: List<CoordinateSegmentConfig>,
    val directionOptions: List<String>
)

// MARK: - Axis Values

/// Holds the current input values for one coordinate axis
data class CoordinateAxisValues(
    val segments: List<String>,
    val direction: String
)

// MARK: - GPS Format Configurations

fun GPSFormat.latitudeConfig(): CoordinateAxisConfig = when (this) {
    GPSFormat.DMM -> CoordinateAxisConfig(
        label = "LATITUDE",
        segments = listOf(
            CoordinateSegmentConfig(unit = "\u00B0", placeholder = "42", maxLength = 2, keyboardType = InputType.TYPE_CLASS_NUMBER),
            CoordinateSegmentConfig(unit = "\u2032", placeholder = "20.740", maxLength = 9, keyboardType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL),
        ),
        directionOptions = listOf("N", "S")
    )
    GPSFormat.DMS -> CoordinateAxisConfig(
        label = "LATITUDE",
        segments = listOf(
            CoordinateSegmentConfig(unit = "\u00B0", placeholder = "42", maxLength = 2, keyboardType = InputType.TYPE_CLASS_NUMBER),
            CoordinateSegmentConfig(unit = "\u2032", placeholder = "20", maxLength = 2, keyboardType = InputType.TYPE_CLASS_NUMBER),
            CoordinateSegmentConfig(unit = "\u2033", placeholder = "44.40", maxLength = 9, keyboardType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL),
        ),
        directionOptions = listOf("N", "S")
    )
    GPSFormat.DD -> CoordinateAxisConfig(
        label = "LATITUDE",
        segments = listOf(
            CoordinateSegmentConfig(unit = "\u00B0", placeholder = "42.3456", maxLength = 9, keyboardType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL),
        ),
        directionOptions = listOf("N", "S")
    )
}

fun GPSFormat.longitudeConfig(): CoordinateAxisConfig = when (this) {
    GPSFormat.DMM -> CoordinateAxisConfig(
        label = "LONGITUDE",
        segments = listOf(
            CoordinateSegmentConfig(unit = "\u00B0", placeholder = "070", maxLength = 3, keyboardType = InputType.TYPE_CLASS_NUMBER),
            CoordinateSegmentConfig(unit = "\u2032", placeholder = "07.407", maxLength = 9, keyboardType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL),
        ),
        directionOptions = listOf("E", "W")
    )
    GPSFormat.DMS -> CoordinateAxisConfig(
        label = "LONGITUDE",
        segments = listOf(
            CoordinateSegmentConfig(unit = "\u00B0", placeholder = "070", maxLength = 3, keyboardType = InputType.TYPE_CLASS_NUMBER),
            CoordinateSegmentConfig(unit = "\u2032", placeholder = "07", maxLength = 2, keyboardType = InputType.TYPE_CLASS_NUMBER),
            CoordinateSegmentConfig(unit = "\u2033", placeholder = "24.42", maxLength = 9, keyboardType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL),
        ),
        directionOptions = listOf("E", "W")
    )
    GPSFormat.DD -> CoordinateAxisConfig(
        label = "LONGITUDE",
        segments = listOf(
            CoordinateSegmentConfig(unit = "\u00B0", placeholder = "070.1234", maxLength = 10, keyboardType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL),
        ),
        directionOptions = listOf("E", "W")
    )
}
