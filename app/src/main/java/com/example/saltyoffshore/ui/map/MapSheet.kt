package com.example.saltyoffshore.ui.map

import com.example.saltyoffshore.data.waypoint.Waypoint

/**
 * All sheets that can be presented over the map.
 * Matches iOS MapSheet enum -- each case carries its own data and presentation metadata.
 *
 * A sealed class is Kotlin's equivalent of Swift's enum with associated values.
 */
sealed class MapSheet {
    data object Tools : MapSheet()
    data object Layers : MapSheet()
    data object DatasetSelector : MapSheet()
    data object DatasetGuide : MapSheet()
    data object DatasetFilter : MapSheet()
    data object WaypointManagement : MapSheet()
    data object CrewList : MapSheet()
    data object CreateCrew : MapSheet()
    data object JoinCrew : MapSheet()
    data object SaveMap : MapSheet()
    data object SavedMaps : MapSheet()
    // Announcement, ShareLink, and StationDetail are managed through ViewModel state
    // (viewModel.showAnnouncementSheet, viewModel.shareLinkUrl, viewModel.selectedStationId)
    // and will be migrated here when those ViewModel properties are consolidated.
    data class ShareWaypoint(val waypoint: Waypoint) : MapSheet()

    /** Whether to skip the half-expanded state and go straight to full. */
    val skipPartiallyExpanded: Boolean
        get() = when (this) {
            is DatasetFilter -> false
            is JoinCrew -> false
            else -> true
        }

    /** Whether the sheet can be dismissed by swiping down. */
    val isDismissable: Boolean
        get() = when (this) {
            is DatasetFilter -> false
            else -> true
        }
}
