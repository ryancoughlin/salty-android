# Satellite Tracker Mode — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Port the iOS satellite tracker feature to Android with exact parity — global tracker (where are satellites now?) and regional coverage (what data is over my water?).

**Architecture:** ViewModel-owned state with a dedicated `SatelliteService` for API calls. `SatelliteTrackingMode` holds mode/selection state as a composable state holder. Map layers render via Mapbox style API through effect composables. UI is a full-screen overlay on top of the map.

**Tech Stack:** Ktor (HTTP), kotlinx.serialization (JSON), Jetpack Compose (UI), Mapbox Maps SDK (layers), Coroutines (async)

---

## File Map

| # | File | Purpose |
|---|------|---------|
| 1 | `data/satellite/SatelliteModels.kt` | All data classes: TrackerResponse, SatelliteTrack, RegionalPass, SatelliteCoverage, enums |
| 2 | `data/satellite/SatelliteService.kt` | API calls: /satellites/swaths, /satellites/coverage, /region/{id}/satellite-coverage |
| 3 | `viewmodel/SatelliteTrackingMode.kt` | Mode state holder: isActive, mode, selections, filters |
| 4 | `viewmodel/SatelliteStore.kt` | Data state holder: tracks, passes, predictions, loading/error |
| 5 | `ui/satellite/SatelliteModeView.kt` | Main overlay: top bar + mode toggle + bottom panel |
| 6 | `ui/satellite/TrackerPanel.kt` | Tracker mode: swipeable satellite cards + page dots |
| 7 | `ui/satellite/CoveragePanel.kt` | Coverage mode: pass list + night toggle + yesterday header |
| 8 | `ui/satellite/PassPredictionPanel.kt` | NextPassRow + PassPredictionSheet (bottom sheet) |
| 9 | `ui/satellite/DayNightBadge.kt` | Day/Night badge composable |
| 10 | `ui/map/satellite/SatelliteTrackLayer.kt` | Tracker map layers: polygons, trails, labels, glow |
| 11 | `ui/map/satellite/CoveragePassLayer.kt` | Coverage map layers: selected polygon + pin annotations |
| 12 | `ui/map/satellite/SatelliteLayers.kt` | Router: switches between tracker/coverage layers |
| 13 | `viewmodel/AppViewModel.kt` | **Modify**: Add SatelliteTrackingMode + SatelliteStore ownership |
| 14 | `ui/screen/MapScreen.kt` | **Modify**: Wire satellite overlay + map layers |

**iOS source directory:** `/Users/ryan/Developer/salty-ios/SaltyOffshore/Features/SatelliteTracking/`

---

## Task 1: Data Models

**Files:**
- Create: `app/src/main/java/com/example/saltyoffshore/data/satellite/SatelliteModels.kt`

**iOS source:** `Models/SatelliteTrack.swift`, `Models/RegionalPass.swift`, `Models/SatelliteCoverage.swift`

**Step 1: Create the models file**

All models in one file — matches iOS's three model files but Kotlin convention favors grouping related types.

