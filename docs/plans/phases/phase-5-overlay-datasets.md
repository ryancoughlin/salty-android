# Phase 5: Overlay Datasets + Dynamic Range

> **iOS is the source of truth.**

## Requirement

Let users stack multiple datasets on the map simultaneously (e.g., SST primary + Currents overlay + Chlorophyll overlay), where each overlay has its own independent filter range, colorscale, opacity, variable/depth selection, and full layer controls.

## UX Description

**Adding an overlay:** User taps the "+" button in the OverlayChipBar (anchored to the right of the dataset control area). A bottom sheet (`OverlayPickerSheet`) slides up showing all available dataset types _except_ the current primary. Each row shows the dataset type icon, name, and a checkmark (active), plus (inactive), or lock (no Pro subscription). Tapping a row toggles the overlay on/off.

**Active overlay chips:** Active overlays render as horizontal scrollable chips in the `OverlayChipBar`. Each chip shows:
- A mini gradient bar matching the overlay's colorscale
- The dataset short name (e.g., "CHL", "CUR")
- The queried crosshair value (if available)
- An X-remove action on long-press/secondary gesture

**Editing overlay settings:** Tapping a chip opens `OverlaySettingsSheet` for that overlay type. This sheet provides:
- Model picker (if multiple datasets in that type group)
- Entry selector (SST satellite passes with multiple entries)
- Depth selector (datasets with depth capability)
- Variable selector (e.g., SST Temperature vs Gradient)
- Full `DatasetLayerControls`: visual toggle + opacity, contour toggle + opacity, arrows, particles, numbers, breaks
- Preset quick actions (filter chips within the visual layer section)

**Removing an overlay:** Secondary action on the chip (X button) calls `deactivateOverlay`. Switching the primary dataset clears all overlays (`deactivateAllOverlays`).

**Dynamic range:** `DynamicRangeManager` auto-adjusts the primary dataset's color filter based on viewport statistics from the COG API. Zoom < 7 clears filter, zoom 7-11 fetches viewport stats with 20% padding and 25% minimum range, zoom > 11 freezes last good range. Debounced at 500ms on viewport changes.

**Overlay frame sync:** When the user scrubs the timeline, each overlay resolves its best matching entry via a 3-day lookback from the primary entry's date, at the overlay's selected depth. Each overlay renders independently via its own Zarr shader host.

## Acceptance Criteria

1. User can open the overlay picker and see all dataset types except the current primary
2. Tapping a dataset type in the picker activates it as an overlay with default config (0.7 visual opacity, hideShow filter mode)
3. Active overlays appear as scrollable chips in the OverlayChipBar with gradient preview and queried value
4. Tapping a chip opens that overlay's settings sheet with model/entry/depth/variable selectors and full layer controls
5. Each overlay has independent colorscale, filter range, opacity, contour, arrows, particles, numbers, and breaks controls
6. Removing an overlay (X action) tears down its Zarr shader, GPU resources, and map layers
7. Switching the primary dataset deactivates all overlays
8. Overlays sync frames when the timeline scrubs: each overlay resolves its closest entry within a 3-day lookback
9. The same dataset type cannot be both primary and overlay simultaneously (activating as primary deactivates the overlay)
10. Overlay crosshair values query independently via OverlayValueManager (throttled at 250ms via COG/TiTiler)
11. DynamicRangeManager adjusts the primary filter based on viewport zoom level (< 7 clear, 7-11 fetch stats, > 11 freeze)
12. Pro subscription gating: locked overlay rows show lock icon and are non-interactive
13. Each overlay gets its own Mapbox CustomLayer with unique layer ID (e.g., `zarr-overlay-{datasetId}`)

## iOS Reference Files

| File | Purpose |
|------|---------|
| `Stores/DatasetStore.swift` (lines 41-47, 606-800) | Overlay state dict, public API, activation, deactivation, frame sync, visual source resolution |
| `Types/DatasetRenderConfig.swift` | Universal render config (primary + overlay), `overlayDefaults()`, `resolveOverlayEntry()` |
| `Map/OverlayLayers/OverlayLayers.swift` | MapContent rendering: visual (Zarr), particles, water type, breaks, contours, numbers, arrows per overlay |
| `Map/Overlays/Components/OverlayChipBar.swift` | Horizontal chip bar with active overlay chips + add button |
| `Map/Overlays/Components/DatasetLayerChip.swift` | Unified chip component for visual/contour/breaks/arrows/overlay layers |
| `Map/Overlays/Components/AddOverlayPicker.swift` | Row-based picker showing all overlay types with toggle state |
| `Map/Overlays/Components/OverlayLayerSettings.swift` | Per-overlay settings: model picker, entry/depth/variable selectors, layer controls |
| `Map/Sheets/OverlayPickerSheet.swift` | Bottom sheet wrapping AddOverlayPicker with header |
| `Map/Sheets/OverlaySettingsSheet.swift` | Bottom sheet wrapping OverlayLayerSettings for a specific overlay type |
| `Managers/OverlayValueManager.swift` | Throttled crosshair value queries for active overlays via COG/TiTiler |
| `Managers/DynamicRangeManager.swift` | Viewport-based auto filter range (zoom-gated, debounced COG stats) |

