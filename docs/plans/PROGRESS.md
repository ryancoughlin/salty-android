# iOS → Android Parity: Progress Tracker

> Updated: 2026-03-22. Check items off, note issues, flag things to revisit.

---

## Phase 1: App Shell + DI + Navigation — COMPLETE

- [x] SaltyApplication subclass + manifest
- [x] AccountButton (gradient circle, press-scale animation)
- [x] TopBar (three-slot Row, appear animation)
- [x] AccountHubSheet (all 11 sections, 2-step delete)
- [x] LoginScreen refactor (email expand animation)
- [x] FTUX region selection (blocking dialog, grouped list)
- [x] AppViewModel additions (auth state, FTUX, foreground refresh)
- [x] MainActivity rewrite (auth state machine, wiring)
- [x] Delete SettingsScreen, remove gear icon from MapScreen
- [x] Final wiring + bug fix (RefreshFailure handling)

**Known gaps:** FTUX thumbnails use placeholder (Coil not added). Hilt DI deferred.

---

## Phase 2: Rendering Config Pipeline — COMPLETE (build testing)

- [x] 4 missing colorscales added (Wind, Wave Height, Wave Period, Water Clarity)
- [x] DatasetRenderConfig (24 properties, factory methods, snapshot())
- [x] FilterMode, EntryOverride types
- [x] CheckerboardPattern, LayerSection, LayerOpacityControl composables
- [x] ViewModel wired through config (updatePrimaryConfig, single StateFlow)
- [x] FilterGradientBar (dual handles, gradient, 60fps drag)
- [x] DatasetFilterSheet (modal, pickers, embedded gradient bar)
- [x] 6 layer controls rewritten for config pattern + ParticlesLayerControl
- [x] LayersControlSheet simplified (config + onConfigChanged)
- [x] MapScreen wired to new DatasetFilterSheet
- [x] Old FilterRangeSheet deleted
- [x] Build errors fixed (imports, type mismatches, API changes)

**Build checkpoint:** Clean + Run needed to verify runtime behavior.

---

## Data Visualization Pipeline Fix — COMPLETE (2026-03-22)

Three critical disconnects fixed + six iOS parity corrections.

### Pipeline Fixes
- [x] **Zarr load timing**: Entries load first, then `loadZarrForDataset()` with populated data (was loading with empty entries → zero frames)
- [x] **PMTiles URL lifecycle**: All vector layers (contours, data query, currents, breaks, numbers) track their URL and tear down + rebuild when it changes (was creating source once and never updating)
- [x] **Shader uniforms wired**: `updatePrimaryConfig()` now pushes opacity/filter/colorscale to GPU via `zarrManager.setUniforms()` + `setColorscale()` + `repaint()` (was updating snapshot only, never telling the shader)

### iOS Parity Corrections
- [x] Contour filter uses `contourFilterRange` (visibility) not `contourRange` (coloring)
- [x] Phytoplankton colorscale → `BLOOM` (was `CHLOROPHYLL`)
- [x] Contour color always `Color.BLACK` (was per-type colored — iOS defaults all to black)
- [x] Contour `symbolSpacing: 170` (was 140), major labels `minZoom: 7`
- [x] `supportsFronts` → SST only (was SST, SSH, Salinity, MLD — iOS only has `BreaksConfig` on SST)
- [x] dissolved_oxygen unit → `mg/L` (was `mmol/m³`)
- [x] water_type `valueKey` → `"label"` (was `"water_type"`)

### Files Modified
- `viewmodel/AppViewModel.kt` — pipeline orchestration, shader sync
- `ui/map/layers/DatasetLayers.kt` — layer lifecycle, URL tracking
- `ui/map/layers/ContourLayer.kt` — styling values
- `data/DatasetType.kt` — contourColor, supportsFronts
- `data/RenderingConfig.kt` — phytoplankton colorscale
- `data/DatasetConfiguration.kt` — dissolved_oxygen unit, water_type key

