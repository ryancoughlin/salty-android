# iOS → Android Full Parity Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Port every iOS feature to Android with 1:1 naming, type, and UX parity — delivering complete functionality per phase.

**Architecture:** Each phase is a skateboard — complete end-to-end loop from user action → state → map update → visual feedback. No disconnected UI. No dead buttons.

**Tech Stack:** Kotlin, Jetpack Compose, Mapbox Android SDK, Ktor, kotlinx.serialization, Supabase Kotlin SDK, DataStore, Room, Hilt

> **CRITICAL: iOS is the source of truth.** When implementing ANY phase, always read the corresponding iOS Swift files first. We will not get everything right on the first pass — refer back to the iOS app at every step. Each phase has its own detailed markdown file in `docs/plans/phases/` that should be consulted and updated as we learn more during implementation.

> **Phase documentation format:** Every phase must state:
> 1. **Requirement** — What does the user need to accomplish? (one sentence)
> 2. **UX Description** — What will the user see, tap, experience? (step-by-step)
> 3. **Acceptance Criteria** — How do we know it works? (testable checklist)

---

## What's Already Built (Phase 0 — Complete)

- Region listing + selection + camera fly-to
- Dataset/entry selection + basic rendering snapshot
- Zarr GPU rendering (OpenGL ES via JNI)
- PMTiles contours, currents arrows, breaks, numbers layers
- Global layers (bathymetry, stations, reefs, tournaments, shipping, LORAN, GPS grid)
- Supabase auth (login/signup/signout)
- User preferences sync (26 keys)
- Settings UI (units), depth selector + filtering
- Dataset selector sheet, filter range sheet, timeline control
- Crosshair overlay + feature query, right-side toolbar
- Layer control sheet with per-layer opacity

---

## Phase Overview

| Phase | Skateboard | End-to-End Loop |
|-------|-----------|-----------------|
| **1** | App Shell + DI | User opens app → auth gate → map with top bar → account hub → settings |
| **2** | Rendering Config Pipeline | User drags filter slider → DatasetRenderConfig updates → GPU shader recolors → scale bar reflects new range |
| **3** | Presets | User taps "Thermal Break" chip → range calculated from COG stats → filter applied → map highlights breaks |
| **4** | Colorscale + Variables | User picks new colorscale → map recolors. User switches variable → new data loads → filter resets |
| **5** | Overlay Datasets | User adds currents overlay → independent layer renders → own filter/opacity → chip bar manages lifecycle |
| **6** | Waypoints | User long-presses map → waypoint created → appears on map → tap shows conditions → manage in list |
| **7** | Measurement Tool | User enters measure mode → tap points → polyline draws → distance labels update |
| **8** | Announcements | App checks for announcement → banner shows → user dismisses → version saved |
| **9** | Route Recording | User taps record → GPS tracks → live stats update → drop waypoints → finish → trip summary |
| **10** | Stations + Weather | User taps station pin → detail sheet → conditions + wave/wind charts |
| **11+** | Crews, Saved Maps, Satellites | Later |

---

## Phase 1: App Shell + DI + Navigation

**Skateboard:** User opens app → auth check → if logged in, sees map with top bar (region name, account button) → taps account → sees hub with settings → signs out → returns to login.

### Task 1.1: Hilt Dependency Injection

**Files:**
- Create: `di/AppModule.kt`
- Create: `di/MapModule.kt`
- Create: `SaltyOffshoreApplication.kt`
- Modify: `app/build.gradle.kts` (Hilt deps)
- Modify: `MainActivity.kt` (@AndroidEntryPoint)

**iOS Reference:** `Core/AppContainer.swift`, `Core/MapboxContainer.swift`

`AppModule` provides: SaltyApi, AppPreferencesDataStore, UserPreferencesRepository, AuthManager
`MapModule` provides: ZarrManager, GlobalLayerManager, CrosshairFeatureQueryManager

### Task 1.2: Extract RegionStore + DatasetStore from AppViewModel

**Files:**
- Create: `viewmodel/RegionStore.kt`
- Create: `viewmodel/DatasetStore.kt`
- Modify: `viewmodel/AppViewModel.kt` (slim to coordinator)

**iOS Reference:** `Stores/RegionStore.swift`, `Stores/DatasetStore.swift`

- `RegionStore`: regions, selectedRegion, appState, browseToRegion(), loadRegions()
- `DatasetStore`: selectedDataset, selectedEntry, depthFilter, selectDataset(), selectEntry(), showEntry()
- `AppViewModel`: cold start, auth state only

### Task 1.3: Auth-Gated Navigation

**Files:**
- Create: `navigation/SaltyNavigation.kt`
- Modify: `MainActivity.kt`

**iOS Reference:** `SaltyOffshoreApp.swift`

### Task 1.4: Top Bar + Account Hub

