package com.example.saltyoffshore.data.satellite

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

// MARK: - Date Helpers

private val isoParser: DateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME

private fun parseInstant(iso: String): Instant? =
    runCatching { Instant.from(isoParser.parse(iso)) }.getOrNull()

private val localTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("h:mm a", Locale.US)

private val utcTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm", Locale.US).withZone(ZoneOffset.UTC)

private val localDateTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d h:mm a", Locale.US)

/** "7:36 PM" */
fun formatTime(iso: String): String {
    val instant = parseInstant(iso) ?: return iso
    return localTimeFormatter.format(instant.atZone(ZoneId.systemDefault()))
}

/** "00:36" */
fun formatTimeUTC(iso: String): String {
    val instant = parseInstant(iso) ?: return iso
    return utcTimeFormatter.format(instant)
}

/** "Today 7:36 PM" or "Mar 5 7:36 PM" */
fun formatShortDateTime(iso: String): String {
    val instant = parseInstant(iso) ?: return iso
    val zoned = instant.atZone(ZoneId.systemDefault())
    val today = LocalDate.now()
    val date = zoned.toLocalDate()
    val time = localTimeFormatter.format(zoned)
    return when {
        date == today -> "Today $time"
        date == today.minusDays(1) -> "Yesterday $time"
        else -> localDateTimeFormatter.format(zoned)
    }
}

/** Seconds until an ISO timestamp from now */
private fun secondsUntil(iso: String): Double {
    val instant = parseInstant(iso) ?: return 0.0
    return (instant.toEpochMilli() - System.currentTimeMillis()) / 1000.0
}

// =============================================================================
// MARK: - Tracker Response (/satellites/swaths)
// =============================================================================

@Serializable
data class TrackerResponse(
    @SerialName("generated_at") val generatedAt: String,
    val satellites: List<SatelliteTrack>
)

// =============================================================================
// MARK: - Satellite Track
// =============================================================================

@Serializable
data class SatelliteTrack(
    @SerialName("satellite_id") val satelliteId: String,
    val instrument: String,
    val name: String,
    @SerialName("dataset_type") val datasetType: SatelliteDatasetType,
    val direction: OrbitDirection,
    val current: SatellitePosition,
    val trail: List<TrailSegment>
) {
    val id: String get() = satelliteId

    /** Center coordinate from bbox [minLon, minLat, maxLon, maxLat] */
    val center: Pair<Double, Double>
        get() {
            val bbox = current.bbox
            if (bbox.size < 4) return 0.0 to 0.0
            return (bbox[1] + bbox[3]) / 2 to (bbox[0] + bbox[2]) / 2
        }

    val directionSymbol: String
        get() = when (direction) {
            OrbitDirection.ASCENDING -> "\u2191"
            OrbitDirection.DESCENDING -> "\u2193"
            OrbitDirection.UNKNOWN -> "\u2013"
        }
}

// =============================================================================
// MARK: - Satellite Position (Current)
// =============================================================================

@Serializable
data class SatellitePosition(
    @SerialName("granule_id") val granuleId: String,
    @SerialName("time_start") val timeStart: String,
    @SerialName("time_end") val timeEnd: String,
    @SerialName("age_minutes") val ageMinutes: Int,
    @SerialName("day_night") val dayNight: DayNight? = null,
    val geometry: GeoJSONPolygon,
    val bbox: List<Double>
) {
    /** Age as short string (e.g., "2h", "45m") */
    val ageShort: String
        get() = if (ageMinutes < 60) "${ageMinutes}m" else "${ageMinutes / 60}h"

    /** Local time display (e.g., "7:36 PM") */
    val timeLocal: String get() = formatTime(timeStart)

    /** UTC time display (e.g., "00:36Z") */
    val timeUTC: String get() = formatTimeUTC(timeStart) + "Z"

    /** Short date/time (e.g., "Today 7:36 PM") */
    val shortDateTime: String get() = formatShortDateTime(timeStart)

    /** Whether daytime pass */
    val isDaytime: Boolean get() = dayNight == DayNight.DAY
}

// =============================================================================
// MARK: - Trail Segment
// =============================================================================

@Serializable
data class TrailSegment(
    @SerialName("granule_id") val granuleId: String,
    @SerialName("time_start") val timeStart: String,
    val geometry: GeoJSONPolygon
) {
    val id: String get() = granuleId

    /** Local time display (e.g., "7:36 PM") */
    val timeLocal: String get() = formatTime(timeStart)
}

// =============================================================================
// MARK: - Shared Types
// =============================================================================

@Serializable
enum class SatelliteDatasetType {
    @SerialName("chlorophyll") CHLOROPHYLL,
    @SerialName("sst") SST,
    @SerialName("altimetry") ALTIMETRY;

    val label: String
        get() = when (this) {
            CHLOROPHYLL -> "Chlorophyll"
            SST -> "SST"
            ALTIMETRY -> "Altimetry"
        }

    val shortLabel: String
        get() = when (this) {
            CHLOROPHYLL -> "CHL"
            SST -> "SST"
            ALTIMETRY -> "ALT"
        }

    /** Material icon name */
    val iconName: String
        get() = when (this) {
            CHLOROPHYLL -> "eco"
            SST -> "thermostat"
            ALTIMETRY -> "waves"
        }
}

@Serializable
enum class OrbitDirection {
    @SerialName("ascending") ASCENDING,
    @SerialName("descending") DESCENDING,
    @SerialName("unknown") UNKNOWN
}

@Serializable
enum class DayNight {
    @SerialName("DAY") DAY,
    @SerialName("NIGHT") NIGHT,
    @SerialName("BOTH") BOTH
}