### Needs Runtime Verification
- [ ] Select region → Zarr visual layer renders (colored ocean data visible)
- [ ] Toggle contours off → disappear; on → reappear
- [ ] Switch datasets → contours update to new dataset's PMTiles
- [ ] Change colorscale → visual layer recolors
- [ ] Change opacity slider → visual layer opacity changes
- [ ] Scrub timeline → visual layer and contours update to new entry

### Known Remaining Gaps (not blocking, future work)
- [x] **Per-dataset contour layer variants**: ~~iOS has 6 specialized contour renderers. Android used one generic.~~ Fixed 2026-03-22: `ContourLayer.kt` now dispatches per dataset type (Standard, SSH, Simple, DissolvedOxygen, Phytoplankton) matching all 6 iOS renderers with correct line widths, spacing, labels.
- [x] **Contour state disconnect**: Toggle in layer controls showed OFF but contours rendered. Fixed: `DatasetRenderingSnapshot` defaults changed (`contourEnabled=false`, `arrowsEnabled=false`), all toggle/opacity methods now flow through `updatePrimaryConfig()` instead of mutating snapshot directly.
- [x] **Chlorophyll/Phytoplankton contour capabilities**: iOS had `hasContours=true`, Android was missing. Fixed in `DatasetType.capabilities`.
- [ ] **SST contour label math**: iOS computes `°F`/`°C` labels from raw `temperature` field with number formatting + unit suffix. Android uses pre-formatted `temp_label` from PMTiles (correct values, but no live unit conversion).
- [ ] **Dynamic contour coloring expression**: iOS builds a data-driven Mapbox expression that colors contour lines by value using dataset colorscale. Android has `dynamicColoring` flag but only SSH uses data-driven color (hardcoded interpolation). Other datasets use static black.
- [ ] **SSH circulation arrows**: iOS renders arrow icons along SSH contour lines (arrow-left/arrow-right by circulation direction). Android SSH contours have lines and labels but no arrows.
- [ ] **Filter drag → direct GPU**: iOS calls `host.setFilterRangeDirect()` during drag for 60fps. Android filter drag goes through state update (may cause jank).
- [ ] **Crossfade animation**: iOS does 0.4s linear blend between frames on timeline scrub. Android shows frames instantly.
- [ ] **Fade-in animation**: iOS fades in first frame with 0.3s opacity animation. Android shows instantly.
- [ ] **Background frame preloading**: Disabled on Android (was causing ANR). iOS loads all ~180 frames into GPU in background.
- [ ] **`repaint` null until map loads**: If Zarr frame loads before MapView initializes, `repaint?.invoke()` is a no-op. First frame may not display until user interacts with map.

---

## Critical Bug: ANR on Region Selection — PARTIALLY FIXED

**Root cause:** Cascading Compose recompositions + main thread blocking.

**Fixes applied:**
1. StateFlow refactor (15 mutableStateOf → single MutableStateFlow<MapScreenState>)
2. IO dispatcher for region loading
3. ZarrManager moved to Dispatchers.IO
4. Background frame preloading disabled

**May still need:** Testing on real device (emulator JIT overhead). selectDataset() and refreshData() paths may also need IO dispatcher.

---

## Phase 7: Measurement Tool — COMPLETE

- [x] Port measurement types (`MeasurementPoint`, `MeasurementSegment`, `MapMeasurement`)
- [x] Port `MeasurementManager` as `MeasurementState` (merged mode + session state)
- [x] Wire `MeasureMode` toggle into `AppViewModel` + `RightSideToolbar`
- [x] Build `MeasurementMapEffect` — GeoJSON lines, circle points, symbol labels via Mapbox Turf
- [x] Build `MeasureModeOverlay` — status pill + undo/clear/done toolbar
- [x] Wire map tap handler to route taps to measurement when mode active
- [x] Material 3 surface hierarchy polish (accent pill, primary Done, secondary icon actions)

