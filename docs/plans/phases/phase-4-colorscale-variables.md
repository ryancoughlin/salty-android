# Phase 4: Colorscale Picker + Variable Selector + Dataset Control Modes

> **iOS is the source of truth.**

## Requirement

Port the colorscale chip (with picker sheet), variable selector chips, and collapsed/expanded dataset control modes from iOS to Android, giving users the ability to customize visualization and switch between dataset variables without leaving the map.

## UX Description

### Colorscale Chip
- A compact chip sits in the QuickActionsBar showing an **angular gradient circle** (14x14pt) plus the colorscale name (13pt medium).
- **Default state:** glass material background (ultraThinMaterial), primary text color.
- **Selected state** (user picked non-default): white background, black text, spring animation.
- **Preview mode** (non-premium): reduced opacity material, tap triggers upgrade prompt.
- Tapping opens a **ColorscalePickerSheet** — a scrollable grid (3 columns) organized into sections: "Single Color", "Neutral", "Colored".
- Each swatch is a 70pt-tall rounded rectangle filled with LinearGradient, overlaid with a black capsule containing the name. Default colorscale shows a "Default" badge. Selected swatch has a white 3pt border and checkmark circle.
- Selecting the default colorscale passes `nil` (clears override). Selecting any other stores the colorscale on `DatasetRenderConfig.colorscale`. Sheet auto-dismisses on selection.

### Variable Selector
- Chips appear in the QuickActionsBar **only when the dataset has multiple visible variables** (e.g., SST has "Temperature" + "Gradient").
- Each chip shows `variable.displayName` (13pt medium). Selected chip: white background, black text. Unselected: glass material.
- Tapping a variable updates `DatasetRenderConfig.selectedVariableId`, which changes the Zarr variable loaded, the range key for the gradient bar, and optionally overrides the colorscale/scaleMode.
- Variables section appears first in the QuickActionsBar, separated from depth/presets by a vertical divider.

### Expanded/Collapsed Dataset Control
- **Expanded** (`isCollapsed = false`): Full `PrimaryDatasetPage` with dataset name, buttons, gradient bar, timeline, and overlay chip bar below.
- **Collapsed** (`isCollapsed = true`): Compact bar showing only overlay chips (scrollable) plus a chevron-up button to re-expand.
- Transition uses spring animation (response 0.3, damping 0.8).
- **`isExpanded`** is a separate flag for the dataset selector overlay (not the same as collapsed). Business rule: collapsing also closes the picker.

## Acceptance Criteria

1. ColorscaleChip displays the active colorscale name and angular gradient circle matching the current colorscale colors.
2. Tapping ColorscaleChip opens ColorscalePickerSheet as a bottom sheet or navigation destination.
3. Picker sheet shows 3 sections (Single Color, Neutral, Colored) in a 3-column grid of gradient swatches.
4. Selecting a colorscale updates the gradient bar, map rendering, and chip display immediately.
5. Selecting the dataset's default colorscale clears the override (stores null).
6. Default swatch shows a "Default" badge; selected swatch shows white border + checkmark.
7. Variable chips appear only for datasets with >1 visible variable (e.g., SST: Temperature/Gradient).
8. Tapping a variable chip switches the Zarr variable, updates range, and applies any variable-specific colorscale/scaleMode override.
9. Variable chips use the same glass material / white-selected styling as preset chips.
10. QuickActionsBar sections are ordered: variables | divider | depth | divider | presets.
11. Collapsed mode shows only overlay chip bar + expand button.
12. Expanded mode shows full PrimaryDatasetPage + overlay chip bar.
13. Collapse/expand transition animates with spring(response=0.3, damping=0.8).
14. Collapsing also sets `isExpanded = false` (closes dataset picker if open).
15. Wind, waveHeight, and wavePeriod colorscales are added to Android `ColorScales.kt` and `Colorscale.kt`.

## Available Colorscales

