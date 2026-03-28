package com.example.saltyoffshore.data.waypoint

/**
 * Distinguishes between a user's own waypoints and shared crew waypoints.
 * Port of iOS SharedWaypointTypes.swift -> WaypointSource enum.
 */
sealed class WaypointSource {
    data object Own : WaypointSource()
    data class Shared(val sharedWaypoint: SharedWaypoint) : WaypointSource()

    val isShared: Boolean get() = this is Shared

    val sharedWaypoint: SharedWaypoint?
        get() = (this as? Shared)?.sharedWaypoint

    val crewId: String?
        get() = (this as? Shared)?.sharedWaypoint?.crewId
}

/**
 * Wrapper for displaying waypoints on the map with source information.
 * Port of iOS SharedWaypointTypes.swift -> WaypointMapAnnotation struct.
 *
 * ID strategy: always uses waypoint.id for consistent identity across own/shared contexts.
 */
data class WaypointMapAnnotation(
    val id: String,
    val waypoint: Waypoint,
    val source: WaypointSource
) {
    constructor(waypoint: Waypoint, source: WaypointSource = WaypointSource.Own) : this(
        id = waypoint.id,
        waypoint = waypoint,
        source = source
    )

    constructor(sharedWaypoint: SharedWaypoint) : this(
        id = sharedWaypoint.waypoint.id,
        waypoint = sharedWaypoint.waypoint,
        source = WaypointSource.Shared(sharedWaypoint)
    )

    val sharedByName: String?
        get() = (source as? WaypointSource.Shared)?.sharedWaypoint?.sharedByName

    val isShared: Boolean get() = source.isShared

    val sharedWaypoint: SharedWaypoint? get() = source.sharedWaypoint
}
