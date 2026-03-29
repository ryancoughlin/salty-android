package com.example.saltyoffshore.data

import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Resolves the best matching entry for an overlay dataset given the primary entry's time.
 * Uses a 3-day lookback from the primary entry's timestamp at the overlay's selected depth.
 * Matches iOS resolveOverlayEntry() exactly.
 */
object OverlayEntryResolver {

    private const val MAX_LOOKBACK_DAYS = 3L

    /**
     * Find the best overlay entry matching the primary entry's time and the overlay's depth.
     *
     * @param overlayDataset The overlay dataset with populated entries
     * @param primaryEntry The currently selected primary entry
     * @param override Optional entry override (manual time/depth selection)
     * @return Best matching entry, or null if none within 3-day window
     */
    fun resolve(
        overlayDataset: Dataset,
        primaryEntry: TimeEntry,
        override: EntryOverride? = null
    ): TimeEntry? {
        val entries = overlayDataset.entries ?: return null
        if (entries.isEmpty()) return null

        val targetDepth = override?.depth ?: 0

        // Filter to matching depth
        val depthEntries = entries.filter { it.depth == targetDepth }
        if (depthEntries.isEmpty()) return null

        // If override has explicit timestamp, find exact match
        if (override?.timestamp != null) {
            return depthEntries.find { it.timestamp == override.timestamp }
                ?: findClosestEntry(depthEntries, override.timestamp)
        }

        // Otherwise match to primary entry's timestamp within 3-day lookback
        return findClosestEntry(depthEntries, primaryEntry.timestamp)
    }

    private fun findClosestEntry(entries: List<TimeEntry>, targetTimestamp: String): TimeEntry? {
        val targetInstant = try {
            Instant.parse(targetTimestamp)
        } catch (e: Exception) {
            return entries.maxByOrNull { it.timestamp }
        }

        val cutoff = targetInstant.minus(MAX_LOOKBACK_DAYS, ChronoUnit.DAYS)

        // Find entries within the lookback window (target - 3 days to target)
        val candidates = entries.filter { entry ->
            val entryInstant = try {
                Instant.parse(entry.timestamp)
            } catch (e: Exception) {
                return@filter false
            }
            entryInstant in cutoff..targetInstant
        }

        if (candidates.isEmpty()) return null

        // Return the most recent candidate (closest to primary)
        return candidates.maxByOrNull { it.timestamp }
    }
}