```kotlin
package com.example.saltyoffshore.data.satellite

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// ============================================================
// TRACKER RESPONSE (/satellites/swaths)
// ============================================================

@Serializable
data class TrackerResponse(
    @SerialName("generated_at") val generatedAt: String,
    val satellites: List<SatelliteTrack>
)

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

    val center: Pair<Double, Double> get() {
        val bbox = current.bbox
        if (bbox.size < 4) return 0.0 to 0.0
        return (bbox[1] + bbox[3]) / 2 to (bbox[0] + bbox[2]) / 2
    }

    val directionSymbol: String get() = when (direction) {
        OrbitDirection.ASCENDING -> "↑"
        OrbitDirection.DESCENDING -> "↓"
        OrbitDirection.UNKNOWN -> "–"
    }
}

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
    val ageShort: String get() = if (ageMinutes < 60) "${ageMinutes}m" else "${ageMinutes / 60}h"

    val timeLocal: String get() = formatTime(timeStart)

    val timeUTC: String get() = formatTimeUTC(timeStart) + "Z"

    val shortDateTime: String get() = formatShortDateTime(timeStart)

    val isDaytime: Boolean get() = dayNight == DayNight.DAY
}

@Serializable
data class TrailSegment(
    @SerialName("granule_id") val granuleId: String,
    @SerialName("time_start") val timeStart: String,
    val geometry: GeoJSONPolygon
) {
    val id: String get() = granuleId
    val timeLocal: String get() = formatTime(timeStart)
}

// ============================================================
// COVERAGE RESPONSE (/satellites/coverage?region_id=xxx)
// ============================================================

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
    val latestAgeDisplay: String? get() {
        val hours = latestAgeHours ?: return null
        return if (hours < 1) "${(hours * 60).toInt()}m ago"
        else String.format("%.1fh ago", hours)
    }
}

@Serializable
data class RegionalPass(
    val satellite: String,
    val instrument: String,
    @SerialName("dataset_type") val datasetType: SatelliteDatasetType,
    @SerialName("captured_at") val capturedAt: String,
    @SerialName("ready_at") val readyAt: String? = null,
    @SerialName("delay_hours") val delayHours: Double? = null,
    @SerialName("age_hours") val ageHours: Double,
    val status: PassStatus? = null,
    @SerialName("coverage_pct") val coveragePct: Double? = null,
    @SerialName("skip_reason") val skipReason: SkipReason? = null,
    @SerialName("day_night") val dayNight: DayNight? = null,
    val geometry: GeoJSONPolygon,
    val bbox: List<Double>,
    @SerialName("entry_id") val entryId: String? = null,
    @SerialName("preview_url") val previewUrl: String? = null
) {
    val id: String get() = "$satellite-$capturedAt"
    val name: String get() = satellite

    val center: Pair<Double, Double> get() {
        if (bbox.size < 4) return 0.0 to 0.0
        return (bbox[1] + bbox[3]) / 2 to (bbox[0] + bbox[2]) / 2
    }

    val timeLocal: String get() = formatTime(capturedAt)

    val shortDateTime: String get() = formatShortDateTime(capturedAt)

    val coverageDisplay: String? get() {
        val pct = coveragePct ?: return null
        return "${pct.toInt()}%"
    }
}

// ============================================================
// SATELLITE COVERAGE (/region/{id}/satellite-coverage)
// ============================================================

@Serializable
data class SatelliteCoverage(
    val freshness: DataFreshness,
    @SerialName("next_usable_pass") val nextUsablePass: NextPass? = null,
    @SerialName("upcoming_passes") val upcomingPasses: List<NextPass>
)

@Serializable
data class NextPass(
    val id: String,
    val satellite: String,
    @SerialName("dataset_type") val datasetType: String,
    @SerialName("estimated_at") val estimatedAt: String,
    @SerialName("is_usable") val isUsable: Boolean
) {
    val countdownDisplay: String get() {
        val estimated = Instant.parse(estimatedAt)
        val seconds = estimated.epochSecond - Instant.now().epochSecond
        if (seconds <= 0) return "now"
        val hours = (seconds / 3600).toInt()
        val minutes = ((seconds % 3600) / 60).toInt()
        return when {
            hours == 0 -> "${minutes}m"
            minutes == 0 -> "${hours}h"
            else -> "${hours}h ${minutes}m"
        }
    }

    val estimatedTimeLocal: String get() = formatTime(estimatedAt)
}

// ============================================================
// SHARED ENUMS
// ============================================================

@Serializable
enum class SatelliteDatasetType {
    @SerialName("chlorophyll") CHLOROPHYLL,
    @SerialName("sst") SST,
    @SerialName("altimetry") ALTIMETRY;

    val label: String get() = when (this) {
        CHLOROPHYLL -> "Chlorophyll"
        SST -> "SST"
        ALTIMETRY -> "Altimetry"
    }

    val shortLabel: String get() = when (this) {
        CHLOROPHYLL -> "CHL"
        SST -> "SST"
        ALTIMETRY -> "ALT"
    }

    /** Material Icon name (Android equivalent of SF Symbols) */
    val iconName: String get() = when (this) {
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
    DAY, NIGHT, BOTH
}

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

    val label: String get() = when (this) {
        LOW_COVERAGE -> "Low coverage"
        INSUFFICIENT_SWATH_DATA -> "No data"
        NIGHTTIME -> "Night pass"
        NO_OVERLAP -> "Outside region"
        MISSING_VARIABLES -> "Missing data"
        NO_DATA_AVAILABLE -> "No data"
    }
}

@Serializable
enum class DataFreshness {
    @SerialName("current") CURRENT,
    @SerialName("recent") RECENT,
    @SerialName("stale") STALE,
    @SerialName("unknown") UNKNOWN
}

@Serializable
data class GeoJSONPolygon(
    val type: String,
    val coordinates: List<List<List<Double>>>
) {
    val center: Pair<Double, Double> get() {
        val ring = coordinates.firstOrNull() ?: return 0.0 to 0.0
        if (ring.isEmpty()) return 0.0 to 0.0
        val lons = ring.map { it[0] }
        val lats = ring.map { it[1] }
        return (lats.min() + lats.max()) / 2 to (lons.min() + lons.max()) / 2
    }
}

// ============================================================
// DATE FORMATTING HELPERS
// ============================================================

private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
private val utcTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val shortDateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")

internal fun formatTime(isoString: String): String = try {
    val instant = Instant.parse(isoString)
    instant.atZone(ZoneId.systemDefault()).format(timeFormatter)
} catch (_: Exception) { isoString }

internal fun formatTimeUTC(isoString: String): String = try {
    val instant = Instant.parse(isoString)
    instant.atZone(ZoneId.of("UTC")).format(utcTimeFormatter)
} catch (_: Exception) { isoString }

internal fun formatShortDateTime(isoString: String): String = try {
    val instant = Instant.parse(isoString)
    val zoned = instant.atZone(ZoneId.systemDefault())
    val now = java.time.LocalDate.now()
    val date = zoned.toLocalDate()
    val prefix = when {
        date == now -> "Today"
        date == now.minusDays(1) -> "Yesterday"
        else -> date.format(DateTimeFormatter.ofPattern("MMM d"))
    }
    "$prefix ${zoned.format(timeFormatter)}"
} catch (_: Exception) { isoString }
```

