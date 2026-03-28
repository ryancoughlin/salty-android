package com.example.saltyoffshore.data.station

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.round

/**
 * Station observation data from /stations/{id}/observations.
 * Matches iOS StationObservation.
 */
@Serializable
data class StationObservation(
    @SerialName("station_id")
    val stationId: String,
    val name: String,
    val location: ObservationLocation,
    val observations: ObservationData
)

@Serializable
data class ObservationLocation(
    val type: String,
    val coordinates: List<Double>
) {
    val longitude: Double get() = coordinates[0]
    val latitude: Double get() = coordinates[1]
}

@Serializable
data class ObservationData(
    val time: String, // ISO8601
    val wind: Wind,
    val wave: Wave,
    val met: Met,
    @SerialName("data_age")
    val dataAge: DataAge
)

@Serializable
data class DataAge(
    val minutes: Double,
    val isStale: Boolean
) {
    val formattedTimeAgo: String
        get() = when {
            minutes < 1 -> "just now"
            minutes < 60 -> "${round(minutes).toInt()}m ago"
            else -> {
                val hours = (minutes / 60).toInt()
                val remainingMinutes = round(minutes % 60).toInt()
                if (remainingMinutes == 0) "${hours}h ago"
                else "${hours}h ${remainingMinutes}m ago"
            }
        }
}

@Serializable
data class Wind(
    val speed: Double? = null,
    val direction: Double? = null,
    val gust: Double? = null
) {
    val formattedWindSpeed: String?
        get() {
            val s = speed ?: return null
            return String.format("%.1f", MarineUnits.metersPerSecondToKnots(s))
        }
}

@Serializable
data class Wave(
    val height: Double? = null,
    val period: Double? = null,
    val direction: Double? = null,
    @SerialName("average_period")
    val averagePeriod: Double? = null,
    val steepness: String? = null
) {
    val formattedPeriod: String?
        get() {
            val p = period ?: return null
            return String.format("%.1fs", p)
        }

    val formattedHeight: String?
        get() {
            val h = height ?: return null
            return String.format("%.1f'", MarineUnits.metersToFeet(h))
        }
}

@Serializable
data class Met(
    val pressure: Double? = null,
    @SerialName("air_temp")
    val airTemp: Double? = null,
    @SerialName("water_temp")
    val waterTemp: Double? = null,
    val dewpoint: Double? = null,
    val visibility: Double? = null,
    @SerialName("pressure_tendency")
    val pressureTendency: Double? = null,
    @SerialName("water_level")
    val waterLevel: Double? = null
)