**Files created:** `data/measurement/MeasurementTypes.kt`, `data/measurement/MeasurementState.kt`, `ui/measurement/MeasurementMapEffect.kt`, `ui/measurement/MeasureModeOverlay.kt`

**Dependency added:** `com.mapbox.mapboxsdk:mapbox-sdk-turf:7.3.0` for distance/midpoint/bearing calculations.

**Design note:** iOS splits `MeasureMode` (toggle) and `MeasurementManager` (session) as two `@Observable` classes injected via `@Environment`. Android merges both into `MeasurementState` held by `AppViewModel` — simpler, same behavior.

---

## Phase 3: Presets + Variables + Quick Actions — COMPLETE

- [x] `COGStatisticsService` — Ktor service fetching band statistics from TiTiler, custom KSerializer for dynamic band JSON
- [x] `COGStatisticsResponse` — `@Serializable` with dynamic band names, `primaryBandStatistics()`, convenience accessors
- [x] `COGBandStatistics` expanded — added all iOS fields (majority, minority, count, sum, unique, validPercent, maskedPixels, validPixels), `@SerialName` for snake_case JSON mapping
- [x] `QuickActionChip` — M3 FilterChip with two-state white alpha styling: active (solid white/black text) vs inactive (white@20%/white@70% text)
- [x] `PresetQuickActions` — preset chip row with range text, loading indicator, break preset disabling
- [x] `VariableQuickActions` — variable chips, only renders for multi-variable datasets
- [x] `DepthQuickAction` — depth cycle chip (tap cycles through available depths)
- [x] `QuickActionsBar` — wired all three sections with vertical dividers, horizontalScroll, 44dp height
- [x] `AppViewModel.applyPreset()` — toggle on/off, calculates range from crosshair + entry data
- [x] `AppViewModel.selectVariable()` — switches variable, clears active filter
- [x] `AppViewModel.loadCOGStatistics()` — 600ms debounced, cancel-and-replace, `Dispatchers.IO` for network, state writes on Main
- [x] `allPresets` computed property — merges static + dynamic presets
- [x] COG stats wired to entry changes — `loadCOGStatistics()` called from `selectEntry()`, `loadEntriesForDataset()`, `selectDataset()`
- [x] `DatasetRenderConfig` cleanup — removed dead `cogStatistics: Any?` field
- [x] `MapScreen` placement — QuickActionsBar positioned above SaltyDatasetControl

**Commit:** `53eb441` — 10 files, 658 insertions

**Design decision:** White alpha chips (active=solid white, inactive=white@20%) over M3 surface containers. M3 surfaces are opaque and designed for app chrome — on a satellite map they'd look like dark blobs. White alpha is the closest Android equivalent to iOS `.ultraThinMaterial` for map overlays. Google Maps uses the same approach.

**Remaining gaps (not blocking, future work):**
- [ ] **Stagger entrance animation**: iOS delays each chip by 0.03s. Android chips appear instantly.
- [ ] **Spring selection animation**: iOS uses spring(response=0.5, damping=0.9). Android uses default M3 chip animation.
- [ ] **Edge fade mask**: iOS QuickActionsBar has `.edgeFadeMask()`. Android has no equivalent modifier.
- [ ] **Haptic feedback**: iOS fires haptic on chip tap. Android has none.
- [ ] **Break preset toast**: iOS shows snackbar when tapping break preset without crosshair value. Android disables the chip but shows no message.
- [ ] **Pro/preview mode gating**: iOS dims chips for non-premium users. Android skips subscription gating entirely.
- [ ] **Statistics caching by COG URL**: iOS caches in DatasetActor. Android fetches every time (debounced but no cache).
- [ ] **Overlay preset support**: iOS has `applyOverlayPreset()`. Android only supports primary dataset presets.

---

## Phase 10: Stations + Weather — COMPLETE

- [x] `StationListService` — fetch station list from API
- [x] `StationDetailViewModel` — individual station detail (observations, forecasts)
- [x] `StationDetailsView` — bottom sheet with forecast charts, weather data
- [x] Station markers on map via `GlobalLayers`