**Step 2: Commit**

```
feat(satellite): add satellite tracker data models
```

---

## Task 2: Satellite Service

**Files:**
- Create: `app/src/main/java/com/example/saltyoffshore/data/satellite/SatelliteService.kt`

**iOS source:** `Services/SatelliteService.swift`

**Step 1: Create the service**

```kotlin
package com.example.saltyoffshore.data.satellite

import android.util.Log
import com.example.saltyoffshore.config.AppConstants
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.Json

/**
 * Fetches satellite data from API.
 * Three endpoints: /swaths (global tracker), /coverage (regional), /satellite-coverage (predictions)
 */
object SatelliteService {
    private const val TAG = "SatelliteService"
    private val baseURL = "${AppConstants.apiBaseURL}/satellites"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /** Fetch global satellite tracks (where are satellites now?) */
    suspend fun fetchTracks(client: HttpClient): TrackerResponse {
        Log.d(TAG, "Fetching satellite tracks (global)")
        val response: HttpResponse = client.get("$baseURL/swaths") {
            header("Cache-Control", "no-cache, no-store")
            header("Pragma", "no-cache")
        }
        val body = response.body<String>()
        return json.decodeFromString<TrackerResponse>(body)
    }

    /** Fetch regional coverage (what data is over my water?) */
    suspend fun fetchCoverage(client: HttpClient, regionId: String): CoverageResponse {
        Log.d(TAG, "Fetching coverage for region: $regionId")
        val response: HttpResponse = client.get("$baseURL/coverage") {
            parameter("region_id", regionId)
            header("Cache-Control", "no-cache, no-store")
            header("Pragma", "no-cache")
        }
        val body = response.body<String>()
        return json.decodeFromString<CoverageResponse>(body)
    }

    /** Fetch satellite coverage predictions for a region */
    suspend fun fetchSatelliteCoverage(client: HttpClient, regionId: String): SatelliteCoverage {
        Log.d(TAG, "Fetching satellite coverage predictions for region: $regionId")
        val response: HttpResponse = client.get("${AppConstants.apiBaseURL}/region/$regionId/satellite-coverage") {
            header("Cache-Control", "no-cache, no-store")
            header("Pragma", "no-cache")
        }
        val body = response.body<String>()
        return json.decodeFromString<SatelliteCoverage>(body)
    }
}
```

