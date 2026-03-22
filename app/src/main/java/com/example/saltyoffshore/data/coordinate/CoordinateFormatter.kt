package com.example.saltyoffshore.data.coordinate

import kotlin.math.abs

/// Utility for formatting coordinates in various GPS formats
object CoordinateFormatter {

    // MARK: - Format-Aware Public Methods

    fun formatLatitude(latitude: Double, format: GPSFormat): String = when (format) {
        GPSFormat.DMM -> formatDMMLat(latitude)
        GPSFormat.DMS -> formatDMSLat(latitude)
        GPSFormat.DD -> formatDDLat(latitude)
    }

    fun formatLongitude(longitude: Double, format: GPSFormat): String = when (format) {
        GPSFormat.DMM -> formatDMMLon(longitude)
        GPSFormat.DMS -> formatDMSLon(longitude)
        GPSFormat.DD -> formatDDLon(longitude)
    }

    /// Format both coordinates as a single display string
    fun formatCoordinate(lat: Double, lon: Double, format: GPSFormat): String {
        return "${formatLatitude(lat, format)} ${formatLongitude(lon, format)}"
    }

    // MARK: - DMM Format (Degrees & Decimal Minutes)

    private fun formatDMMLat(latitude: Double): String {
        val direction = if (latitude >= 0) "N" else "S"
        val absLat = abs(latitude)
        var degrees = absLat.toInt()
        var minutes = (absLat - degrees) * 60.0
        // Handle rounding overflow
        if (minutes >= 59.995) {
            minutes = 0.0
            degrees += 1
        }
        return String.format("%d\u00B0 %.6f\u2032 %s", degrees, minutes, direction)
    }

    private fun formatDMMLon(longitude: Double): String {
        val direction = if (longitude >= 0) "E" else "W"
        val absLon = abs(longitude)
        var degrees = absLon.toInt()
        var minutes = (absLon - degrees) * 60.0
        // Handle rounding overflow
        if (minutes >= 59.995) {
            minutes = 0.0
            degrees += 1
        }
        return String.format("%03d\u00B0 %.6f\u2032 %s", degrees, minutes, direction)
    }

    // MARK: - DMS Format (Degrees, Minutes, Seconds)

    private fun formatDMSLat(latitude: Double): String {
        val direction = if (latitude >= 0) "N" else "S"
        val absLat = abs(latitude)
        var degrees = absLat.toInt()
        val minutesDecimal = (absLat - degrees) * 60.0
        var minutes = minutesDecimal.toInt()
        var seconds = (minutesDecimal - minutes) * 60.0
        // Handle rounding overflow
        if (seconds >= 59.995) {
            seconds = 0.0
            minutes += 1
        }
        if (minutes >= 60) {
            minutes = 0
            degrees += 1
        }
        return String.format("%d\u00B0 %02d\u2032 %05.2f\u2033 %s", degrees, minutes, seconds, direction)
    }

    private fun formatDMSLon(longitude: Double): String {
        val direction = if (longitude >= 0) "E" else "W"
        val absLon = abs(longitude)
        var degrees = absLon.toInt()
        val minutesDecimal = (absLon - degrees) * 60.0
        var minutes = minutesDecimal.toInt()
        var seconds = (minutesDecimal - minutes) * 60.0
        // Handle rounding overflow
        if (seconds >= 59.995) {
            seconds = 0.0
            minutes += 1
        }
        if (minutes >= 60) {
            minutes = 0
            degrees += 1
        }
        return String.format("%03d\u00B0 %02d\u2032 %05.2f\u2033 %s", degrees, minutes, seconds, direction)
    }

    // MARK: - DD Format (Decimal Degrees)

    private fun formatDDLat(latitude: Double): String {
        val direction = if (latitude >= 0) "N" else "S"
        return String.format("%.6f\u00B0 %s", abs(latitude), direction)
    }

    private fun formatDDLon(longitude: Double): String {
        val direction = if (longitude >= 0) "E" else "W"
        return String.format("%.6f\u00B0 %s", abs(longitude), direction)
    }

    // MARK: - Unified Parsing (from CoordinateAxisValues)

    fun parse(values: CoordinateAxisValues, format: GPSFormat, isLatitude: Boolean): Double? {
        val s = values.segments
        return when (format) {
            GPSFormat.DMM -> {
                if (s.size < 2) return null
                if (isLatitude) parseLatitudeDMM(s[0], s[1], values.direction)
                else parseLongitudeDMM(s[0], s[1], values.direction)
            }
            GPSFormat.DMS -> {
                if (s.size < 3) return null
                if (isLatitude) parseLatitudeDMS(s[0], s[1], s[2], values.direction)
                else parseLongitudeDMS(s[0], s[1], s[2], values.direction)
            }
            GPSFormat.DD -> {
                if (s.isEmpty()) return null
                if (isLatitude) parseLatitudeDD(s[0], values.direction)
                else parseLongitudeDD(s[0], values.direction)
            }
        }
    }

    // MARK: - DMM Component-Based Parsing

