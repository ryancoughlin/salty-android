# iOS → Android Migration Plan

> Source of truth: `/Users/ryan/Developer/salty-ios`

---

## Detailed Plans

**See `.claude/plans/` for comprehensive implementation plans:**

| Phase | Plan File | Description |
|-------|-----------|-------------|
| 0 | [00-foundation-parity.md](.claude/plans/00-foundation-parity.md) | Preferences, dataset types, rendering config |
| 1 | 01-map-layers.md | BreaksLayer, NumbersLayer, Zarr visual |
| 2 | 02-core-features.md | Waypoints, Crews, Timeline |
| 3 | 03-polish.md | Offline, Tracks, AI Reports |

---

## Migration Principles

1. **1:1 Parity** — Same types, same names, same behavior
2. **iOS is source of truth** — When in doubt, match iOS exactly
3. **Foundation first** — Core infrastructure before features

---

## iOS → Android Translation

| iOS | Android |
|-----|---------|
| `@Observable class` | `ViewModel` + `mutableStateOf` |
| `@Environment(Type.self)` | Parameter passing |
| `actor` | `object` + `Mutex` |
| `async/await` | `suspend fun` + `CoroutineScope` |
| `.task {}` | `LaunchedEffect` |
| `UserDefaults` | `DataStore<Preferences>` |
| `Supabase Swift` | `io.github.jan-tennert.supabase` |
| `Codable` | `@Serializable` (kotlinx.serialization) |
| `struct` (data) | `data class` |
| `enum` with associated values | `sealed class` |

---

## Out of Scope

- Forecast models (GFS, HRRR wind/wave)
- Weather overlays
- Fronts layer

---

## Current State

### Implemented ✅

**Foundation:**
- Region listing + metadata fetch (`SaltyApi`)
- Dataset selection + entry selection (uses `mostRecentEntry`)
- State persistence (`AppPreferencesDataStore`)
- Supabase auth (sign up, sign in, sign out)
- User preferences (Supabase sync)
- Settings UI (units + sign out)
- Camera fly-to animation
- Depth selection with filtered entries

**Map Layers:**
- Visual raster via TiTiler (`COGVisualLayer`) — ⚠️ *iOS uses Zarr shader instead*
- PMTiles contours (`ContourLayer`) — ✅ major/minor lines + labels
- PMTiles currents arrows (`CurrentsLayer`) — ✅ rotation + log color ramp
- Data query layer (`DataQueryLayer`) — ✅ crosshair queries
- Region bounds outline — ✅

**Layers Complete:**
- `BreaksVectorLayer` — ✅ thermal front visualization
- `NumbersLayer` — ✅ grid value labels
- `ParticleLayer` — GPU flow animation (Later)

**Missing UI:**
- `DatasetSelectorSheet` — modal to change datasets
- `FilterRangeSheet` — data range slider for filtering

**Data Models (Aligned with iOS):**
- `RegionMetadata` with `region_id` → `id`
- `Dataset` with `mostRecentEntry`, `hasMultipleDepths`, `depths`
- `TimeEntry` with `entry_id` → `id`, `dataCoveragePercentage`, `isComposite`, `isSurface`, `isSubsurface`
- `RegionsResponse` with `serverUrl`

---

## Phase 1: Dataset Selection UI (Current Focus)

**Goal:** Users can change datasets and adjust filtering, matching iOS UI.

### iOS Dataset UI Architecture

```
DatasetControl.swift (root)
├── PrimaryDatasetPage.swift (shows dataset + timeline)
│   └── "Change" button → DatasetSelectorView overlay
├── DatasetSelectorView.swift (grouped dataset picker)
│   └── DatasetListItem (preview + metadata chips)
└── DatasetFilterSheet.swift (range slider modal)
    └── FilterGradientBar (dual-handle slider)
```

### Android Target Architecture