**Commit:** `5a6071b`

---

## Tools Menu + Announcements + Share Links — COMPLETE (merged from main)

From `main` branch merge (`ad47c34`):

- [x] **Map Tools Menu** — grid layout bottom sheet matching iOS MapToolBar (waypoints, satellites, my location, share, dataset guide)
- [x] **Announcements** — fetch from Supabase, display sheet, dismiss tracking
- [x] **Share Links** — create shareable map links with camera/layers/region state, preview sheet with map snapshot

---

## Satellite Tracker Mode — IN PROGRESS

Satellite tracking UI, state, and API fully ported. Map layer rendering not yet working.

**Plan:** `docs/plans/2026-03-27-satellite-tracker.md`

### What's Done

- [x] **Data models** — 16 types: TrackerResponse, SatelliteTrack, RegionalPass, SatelliteCoverage, enums (SatelliteDatasetType, OrbitDirection, DayNight, PassStatus, SkipReason, DataFreshness, GeoJSONPolygon)
- [x] **SatelliteService** — 3 API endpoints: /satellites/swaths, /satellites/coverage, /region/{id}/satellite-coverage
- [x] **SatelliteTrackingMode** — mode state: isActive, mode (tracker/coverage), selections, night filter
- [x] **SatelliteStore** — data state: tracks, passes, predictions, parallel loading
- [x] **SatelliteModeView** — full-screen overlay: mode toggle pill, data loading via LaunchedEffect, auto-select
- [x] **TrackerPanel** — HorizontalPager carousel with satellite cards, bidirectional sync, page dots
- [x] **CoveragePanel** — pass list with status indicators, night toggle, yesterday header, auto-scroll
- [x] **PassPredictionPanel** — NextPassRow + ModalBottomSheet with freshness dots, countdown
- [x] **DayNightBadge** — day/night/both badge composable
- [x] **SatelliteTrackLayer** — Mapbox layers: unselected outlines, selected fill+glow, trail with fading opacity, labels
- [x] **CoveragePassLayer** — selected polygon + tappable circle pins with status colors
- [x] **SatelliteLayers** — router with cross-mode cleanup
- [x] **Globe viewport** — fly animation to 20°N/40°W zoom 0 on enter, fly back to region on exit
- [x] **Globe projection** — Mercator → Globe when active, dark theme forced
- [x] **Zoom constraints** — 0-4 in satellite mode (vs 1-24 normal)
- [x] **Selection camera** — fly to track (zoom 2.0) and pass (zoom 3.0) on selection
- [x] **Region annotations** — hidden during satellite mode
- [x] **Tools menu wiring** — satellite mode launches from map tools menu
- [x] **Sheet dismissal** — all sheets close on satellite mode entry

### Files Created (14)

| File | Lines |
|------|-------|
| `data/satellite/SatelliteModels.kt` | ~350 |
| `data/satellite/SatelliteService.kt` | ~65 |
| `viewmodel/SatelliteTrackingMode.kt` | ~80 |
| `viewmodel/SatelliteStore.kt` | ~120 |
| `ui/satellite/SatelliteModeView.kt` | ~250 |
| `ui/satellite/TrackerPanel.kt` | ~200 |
| `ui/satellite/CoveragePanel.kt` | ~370 |
| `ui/satellite/PassPredictionPanel.kt` | ~280 |
| `ui/satellite/DayNightBadge.kt` | ~50 |
| `ui/map/satellite/SatelliteTrackLayer.kt` | ~250 |
| `ui/map/satellite/CoveragePassLayer.kt` | ~230 |
| `ui/map/satellite/SatelliteLayers.kt` | ~80 |
| `res/drawable/ic_satellite.xml` | vector drawable |
| `docs/plans/2026-03-27-satellite-tracker.md` | implementation plan |

### Files Modified (4)

