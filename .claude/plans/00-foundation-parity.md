# Phase 0: Foundation Parity

> Complete the foundation layer to match iOS exactly before building features.

**Source of Truth:** `/Users/ryan/Developer/salty-ios`

---

## Overview

The Android app has basic data models and rendering, but is missing 90% of the preferences system, multi-variable dataset support, and the complete rendering configuration architecture that iOS uses.

**Goal:** Any feature we build after this phase will have the same foundation as iOS.

---

## Task 0.1: Preferences System

**iOS Source:** `SaltyOffshore/Types/Preferences/`

Android currently has 2 DataStore keys. iOS has 30+.

### 0.1.1 Expand AppPreferencesDataStore

**File:** `preferences/AppPreferencesDataStore.kt`

Add these keys with Flow getters and suspend setters:

| Key | Type | Default | iOS Source |
|-----|------|---------|------------|
| `map_theme` | String | `"light"` | AppPreferences.mapTheme |
| `depth_units` | String | `"feet"` | AppPreferences.depthUnits |
| `distance_units` | String | `"miles"` | AppPreferences.distanceUnits |
| `speed_units` | String | `"knots"` | AppPreferences.speedUnits |
| `temperature_units` | String | `"fahrenheit"` | AppPreferences.temperatureUnits |
| `coordinate_system` | String | `"gps"` | AppPreferences.coordinateSystem |
| `gps_format` | String | `"dmm"` | AppPreferences.gpsFormat |
| `selected_region_id` | String? | null | RegionPreferences.selectedRegionId |
| `preferred_region_id` | String? | null | RegionPreferences.preferredRegionId |
| `region_bounds` | String? (JSON) | null | RegionPreferences.regionBounds |
| `selected_loran_region` | String? | null | RegionPreferences.selectedLoranRegion |
| `loran_chain` | String | default | RegionPreferences.loranChain |
| `selected_tournament_id` | String? | null | RegionPreferences.selectedTournamentId |
| `region_last_updated` | String (JSON) | `"{}"` | RegionPreferences.regionLastUpdated |
| `network_download_preference` | String | `"wifi_only"` | CachePreferences.networkDownloadPreference |
| `last_offline_download_date` | Long? | null | CachePreferences.lastOfflineDownloadDate |
| `offline_download_day_range` | Int | 3 | CachePreferences.offlineDownloadDayRange |
| `last_announcement_version` | Int | 0 | TechnicalPreferences.lastAnnouncementVersion |
| `has_seen_overlay_migration` | Boolean | false | TechnicalPreferences.hasSeenOverlayMigrationBanner |
| `enable_crew_map_overlay` | Boolean | false | TechnicalPreferences.enableCrewMapOverlay |
| `enable_dynamic_range` | Boolean | false | TechnicalPreferences.enableDynamicRange |
| `enable_weather_layers` | Boolean | false | TechnicalPreferences.enableWeatherLayers |
| `waypoint_sort_option` | String | `"date_created"` | WaypointPreferences.sortOption |

### 0.1.2 Create Preferences Manager

**New File:** `preferences/AppPreferences.kt`

Singleton that wraps DataStore with in-memory state for Compose observation:

```kotlin
@Singleton
class AppPreferences @Inject constructor(
    private val dataStore: AppPreferencesDataStore
) {
    // Display preferences
    var mapTheme by mutableStateOf(MapTheme.LIGHT)
    var depthUnits by mutableStateOf(DepthUnits.FEET)
    var distanceUnits by mutableStateOf(DistanceUnits.MILES)
    var speedUnits by mutableStateOf(SpeedUnits.KNOTS)
    var temperatureUnits by mutableStateOf(TemperatureUnits.FAHRENHEIT)
    var coordinateSystem by mutableStateOf(CoordinateSystem.GPS)
    var gpsFormat by mutableStateOf(GpsFormat.DMM)

    // Region preferences
    var selectedRegionId: String? by mutableStateOf(null)
    var preferredRegionId: String? by mutableStateOf(null)
    // ... etc

    // Load from DataStore on init
    suspend fun loadFromDisk() { ... }

    // Save immediately on change (didSet equivalent)
    private fun <T> persistOnChange(key: Preferences.Key<T>, value: T) { ... }
}
```

