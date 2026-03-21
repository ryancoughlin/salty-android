# Phase 3: Presets

> **iOS is the source of truth.** Always read the Swift files before writing Android code.

## Requirement

Port the preset quick-actions system — static, dynamic, and micro-break filter presets that let users one-tap highlight fishing-relevant ocean data ranges (temperature breaks, river plumes, prime zones) — driven by COG statistics fetched from the tiler service.

## UX Description

### Quick Actions Bar

A horizontally-scrollable chip bar sits above the bottom map controls (below the map, above the dataset control panel). Height is 44dp. Horizontal padding 8dp. Edge-fade mask (3% transparent on each side) hides clip edges during scroll.

The bar composes three optional sections separated by thin vertical dividers (1dp wide, 24dp tall, primary color at 20% opacity):

1. **Variables** (first) — only shown when dataset has multiple variables (e.g., SST has Temperature + Gradient)
2. **Depth** (second) — only shown when dataset has multiple depths and not in playback mode
3. **Presets** (last) — only shown when `PresetConfiguration` supports the active dataset type AND user is online

### Preset Chips

Each preset renders as a rounded-rectangle chip (cornerRadius 8dp, horizontal padding 12dp, vertical padding 8dp):

- **Unselected**: `ultraThinMaterial` background (translucent blur), primary text color, subtle shadow
- **Selected**: solid white background, black text, shadow. Shows calculated range text in parentheses (smaller font, 11sp) for static presets. Dynamic presets already embed range in their label.
- **Disabled** (break presets with no crosshair value): secondary text at 50% opacity

Chips animate in with staggered opacity spring (0.03s delay per chip). Selection toggles with spring animation (response 0.5, damping 0.9).

### Tap Behavior

- **Toggle on**: Calculates range from preset type, sets `config.customRange` and `config.selectedPreset`. Map re-renders with filtered range.
- **Toggle off**: Tapping the already-selected preset clears `customRange` and `selectedPreset`. Map returns to full data range.
- **Break preset without crosshair value**: Shows toast — "A value must be shown in the crosshairs to use this preset" with warning haptic, 3s duration. Does not apply.
- **Pro gating**: In preview mode (non-premium), chips appear dimmed. Tapping triggers upgrade banner instead of applying.

### Dynamic Presets Loading

When a dataset supports dynamic presets but COG statistics haven't loaded yet, a "Loading..." indicator chip appears (spinner + text, same chip styling). Once stats arrive, dynamic preset chips replace the loading indicator.

### Variable Chips

Same visual style as preset chips. Tapping selects a dataset variable (e.g., Temperature vs. Gradient for SST). Selected chip is white with black text; unselected is translucent blur.

## Acceptance Criteria

1. `PresetType` sealed class exists with three variants: `FixedRange(min, max)`, `MicroBreak`, `CurrentValueRange(offset)`.
2. `DatasetPreset` data class has `id`, `label`, `type: PresetType`, `datasetType: DatasetType` and `calculateRange(currentValue, valueRange)` method matching iOS logic exactly.
3. `PresetConfiguration` maps each supported `DatasetType` to its static presets, `supportsDynamicPresets` flag, and optional `dynamicBuilder` lambda.
4. SST has 2 static presets (0.5°F Break, 1°F Break) plus 3 dynamic presets (Prime Zone, Warm Water, Thermal Fronts).
5. MLD has 5 static presets (Surface, Shallow, Mid, Deep, Very Deep) and no dynamic presets.
6. Chlorophyll has no static presets, supports dynamic stats loading (no preset builder).
7. Currents has 4 dynamic presets (Current Edges, Slack Water, Moderate Current, Strong Current).
8. Salinity has 4 dynamic presets (Salinity Edges, Blue Water, Transition, River Plume).
9. FSLE has no static presets, supports dynamic stats loading (no preset builder).
10. `COGBandStatistics` data class has all fields: min, max, mean, std, median, majority, minority, percentile_2, percentile_98, count, sum, unique, valid_percent, masked_pixels, valid_pixels.
11. `COGStatisticsResponse` decodes dynamic band names from JSON (dictionary of band name → stats). Provides convenience accessors (`b1`, `sla`, `u`, `v`, etc.) and `primaryBandStatistics()` with fallback order: preferred band → b1 → sla → first.
12. `COGStatisticsService` fetches from `{titilerBaseURL}/cog/statistics?url={encodedCogURL}&max_size=1024` and decodes response.
13. Statistics fetching is debounced (waits for timeline scrubbing to stop) and cached by COG URL in `DatasetActor`.
14. `DatasetRenderConfig.selectedPreset` and `cogStatistics` fields exist (from Phase 2).
15. `applyPreset()` on ViewModel toggles: if same preset tapped, clears `customRange` and `selectedPreset`; otherwise calculates range and sets both.
16. Micro-break presets use dataset-specific offsets: SST ±0.25°F, chlorophyll ±0.01 mg/m³, water_clarity ±0.001 m⁻¹, default ±0.01.
17. Quick Actions Bar renders horizontally scrollable with edge-fade mask, 44dp height, sections separated by dividers.
18. Break preset chips show disabled state when crosshair has no value; tap shows toast.
19. Dynamic preset loading indicator appears while COG stats are in flight, disappears when stats arrive.
20. Preset range text displays with dataset-appropriate format: SST "%.1f°", MLD "%.0fm", chlorophyll "%.2fmg". Dynamic presets skip range text (already in label).
21. Variable chips render for multi-variable datasets, with selected/unselected styling matching preset chips.

