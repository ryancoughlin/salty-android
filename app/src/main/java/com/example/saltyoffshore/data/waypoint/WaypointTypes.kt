package com.example.saltyoffshore.data.waypoint

import kotlinx.serialization.Serializable

// -- WaypointSource --

sealed class WaypointSource {
    data object Own : WaypointSource()
    data class Shared(val sharedWaypoint: SharedWaypoint) : WaypointSource()

    val isShared: Boolean get() = this is Shared

    val sharedWaypointOrNull: SharedWaypoint?
        get() = (this as? Shared)?.sharedWaypoint

    val crewId: String?
        get() = (this as? Shared)?.sharedWaypoint?.crewId
}

// -- WaypointMapAnnotation --

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
        get() = source.sharedWaypointOrNull?.sharedByName

    val isShared: Boolean
        get() = source.isShared

    val sharedWaypoint: SharedWaypoint?
        get() = source.sharedWaypointOrNull
}

// -- WaypointSection --

data class WaypointSection(
    val id: String,
    val title: String,
    val waypoints: List<Waypoint>
)

// -- WaypointSheet --

sealed class WaypointSheet {
    data class Details(val waypointId: String) : WaypointSheet()
    data class Form(val waypoint: Waypoint) : WaypointSheet()

    val id: String
        get() = when (this) {
            is Details -> "details-$waypointId"
            is Form -> "form-${waypoint.id}"
        }
}

// -- WaypointSelectionSource --

enum class WaypointSelectionSource {
    MAP_TAP,
    MANAGEMENT_SHEET,
    MANAGEMENT_NAV,
    NOTIFICATION;

    val shouldDismissSheets: Boolean
        get() = when (this) {
            MAP_TAP, NOTIFICATION -> false
            MANAGEMENT_SHEET, MANAGEMENT_NAV -> true
        }

    val requiresNavigationDismissal: Boolean
        get() = when (this) {
            MAP_TAP, MANAGEMENT_SHEET, NOTIFICATION -> false
            MANAGEMENT_NAV -> true
        }
}

// -- WaypointSortOption --

@Serializable
enum class WaypointSortOption(val displayName: String) {
    DATE_CREATED("Date"),
    SYMBOL("Symbol");

    val iconName: String
        get() = when (this) {
            DATE_CREATED -> "calendar"
            SYMBOL -> "circle.hexagongrid.fill"
        }
}

// -- WaypointFilter --

@Serializable
enum class WaypointFilter(val displayName: String) {
    ALL("All"),
    MINE("Mine"),
    SHARED("Shared")
}

// -- WaypointPresentationMode --

enum class WaypointPresentationMode {
    NAVIGATION,
    SHEET
}