### 0.1.3 Supabase Sync

**File:** `repository/UserPreferencesRepository.kt`

Add methods to match iOS `UserPreferencesViewModel`:

```kotlin
suspend fun syncAppPreferences(userId: String, prefs: AppPreferences): UserPreferences
suspend fun applyRemotePreferencesToLocal(remote: UserPreferences, local: AppPreferences)
suspend fun migrateLocalPreferencesToRemote(userId: String, local: AppPreferences)
```

**Sync Flow:**
1. App launch → `fetchPreferences()` from Supabase
2. If missing → `createInitialPreferences()`
3. Apply remote → `applyRemotePreferencesToLocal()`
4. On settings change → immediate DataStore write + deferred Supabase sync

---

## Task 0.2: DatasetType Completion — COMPLETE

**Status:** All 11 dataset types exist with complete properties.

**Parity fixes applied (2026-03-22):**
- `supportsFronts` → SST only (iOS only has `BreaksConfig` on SST, not SSH/Salinity/MLD)
- `contourColor` → always `Color.BLACK` (iOS defaults all contour colors to black; user-editable)
- Phytoplankton `defaultColorscale` → `BLOOM` (was incorrectly `CHLOROPHYLL`)
- dissolved_oxygen unit → `mg/L` (was `mmol/m³`)
- water_type `valueKey` → `"label"` (was `"water_type"`)

**Files:** `data/DatasetType.kt`, `data/LayerCapabilities.kt`, `data/DatasetDefaults.kt`, `data/DatasetConfiguration.kt`

---

## Task 0.3: Dataset Variable System

**iOS Source:** `SaltyOffshore/Models/DatasetVariable.swift`

Some datasets have multiple variables (e.g., SST has temperature + gradient).

### 0.3.1 Create DatasetVariable

**New File:** `data/DatasetVariable.kt`

```kotlin
data class DatasetVariable(
    val id: String,
    val displayName: String,
    val zarrVariableName: String,
    val rangeKey: String,
    val hasPMTilesData: Boolean,
    val isVisible: Boolean,
    val colorscale: Colorscale? = null,  // Override type's default
    val scaleMode: ScaleMode? = null,     // Override type's default
    val unit: DatasetUnit,
    val decimalPlaces: Int
) {
    val isPrimary: Boolean get() = hasPMTilesData
}
```

### 0.3.2 Add Variables to DatasetType

**File:** `data/DatasetType.kt`

```kotlin
val DatasetType.availableVariables: List<DatasetVariable>
    get() = when (this) {
        SST -> listOf(
            DatasetVariable(
                id = "sst",
                displayName = "Temperature",
                zarrVariableName = "sea_surface_temperature",
                rangeKey = "sea_surface_temperature",
                hasPMTilesData = true,
                isVisible = true,
                unit = DatasetUnit.FAHRENHEIT,
                decimalPlaces = 1
            ),
            DatasetVariable(
                id = "sst_gradient",
                displayName = "Gradient",
                zarrVariableName = "sst_gradient_magnitude",
                rangeKey = "sst_gradient",
                hasPMTilesData = false,
                isVisible = true,
                colorscale = Colorscale.MAGENTA,
                unit = DatasetUnit.CELSIUS_PER_KM,
                decimalPlaces = 2
            )
        )
        CURRENTS -> listOf(
            DatasetVariable(
                id = "speed",
                displayName = "Speed",
                zarrVariableName = "speed",
                rangeKey = "speed",
                hasPMTilesData = true,
                isVisible = true,
                unit = DatasetUnit.KNOTS,
                decimalPlaces = 1
            )
        )
        // ... etc for all 11 types
    }

val DatasetType.primaryVariable: DatasetVariable
    get() = availableVariables.first { it.isPrimary }
```

---

## Task 0.4: Rendering Configuration

**iOS Source:** `SaltyOffshore/Models/RenderingConfig.swift`

### 0.4.1 Create ScaleMode

**New File:** `data/ScaleMode.kt`