## Preset Definitions

### SST (Sea Surface Temperature)

| Preset | ID | Type | Calculation | Display |
|---|---|---|---|---|
| 0.5°F Break | `micro_break` | MicroBreak | crosshairValue ± 0.25°F | "0.5°F Break (72.1-72.6°)" |
| 1°F Break | `micro_break_1deg` | CurrentValueRange(0.5) | crosshairValue ± 0.5°F | "1°F Break (71.8-72.8°)" |
| Prime Zone | `dynamic_prime_zone` | FixedRange | mean ± std | "Prime Zone (68±6°F)" |
| Warm Water | `dynamic_warm_water` | FixedRange | mean → p98 | "Warm Water (85°F)" |
| Thermal Fronts | `dynamic_temperature_breaks` | FixedRange | optimal break algorithm (see below) | "Thermal Fronts (66-70°F)" |

**Thermal Fronts algorithm:**
1. Clip extremes: safeMin = p2, safeMax = p98
2. Target spread = clamp(std × 0.5, min=2.0, max=6.0)
3. Center = median (fallback: mean)
4. Range = center ± halfSpread, clamped to [safeMin, safeMax]
5. If resulting range < 2°F, fallback to center ± 2.0

### MLD (Mixed Layer Depth)

| Preset | ID | Type | Range | Display |
|---|---|---|---|---|
| Surface | `surface` | FixedRange | 0–15m | "Surface (0-15m)" |
| Shallow | `shallow` | FixedRange | 15–30m | "Shallow (15-30m)" |
| Mid | `mid` | FixedRange | 30–60m | "Mid (30-60m)" |
| Deep | `deep` | FixedRange | 60–100m | "Deep (60-100m)" |
| Very Deep | `very_deep` | FixedRange | 100–200m | "Very Deep (100-200m)" |

### Chlorophyll

No presets. Supports dynamic stats loading only (used for auto-ranging, not preset chips).

### Currents

| Preset | ID | Type | Calculation | Display |
|---|---|---|---|---|
| Current Edges | `dynamic_current_edges` | FixedRange | mean ± (std × 0.5) | "Current Edges (0.3-0.8 kts)" |
| Slack Water | `dynamic_slack_current` | FixedRange | min → p2 | "Slack Water (<0.1 kts)" |
| Moderate Current | `dynamic_moderate_current` | FixedRange | mean ± std | "Moderate Current (0.5±0.3 kts)" |
| Strong Current | `dynamic_strong_current` | FixedRange | p98 → max | "Strong Current (>1.2 kts)" |

### Salinity