**Files:**
- Create: `ui/components/TopBar.kt`
- Create: `ui/components/AccountHub.kt`
- Create: `ui/components/OfflineModeIndicator.kt`
- Modify: `ui/screen/MapScreen.kt`

**iOS Reference:** `ContentView.swift`, `Views/AccountHub/`

### Task 1.5: First-Time Region Selection

**Files:**
- Create: `ui/screen/RegionSelectionScreen.kt`

**iOS Reference:** `Views/FTUX/FTUXRegionSelectionView.swift`

---

## Phase 2: Rendering Config Pipeline

**Skateboard:** User drags the filter range slider → `DatasetRenderConfig.customRange` updates → GPU shader recolors the map to show only that range → scale bar gradient reflects new min/max → layer toggles flow through config → opacity changes apply instantly.

This phase wires the existing UI components through a proper config object. Currently the Android app has filter/layer UI but it's connected directly to the rendering snapshot. iOS has an intermediary `DatasetRenderConfig` that is the single source of truth for how a dataset renders. This phase ports that pattern.

### Task 2.1: DatasetRenderConfig Type

**Files:**
- Create: `data/DatasetRenderConfig.kt`

**iOS Reference:** `Types/DatasetRenderConfig.swift`

```kotlin
data class DatasetRenderConfig(
    var colorscale: Colorscale? = null,
    var customRange: ClosedRange<Double>? = null,
    var selectedPreset: DatasetPreset? = null,
    var cogStatistics: COGStatisticsResponse? = null,
    var selectedVariableId: String? = null,
    var filterMode: FilterMode = FilterMode.SQUASH,
    var particlesEnabled: Boolean = false,
    var visualEnabled: Boolean = true,
    var visualOpacity: Double = 1.0,
    var contourEnabled: Boolean = true,
    var contourOpacity: Double = 1.0,
    var contourColor: Long? = null,
    var contourDynamicColoring: Boolean = false,
    var breaksEnabled: Boolean = false,
    var breaksOpacity: Double = 1.0,
    var arrowsEnabled: Boolean = false,
    var arrowsOpacity: Double = 1.0,
    var numbersEnabled: Boolean = false,
    var numbersOpacity: Double = 1.0,
) {
    fun activeRange(entry: TimeEntry, dataset: Dataset): ClosedRange<Double>
    fun clearFilter()
    fun snapshot(dataRange: ClosedRange<Double>): DatasetRenderingSnapshot

    companion object {
        fun primaryDefaults(type: DatasetType): DatasetRenderConfig
        fun overlayDefaults(type: DatasetType): DatasetRenderConfig
    }
}
```

### Task 2.2: Wire DatasetStore Through Config

**Files:**
- Modify: `viewmodel/DatasetStore.kt` (add primaryConfig, updateSnapshot flow)
- Modify: `viewmodel/AppViewModel.kt` (delegate to DatasetStore)

**iOS Reference:** `Stores/DatasetStore.swift` — `updateSnapshot(entry:)` method

**End-to-end flow:**
1. `DatasetStore.selectDataset()` → creates `primaryConfig = DatasetRenderConfig.primaryDefaults(type)`
2. `DatasetStore.showEntry(entry)` → calls `primaryConfig.snapshot(dataRange)` → updates `renderingSnapshot`
3. Rendering snapshot feeds existing GPU shader + scale bar

### Task 2.3: Wire Filter Range Sheet to Config

**Files:**
- Modify: `ui/components/FilterRangeSheet.kt`
- Modify: `ui/components/GradientScaleBar.kt`

**What changes:**
- Slider drag writes to `DatasetStore.primaryConfig.customRange`
- Min/max text fields edit the same value
- Reset button calls `DatasetStore.primaryConfig.clearFilter()`
- Scale bar gradient reflects `primaryConfig.colorscale` + current range
- Keyboard toolbar with Reset/Clear/Done (matches iOS)

### Task 2.4: Wire Layer Controls to Config

**Files:**
- Modify: `ui/controls/LayersControlSheet.kt`
- Modify: `ui/controls/layercontrols/VisualLayerControl.kt`
- Modify: `ui/controls/layercontrols/ContourLayerControl.kt`
- Modify: `ui/controls/layercontrols/ArrowsLayerControl.kt`
- Modify: `ui/controls/layercontrols/BreaksLayerControl.kt`
- Modify: `ui/controls/layercontrols/NumbersLayerControl.kt`

**What changes:**
- Each toggle reads/writes `primaryConfig.{layer}Enabled`
- Each opacity slider reads/writes `primaryConfig.{layer}Opacity`
- Contour color picker writes `primaryConfig.contourColor`
- Dynamic contour toggle writes `primaryConfig.contourDynamicColoring`
- All changes flow through `DatasetStore.updateSnapshot()` → map updates

### Task 2.5: Status Banners