@Serializable
data class GeoJSONPolygon(
    val type: String,
    val coordinates: List<List<List<Double>>>
) {
    /** Center lat/lon from polygon ring bounds */
    val center: Pair<Double, Double>
        get() {
            val ring = coordinates.firstOrNull() ?: return 0.0 to 0.0
            if (ring.isEmpty()) return 0.0 to 0.0
            val lons = ring.map { it[0] }
            val lats = ring.map { it[1] }
            return (lats.min() + lats.max()) / 2 to (lons.min() + lons.max()) / 2
        }
}

// =============================================================================
// MARK: - Coverage Response (/satellites/coverage?region_id=xxx)
// =============================================================================

@Serializable
data class CoverageResponse(
    @SerialName("generated_at") val generatedAt: String,
    @SerialName("region_id") val regionId: String,
    val bbox: List<Double>,
    val summary: CoverageSummary,
    val passes: List<RegionalPass>
)

@Serializable
data class CoverageSummary(
    @SerialName("latest_satellite") val latestSatellite: String? = null,
    @SerialName("latest_age_hours") val latestAgeHours: Double? = null
) {
    val latestAgeDisplay: String?
        get() {
            val hours = latestAgeHours ?: return null
            return if (hours < 1) {
                "${(hours * 60).toInt()}m ago"
            } else {
                String.format(Locale.US, "%.1fh ago", hours)
            }
        }
}

// =============================================================================
// MARK: - Pass Status / Skip Reason
// =============================================================================

@Serializable
enum class PassStatus {
    @SerialName("success") SUCCESS,
    @SerialName("running") RUNNING,
    @SerialName("skipped") SKIPPED,
    @SerialName("unavailable") UNAVAILABLE
}

@Serializable
enum class SkipReason {
    @SerialName("low_coverage") LOW_COVERAGE,
    @SerialName("insufficient_swath_data") INSUFFICIENT_SWATH_DATA,
    @SerialName("nighttime") NIGHTTIME,
    @SerialName("no_overlap") NO_OVERLAP,
    @SerialName("missing_variables") MISSING_VARIABLES,
    @SerialName("no_data_available") NO_DATA_AVAILABLE;

    val label: String
        get() = when (this) {
            LOW_COVERAGE -> "Low coverage"
            INSUFFICIENT_SWATH_DATA -> "No data"
            NIGHTTIME -> "Night pass"
            NO_OVERLAP -> "Outside region"
            MISSING_VARIABLES -> "Missing data"
            NO_DATA_AVAILABLE -> "No data"
        }
}

// =============================================================================
// MARK: - Regional Pass
// =============================================================================

@Serializable
data class RegionalPass(
    // Identity
    val satellite: String,
    val instrument: String,
    @SerialName("dataset_type") val datasetType: SatelliteDatasetType,

    // Timing
    @SerialName("captured_at") val capturedAt: String,
    @SerialName("ready_at") val readyAt: String? = null,
    @SerialName("delay_hours") val delayHours: Double? = null,
    @SerialName("age_hours") val ageHours: Double,

    // Processing
    val status: PassStatus? = null,
    @SerialName("coverage_pct") val coveragePct: Double? = null,
    @SerialName("skip_reason") val skipReason: SkipReason? = null,

    // Conditions
    @SerialName("day_night") val dayNight: DayNight? = null,

    // Geography
    val geometry: GeoJSONPolygon,
    val bbox: List<Double>,

    // Links
    @SerialName("entry_id") val entryId: String? = null,
    @SerialName("preview_url") val previewUrl: String? = null
) {
    val id: String get() = "$satellite-$capturedAt"

    val name: String get() = satellite

    /** Center coordinate from bbox [minLon, minLat, maxLon, maxLat] */
    val center: Pair<Double, Double>
        get() {
            if (bbox.size < 4) return 0.0 to 0.0
            return (bbox[1] + bbox[3]) / 2 to (bbox[0] + bbox[2]) / 2
        }

    /** Local time display (e.g., "7:36 PM") */
    val timeLocal: String get() = formatTime(capturedAt)

    /** Short date/time (e.g., "Today 7:36 PM") */
    val shortDateTime: String get() = formatShortDateTime(capturedAt)

    /** Coverage percentage display */
    val coverageDisplay: String?
        get() = coveragePct?.let { "${it.toInt()}%" }
}

// =============================================================================
// MARK: - Satellite Coverage (/region/{id}/satellite-coverage)
// =============================================================================

@Serializable
data class SatelliteCoverage(
    val freshness: DataFreshness,
    @SerialName("next_usable_pass") val nextUsablePass: NextPass? = null,
    @SerialName("upcoming_passes") val upcomingPasses: List<NextPass>
) {
    val id: String get() = nextUsablePass?.id ?: "no-upcoming-pass"
}

@Serializable
data class NextPass(
    val id: String,
    val satellite: String,
    @SerialName("dataset_type") val datasetType: String,
    @SerialName("estimated_at") val estimatedAt: String,
    @SerialName("is_usable") val isUsable: Boolean
) {
    /** Countdown display (e.g., "2h 30m", "45m", "now") */
    val countdownDisplay: String
        get() {
            val seconds = secondsUntil(estimatedAt)
            if (seconds <= 0) return "now"
            val hours = (seconds / 3600).toInt()
            val minutes = ((seconds % 3600) / 60).toInt()
            return when {
                hours == 0 -> "${minutes}m"
                minutes == 0 -> "${hours}h"
                else -> "${hours}h ${minutes}m"
            }
        }

    /** Formatted estimated time (e.g., "3:45 PM") */
    val estimatedTimeLocal: String get() = formatTime(estimatedAt)
}

@Serializable
enum class DataFreshness {
    @SerialName("current") CURRENT,
    @SerialName("recent") RECENT,
    @SerialName("stale") STALE,
    @SerialName("unknown") UNKNOWN
}
