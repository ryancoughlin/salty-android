package com.example.saltyoffshore.data.station

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Station currents data from /stations/{id}/currents.
 * Matches iOS StationCurrents.
 */
@Serializable
data class StationCurrents(
    @SerialName("station_id")
    val stationId: String,
    @SerialName("station_name")
    val stationName: String,
    val latest: CurrentReading? = null,
    val history: List<CurrentReading>,
    @SerialName("depth_count")
    val depthCount: Int,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class CurrentReading(
    val time: String,
    val depths: List<DepthReading>
)

@Serializable
data class DepthReading(
    @SerialName("depth_ft")
    val depthFt: Int,
    @SerialName("speed_knots")
    val speedKnots: Double? = null,
    val direction: Double? = null
)