**Files:**
- Create: `ui/components/StaleDataBanner.kt` — shows when entry > 1 day old
- Create: `ui/components/CompositeBadge.kt` — shows when 3-day composite
- Create: `ui/components/LowCoverageBanner.kt` — shows when < 30% coverage
- Modify: `ui/screen/MapScreen.kt` (add banners)

**iOS Reference:** Conditional banners in `ContentView.swift`, `RightSideToolbar.swift`

---

## Phase 3: Presets

**Skateboard:** User sees preset chips above dataset control → taps "Thermal Break" → COG statistics fetched (loading spinner) → range calculated (mean ± optimal spread) → filter applied → map highlights temperature gradients → tapping again clears filter.

### Task 3.1: Preset Types + COG Statistics

**Files:**
- Create: `data/PresetTypes.kt` — DatasetPreset, PresetConfiguration, PresetType (static/dynamic/microBreak)
- Create: `data/COGStatistics.kt` — COGStatisticsResponse, COGBandStatistics
- Create: `services/COGStatisticsService.kt` — fetch band stats from tiler

**iOS Reference:** `Types/PresetTypes.swift`, `Features/Presets/Types/COGStatistics.swift`

**Preset definitions per dataset:**
- SST: "Cold Water" (65-70°F), "Warm Water" (78-82°F), "Thermal Break" (±0.25° from crosshair), "Prime Zone" (mean ± 1std)
- Chlorophyll: "High Productivity" (static), "Color Line" (dynamic)
- Salinity: "River Plume" (bottom 2%), "Fresh Influence" (dynamic)
- Currents: "Slack Water" (bottom 2%), "Strong Flow" (dynamic)
- MLD: "Surface" (0-15m), "Thermocline" (dynamic)

### Task 3.2: Quick Actions Bar + Preset Chips

**Files:**
- Create: `ui/components/QuickActionsBar.kt` — floating bar above bottom control
- Create: `ui/components/PresetQuickActions.kt` — horizontal scroll of preset chips

**iOS Reference:** `Map/Controls/QuickActionsBar.swift`, `Features/Presets/Views/PresetQuickActions.swift`

**UI behavior:**
- Chips: selected = white bg + range text, unselected = glass/dark
- Loading spinner while fetching dynamic stats
- Micro-break chips disabled + tooltip if crosshair empty
- Toggle: tap to apply, tap again to clear

### Task 3.3: Wire Presets to Config Pipeline

**Files:**
- Modify: `viewmodel/DatasetStore.kt` (add applyPreset, loadStatistics)

**End-to-end flow:**
1. User taps preset → `DatasetStore.applyPreset(preset)`
2. `preset.calculateRange(currentValue, valueRange)` → returns range
3. Sets `primaryConfig.customRange` + `primaryConfig.selectedPreset`
4. `updateSnapshot()` → GPU shader + scale bar update
5. Filter range sheet reflects new range (synced)

---

## Phase 4: Colorscale + Variable Selector

**Skateboard:** User taps colorscale chip → picker opens → selects "Plasma" → map instantly recolors from viridis to plasma. User sees variable chips (on multi-variable dataset) → taps "Salinity" → data reloads → filter resets to new range → map shows salinity.

### Task 4.1: Colorscale Picker

**Files:**
- Create: `ui/components/ColorscalePickerSheet.kt` — grid of all colorscales with previews
- Modify: `ui/components/ColorscaleGradient.kt` (if needed)
- Modify: `ui/controls/layercontrols/VisualLayerControl.kt` (add colorscale chip trigger)

**iOS Reference:** `Views/DatasetControls/Components/ColorscaleChip.swift`, `ColorscaleComponents.swift`

### Task 4.2: Variable Selector

**Files:**
- Create: `ui/components/VariableQuickActions.kt` — horizontal chips per variable
- Modify: `viewmodel/DatasetStore.kt` (add selectVariable method)

**iOS Reference:** `Map/Controls/QuickActionsBar.swift` variable section

**End-to-end flow:**
1. Variable chip tapped → `DatasetStore.selectVariable(variableId)`
2. Updates `primaryConfig.selectedVariableId`
3. Reloads Zarr data for new variable
4. Resets filter range to new variable's data range
5. Map re-renders with new variable data

### Task 4.3: Expanded/Collapsed Dataset Control

**Files:**
- Modify: `ui/components/SaltyDatasetControl.kt` (add expand/collapse modes)
- Create: `ui/components/ExpandedDatasetView.kt` — full page with header, scale bar, timeline, layer controls
- Create: `ui/components/CollapsedDatasetView.kt` — compact timeline + chip bar

**iOS Reference:** `Views/DatasetControls/DatasetControl.swift`, `ExpandedDatasetView.swift`, `CollapsedDatasetView.swift`