```kotlin
enum class ScaleMode {
    LINEAR,      // SST, MLD, Salinity, DO
    LOGARITHMIC, // Chlorophyll, Currents, Phytoplankton
    DIVERGING,   // SSH (zero-centered)
    SQRT         // Currents alternative
}
```

### 0.4.2 Create DomainStrategy

**New File:** `data/DomainStrategy.kt`

```kotlin
sealed class DomainStrategy {
    data class Fixed(val range: ClosedFloatingPointRange<Float>) : DomainStrategy()
    data class PercentileClipped(val minPercentile: Int = 10, val maxPercentile: Int = 75) : DomainStrategy()

    fun aggregateRange(entryMins: List<Float>, entryMaxes: List<Float>): ClosedFloatingPointRange<Float>? {
        return when (this) {
            is Fixed -> range
            is PercentileClipped -> {
                // Compute percentile-based range from entry statistics
                // ...
            }
        }
    }
}
```

### 0.4.3 Create RenderingConfig

**New File:** `data/RenderingConfig.kt`

```kotlin
data class RenderingConfig(
    val scaleMode: ScaleMode,
    val colorscale: Colorscale,
    val snapIncrement: Double? = null,  // nil = no snapping
    val domainStrategy: DomainStrategy = DomainStrategy.PercentileClipped()
)

val DatasetType.renderingConfig: RenderingConfig
    get() = when (this) {
        SST -> RenderingConfig(
            scaleMode = ScaleMode.LINEAR,
            colorscale = Colorscale.SST,
            snapIncrement = 0.1
        )
        CHLOROPHYLL -> RenderingConfig(
            scaleMode = ScaleMode.LOGARITHMIC,
            colorscale = Colorscale.CHLOROPHYLL,
            domainStrategy = DomainStrategy.Fixed(0.01f..8.0f)
        )
        EDDYS -> RenderingConfig(
            scaleMode = ScaleMode.DIVERGING,
            colorscale = Colorscale.RDBU
        )
        CURRENTS -> RenderingConfig(
            scaleMode = ScaleMode.SQRT,
            colorscale = Colorscale.CURRENTS
        )
        // ... etc
    }
```

---

## Task 0.5: Preset System

**iOS Source:** `SaltyOffshore/Types/PresetTypes.swift`

### 0.5.1 Create PresetTypes

**New File:** `data/PresetTypes.kt`

```kotlin
sealed class PresetType {
    data class FixedRange(val min: Double, val max: Double) : PresetType()
    object MicroBreak : PresetType()
    data class CurrentValueRange(val offset: Double) : PresetType()
}

data class DatasetPreset(
    val id: String,
    val label: String,
    val type: PresetType,
    val datasetType: DatasetType
) {
    fun calculateRange(
        currentValue: Double?,
        valueRange: ClosedFloatingPointRange<Double>
    ): ClosedFloatingPointRange<Double>? {
        return when (type) {
            is PresetType.FixedRange -> type.min..type.max
            is PresetType.MicroBreak -> {
                // 1°F range around current value
                currentValue?.let { (it - 0.5)..(it + 0.5) }
            }
            is PresetType.CurrentValueRange -> {
                currentValue?.let { (it - type.offset)..(it + type.offset) }
            }
        }
    }
}

// Static preset configurations per dataset type
object PresetConfiguration {
    val sst = listOf(
        DatasetPreset("warm", "Warm Water (75°F+)", PresetType.FixedRange(75.0, 90.0), DatasetType.SST),
        DatasetPreset("cold", "Cold Water (<65°F)", PresetType.FixedRange(50.0, 65.0), DatasetType.SST),
        DatasetPreset("micro", "Micro Break", PresetType.MicroBreak, DatasetType.SST)
    )
    // ... etc for other dataset types
}
```

---

## Task 0.6: Map Configuration (Share Links)

**iOS Source:** `SaltyOffshore/Types/MapConfiguration.swift`

### 0.6.1 Create MapConfiguration

**New File:** `data/MapConfiguration.kt`