**Step 2: Commit**

```
feat(satellite): add satellite service for API calls
```

---

## Task 3: State Holders (SatelliteTrackingMode + SatelliteStore)

**Files:**
- Create: `app/src/main/java/com/example/saltyoffshore/viewmodel/SatelliteTrackingMode.kt`
- Create: `app/src/main/java/com/example/saltyoffshore/viewmodel/SatelliteStore.kt`

**iOS source:** `SatelliteTrackingMode.swift`, `SatelliteStore.swift`

**Step 1: Create SatelliteTrackingMode**

```kotlin
package com.example.saltyoffshore.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Tracker vs Coverage mode */
enum class SatelliteMode(val label: String) {
    TRACKER("Tracker"),
    COVERAGE("My Region")
}

/**
 * Mode state for satellite tracking.
 * Manages lifecycle (active/inactive) and UI state (selections, filters).
 * Data lives in SatelliteStore — this coordinates mode and selection.
 */
class SatelliteTrackingMode {
    var isActive by mutableStateOf(false)
        private set

    var mode by mutableStateOf(SatelliteMode.TRACKER)
        private set

    var regionId by mutableStateOf<String?>(null)

    var selectedTrackId by mutableStateOf<String?>(null)
    var selectedPassId by mutableStateOf<String?>(null)
    var showNightPasses by mutableStateOf(false)

    /** Provider for current region ID — avoids coupling to AppViewModel */
    var selectedRegionIdProvider: () -> String? = { null }

    fun enter() {
        isActive = true
        regionId = selectedRegionIdProvider()
    }

    fun exit() {
        isActive = false
        mode = SatelliteMode.TRACKER
        selectedTrackId = null
        selectedPassId = null
    }

    fun setMode(newMode: SatelliteMode) {
        if (newMode == mode || !isActive) return
        mode = newMode
    }

    fun selectTrack(id: String?) { selectedTrackId = id }
    fun selectPass(id: String?) { selectedPassId = id }
}
```

**Step 2: Create SatelliteStore**

```kotlin
package com.example.saltyoffshore.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.saltyoffshore.data.satellite.*
import io.ktor.client.HttpClient
import kotlinx.coroutines.*

/**
 * Unified store for all satellite data.
 * Store Pattern: View → Store → Service
 */
class SatelliteStore(private val client: HttpClient) {
    private companion object { const val TAG = "SatelliteStore" }

    var tracks by mutableStateOf<List<SatelliteTrack>>(emptyList())
        private set

    var passes by mutableStateOf<List<RegionalPass>>(emptyList())
        private set

    var summary by mutableStateOf<CoverageSummary?>(null)
        private set

    var predictions by mutableStateOf<SatelliteCoverage?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    /** Load global satellite tracks */
    suspend fun loadTracks() {
        isLoading = true
        error = null
        try {
            val response = SatelliteService.fetchTracks(client)
            tracks = response.satellites
            Log.d(TAG, "Loaded ${tracks.size} satellite tracks")
        } catch (e: Exception) {
            error = e.message
            tracks = emptyList()
            Log.e(TAG, "Failed to load tracks", e)
        } finally {
            isLoading = false
        }
    }

    /** Load regional coverage data (passes + predictions in parallel) */
    suspend fun loadCoverage(regionId: String) = coroutineScope {
        isLoading = true
        error = null
        try {
            val passesDeferred = async { SatelliteService.fetchCoverage(client, regionId) }
            val predictionsDeferred = async {
                try { SatelliteService.fetchSatelliteCoverage(client, regionId) }
                catch (e: Exception) {
                    Log.w(TAG, "Failed to load predictions (non-fatal)", e)
                    null
                }
            }

            val coverageResponse = passesDeferred.await()
            summary = coverageResponse.summary
            passes = coverageResponse.passes
            Log.d(TAG, "Loaded ${passes.size} regional passes")

            predictions = predictionsDeferred.await()
        } catch (e: Exception) {
            error = e.message
            summary = null
            passes = emptyList()
            Log.e(TAG, "Failed to load coverage", e)
        } finally {
            isLoading = false
        }
    }

    fun clear() {
        tracks = emptyList()
        passes = emptyList()
        summary = null
        predictions = null
        error = null
    }
}
```