```
MapScreen.kt (orchestrator)
├── SaltyDatasetControl.kt (shows dataset + timeline) ✅
│   └── "Change" button → DatasetSelectorSheet (modal)
├── DatasetSelectorSheet.kt (grouped dataset picker)
│   └── DatasetListItem (preview + metadata)
└── FilterRangeSheet.kt (range slider modal)
    └── RangeSlider (dual-handle slider)
```

### Task 1.1: DatasetSelectorSheet ✅

**iOS Source:** `SaltyOffshore/Views/DatasetControls/DatasetSelectorView.swift`

| Step | iOS Reference | Android Target | Status |
|------|---------------|----------------|--------|
| 1.1.1 | `DatasetSelectorView` | `DatasetSelectorSheet.kt` | ✅ |
| 1.1.2 | `CategorySection` | Inline in sheet | ✅ |
| 1.1.3 | `DatasetListItem` | Inline in sheet | ✅ |
| 1.1.4 | Wire to `onChange` | `SaltyDatasetControl.kt` | ✅ |

### Task 1.2: FilterRangeSheet ✅

**iOS Source:** `SaltyOffshore/Views/DatasetControls/Components/DatasetFilterSheet.swift`

| Step | iOS Reference | Android Target | Status |
|------|---------------|----------------|--------|
| 1.2.1 | `DatasetFilterSheet` | `FilterRangeSheet.kt` | ✅ |
| 1.2.2 | `FilterGradientBar` | RangeSlider + gradient | ✅ |
| 1.2.3 | Min/Max inputs | Value labels | ✅ |
| 1.2.4 | Wire to ViewModel | `updateDataRange()` | ✅ |

### Task 1.3: Visual Layer Parity

**iOS Source:** `SaltyOffshore/Map/CustomShaders/ZarrShaderHost.swift`

Current Android uses TiTiler COG tiles (server-side rendering).
iOS uses Zarr Metal shader (client-side GPU rendering).

Options:
1. **Keep COG/TiTiler** — Works now, simpler, server-dependent
2. **CustomRasterSource** — Kotlin-only, experimental Mapbox API
3. **CustomLayerHost** — OpenGL ES, 1:1 parity with iOS Metal

Decision: Evaluate after UI is complete

---

## Phase 2: Core Features (After Map Layers)

| Feature | iOS Source | Status |
|---------|------------|--------|
| Waypoints | `WaypointStore.swift` | ✅ Phase 1 complete ([details](docs/WAYPOINT_MIGRATION.md)) |
| Measurement | `Features/Measurement/` | ✅ Complete ([phase-7](docs/plans/phases/phase-7-measurement.md)) |
| Crews | `CrewViewModel.swift` | ❌ |
| Saved Maps | `SavedMapViewModel.swift` | ❌ |
| Timeline Scrubber | `TimelineCoordinator.swift` | ❌ |

---

## Phase 3: Polish (Later)

| Feature | iOS Source | Status |
|---------|------------|--------|
| Offline download | `OfflineDownloadManager.swift` | ❌ |
| Track recording | `TrackViewModel.swift` | ❌ |
| AI reports | `AIReportService.swift` | ❌ |
| Satellite tracking | `SatelliteStore.swift` | ❌ |
| Global layers | `GlobalLayers.swift` | ❌ |

---

## Progress Summary

| Phase | Status | Details |
|-------|--------|---------|
| Foundation | ✅ Complete | Auth, preferences (26 keys), API, dataset types, rendering config, presets |
| Map Layers | ✅ Complete | All vector layers working (Contours, Currents, Breaks, Numbers) |
| Dataset UI | ✅ Complete | DatasetSelectorSheet, FilterRangeSheet, Filter button |
| Visual Layer | 🔄 Evaluate | Currently using COG/TiTiler, iOS uses Zarr Metal shader |
| Core Features | 🔄 In Progress | Waypoints ✅, Measurement ✅, Crews ❌, Saved Maps ❌ |
| Polish | ❌ Not Started | Offline, Tracks, AI Reports |

**Current Focus:** Evaluate visual layer approach (COG vs Zarr).