    fun parseLatitudeDMM(degrees: String, minutes: String, direction: String): Double? {
        val deg = degrees.toDoubleOrNull() ?: return null
        val min = minutes.toDoubleOrNull() ?: return null
        if (deg < 0 || deg > 90 || min < 0 || min >= 60) return null
        val lat = deg + (min / 60.0)
        return if (direction == "N") lat else -lat
    }

    fun parseLongitudeDMM(degrees: String, minutes: String, direction: String): Double? {
        val deg = degrees.toDoubleOrNull() ?: return null
        val min = minutes.toDoubleOrNull() ?: return null
        if (deg < 0 || deg > 180 || min < 0 || min >= 60) return null
        val lon = deg + (min / 60.0)
        return if (direction == "E") lon else -lon
    }

    // MARK: - DMS Component-Based Parsing

    fun parseLatitudeDMS(degrees: String, minutes: String, seconds: String, direction: String): Double? {
        val deg = degrees.toDoubleOrNull() ?: return null
        val min = minutes.toDoubleOrNull() ?: return null
        val sec = seconds.toDoubleOrNull() ?: return null
        if (deg < 0 || deg > 90 || min < 0 || min >= 60 || sec < 0 || sec >= 60) return null
        val lat = deg + (min / 60.0) + (sec / 3600.0)
        if (lat > 90) return null
        return if (direction == "N") lat else -lat
    }

    fun parseLongitudeDMS(degrees: String, minutes: String, seconds: String, direction: String): Double? {
        val deg = degrees.toDoubleOrNull() ?: return null
        val min = minutes.toDoubleOrNull() ?: return null
        val sec = seconds.toDoubleOrNull() ?: return null
        if (deg < 0 || deg > 180 || min < 0 || min >= 60 || sec < 0 || sec >= 60) return null
        val lon = deg + (min / 60.0) + (sec / 3600.0)
        if (lon > 180) return null
        return if (direction == "E") lon else -lon
    }

    // MARK: - DD Component-Based Parsing

    fun parseLatitudeDD(degrees: String, direction: String): Double? {
        val deg = degrees.toDoubleOrNull() ?: return null
        if (deg < 0 || deg > 90) return null
        return if (direction == "N") deg else -deg
    }

    fun parseLongitudeDD(degrees: String, direction: String): Double? {
        val deg = degrees.toDoubleOrNull() ?: return null
        if (deg < 0 || deg > 180) return null
        return if (direction == "E") deg else -deg
    }

    // MARK: - Populate Form Values from Decimal Degrees

    fun valuesFromCoordinate(degrees: Double, format: GPSFormat, isLatitude: Boolean): CoordinateAxisValues {
        val absDeg = abs(degrees)
        val direction = if (isLatitude) {
            if (degrees >= 0) "N" else "S"
        } else {
            if (degrees >= 0) "E" else "W"
        }

        return when (format) {
            GPSFormat.DMM -> {
                val deg = absDeg.toInt()
                val min = (absDeg - deg) * 60.0
                CoordinateAxisValues(
                    segments = listOf(deg.toString(), String.format("%.6f", min)),
                    direction = direction
                )
            }
            GPSFormat.DMS -> {
                val deg = absDeg.toInt()
                val minDecimal = (absDeg - deg) * 60.0
                val min = minDecimal.toInt()
                val sec = (minDecimal - min) * 60.0
                CoordinateAxisValues(
                    segments = listOf(deg.toString(), min.toString(), String.format("%.2f", sec)),
                    direction = direction
                )
            }
            GPSFormat.DD -> {
                CoordinateAxisValues(
                    segments = listOf(String.format("%.6f", absDeg)),
                    direction = direction
                )
            }
        }
    }

    // MARK: - String Parsing (for waypoint input)

    fun parseLatitude(string: String): Double? = parseDMM(string, isLatitude = true)

    fun parseLongitude(string: String): Double? = parseDMM(string, isLatitude = false)

    private fun parseDMM(string: String, isLatitude: Boolean): Double? {
        val trimmed = string.trim().uppercase()

        // Extract direction (N/S/E/W) - standard format has direction at end
        var direction: String? = null
        var remaining = trimmed

        if (trimmed.endsWith(" N") || trimmed.endsWith(" S") ||
            trimmed.endsWith(" E") || trimmed.endsWith(" W")
        ) {
            direction = trimmed.takeLast(1)
            remaining = trimmed.dropLast(2)
        }

        // Extract degrees and minutes by splitting on degree/minute symbols
        val components = remaining.split(*charArrayOf('\u00B0', '\'', '\u2032'))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (components.size < 2) return null
        val deg = components[0].toDoubleOrNull() ?: return null
        val min = components[1].toDoubleOrNull() ?: return null

        val decimalDegrees = abs(deg) + min / 60.0

        if (isLatitude && decimalDegrees > 90) return null
        if (!isLatitude && decimalDegrees > 180) return null

        if (direction != null) {
            return if (isLatitude) {
                if (direction == "S") -decimalDegrees else decimalDegrees
            } else {
                if (direction == "W") -decimalDegrees else decimalDegrees
            }
        }

        return decimalDegrees
    }
}