**Step 3: Commit**

```
feat(satellite): add SatelliteTrackingMode and SatelliteStore state holders
```

---

## Task 4: DayNightBadge Composable

**Files:**
- Create: `app/src/main/java/com/example/saltyoffshore/ui/satellite/DayNightBadge.kt`

**iOS source:** `Views/Components/DayNightBadge.swift`

**Step 1: Create the badge**

```kotlin
package com.example.saltyoffshore.ui.satellite

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.data.satellite.DayNight

enum class BadgeSize(val frame: Dp, val iconSize: Dp, val cornerRadius: Dp) {
    COMPACT(20.dp, 10.dp, 4.dp),
    REGULAR(28.dp, 14.dp, 6.dp)
}

@Composable
fun DayNightBadge(
    dayNight: DayNight,
    size: BadgeSize = BadgeSize.REGULAR
) {
    val (icon, bgColor, fgColor) = when (dayNight) {
        DayNight.DAY -> Triple(Icons.Filled.WbSunny, Color(0xFFFFEB3B), Color.Black)
        DayNight.NIGHT -> Triple(Icons.Filled.DarkMode, Color(0xFF3F51B5), Color.White)
        DayNight.BOTH -> Triple(Icons.Filled.Contrast, Color(0xFF9C27B0), Color.White)
    }

    Box(
        modifier = Modifier
            .size(size.frame)
            .clip(RoundedCornerShape(size.cornerRadius))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = dayNight.name,
            tint = fgColor,
            modifier = Modifier.size(size.iconSize)
        )
    }
}
```

**Step 2: Commit**

```
feat(satellite): add DayNightBadge composable
```

---

## Task 5: TrackerPanel (Satellite Cards Carousel)

**Files:**
- Create: `app/src/main/java/com/example/saltyoffshore/ui/satellite/TrackerPanel.kt`

**iOS source:** `Views/TrackerPanel.swift`

**Step 1: Create TrackerPanel with card carousel**

Port the TabView carousel → HorizontalPager. Includes TrackerCard, StatBlock, and PageDots. Match iOS card layout: name, instrument, dataset type, time, age, direction, day/night badge. Selected card is white-on-black, unselected is black-on-white (inverted).

Key behaviors:
- Swiping pages syncs selection with `onSelect` callback
- External selection changes (e.g., map tap) sync pager position
- Page dots: selected = 26dp wide capsule, unselected = 8dp circle

**Step 2: Commit**

```
feat(satellite): add TrackerPanel with satellite card carousel
```

---

## Task 6: CoveragePanel (Pass List)

**Files:**
- Create: `app/src/main/java/com/example/saltyoffshore/ui/satellite/CoveragePanel.kt`

**iOS source:** `Views/CoveragePanel.swift`

**Step 1: Create CoveragePanel with pass rows**

Port the scrollable pass list. Includes:
- Header: "Recent Passes" + "24h" + night pass toggle (moon icon)
- PassRow: status dot (green/spinner/slash), satellite name, dataset icon, time, trailing content (coverage %, skip reason, day/night badge)
- YesterdayHeader: divider when transitioning from today to yesterday
- Footer: "Not all satellite passes have usable data."
- Auto-scroll to selected pass via LazyColumn + `animateScrollToItem`

