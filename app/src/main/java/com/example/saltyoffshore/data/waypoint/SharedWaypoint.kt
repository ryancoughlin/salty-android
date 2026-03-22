package com.example.saltyoffshore.data.waypoint

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SharedWaypoint(
    val id: String,
    @SerialName("waypoint_data") var waypoint: Waypoint,
    @SerialName("shared_by_user_id") val sharedByUserId: String,
    @SerialName("crew_id") val crewId: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("read_by_user_ids") val readByUserIds: List<String> = emptyList(),
    var sharedByName: String? = null,
    var sharedByFirstName: String? = null
) {
    /** Whether this waypoint is unread for a given user.
     *  Shared by someone else AND not yet read by this user. */
    fun isUnread(userId: String): Boolean =
        sharedByUserId != userId && !readByUserIds.contains(userId)
}
