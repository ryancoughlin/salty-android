# iOS → Android Migration Plan

> Source of truth: `/Users/ryan/Developer/salty-ios`

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

**Missing Layers:**
- `BreaksLayer` — thermal front visualization (PMTiles)
- `NumbersLayer` — grid value labels (PMTiles)
- `ParticleLayer` — GPU flow animation (Later)

**Data Models (Aligned with iOS):**
- `RegionMetadata` with `region_id` → `id`
- `Dataset` with `mostRecentEntry`, `hasMultipleDepths`, `depths`
- `TimeEntry` with `entry_id` → `id`, `dataCoveragePercentage`, `isComposite`, `isSurface`, `isSubsurface`
- `RegionsResponse` with `serverUrl`

---

## Phase 1: Map Layers (Current Focus)

**Goal:** All dataset visualization layers working, matching iOS architecture.

### iOS Layer Architecture

```
MapboxMapView_V2.swift (orchestrator)
├── DatasetLayers.swift (primary dataset stack)
│   ├── 1. Visual Layer (Zarr Metal shader)
│   ├── 2. Particles Layer (GPU flow animation)
│   ├── 3. Data Query Layer (crosshair sampling)
│   ├── 4. Breaks Layer (thermal fronts)
│   ├── 5. Contour Layer (isolines)
│   ├── 6. Currents Arrows (velocity vectors)
│   └── 7. Numbers Layer (grid values)
├── OverlayLayers.swift (secondary datasets)
└── GlobalLayers.swift (static reference layers)
```

### Android Target Architecture

```
MapScreen.kt (orchestrator)
├── DatasetLayerManager.kt (primary dataset stack)
│   ├── ZarrVisualLayer.kt (Zarr → texture)
│   ├── ParticleLayer.kt (GPU particles) — later
│   ├── DataQueryLayer.kt (crosshair) ✅
│   ├── BreaksLayer.kt (PMTiles)
│   ├── ContourLayer.kt (PMTiles) ✅ partial
│   ├── CurrentsLayer.kt (PMTiles) ✅ partial
│   └── NumbersLayer.kt (PMTiles)
└── GlobalLayerManager.kt (static layers) — later
```

### Task 1.1: Zarr Visual Layer

**iOS Source:** `SaltyOffshore/Map/CustomShaders/ZarrShaderHost.swift`

Replace COG/TiTiler with Zarr-based rendering:

| Step | iOS Reference | Android Target | Status |
|------|---------------|----------------|--------|
| 1.1.1 | `ZarrManager` actor | `ZarrManager.kt` object | ❌ |
| 1.1.2 | `.zmetadata` parsing | `ZarrMetadata.kt` | ❌ |
| 1.1.3 | Chunk loading (HTTP range) | `ZarrChunkLoader.kt` | ❌ |
| 1.1.4 | Float16/32 decompression | `ZarrDecompressor.kt` | ❌ |
| 1.1.5 | Web Mercator transforms | `ProjectionUtils.kt` | ❌ |
| 1.1.6 | Colorscale texture | `ColorscaleTexture.kt` | ❌ |
| 1.1.7 | Mapbox custom layer | `ZarrVisualLayer.kt` | ❌ |

### Task 1.2: Complete PMTiles Layers

**iOS Sources:**
- `SaltyOffshore/Map/Primitives/NumbersLayer.swift`
- `SaltyOffshore/Map/BreaksVectorLayer.swift`
- `SaltyOffshore/Map/ContourLayerState.swift`

| Step | iOS Reference | Android Target | Status |
|------|---------------|----------------|--------|
| 1.2.1 | `ContourRenderer` | `ContourLayer.kt` | ✅ Complete |
| 1.2.2 | Line styling + labels | major/minor + labels | ✅ Complete |
| 1.2.3 | Range filtering | Filter expressions | ✅ Complete |
| 1.2.4 | `CurrentsLayer` arrows | `CurrentsLayer.kt` | ✅ Complete |
| 1.2.5 | `BreaksVectorLayer` | `BreaksLayer.kt` | ❌ Missing |
| 1.2.6 | `NumbersLayer` | `NumbersLayer.kt` | ❌ Missing |

### Task 1.3: Layer State Management

**iOS Source:** `SaltyOffshore/Types/DatasetRenderingSnapshot.swift`

| Step | iOS Reference | Android Target | Status |
|------|---------------|----------------|--------|
| 1.3.1 | `DatasetRenderingSnapshot` | `DatasetRenderingSnapshot.kt` | ✅ Complete |
| 1.3.2 | Visual/contour/arrows toggles | Layer enable flags | ✅ Complete |
| 1.3.3 | Opacity sliders | Per-layer opacity | ✅ Complete |
| 1.3.4 | Range filtering | `dataMin`/`dataMax` | ✅ Complete |
| 1.3.5 | Breaks/Numbers toggles | Layer enable flags | ❌ Missing |
| 1.3.6 | Colorscale selection | `colorscaleId` field | ❌ Later |

---

## Next Priority: BreaksLayer + NumbersLayer

These are the simplest remaining gaps - pure PMTiles vector layers like ContourLayer/CurrentsLayer.

### BreaksLayer (Thermal Fronts)

**iOS Source:** `SaltyOffshore/Map/BreaksVectorLayer.swift`

Features to implement:
- Line glow + main line (strength-based width/color)
- Temperature range labels (`"75°F → 82°F"`)
- Selection highlight (optional)
- Color ramp: weak → moderate → strong → very_strong

### NumbersLayer (Grid Values)

**iOS Source:** `SaltyOffshore/Map/Primitives/NumbersLayer.swift`

Features to implement:
- Symbol layer with numeric text
- Number formatting (decimal places per dataset type)
- Fixed spacing (200px), minZoom 7
- Black text with white halo

---

## Phase 2: Core Features (After Map Layers)

| Feature | iOS Source | Status |
|---------|------------|--------|
| Waypoints | `WaypointStore.swift` | ❌ |
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
| Foundation | ✅ Complete | Auth, preferences, API, state |
| Map Layers | 🔄 ~70% | Missing: BreaksLayer, NumbersLayer |
| Core Features | ❌ Not Started | Waypoints, Crews, Saved Maps |
| Polish | ❌ Not Started | Offline, Tracks, AI Reports |

### Visual Layer Decision

iOS uses a custom Metal shader (`ZarrShaderHost`) to render Zarr data directly on GPU. Android currently uses TiTiler-generated COG tiles via `COGVisualLayer`. Options:

1. **Keep COG/TiTiler** — Works now, server-side rendering, simpler
2. **Build Zarr shader** — Matches iOS exactly, complex OpenGL ES implementation

Recommendation: Complete BreaksLayer + NumbersLayer first, then evaluate Zarr based on performance requirements.