PassRow selection:
- `.success` and `.running` are selectable
- `.skipped`, `.unavailable`, `null` are disabled (dimmed, strikethrough)
- Selected row: white background, black text

**Step 2: Commit**

```
feat(satellite): add CoveragePanel with pass list
```

---

## Task 7: PassPredictionPanel (NextPassRow + Sheet)

**Files:**
- Create: `app/src/main/java/com/example/saltyoffshore/ui/satellite/PassPredictionPanel.kt`

**iOS source:** `Views/PassPredictionPanel.swift`

**Step 1: Create NextPassRow + PassPredictionSheet**

NextPassRow: inline compact row showing freshness dot + satellite name + countdown + estimated time + chevron. Tapping opens sheet.

PassPredictionSheet (ModalBottomSheet):
- Header with title "Pass Predictions"
- Next usable pass highlighted (white bg, black text)
- Upcoming passes list: usability dot (green/slash), satellite, countdown, time, dataset type badge
- Footer disclaimer

DataFreshness → color mapping:
- CURRENT → green
- RECENT → yellow
- STALE → orange
- UNKNOWN → white 30% opacity

**Step 2: Commit**

```
feat(satellite): add NextPassRow and PassPredictionSheet
```

---

## Task 8: SatelliteModeView (Main Overlay)

**Files:**
- Create: `app/src/main/java/com/example/saltyoffshore/ui/satellite/SatelliteModeView.kt`

**iOS source:** `Views/SatelliteModeView.swift`

**Step 1: Create main satellite overlay**

Full-screen overlay composable:
- Top bar: close button (leading) + mode toggle pill (center) + loading indicator (trailing)
- Mode toggle: capsule with "Tracker" / "My Region" segments, selected = white bg + black text
- Spacer pushes bottom panel to bottom
- Bottom panel switches on mode:
  - TRACKER → TrackerPanel (if tracks not empty)
  - COVERAGE → NextPassRow (if predictions exist) + CoveragePanel (if passes not empty)
- Data loading via `LaunchedEffect(mode)` and `LaunchedEffect(regionId)`
- Auto-select first track/pass on load
- `DisposableEffect` calls `store.clear()` on removal

**Step 2: Commit**

```
feat(satellite): add SatelliteModeView overlay
```

---

## Task 9: Map Layers — SatelliteTrackLayer

**Files:**
- Create: `app/src/main/java/com/example/saltyoffshore/ui/map/satellite/SatelliteTrackLayer.kt`

**iOS source:** `Layers/SatelliteTrackLayer.swift`

**Step 1: Create tracker mode map layers**

Effect composable that adds/removes Mapbox sources and layers:

1. **Unselected satellites**: White outline (1.5px, 0.5 opacity) for all non-selected tracks
2. **Selected trail**: Fading fill (0.5→0.15 opacity by index) + fading outline (0.9→0.3)
3. **Selected current**: White fill (0.65 opacity) + yellow glow (12px blur) + white outline (3px)
4. **Labels**: Selected satellite name + timestamp at center (text halo)
5. **Trail labels**: Timestamps for trail segments

Uses `MapEffect` or `MapboxMapComposable` + `Style.addSource/addLayer` APIs. GeoJSON sources built from polygon coordinates → Mapbox Feature/FeatureCollection.

Layer IDs match iOS: `sat-unselected-source`, `sat-selected-fill`, `sat-trail-fill`, etc.

**Step 2: Commit**

```
feat(satellite): add SatelliteTrackLayer map rendering
```

---

## Task 10: Map Layers — CoveragePassLayer

**Files:**
- Create: `app/src/main/java/com/example/saltyoffshore/ui/map/satellite/CoveragePassLayer.kt`

**iOS source:** `Layers/CoveragePassLayer.swift`

**Step 1: Create coverage mode map layers**

Effect composable for "Pins + Focus" pattern:

