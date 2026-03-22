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
- [ ] **Per-dataset contour layer variants**: iOS has 6 specialized contour renderers (StandardContourLayer, MLDContourLayer, SalinityContourLayer, DissolvedOxygenContourLayer, PhytoplanktonContourLayer, SSHContourLayer) with different line widths, spacing, label formatting. Android uses one generic ContourLayer for all.
- [ ] **SST contour label math**: iOS computes `°F`/`°C` labels from raw `temperature` field with number formatting. Android uses pre-formatted `temp_label` from PMTiles.
- [ ] **Dynamic contour coloring expression**: iOS builds a data-driven Mapbox expression that colors contour lines by value. Android has `dynamicColoring` flag but no expression builder.
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

## Upcoming Phases

| Phase | Status | Spec |
|-------|--------|------|
| 2.5 — Data Viz Pipeline | **Fixed** | See "Data Visualization Pipeline Fix" above |
| 3 — Presets | Not started | `phases/phase-3-presets.md` |
| 4 — Colorscale + Variables | Not started | `phases/phase-4-colorscale-variables.md` |
| 5 — Overlay Datasets | Not started | `phases/phase-5-overlay-datasets.md` |
| 6 — Waypoints | Phase 1 complete | `phases/phase-6-waypoints.md` |
| 7 — Measurement | **Complete** | `phases/phase-7-measurement.md` |
| 8 — Announcements | Not started | `phases/phase-8-announcements.md` |
| 9 — Route Recording | Not started | `phases/phase-9-route-recording.md` |
| 10 — Stations + Weather | Not started | `phases/phase-10-stations-weather.md` |

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