## Data Flow

```
User taps "+" → OverlayPickerSheet
    → toggleOverlay(group)
        → DatasetStore.activateOverlay(type, config, primaryEntry)
            → overlays[type] = config
            → fetchDatasetEntries() (async, gets entries for overlay dataset)
            → zarrManager.load(dataset) (loads Zarr data into GPU)
            → resolveOverlayVisualSources() (creates ZarrShaderHost per overlay)
            → resolveOverlayEntry() (3-day lookback from primary)
            → zarrManager.showFrame() (display matched frame)

Timeline scrub → DatasetStore.showOverlayFrames(primaryEntry)
    → for each overlay config:
        → resolveOverlayEntry(overlayDataset, primaryEntry, override)
        → zarrManager.showFrame(overlayEntry.id, key: datasetId)

Map renders → OverlayLayers (per overlay):
    → ZarrCustomLayer (GPU shader, unique layerId)
    → ContourRenderer (PMTiles contours)
    → CurrentsLayer (PMTiles arrows)
    → NumbersLayer (PMTiles data)
    → BreaksVectorLayer (PMTiles breaks)

Crosshair moves → OverlayValueManager.queryOverlayValues(coordinate)
    → throttle 250ms
    → OverlayPointSampler.sampleOverlays() (parallel COG queries)
    → overlayValues[datasetId] = CurrentValue
    → OverlayChipBar re-renders with queried values
```

## Tasks

### 5.1 — DatasetRenderConfig (port full type)

Port the complete `DatasetRenderConfig` to Android, including `overlayDefaults()` factory and `EntryOverride`.

- **New file:** `app/src/main/java/com/example/saltyoffshore/data/DatasetRenderConfig.kt`
  - `data class EntryOverride(timestamp: Instant?, depth: Int)`
  - `data class DatasetRenderConfig(datasetId, colorscale, customRange, visualEnabled, visualOpacity, contourEnabled, contourOpacity, contourColor, dynamicContourColoring, entryOverride, arrowsEnabled, arrowsOpacity, particlesEnabled, breaksEnabled, breaksOpacity, numbersEnabled, numbersOpacity, selectedPreset, filterMode, selectedVariableId)`
  - `fun overlayDefaults(type, datasetId)` — 0.7 visual opacity, hideShow filter mode
  - `fun primaryDefaults(type, datasetId)` — squash filter mode
  - `fun snapshot(dataRange, resamplingMethod, selectedBreakId)` — creates `DatasetRenderingSnapshot`
- **iOS ref:** `Types/DatasetRenderConfig.swift`

### 5.2 — Overlay entry resolution

Port the 3-day lookback entry resolver as a standalone function.

- **New file:** `app/src/main/java/com/example/saltyoffshore/data/OverlayEntryResolver.kt`
  - `fun resolveOverlayEntry(overlayDataset, primaryEntry, override): TimeEntry?`
  - Matches depth, then searches day-by-day up to 3 days back from primary entry
- **iOS ref:** `Types/DatasetRenderConfig.swift` (lines 190-240)

### 5.3 — ViewModel overlay state + public API

Add overlay state management to ViewModel, mirroring `DatasetStore`'s overlay section.

- **Edit:** `app/src/main/java/com/example/saltyoffshore/viewmodel/AppViewModel.kt`
  - State: `overlays: MutableStateFlow<Map<DatasetType, DatasetRenderConfig>>`
  - State: `overlayVisualSources: MutableStateFlow<Map<String, VisualLayerSource>>`
  - `fun activateOverlay(type, config, primaryEntry)` — fetch entries, load Zarr, resolve visual sources
  - `fun deactivateOverlay(type)` — remove Zarr, clear GPU, remove from map
  - `fun deactivateAllOverlays()` — called on primary dataset switch
  - `fun updateOverlayConfig(type, config)` — sync shader, no Zarr reload
  - `fun selectOverlayVariable(variable, type, primaryEntry)`
  - `fun selectOverlayDepth(depth, type, primaryEntry)`
  - `fun showOverlayFrames(primaryEntry)` — called on timeline scrub
  - `fun applyOverlayPreset(type, preset, valueRange)`
  - Derived: `hasActiveOverlays`, `activeOverlayTypes`, `isOverlayActive(type)`, `overlayConfig(type)`
- **iOS ref:** `Stores/DatasetStore.swift` (lines 606-800)

### 5.4 — Overlay map layers (rendering)

Extend `DatasetLayers` to render overlay datasets as additional Mapbox layers.

- **Edit:** `app/src/main/java/com/example/saltyoffshore/ui/map/layers/DatasetLayers.kt`
  - Add `renderOverlays(overlayConfigs, primaryEntry, availableDatasets, overlayVisualSources)` method
  - Each overlay gets its own Zarr CustomLayer: `zarr-overlay-{datasetId}`
  - Each overlay gets own contour/arrows/numbers/breaks layers with `overlay-{layerType}-{datasetId}` IDs
  - Cleanup method for per-overlay layers
