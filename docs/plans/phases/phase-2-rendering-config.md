# Phase 2: Dataset Filtering & Layer Control

> **iOS is the source of truth.** Always read the Swift files before writing Android code. We won't get everything on the first pass — refer back constantly.

---

## Requirement

The user must be able to filter ocean data to a custom range, toggle individual map layers on/off, adjust opacity, change colorscales, and switch filter modes — all with real-time 60fps map feedback matching the iOS experience exactly.

---

## User Jobs

### Job 1: "I want to filter the data to highlight temperature breaks"

1. User taps the **Filter button** (funnel icon, right-side toolbar)
2. A **half-sheet** appears (fixed 240dp height, non-swipe-dismissable)
3. Header: "Range & Color" + Reset button (only if filter active) + X close
4. Top control bar: **Resampling picker** | **Filter Mode picker** (Hide/Show vs Squash) | **Colorscale button**
5. Below: **FilterGradientBar** — dual drag handles on a gradient bar with checkerboard background for filtered areas
6. Below bar: **Min/Max text fields** showing values in display units (e.g., "72.3 °F")
7. User drags lower handle to 65°F → **map updates at 60fps** during drag (direct GPU shader call, bypasses state)
8. On release → filter committed to `DatasetRenderConfig.customRange`
9. User taps Reset → filter clears, map shows full data range

**Filter modes:**
- **Squash** (default for primary): Compress selected range to full colorscale → more color detail in range
- **Hide/Show** (default for overlays): Values outside range become transparent (checkerboard)

### Job 2: "I want to turn off contour lines and reduce visual opacity"

1. User taps the **Layers button** (3D stack icon, right-side toolbar)
2. Full sheet appears with **Dataset** and **Overlays** tabs
3. Dataset tab shows collapsible sections per layer type:
   - **Visual**: toggle + opacity slider + colorscale picker
   - **Contours**: toggle + opacity slider + color picker + "Dynamic Colors" toggle (primary only)
   - **Breaks**: toggle + opacity slider
   - **Arrows**: toggle + opacity slider
   - **Particles**: toggle only (no opacity)
   - **Numbers**: toggle + opacity slider
4. User toggles Contours OFF → contour lines vanish instantly
5. User drags Visual opacity to 50% → raster becomes semi-transparent at 60fps

### Job 3: "I want to change the colorscale"

1. From Filter sheet OR Layers sheet Visual section → tap **Colorscale button** (gradient preview + chevron)
2. Colorscale picker opens showing all available colorscales as gradient previews + names
3. User taps "Viridis" → map instantly recolors, gradient bar updates, filter still active
4. Selection stored in `DatasetRenderConfig.colorscale`

---

## Acceptance Criteria

1. DatasetRenderConfig data class exists with all 24 properties matching iOS
2. `primaryDefaults(type)` and `overlayDefaults(type)` factory methods produce correct defaults per dataset type
3. FilterGradientBar has dual drag handles (18×36dp white rounded rects with grip lines)
4. Dragging handles updates GPU shader at 60fps (direct call, not through state)
5. On drag release, `customRange` commits to config, snapshot rebuilds
6. Min/max text fields accept keyboard input, convert display→API units, clamp to valid range
7. Setting range equal to full data range clears `customRange` to null
8. Filter Mode picker switches between Squash and Hide/Show
9. Checkerboard pattern (8dp cells, white 50% + gray 10% alternating) shows filtered areas in Hide/Show mode
10. All 7 layer controls match iOS (toggle + opacity + type-specific controls)
11. Contour color picker works (static color selection)
12. Dynamic contour coloring toggle switches between fixed color and data-driven coloring
13. Opacity sliders update at 60fps (5% step increments, "95%" label)
14. Colorscale picker shows all 34 colorscales in a grid with gradient previews
15. All 4 missing colorscales added (Wind, Wave Height, Wave Period, Water Clarity)
16. LayersControlSheet rewired to use DatasetRenderConfig instead of individual callbacks
17. ViewModel derives `renderingSnapshot` from `config.snapshot(dataRange)` — no direct snapshot mutation

