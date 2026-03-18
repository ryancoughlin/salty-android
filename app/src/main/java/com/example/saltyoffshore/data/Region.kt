package com.example.saltyoffshore.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegionsResponse(
    val groups: List<RegionGroup>,
    @SerialName("server_url") val serverUrl: String
)

@Serializable
data class RegionGroup(
    val group: String,
    val regions: List<RegionListItem>
)

@Serializable
enum class RegionStatus {
    @SerialName("active") ACTIVE,
    @SerialName("coming_soon") COMING_SOON,
    @SerialName("maintenance") MAINTENANCE,
    @SerialName("populating") POPULATING,
    @SerialName("unknown") UNKNOWN
}

@Serializable
data class RegionListItem(
    val id: String,
    val name: String,
    val group: String,
    val bounds: List<List<Double>>,
    val thumbnail: String,
    val status: RegionStatus = RegionStatus.ACTIVE
) {
    val centerLat: Double get() = (bounds[0][1] + bounds[1][1]) / 2.0
    val centerLon: Double get() = (bounds[0][0] + bounds[1][0]) / 2.0
}
