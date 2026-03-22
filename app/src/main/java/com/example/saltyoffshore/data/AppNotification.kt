package com.example.saltyoffshore.data

/**
 * Notification types shown in the top-center notification capsules.
 * Matches iOS AppNotification enum in UnifiedNotificationManager.swift.
 */
sealed class AppNotification {
    data class Loading(val message: String = "Loading") : AppNotification()
    data class Error(val message: String) : AppNotification()
    // WaypointShare deferred to crew phase
}