---

## Components Inventory

### New Files to Create

| File | Type | iOS Reference |
|------|------|---------------|
| `data/DatasetRenderConfig.kt` | Data class (24 properties) | `Types/DatasetRenderConfig.swift` |
| `data/FilterMode.kt` | Enum (squash, hideShow) | `Types/FilterMode.swift` |
| `data/EntryOverride.kt` | Data class | `Types/DatasetRenderConfig.swift` |
| `ui/components/FilterGradientBar.kt` | Composable — dual handles + gradient + checkerboard | `Views/DatasetControls/Components/FilterGradientBar.swift` |
| `ui/components/DatasetFilterSheet.kt` | Composable — modal sheet container | `Views/DatasetControls/Components/DatasetFilterSheet.swift` |
| `ui/components/CheckerboardPattern.kt` | Composable — Canvas-based pattern | `Views/Components/CheckerboardPattern.swift` |
| `ui/controls/layercontrols/LayerSection.kt` | Composable — collapsible section with toggle | Wrapper pattern from iOS |
| `ui/controls/layercontrols/LayerOpacityControl.kt` | Composable — label + slider + percentage | `LayerControls/OpacityControl.swift` |

### Files to Modify

| File | Change |
|------|--------|
| `ui/theme/ColorScales.kt` | Add 4 missing colorscales (Wind, Wave Height, Wave Period, Water Clarity) |
| `data/Colorscale.kt` | Add 4 companion object entries |
| `data/DatasetRenderingSnapshot.kt` | Add `particlesEnabled`, `selectedPreset`, `contourFilterRange` |
| `viewmodel/AppViewModel.kt` | Add `primaryConfig`, derive snapshot from config, remove individual toggle methods |
| `ui/controls/LayersControlSheet.kt` | Accept DatasetRenderConfig, remove individual callbacks |
| `ui/controls/layercontrols/VisualLayerControl.kt` | Embed FilterGradientBar + colorscale picker, accept config |
| `ui/controls/layercontrols/ContourLayerControl.kt` | Add color picker + dynamic coloring toggle |
| `ui/controls/layercontrols/BreaksLayerControl.kt` | Accept config binding |
| `ui/controls/layercontrols/ArrowsLayerControl.kt` | Accept config binding |
| `ui/controls/layercontrols/NumbersLayerControl.kt` | Accept config binding |
| `ui/screen/MapScreen.kt` | Wire filter sheet trigger from right toolbar |
| `ui/components/SaltyDatasetControl.kt` | Wire filter button to DatasetFilterSheet |

---

## Missing Colorscales (Add to Android)

| Name | ID | Stops | Category | Hex Colors (from iOS) |
|------|----|----|----------|----------------------|
| Wind | `wind` | 20 | Colorful | Beaufort scale: grey→blue→cyan→green→yellow→orange→red→purple |
| Wave Height | `wave_height` | 10 | Colorful | Cyan→teal→purple→magenta→red→dark red |
| Wave Period | `wave_period` | 10 | Colorful | Cyan→green→yellow→amber→orange |
| Water Clarity | `water_clarity` | 25 | Colorful | Navy→blue→cyan→green→lime (Kd_490) |

---

## DatasetRenderConfig Properties (24 total)