```kotlin
@Serializable
data class MapConfiguration(
    val regionId: String,
    val datasetId: String,
    val timestamp: String,  // ISO 8601
    val entryId: String? = null,
    val primaryConfig: LayerConfig? = null,
    val overlays: List<OverlayConfig>? = null,
    val camera: CameraConfig? = null
) {
    @Serializable
    data class LayerConfig(
        val datasetId: String,
        val colorscaleId: String? = null,
        val customRangeMin: Double? = null,
        val customRangeMax: Double? = null,
        val filterMode: String = "squash",
        val visualEnabled: Boolean = true,
        val visualOpacity: Double = 1.0,
        val contourEnabled: Boolean = false,
        val contourOpacity: Double = 1.0,
        val arrowsEnabled: Boolean? = null,
        val arrowsOpacity: Double? = null,
        val breaksEnabled: Boolean? = null,
        val breaksOpacity: Double? = null,
        val numbersEnabled: Boolean? = null,
        val numbersOpacity: Double? = null,
        val selectedDepth: Int? = null
    )

    @Serializable
    data class OverlayConfig(
        val datasetType: String,
        val config: LayerConfig,
        val entryId: String? = null,
        val depth: Int? = null
    )

    @Serializable
    data class CameraConfig(
        val centerLongitude: Double,
        val centerLatitude: Double,
        val zoom: Double,
        val bearing: Double = 0.0,
        val pitch: Double = 0.0
    )
}
```

---

## Task 0.7: Dataset Field Configuration

**iOS Source:** `DatasetType.fields` property

### 0.7.1 Create DatasetFieldConfig

**New File:** `data/DatasetFieldConfig.kt`

```kotlin
data class BreaksConfig(
    val fieldName: String,
    val bandName: String,
    val colorscale: Colorscale
)

data class DatasetFieldConfig(
    val rangeKey: String,
    val dataField: String,
    val contourLabel: String,
    val unit: DatasetUnit,
    val breaks: BreaksConfig? = null,
    val contourFilterFieldOverride: String? = null
) {
    val contourFilterField: String
        get() = contourFilterFieldOverride ?: dataField

    val supportsBreaks: Boolean
        get() = breaks != null
}

val DatasetType.fields: DatasetFieldConfig
    get() = when (this) {
        SST -> DatasetFieldConfig(
            rangeKey = "sea_surface_temperature",
            dataField = "temperature",
            contourLabel = "temp_label",
            unit = DatasetUnit.FAHRENHEIT,
            breaks = BreaksConfig("sst_gradient_magnitude", "b2", Colorscale.MAGENTA)
        )
        CURRENTS -> DatasetFieldConfig(
            rangeKey = "speed",
            dataField = "speed",
            contourLabel = "speed",
            unit = DatasetUnit.KNOTS
        )
        EDDYS -> DatasetFieldConfig(
            rangeKey = "sea_surface_height",
            dataField = "sea_surface_height",
            contourLabel = "ssh",
            unit = DatasetUnit.CENTIMETERS,
            contourFilterFieldOverride = "ssh"
        )
        // ... etc for all 11 types
    }
```

---

## Verification Checklist

After completing Phase 0, verify:

- [ ] All 22 preference keys persist to DataStore
- [ ] Preferences sync to Supabase on sign-in
- [ ] Remote preferences override local on app launch
- [ ] All 11 DatasetType cases exist with complete properties
- [ ] Each dataset type has: capabilities, defaults, renderingConfig, fields, availableVariables
- [ ] MapConfiguration can serialize/deserialize for share links
- [ ] Preset system can calculate ranges for all dataset types

---

## Files Created/Modified

| Action | File |
|--------|------|
| Modify | `preferences/AppPreferencesDataStore.kt` |
| Create | `preferences/AppPreferences.kt` |
| Modify | `repository/UserPreferencesRepository.kt` |
| Modify | `data/DatasetType.kt` |
| Create | `data/LayerCapabilities.kt` |
| Create | `data/DatasetDefaults.kt` |
| Create | `data/DatasetVariable.kt` |
| Create | `data/ScaleMode.kt` |
| Create | `data/DomainStrategy.kt` |
| Create | `data/RenderingConfig.kt` |
| Create | `data/PresetTypes.kt` |
| Create | `data/MapConfiguration.kt` |
| Create | `data/DatasetFieldConfig.kt` |

---

## Next Phase

After foundation parity, proceed to `01-map-layers.md` for BreaksLayer + NumbersLayer completion.