### Colorful (multi-hue, primary layers)
| ID | Name | Stops | Description |
|----|------|-------|-------------|
| `sst_high_contrast` | SST | 12 | Navy -> blue -> cyan -> green -> yellow -> orange -> red -> brown |
| `salty_vibes` | Salty Vibes | 12 | Purple cold-end -> violet -> blue -> cyan -> green -> yellow -> red -> brown |
| `thermal` | Thermal | 12 | Black -> dark red -> red -> orange -> yellow -> white |
| `chlorophyll` | Chlorophyll | 29 | Magenta -> indigo -> blue -> cyan -> teal -> green -> yellow -> orange (log10 scale) |
| `bloom` | Bloom | 28 | Purple/magenta -> blue -> cyan -> teal/green -> yellow -> orange -> red-brown |
| `currents` | Currents | 8 | Deep purple -> purple -> blue -> cyan -> teal -> amber -> orange -> red |
| `spectral` | Spectral | 11 | Red -> orange -> yellow -> green -> teal -> blue -> purple |
| `bwr` | RdBu | 11 | Blue (negative) -> white (zero) -> red (positive), diverging |
| `cascade` | Cascade | 12 | Indigo -> blue -> cyan -> teal -> green -> lime -> amber -> orange -> red |
| `flow` | Flow | 11 | Deep indigo -> blue -> cyan -> teal -> green -> yellow |
| `wind` | Wind | 20 | Slate -> blue -> cyan -> teal -> green -> lime -> yellow -> orange -> red -> purple |
| `wave_height` | Wave Height | 10 | Cyan -> teal -> purple -> pink -> red -> dark red |
| `wave_period` | Wave Period | 10 | Light cyan -> cyan -> teal -> green -> lime -> amber -> orange |
| `boundary_fire` | Boundary Fire | 9 | Purple -> red -> orange -> gold -> green-yellow -> blue -> midnight |
| `magnitude` | Magnitude | 19 | Navy -> indigo -> lavender -> teal-green -> green -> orange -> red (FSLE) |
| `viridis` | Viridis | 10 | Purple -> blue -> teal -> green -> yellow |

### Single Color (monochromatic, overlays)
| ID | Name | Description |
|----|------|-------------|
| `greens` | Greens | White -> light green -> dark green |
| `blues` | Blues | White -> light blue -> dark blue |
| `purple` | Purple | Light purple -> dark purple |
| `magenta` | Magenta | Light magenta -> dark magenta |
| `cyan` | Cyan | Light cyan -> dark teal |
| `yellow` | Yellow | Light yellow -> dark gold |
| `lime` | Lime | Light lime -> dark lime-green |

### Neutral (greyscale, overlays)
| ID | Name | Description |
|----|------|-------------|
| `greys` | Greys | White -> black |
| `bone` | Bone | Black -> off-white/bone |

## iOS Reference Files

| File | Purpose |
|------|---------|
| `Views/DatasetControls/Components/ColorscaleChip.swift` | Compact chip with angular gradient + name |
| `Views/DatasetControls/Components/ColorscaleComponents.swift` | Reusable `ColorPreview` (angular gradient circle) |
| `Views/Components/ColorscalePickerSheet.swift` | 3-column grid picker with swatch selection |
| `Views/Components/ColorscalePickerButton.swift` | Navigation link wrapper opening picker |
| `Types/ColorscaleTypes.swift` | `Colorscale` struct, category enum, all static instances |
| `Theme/ColorScales.swift` | Raw hex arrays for all colorscales |
| `Features/Presets/Views/VariableQuickActions.swift` | Variable chip row (ForEach + VariableChipButton) |
| `Map/Controls/QuickActionsBar.swift` | Composable bar: variables -> depth -> presets |
| `Views/DatasetControls/DatasetControl.swift` | Parent: switches collapsed vs expanded |
| `Views/DatasetControls/Components/ExpandedDatasetView.swift` | Full view: PrimaryDatasetPage + overlay chips |
| `Views/DatasetControls/Components/CollapsedDatasetView.swift` | Compact: overlay chips + expand button |
| `ViewModels/DatasetControlState.swift` | `isCollapsed`, `isExpanded`, spring animation constant |
| `Types/DatasetRenderConfig.swift` | `selectedVariableId`, `colorscale`, `selectedVariable(for:)` |
| `Models/DatasetVariable.swift` | Variable definition with colorscale/scaleMode overrides |

## Data Flow

