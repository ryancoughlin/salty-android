package com.example.saltyoffshore.data.waypoint

import java.util.Date

/**
 * Lightweight notification for waypoint sharing events.
 * Port of iOS WaypointShareNotification.swift.
 *
 * Constructed from a SharedWaypoint + sender display name.
 */
data class WaypointShareNotification(
    val id: String,
    val waypointId: String,
    val senderName: String,
    val senderFirstName: String?,
    val latitude: Double,
    val longitude: Double,
    val waypointName: String?,
    val waypointSymbol: WaypointSymbol,
    val receivedAt: Date
) {
    companion object {
        fun from(sharedWaypoint: SharedWaypoint, senderName: String) = WaypointShareNotification(
            id = sharedWaypoint.id,
            waypointId = sharedWaypoint.waypoint.id,
            senderName = senderName,
            senderFirstName = sharedWaypoint.sharedByFirstName,
            latitude = sharedWaypoint.waypoint.latitude,
            longitude = sharedWaypoint.waypoint.longitude,
            waypointName = sharedWaypoint.waypoint.name,
            waypointSymbol = sharedWaypoint.waypoint.symbol,
            receivedAt = Date()
        )
    }
}