| Property | Type | Primary Default | Overlay Default |
|----------|------|-----------------|-----------------|
| `datasetId` | String | per dataset | per dataset |
| `colorscale` | Colorscale? | null (uses type default) | null |
| `customRange` | ClosedRange<Double>? | null | null |
| `visualEnabled` | Boolean | true | true |
| `visualOpacity` | Double | varies by type | 0.7 |
| `contourEnabled` | Boolean | varies by type | varies |
| `contourOpacity` | Double | varies by type | varies |
| `contourColor` | Long? | null (black) | null |
| `dynamicContourColoring` | Boolean | false | false |
| `arrowsEnabled` | Boolean | varies | varies |
| `arrowsOpacity` | Double | varies | varies |
| `particlesEnabled` | Boolean | varies | varies |
| `breaksEnabled` | Boolean | false | false |
| `breaksOpacity` | Double | 1.0 | 1.0 |
| `numbersEnabled` | Boolean | false | false |
| `numbersOpacity` | Double | 1.0 | 1.0 |
| `selectedPreset` | DatasetPreset? | null | null |
| `cogStatistics` | COGStatisticsResponse? | null | null |
| `filterMode` | FilterMode | SQUASH | HIDE_SHOW |
| `selectedVariableId` | String? | null | null |
| `entryOverride` | EntryOverride? | null | null |

---

## FilterGradientBar Design Spec

```
┌──────────────────────────────────────────────┐
│  [65.3 °F]                      [82.1 °F]   │  ← Min/Max text inputs (monospace 16sp)
│                                              │
│  ☐☐☐☐│██████████████████████│☐☐☐☐☐☐         │  ← Gradient bar (height 30dp)
│      ┃                      ┃               │     Checkerboard = filtered areas
│      ┃  (active colorscale) ┃               │     Gradient = selected range
│  ☐☐☐☐│██████████████████████│☐☐☐☐☐☐         │
│     [▐]                    [▐]              │  ← Drag handles (18×36dp white, grip lines)
└──────────────────────────────────────────────┘
```

- **Handle**: 18dp wide × 36dp tall, white rounded rect (4dp), 2 vertical grip lines (1dp × 30%)
- **Checkerboard**: 8dp cells, white 50% opacity + gray 10% opacity alternating
- **Gradient**: `Brush.horizontalGradient(colorscale.colors)` clipped to handle range
- **Haptics**: Light on drag start, medium on value change, medium on release
- **Constraints**: Handles can't cross, lower ≥ min, upper ≤ max

---

## Android Best Practices Applied

| Pattern | Implementation |
|---------|---------------|
| **Dual range slider** | Material3 `RangeSlider` with custom `startThumb`/`endThumb` lambdas |
| **60fps drag updates** | Local `mutableStateOf` for slider, `snapshotFlow` debounce, direct GPU callback |
| **Keyboard toolbar** | `Modifier.imePadding()` + `AnimatedVisibility` for Reset/Clear/Done bar |
| **Checkerboard** | `Canvas` with `drawRect` loop (8dp cells) |
| **Collapsible sections** | `AnimatedVisibility` + `expandVertically()` + `fadeIn()` |
| **Color picker** | Slider-based RGB picker (no Material3 built-in) |
| **Fixed-height sheet** | `ModalBottomSheet` + `skipPartiallyExpanded = true` + `heightIn(max = 240.dp)` |
| **Config updates** | `_state.update { it.copy(config = it.config.copy(opacity = 0.5)) }` |
| **@Stable annotation** | On `DatasetRenderConfig` data class for recomposition optimization |

---

## Data Flow

```
User drags filter handle
    │
    ├─ During drag (60fps): Direct GPU call
    │   zarrManager.shaderHost.setFilterRangeDirect(min, max)
    │   repaint()
    │
    └─ On release: Commit to state
        updateState { copy(
            primaryConfig = primaryConfig.copy(customRange = newMin..newMax)
        )}
            │
            ├─ config.snapshot(dataRange) → DatasetRenderingSnapshot
            │   └─ renderRange, contourRange, contourFilterRange computed
            │
            └─ Compose recomposes ONCE → map layers update
```

---

## iOS Reference Files