```
User taps ColorscaleChip
  -> Opens ColorscalePickerSheet (BottomSheet)
  -> User taps swatch
  -> onSelect(colorscale) fires
  -> ViewModel updates DatasetRenderConfig.colorscale (or null for default)
  -> GradientScaleBar re-renders with new colors
  -> GPU shader receives new colorscale via renderingSnapshot
  -> Sheet auto-dismisses

User taps Variable chip
  -> onVariableSelected(variable) fires
  -> ViewModel updates DatasetRenderConfig.selectedVariableId
  -> selectedVariable(for:) resolves the active DatasetVariable
  -> Zarr loader switches to variable.zarrVariableName
  -> Range lookup switches to variable.rangeKey
  -> If variable.colorscale != null, it overrides the display colorscale
  -> If variable.scaleMode != null, it overrides the scale mode
  -> GradientScaleBar + shader update

User taps collapse button (chevron-down)
  -> DatasetControlState.isCollapsed = true
  -> Spring animation transitions to CollapsedDatasetView
  -> Also sets isExpanded = false

User taps expand button (chevron-up)
  -> DatasetControlState.isCollapsed = false
  -> Spring animation transitions to ExpandedDatasetView
```

## Tasks

### 1. Add missing colorscales to ColorScales.kt
**File:** `app/src/main/java/com/example/saltyoffshore/ui/theme/ColorScales.kt`
- Add `wind` (20 stops), `waveHeight` (10 stops), `wavePeriod` (10 stops) hex arrays matching iOS `ColorScales.swift`.

### 2. Add missing Colorscale instances
**File:** `app/src/main/java/com/example/saltyoffshore/data/Colorscale.kt`
- Add `WIND`, `WAVE_HEIGHT`, `WAVE_PERIOD` companion object entries.
- Add them to `ALL` list in the correct position.

### 3. Add DatasetControlState
**File:** `app/src/main/java/com/example/saltyoffshore/viewmodel/DatasetControlState.kt` (new)
- Two `mutableStateOf` booleans: `isCollapsed`, `isExpanded`.
- Companion object with `animation` spring spec (response=0.3, damping=0.8).
- Owned by `AppViewModel` or hoisted to `MapScreen`.

### 4. Add selectedVariableId + colorscale override to rendering state
**File:** `app/src/main/java/com/example/saltyoffshore/viewmodel/AppViewModel.kt`
- Add `selectedVariableId: String?` state field.
- Add `selectedColorscale: Colorscale?` state field (user override, null = dataset default).
- Add `fun selectVariable(variable: DatasetVariable)` — updates selectedVariableId, triggers re-render.
- Add `fun selectColorscale(colorscale: Colorscale?)` — updates selectedColorscale, triggers re-render.
- Wire `selectedVariable(for:)` logic matching iOS `DatasetRenderConfig.selectedVariable(for:)`.

### 5. Build ColorscaleChip composable
**File:** `app/src/main/java/com/example/saltyoffshore/ui/components/ColorscaleChip.kt` (new)
- Angular gradient circle (14dp) using `Brush.sweepGradient`.
- Colorscale name text (13sp medium).
- Glass material background (default) vs white (selected) with spring animation.
- Preview mode with reduced opacity + upgrade trigger.

### 6. Rewrite ColorscalePickerSheet as swatch grid
**File:** `app/src/main/java/com/example/saltyoffshore/ui/components/ColorscalePicker.kt` (rewrite)
- Replace current `LazyColumn` row-based picker with 3-column `LazyVerticalGrid` of swatches.
- Each swatch: 70dp tall, rounded rect with LinearGradient fill, black capsule name overlay.
- Default badge, checkmark + white border for selected.
- Three sections: "Single Color", "Neutral", "Colored" with section headers.
- Auto-dismiss on selection.

### 7. Build VariableChipButton composable
**File:** `app/src/main/java/com/example/saltyoffshore/ui/components/VariableChipButton.kt` (new)
- Text-only chip matching preset chip styling.
- White background when selected, glass material when not.
- Spring animation on selection change.

### 8. Build QuickActionsBar composable
**File:** `app/src/main/java/com/example/saltyoffshore/ui/components/QuickActionsBar.kt` (new)
- Horizontal scroll row, 44dp height.
- Sections: variables (conditional) | divider | depth (conditional) | divider | presets (conditional).
- Vertical divider: 1dp wide, 24dp tall, primary @ 0.2 opacity.
- Edge fade mask on horizontal edges.

