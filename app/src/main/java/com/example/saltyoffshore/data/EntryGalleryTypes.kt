package com.example.saltyoffshore.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.min

// MARK: - Entry Gallery Filter

enum class EntryGalleryFilter(val label: String, val icon: ImageVector, val description: String) {
    ALL("All Entries", Icons.Filled.List, "Show all available entries"),
    LOW_CLOUD_COVER("Clear Skies", Icons.Filled.WbSunny, "Show entries with minimal cloud coverage"),
    HIGH_CLOUD_COVER("Cloudy", Icons.Filled.Cloud, "Show entries with significant cloud coverage")
}

// MARK: - Entry Quality

enum class EntryQuality(val label: String, val color: Color, val icon: ImageVector) {
    EXCELLENT("Excellent", Color(0xFF4CAF50), Icons.Filled.Star),
    GOOD("Good", Color(0xFF2196F3), Icons.Filled.StarHalf),
    FAIR("Fair", Color(0xFFFF9800), Icons.Filled.StarOutline),
    POOR("Poor", Color(0xFFF44336), Icons.Filled.Warning)
}

// MARK: - Entry Gallery Item

data class EntryGalleryItem(
    val id: String,
    val entry: TimeEntry,
    val hasLowCloudCover: Boolean,
    val qualityScore: Double
) {
    val qualityIndicator: EntryQuality
        get() = when {
            qualityScore >= 0.8 -> EntryQuality.EXCELLENT
            qualityScore >= 0.6 -> EntryQuality.GOOD
            qualityScore >= 0.4 -> EntryQuality.FAIR
            else -> EntryQuality.POOR
        }

    fun matches(filter: EntryGalleryFilter): Boolean = when (filter) {
        EntryGalleryFilter.ALL -> true
        EntryGalleryFilter.LOW_CLOUD_COVER -> hasLowCloudCover
        EntryGalleryFilter.HIGH_CLOUD_COVER -> !hasLowCloudCover
    }

    val formattedTimestamp: String
        get() = formatTimestamp(entry.timestamp, "E, MMM d, h:mm a")

    val shortDate: String
        get() = formatTimestamp(entry.timestamp, "E, MMM d")

    val timeOnly: String
        get() = formatTimestamp(entry.timestamp, "h:mm a")
}

fun TimeEntry.toGalleryItem(): EntryGalleryItem {
    val hasLowCloudCover = dataCoveragePercentage?.let { it >= 30.0 } ?: false

    var score = 1.0

    val coverage = dataCoveragePercentage
    if (coverage != null) {
        if (coverage < 30.0) score *= 0.3
    } else {
        score *= 0.7
    }

    val daysOld = try {
        val instant = Instant.parse(timestamp)
        ChronoUnit.DAYS.between(instant, Instant.now()).toDouble()
    } catch (_: Exception) {
        0.0
    }
    val agePenalty = min(0.3, daysOld * 0.05)
    score *= (1.0 - agePenalty)
    score = score.coerceIn(0.0, 1.0)

    return EntryGalleryItem(
        id = id,
        entry = this,
        hasLowCloudCover = hasLowCloudCover,
        qualityScore = score
    )
}

fun List<TimeEntry>.toGalleryItems(): List<EntryGalleryItem> = map { it.toGalleryItem() }

fun List<TimeEntry>.filtered(by: EntryGalleryFilter): List<TimeEntry> =
    toGalleryItems().filter { it.matches(by) }.map { it.entry }

// MARK: - Entry Gallery State

data class EntryGalleryState(
    val isPresented: Boolean = false,
    val selectedFilter: EntryGalleryFilter = EntryGalleryFilter.ALL,
    val selectedEntry: TimeEntry? = null,
    val isLoading: Boolean = false
)

// MARK: - Coverage Filter State

data class CoverageFilterState(
    val minClearPercentage: Double = 0.0,
    val filteredCount: Int = 0,
    val totalCount: Int = 0
) {
    val isActive: Boolean get() = minClearPercentage > 0

    val displayLabel: String
        get() = if (isActive) "≥ ${minClearPercentage.toInt()}%" else "All"

    val countLabel: String
        get() = if (isActive) "$filteredCount of $totalCount" else ""
}

// MARK: - Private Helpers

private fun formatTimestamp(timestamp: String, pattern: String): String = try {
    val instant = Instant.parse(timestamp)
    val zoned = instant.atZone(ZoneId.systemDefault())
    zoned.format(DateTimeFormatter.ofPattern(pattern))
} catch (_: Exception) {
    timestamp
}
