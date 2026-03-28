package com.example.saltyoffshore.data.weather

import com.example.saltyoffshore.data.station.MarineUnits
import java.time.Instant
import java.util.UUID

/**
 * Wave measurement data. Matches iOS WaveData.
 */
data class WaveData(
    val height: Double,   // feet
    val period: Double,   // seconds
    val direction: Double // degrees
) {
    val heightFormatted: String get() = String.format("%.1f'", height)
    val periodFormatted: String get() = String.format("%.1fs", period)
    val directionFormatted: String get() = String.format("%.0f°", direction)
    val directionCardinal: String get() = MarineUnits.cardinalDirection(direction)
}

data class WaveConditions(
    val id: String = UUID.randomUUID().toString(),
    val time: Instant,
    val wave: WaveData
) {
    val height: Double get() = wave.height
    val period: Double get() = wave.period
    val direction: Double get() = wave.direction
}