1. **Selected pass**: Full polygon (white fill 0.45, white outline 2.5px) + center label
2. **Unselected passes**: Point annotations at pass centers with time labels
   - Pin icon varies by status: green (success), outline (running), slash (unavailable)
   - Tappable → calls `onPassTap` callback

Uses same source/layer pattern as SatelliteTrackLayer. Pin icons can use Mapbox's built-in markers or custom drawables.

**Step 2: Commit**

```
feat(satellite): add CoveragePassLayer map rendering
```

---

## Task 11: SatelliteLayers Router

**Files:**
- Create: `app/src/main/java/com/example/saltyoffshore/ui/map/satellite/SatelliteLayers.kt`

**iOS source:** `Layers/SatelliteLayers.swift`

**Step 1: Create layer router effect**

Single composable that conditionally renders either `SatelliteTrackLayer` or `CoveragePassLayer` based on mode and active state. Also handles cleanup when satellite mode deactivates (remove all satellite sources/layers).

```kotlin
@Composable
fun SatelliteLayersEffect(
    mapView: MapView,
    trackingMode: SatelliteTrackingMode,
    store: SatelliteStore,
) {
    if (!trackingMode.isActive) return

    when (trackingMode.mode) {
        SatelliteMode.TRACKER -> SatelliteTrackLayerEffect(
            mapView = mapView,
            tracks = store.tracks,
            selectedId = trackingMode.selectedTrackId
        )
        SatelliteMode.COVERAGE -> CoveragePassLayerEffect(
            mapView = mapView,
            passes = store.passes,
            selectedId = trackingMode.selectedPassId,
            onPassTap = { trackingMode.selectPass(it) }
        )
    }
}
```

**Step 2: Commit**

```
feat(satellite): add SatelliteLayers router composable
```

---

## Task 12: Wire into AppViewModel + MapScreen

**Files:**
- Modify: `app/src/main/java/com/example/saltyoffshore/viewmodel/AppViewModel.kt`
- Modify: `app/src/main/java/com/example/saltyoffshore/ui/screen/MapScreen.kt`

**Step 1: Add satellite state to AppViewModel**

```kotlin
// In AppViewModel, add:
val satelliteTrackingMode = SatelliteTrackingMode()
val satelliteStore = SatelliteStore(client)

// In init block, wire region provider:
satelliteTrackingMode.selectedRegionIdProvider = { selectedRegion?.id }
```

**Step 2: Add satellite overlay to MapScreen**

In MapScreen composable:
1. Add `SatelliteLayersEffect` inside the MapboxMap (after other layer effects)
2. Add `SatelliteModeView` overlay (conditionally shown when `satelliteTrackingMode.isActive`)
3. Add a satellite button to the toolbar/controls that calls `satelliteTrackingMode.enter()`

**Step 3: Commit**

```
feat(satellite): wire satellite tracker into AppViewModel and MapScreen
```

---

## Task 13: Verify End-to-End

**Step 1: Build**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL

**Step 2: Verify file count**

12 new files created, 2 files modified. All in correct packages.

**Step 3: Commit final**

If any fixes needed during build, commit them:

```
fix(satellite): resolve build issues from satellite tracker integration
```

---

## Dependency Notes

- **No new libraries needed** — Ktor, kotlinx.serialization, Mapbox SDK, Compose all already in the project
- **Material Icons** — `WbSunny`, `DarkMode`, `Contrast` are in `material-icons-extended` (verify it's in dependencies, add if not)
- **HorizontalPager** — From `accompanist` or `foundation` (check project's compose version)

## Risk Areas

1. **Mapbox layer API** — Android SDK uses imperative `Style.addSource/addLayer`, not declarative like iOS. All map layers need `LaunchedEffect` + cleanup in `DisposableEffect`.
2. **GeoJSON → Mapbox Feature** — Need to convert `GeoJSONPolygon` coordinates to Mapbox's `Polygon.fromLngLats()` format.
3. **HorizontalPager sync** — Bidirectional sync between pager position and selection requires care to avoid loops.
