package com.example.saltyoffshore.data.waypoint

/**
 * Generates default waypoint names following marine GPS convention.
 * Produces incremental names like "WPT001", "WPT002", etc.
 *
 * iOS ref: Features/Waypoints/Services/WaypointDefaultNamingService.swift
 */
object WaypointDefaultNamingService {

    private const val DEFAULT_PREFIX = "WPT"

    /**
     * Generate the next available default waypoint name.
     * Finds the highest existing WPT### number and increments by 1.
     */
    fun generateDefaultName(existingWaypoints: List<Waypoint>): String {
        val nextNumber = getNextNumber(existingWaypoints)
        return formatWaypointName(nextNumber)
    }

    /**
     * Generate the next available name on a background dispatcher.
     * Use when the waypoint list is large to avoid blocking the main thread.
     */
    suspend fun generateDefaultNameAsync(existingWaypoints: List<Waypoint>): String =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            generateDefaultName(existingWaypoints)
        }

    private fun getNextNumber(existingWaypoints: List<Waypoint>): Int {
        val highest = existingWaypoints
            .mapNotNull { extractWaypointNumber(it.name) }
            .maxOrNull() ?: 0
        return highest + 1
    }

    private fun extractWaypointNumber(name: String?): Int? {
        if (name == null || !name.startsWith(DEFAULT_PREFIX)) return null
        return name.removePrefix(DEFAULT_PREFIX).toIntOrNull()
    }

    private fun formatWaypointName(number: Int): String =
        String.format("%s%03d", DEFAULT_PREFIX, number)

    /** Returns true if the name follows the WPT### auto-naming pattern. */
    fun isDefaultName(name: String?): Boolean =
        extractWaypointNumber(name) != null
}