| File | Path |
|------|------|
| DatasetRenderConfig | `/Users/ryan/Developer/salty-ios/SaltyOffshore/Types/DatasetRenderConfig.swift` |
| FilterGradientBar | `/Users/ryan/Developer/salty-ios/SaltyOffshore/Views/DatasetControls/Components/FilterGradientBar.swift` |
| DatasetFilterSheet | `/Users/ryan/Developer/salty-ios/SaltyOffshore/Views/DatasetControls/Components/DatasetFilterSheet.swift` |
| CheckerboardPattern | `/Users/ryan/Developer/salty-ios/SaltyOffshore/Views/Components/CheckerboardPattern.swift` |
| VisualLayerControl | `/Users/ryan/Developer/salty-ios/SaltyOffshore/Views/DatasetControls/Components/LayerControls/VisualLayerControl.swift` |
| ContoursLayerControl | `/Users/ryan/Developer/salty-ios/SaltyOffshore/Views/DatasetControls/Components/LayerControls/ContoursLayerControl.swift` |
| OpacityControl | `/Users/ryan/Developer/salty-ios/SaltyOffshore/Views/DatasetControls/Components/LayerControls/OpacityControl.swift` |
| DynamicColoringToggle | `/Users/ryan/Developer/salty-ios/SaltyOffshore/Views/DatasetControls/Components/LayerControls/DynamicColoringToggle.swift` |
| ColorScales | `/Users/ryan/Developer/salty-ios/SaltyOffshore/Theme/ColorScales.swift` |

---

## Tasks

### Task 2.1: Add Missing Colorscales
**Files:** `ui/theme/ColorScales.kt`, `data/Colorscale.kt`
Add Wind (20 stops), Wave Height (10), Wave Period (10), Water Clarity (25) from iOS ColorScales.swift.

### Task 2.2: Create DatasetRenderConfig + FilterMode + EntryOverride
**Files:** `data/DatasetRenderConfig.kt`, `data/FilterMode.kt`, `data/EntryOverride.kt`
Port all 24 properties, factory methods, `snapshot()`, `activeRange()`, `clearFilter()`.

### Task 2.3: Update DatasetRenderingSnapshot
**File:** `data/DatasetRenderingSnapshot.kt`
Add `particlesEnabled`, `selectedPreset`, `contourFilterRange` computed property.

### Task 2.4: Wire ViewModel through Config
**File:** `viewmodel/AppViewModel.kt`
Add `primaryConfig` to `MapScreenState`. Derive `renderingSnapshot` from `config.snapshot(dataRange)`. Remove individual toggle/opacity methods — replace with `updatePrimaryConfig()`.

### Task 2.5: Create CheckerboardPattern
**File:** `ui/components/CheckerboardPattern.kt`
Canvas-based composable with `small` (3dp) and `large` (8dp) cell sizes.

### Task 2.6: Create LayerSection + LayerOpacityControl
**Files:** `ui/controls/layercontrols/LayerSection.kt`, `ui/controls/layercontrols/LayerOpacityControl.kt`
Collapsible section with toggle header + AnimatedVisibility content. Opacity slider with label + percentage.

### Task 2.7: Create FilterGradientBar
**File:** `ui/components/FilterGradientBar.kt`
Dual drag handles, gradient bar, checkerboard background, min/max text inputs. 60fps GPU updates during drag.

### Task 2.8: Create DatasetFilterSheet
**File:** `ui/components/DatasetFilterSheet.kt`
Modal sheet (240dp fixed height). Header with Reset/Close. Top controls bar (resampling, filter mode, colorscale). Embedded FilterGradientBar.

### Task 2.9: Rewrite Layer Controls to Accept Config
**Files:** All 6 layer control files in `ui/controls/layercontrols/`
Change from snapshot+callbacks to config binding pattern. Add contour color picker + dynamic coloring toggle to ContoursLayerControl.

### Task 2.10: Wire LayersControlSheet through Config
**File:** `ui/controls/LayersControlSheet.kt`
Accept DatasetRenderConfig. Remove individual toggle/opacity callback parameters.

### Task 2.11: Wire Filter Sheet to MapScreen
**File:** `ui/screen/MapScreen.kt`
Add filter button to right toolbar. Wire DatasetFilterSheet modal trigger.