| Preset | ID | Type | Calculation | Display |
|---|---|---|---|---|
| Salinity Edges | `dynamic_salinity_edges` | FixedRange | mean ± (std × 0.5) | "Salinity Edges (34.2-35.1 PSU)" |
| Blue Water | `dynamic_blue_water` | FixedRange | p98 → max | "Blue Water (>36.2 PSU)" |
| Transition | `dynamic_transition_zone` | FixedRange | mean ± std | "Transition (34.8±0.9 PSU)" |
| River Plume | `dynamic_river_plume` | FixedRange | min → p2 | "River Plume (<31.5 PSU)" |

### FSLE

No presets. Supports dynamic stats loading only.

## iOS Reference Files

| File | Purpose |
|---|---|
| `SaltyOffshore/Types/PresetTypes.swift` | `DatasetPreset`, `PresetType`, `PresetConfiguration`, `DynamicPresetBuilder` |
| `SaltyOffshore/Features/Presets/Views/PresetQuickActions.swift` | Preset chip UI, tap handling, break-preset gating, loading indicator |
| `SaltyOffshore/Features/Presets/Views/VariableQuickActions.swift` | Variable selection chips |
| `SaltyOffshore/Features/Presets/Types/COGStatistics.swift` | `COGStatisticsResponse`, `COGBandStatistics` |
| `SaltyOffshore/Features/Presets/Services/COGStatisticsService.swift` | Tiler statistics fetching |
| `SaltyOffshore/Services/DatasetActor.swift` | Statistics caching, debounced loading |
| `SaltyOffshore/Stores/DatasetStore.swift` | `applyPreset()`, `configureWithStatistics()`, `loadStatisticsDebounced()` |
| `SaltyOffshore/Types/DatasetRenderConfig.swift` | `selectedPreset`, `cogStatistics` fields, `clearFilter()` |
| `SaltyOffshore/Map/Controls/QuickActionsBar.swift` | Container bar composing variables + depth + presets |
| `SaltyOffshore/Map/Controls/MapControlsContainer.swift` | Wiring: showPresets gate, onPresetSelected callback |

## Data Flow

```
User taps preset chip
    → handlePresetTap(preset)
        → if break preset && crosshair empty → toast, return
        → onPresetSelected(preset, crosshairValue)
            → DatasetStore.applyPreset(preset, currentValue, entry)
                → if same preset already selected → clear customRange + selectedPreset
                → else → preset.calculateRange(currentValue, valueRange)
                    → config.customRange = presetRange
                    → config.selectedPreset = preset
                → updatePrimaryConfig(config, animated: true)
                    → config.snapshot(dataRange, resamplingMethod)
                        → snapshot.isFilterActive = customRange != null
                        → snapshot.filterMin / filterMax = effectiveRange
                    → GPU shader re-renders with new filter bounds
```

**Statistics loading flow:**
```
Timeline entry changes (showEntry)
    → loadStatisticsDebounced(entry)
        → cancel previous task
        → sleep(debounceMs)
        → DatasetActor.loadStatistics(cogURL)
            → check cache → hit → return
            → COGStatisticsService.fetchStatistics(cogURL)
                → GET {titilerBaseURL}/cog/statistics?url={url}&max_size=1024
                → decode COGStatisticsResponse
            → cache result
        → configureWithStatistics(stats)
            → primaryConfig.cogStatistics = stats
            → PresetQuickActions rebuilds with dynamic presets from builder(stats.b1)
```

## Tasks

1. **Create preset type definitions** — `app/.../models/PresetTypes.kt`
   - `PresetType` sealed class: `FixedRange`, `MicroBreak`, `CurrentValueRange`
   - `DatasetPreset` data class with `calculateRange()` method
   - Micro-break offsets per dataset type matching iOS exactly

2. **Create preset configuration registry** — `app/.../models/PresetConfiguration.kt`
   - `PresetConfiguration` data class with `staticPresets`, `supportsDynamicPresets`, `dynamicBuilder`
   - Static `configurations` map for all 6 dataset types
   - `supportsPresets()` and `configuration()` lookup methods