- **iOS ref:** `Map/OverlayLayers/OverlayLayers.swift`

### 5.5 — OverlayChipBar composable

Horizontal scrollable row of active overlay chips plus add button.

- **New file:** `app/src/main/java/com/example/saltyoffshore/ui/controls/overlay/OverlayChipBar.kt`
  - Empty state: shimmer text "NEW! See More. Overlay Datasets" + add button
  - Active state: `LazyRow` of `DatasetLayerChip` per overlay + add button
  - Each chip shows gradient, short name, queried value
  - Chip tap → open settings sheet, chip X → remove overlay
- **iOS ref:** `Map/Overlays/Components/OverlayChipBar.swift`

### 5.6 — DatasetLayerChip composable

Unified chip component used by both primary layer chips and overlay chips.

- **New file:** `app/src/main/java/com/example/saltyoffshore/ui/controls/overlay/DatasetLayerChip.kt`
  - `enum Layer { VISUAL, CONTOUR, BREAKS, ARROWS, OVERLAY }`
  - Mini gradient bar, label, value, enabled state
  - Secondary action: overlay → X remove, others → toggle
- **iOS ref:** `Map/Overlays/Components/DatasetLayerChip.swift`

### 5.7 — OverlayPickerSheet composable

Bottom sheet for adding/removing overlays.

- **New file:** `app/src/main/java/com/example/saltyoffshore/ui/controls/overlay/OverlayPickerSheet.kt`
  - Header "Add Overlay" + close button
  - `AddOverlayPicker`: list of all dataset type groups (excluding primary)
  - Each row: icon, name, status indicator (checkmark/plus/lock)
  - Toggle on tap: activate or deactivate overlay
  - Pro gate: locked rows at 60% opacity with lock icon
- **iOS ref:** `Map/Sheets/OverlayPickerSheet.swift`, `Map/Overlays/Components/AddOverlayPicker.swift`

### 5.8 — OverlaySettingsSheet composable

Per-overlay settings bottom sheet.

- **New file:** `app/src/main/java/com/example/saltyoffshore/ui/controls/overlay/OverlaySettingsSheet.kt`
  - Model picker (if group has multiple datasets)
  - Entry selector (datasets with entry selection capability)
  - Depth selector (datasets with depth capability)
  - Variable selector (datasets with multiple variables)
  - Full `DatasetLayerControls` with `isPrimary = false`
  - Preset quick actions within visual layer section
- **iOS ref:** `Map/Sheets/OverlaySettingsSheet.swift`, `Map/Overlays/Components/OverlayLayerSettings.swift`

### 5.9 — OverlayValueManager

Throttled crosshair value queries for active overlay datasets.

- **New file:** `app/src/main/java/com/example/saltyoffshore/services/OverlayValueManager.kt`
  - `configure(activeOverlays, primaryEntry, viewModel)` — register which overlays to query
  - `queryOverlayValues(coordinate)` — throttled at 250ms
  - Internal: parallel COG/TiTiler point queries per registered overlay
  - State: `overlayValues: StateFlow<Map<String, CurrentValue>>`
  - GPS-style: keep last known value on null results (no flicker)
- **iOS ref:** `Managers/OverlayValueManager.swift`

### 5.10 — DynamicRangeManager

Auto-adjust primary filter range based on viewport statistics.

- **New file:** `app/src/main/java/com/example/saltyoffshore/services/DynamicRangeManager.kt`
  - `setEnabled(enabled)` — toggle + persist to DataStore
  - `onViewportChanged()` — debounced fetch (500ms)
  - `onEntryChanged()` — immediate fetch
  - Zoom gating: < 7 clear filter, 7-11 fetch COG viewport stats, > 11 freeze last range
  - Padded range: 20% padding, 25% minimum range width, clamped to dataset bounds
  - Calls `viewModel.updatePrimaryConfig()` to apply range
- **iOS ref:** `Managers/DynamicRangeManager.swift`

### 5.11 — Wire overlays into MapScreen

Connect overlay state, chip bar, sheets, and rendering into the main map screen.

- **Edit:** `app/src/main/java/com/example/saltyoffshore/ui/screen/MapScreen.kt`
  - Add `OverlayChipBar` to map controls layout (below/beside primary dataset control)
  - Sheet routing: overlay picker sheet, overlay settings sheet (keyed by DatasetType)
  - Pass overlay configs + visual sources to `DatasetLayers.renderOverlays()`
  - Wire timeline scrub to `viewModel.showOverlayFrames()`
  - Wire camera changes to `DynamicRangeManager.onViewportChanged()`
  - Wire crosshair position to `OverlayValueManager.queryOverlayValues()`

### 5.12 — Conflict resolution + cleanup

Handle edge cases around primary/overlay conflicts.

- **Edit:** `app/src/main/java/com/example/saltyoffshore/viewmodel/AppViewModel.kt`
  - On `selectDataset()`: call `deactivateAllOverlays()` before switching
  - On `activateOverlay()`: if type matches primary, deactivate the overlay (same type can't be both)
  - On region change: `deactivateAllOverlays()`
  - Cancel overlay tasks on deactivation (prevent stale GPU loads)