Spring animation between modes: `spring(response: 0.3, dampingFraction: 0.8)`

---

## Phase 5: Overlay Datasets

**Skateboard:** User taps "+" on overlay chip bar → picker shows available types → selects "Currents" → currents layer renders with independent opacity/colorscale → chip appears in bar → X removes it → each overlay has its own filter range.

### Task 5.1: Overlay State in DatasetStore

**Files:**
- Modify: `viewmodel/DatasetStore.kt` (add overlays map, addOverlay, removeOverlay)

**iOS Reference:** `Stores/DatasetStore.swift` overlay management

`overlays: Map<DatasetType, DatasetRenderConfig>` — each overlay gets its own config with `overlayDefaults(type)`

### Task 5.2: Overlay Chip Bar + Picker

**Files:**
- Create: `ui/components/OverlayChipBar.kt` — horizontal scroll of active overlays with X-remove + add button
- Create: `ui/components/OverlayPickerSheet.kt` — available types not currently active

### Task 5.3: Overlay Layer Rendering

**Files:**
- Modify: `ui/map/layers/DatasetLayers.kt` (render overlay layers from overlay configs)
- Modify: layer control sheet (show overlay-specific controls)

### Task 5.4: Dynamic Range Manager

**Files:**
- Create: `managers/DynamicRangeManager.kt`

**iOS Reference:** `Managers/DynamicRangeManager.swift`

Auto-scale by zoom: <7 = full range, 7-11 = viewport stats + 20% padding, >11 = freeze. User toggle in settings.

---

## Phase 6: Waypoints

**Skateboard:** User long-presses map → waypoint created with auto-name "WPT001" → pin appears on map → tap opens detail sheet → Conditions tab shows SST/salinity/moon phase → edit name/symbol/notes → manage list in account hub → import GPX → export GPX.

### Task 6.1: Types + Storage

- `data/waypoint/Waypoint.kt`, `WaypointSymbol.kt`, `WaypointConditionTypes.kt`, `WaypointFormState.kt`
- `data/waypoint/WaypointEntity.kt`, `WaypointDao.kt`, `WaypointDatabase.kt`

### Task 6.2: Store + Services

- `viewmodel/WaypointStore.kt`, `viewmodel/WaypointNavigation.kt`
- `services/WaypointConditionsService.kt`, `services/WaypointDefaultNamingService.kt`

### Task 6.3: Map Layer + UI

- `ui/map/layers/WaypointLayer.kt`
- `ui/waypoints/WaypointManagementView.kt`, `WaypointDetailsView.kt`, `WaypointConditionsView.kt`
- `ui/waypoints/WaypointFormView.kt`, `SymbolChipPicker.kt`, `CoordinateInputView.kt`
- `services/GPXParser.kt`, `services/GPXExportService.kt`

---

## Phase 7: Measurement Tool

**Skateboard:** User taps ruler button → enters measure mode (hides other controls) → taps two+ points on map → polyline draws with distance labels per segment → total distance shown → undo last point → done exits mode.

- `data/measurement/MeasurementTypes.kt`
- `viewmodel/MeasureMode.kt`, `viewmodel/MeasurementManager.kt`
- `ui/map/layers/MeasurementLayer.kt`, `ui/components/MeasureModeOverlay.kt`

---

## Phase 8: Announcements

**Skateboard:** App launches → checks API → announcement exists with version > last seen → banner appears → user reads → dismisses → version saved → won't show again until new version.

- `data/announcement/Announcement.kt`
- `services/AnnouncementService.kt`
- `ui/components/AnnouncementSheetView.kt`

---

## Phase 9: Route Recording

**Skateboard:** User taps record FAB → foreground service starts → GPS tracks at 10s intervals → live stats (speed/distance/depth/marks) → drop waypoints mid-trip → finish → trip completion screen → saved to Room → browse track list → tap track → detail with gradient polyline + ocean data.

### Tasks:
- Types: Track, TrackPoint, TrackStats, MotionState, SpeedBracket
- Storage: Room entities + DAO
- Services: TrackRecordingService (foreground service), TrackOceanDataService
- ViewModels: TrackViewModel, TrackMode
- Map: TrackPolylineLayer
- UI: RecordingControlsFAB, RecordingModeView, RecordingStatusPill, TrackDetailView, TrackListView

---

## Phase 10: Stations + Weather

**Skateboard:** User taps station pin on map → detail sheet opens → current conditions (wind/temp/pressure) → wave forecast chart → wind forecast chart → toggle between forecasts.

- Types: Station, StationObservation, WaveForecast, WeatherData
- Services: StationService
- ViewModels: StationDetailsViewModel
- UI: StationDetailsView, StationConditionsView, WaveConditionsChart, WindConditionsChart

---

## Phase 11+: Later

- Crews + Sharing
- Saved Maps
- Satellite Tracking