3. **Create dynamic preset builders** — `app/.../models/DynamicPresetBuilder.kt`
   - `buildSSTPresets(stats)`: Prime Zone (mean±std), Warm Water (mean→p98), Thermal Fronts (optimal break algorithm)
   - `buildCurrentsPresets(stats)`: Current Edges (mean±0.5std), Slack Water (min→p2), Moderate Current (mean±std), Strong Current (p98→max)
   - `buildSalinityPresets(stats)`: Salinity Edges (mean±0.5std), Blue Water (p98→max), Transition (mean±std), River Plume (min→p2)
   - Thermal Fronts `calculateOptimalBreakRange()` — exact port of iOS algorithm

4. **Create COG statistics types** — `app/.../models/COGStatistics.kt`
   - `COGBandStatistics` data class with all 14 fields, `@Serializable`
   - `COGStatisticsResponse` with dynamic band name deserialization (custom `JsonObject` decoder)
   - Convenience accessors: `b1`, `sla`, `u`, `v`, `primaryBandStatistics()`

5. **Create COG statistics service** — `app/.../services/COGStatisticsService.kt`
   - Ktor GET to `{titilerBaseURL}/cog/statistics?url={encoded}&max_size=1024`
   - Decode `COGStatisticsResponse`
   - Error types: `InvalidURL`, `HttpError`, `DecodingFailed`, `NoDataAvailable`

6. **Add statistics caching to DatasetActor** — `app/.../services/DatasetActor.kt`
   - In-memory `statisticsCache: MutableMap<String, COGStatisticsResponse>`
   - `suspend fun loadStatistics(cogURL: String): COGStatisticsResponse?` with cache-first

7. **Add debounced statistics loading to ViewModel** — `app/.../viewmodel/SaltyViewModel.kt`
   - `loadStatisticsDebounced(entry)` — cancel-previous-job pattern with delay
   - Gate on `PresetConfiguration.supportsPresets(datasetType)` and non-empty COG URL
   - Call on every `showEntry()` transition
   - `configureWithStatistics()` sets `primaryConfig.cogStatistics`

8. **Add `applyPreset()` to ViewModel** — `app/.../viewmodel/SaltyViewModel.kt`
   - Toggle logic: same preset → clear, new preset → calculate and set
   - Reads `entry.range(for: dataset, config)` for valueRange
   - Sets `config.customRange` and `config.selectedPreset`
   - Calls `updatePrimaryConfig(config, animated = true)`

9. **Create PresetChip composable** — `app/.../ui/controls/PresetChip.kt`
   - Selected/unselected/disabled states with matching iOS styling
   - Range text calculation with dataset-specific formatting
   - Spring animation on selection toggle
   - Staggered entrance animation (0.03s delay per index)

10. **Create PresetQuickActions composable** — `app/.../ui/controls/PresetQuickActions.kt`
    - Combines static + dynamic presets from `PresetConfiguration`
    - Dynamic loading indicator while `cogStatistics == null`
    - Break preset gating: check crosshair value, show toast if null

11. **Create VariableQuickActions composable** — `app/.../ui/controls/VariableQuickActions.kt`
    - Chips for each `DatasetVariable` on the active dataset
    - Selected/unselected styling matching preset chips
    - `onSelected` callback to ViewModel

12. **Create QuickActionsBar composable** — `app/.../ui/controls/QuickActionsBar.kt`
    - Horizontal `LazyRow` with edge-fade mask, 44dp height
    - Composes: VariableQuickActions | Divider | DepthQuickAction | Divider | PresetQuickActions
    - Sections conditionally shown based on dataset capabilities + online state

13. **Wire QuickActionsBar into MapControlsContainer** — `app/.../ui/map/MapControlsContainer.kt`
    - `showPresets = supportsPresets(dataset.type) && !isOffline`
    - `showVariables = dataset.hasMultipleVariables`
    - `onPresetSelected → viewModel.applyPreset(preset, currentValue, entry)`
    - Pro gating: `isPreviewMode = !isPremium`, trigger upgrade on tap

14. **Add overlay preset support** — `app/.../viewmodel/SaltyViewModel.kt`
    - `applyOverlayPreset(type, preset, currentValue, valueRange)` with same toggle logic
    - `storeOverlayCOGStatistics(type, stats)` for overlay dynamic presets
    - Load overlay COG stats when overlay entry changes