### 9. Build CollapsedDatasetView composable
**File:** `app/src/main/java/com/example/saltyoffshore/ui/components/CollapsedDatasetView.kt` (new)
- Row: overlay chip bar (scrollable) + chevron-up ControlButton.
- Padding: horizontal 16dp, top 12dp, bottom 8dp.

### 10. Refactor SaltyDatasetControl for expand/collapse
**File:** `app/src/main/java/com/example/saltyoffshore/ui/components/SaltyDatasetControl.kt`
- Accept `DatasetControlState` parameter.
- When `isCollapsed`, render `CollapsedDatasetView`.
- When not collapsed, render current expanded layout (rename to `ExpandedDatasetView`).
- Animate transition with `AnimatedContent` using spring spec.
- Wire collapse button (existing chevron-down) to set `isCollapsed = true`.

### 11. Wire QuickActionsBar into MapScreen
**File:** `app/src/main/java/com/example/saltyoffshore/ui/screen/MapScreen.kt`
- Position QuickActionsBar above dataset control.
- Pass variable/depth/preset state from ViewModel.
- Show variable chips only when `dataset.variables.filter { it.isVisible }.size > 1`.

## Implementation Status (as of 2026-03-22)

### Done (completed in Phase 3)
- [x] **VariableQuickActions** — chips for multi-variable datasets, white alpha styling
- [x] **QuickActionsBar** — wired with variables | divider | depth | divider | presets sections
- [x] **AppViewModel.selectVariable()** — switches selectedVariableId, clears filter
- [x] **DatasetRenderConfig.selectedVariableId** — already existed, now wired to UI
- [x] **MapScreen placement** — QuickActionsBar above SaltyDatasetControl

### Remaining
- [ ] **ColorscaleChip** — angular gradient circle + name chip in QuickActionsBar
- [ ] **ColorscalePickerSheet** — 3-column swatch grid (rewrite current row-based picker)
- [ ] **AppViewModel.selectColorscale()** — colorscale override field + mutator
- [ ] **CollapsedDatasetView** — compact overlay chip bar + expand button
- [ ] **SaltyDatasetControl expand/collapse** — AnimatedContent with spring transition
- [ ] **Missing colorscales** — wind, waveHeight, wavePeriod hex arrays + Colorscale instances

---

## Current Android State

### Can Reuse
- **`Colorscale.kt`** — Data model is complete and matches iOS. Just needs WIND/WAVE_HEIGHT/WAVE_PERIOD additions.
- **`ColorscaleCategory.kt`** — Already matches iOS categories (SINGLE_COLOR, NEUTRAL, COLORFUL).
- **`ColorScales.kt`** — Most hex arrays present. Needs wind/wave additions.
- **`ColorscaleGradient.kt`** — Existing gradient rendering component for linear previews.
- **`DatasetVariable.kt`** — Already ported with all fields including colorscale/scaleMode overrides.
- **`ControlButton.kt`** — Existing icon button component reusable for collapse/expand.
- **`GradientScaleBar.kt`** — Existing gradient bar, will re-render when colorscale changes.

### Needs Rewrite
- **`ColorscalePicker.kt`** — Current implementation is a simple row-based LazyColumn. Needs complete rewrite to 3-column swatch grid matching iOS `ColorscalePickerSheet`.

### Needs Creation
- **`DatasetControlState`** — No equivalent exists on Android.
- **`ColorscaleChip`** — No chip component exists (only the full picker).
- ~~**`VariableChipButton`**~~ — Done: `VariableQuickActions.kt` created in Phase 3.
- ~~**`QuickActionsBar`**~~ — Done: populated in Phase 3 with all three sections.
- **`CollapsedDatasetView`** — No collapsed mode exists.
- ~~**ViewModel variable selection**~~ — Done: `selectVariable()` added in Phase 3. Colorscale selection still needed.

### Needs Modification
- **`SaltyDatasetControl.kt`** — Currently always-expanded. Needs collapse/expand branching + DatasetControlState parameter.
- **`AppViewModel.kt`** — Needs colorscale override field, variable selection field, and corresponding mutators.
- **`MapScreen.kt`** — Needs QuickActionsBar placement and DatasetControlState wiring.