- `viewmodel/AppViewModel.kt` — added satelliteTrackingMode + satelliteStore
- `ui/screen/MapScreen.kt` — viewport animations, projection, zoom bounds, overlay, tools wiring
- `ui/controls/RightSideToolbar.kt` — resolved merge conflict (tools menu from main)
- `data/SaltyApi.kt` — made client internal for SatelliteStore access

### Blocking: Map Layer Rendering

- [ ] **SatelliteTrackLayer polygons/trails/labels not rendering on map** — UI panel works, API returns data, camera animates to satellites, but Mapbox GeoJSON layers don't appear. Investigated: thread safety (subscribeStyleLoaded dispatches to main), style reload survival (subscribeStyleLoaded + getStyle pattern), MapEffect composition timing. Debug logging added. Needs further investigation — check Logcat `SatelliteLayers` tag.
- [ ] **CoveragePassLayer pins/polygons not rendering** — same root cause as tracker layers

### Remaining Gaps (cosmetic, not blocking)

- [ ] **Foreground color animation on TrackerCard**: iOS animates both bg and text color; Android animates bg only, text snaps
- [ ] **Duplicate Mapbox helpers**: `toMapboxFeature`, `addOrUpdateSource`, etc. copied in both track/coverage layers (could extract to shared file)
- [ ] **CoveragePanel date formatter**: has its own ISO parser instead of reusing SatelliteModels helpers
- [ ] **Pin icons**: iOS uses custom coverage-pin-success/pending/unavailable drawables; Android uses CircleLayer (functional but less polished)

---

## Upcoming Phases

| Phase | Status | Spec |
|-------|--------|------|
| 2.5 — Data Viz Pipeline | **Fixed** | See "Data Visualization Pipeline Fix" above |
| 3 — Presets | **Complete** | `phases/phase-3-presets.md` |
| 4 — Colorscale + Variables | Partially done (variables done, colorscale picker remaining) | `phases/phase-4-colorscale-variables.md` |
| 5 — Overlay Datasets | Not started | `phases/phase-5-overlay-datasets.md` |
| 6 — Waypoints | Phase 1 complete | `phases/phase-6-waypoints.md` |
| 7 — Measurement | **Complete** | `phases/phase-7-measurement.md` |
| 8 — Announcements | **Complete** (merged from main) | `phases/phase-8-announcements.md` |
| 9 — Route Recording | Not started | `phases/phase-9-route-recording.md` |
| 10 — Stations + Weather | **Complete** | `phases/phase-10-stations-weather.md` |
| — Tools Menu | **Complete** (merged from main) | MapToolBar grid layout |
| — Share Links | **Complete** (merged from main) | Create + preview shareable map links |
| — Satellite Tracker | **In Progress** (UI done, map layers not rendering) | `plans/2026-03-27-satellite-tracker.md` |

---

## File Locations

| What | Where | Persists? |
|------|-------|-----------|
| **Phase specs** | `docs/plans/phases/phase-N-*.md` | Git (in repo) |
| **Master plan** | `docs/plans/2026-03-21-ios-parity-phases.md` | Git (in repo) |
| **This file** | `docs/plans/PROGRESS.md` | Git (in repo) |
| **Session memory** | `~/.claude/projects/.../memory/` | Claude memory (cross-session) |
| **Project rules** | `CLAUDE.md` | Git (in repo) |

---

## Lessons Learned

1. **Always use Run, not Build** — Assemble doesn't deploy to emulator
2. **Sync Now** after changing gradle files
3. **Clean Project** when changes don't seem to take effect
4. **No hacks** — follow standard Android patterns from official docs
5. **Worktree agents can conflict** — when two agents touch the same files, merge one first, skip the other
6. **StateFlow.update is thread-safe** — no need for withContext(Main) for state updates
7. **AGP 9.1 has built-in Kotlin** — don't apply kotlin.android plugin separately
8. **Mapbox 11.16 API changes** — CustomLayer → addStyleCustomLayer, mapboxMap → getMapboxMap(), Projection constructor is internal
