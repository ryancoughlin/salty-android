# iOS → Android Parity: Progress Tracker

> Updated as we go. Check items off, note issues, flag things to revisit.

## Wave 1: Phase 1 — App Shell + DI + Navigation

### Status: CODE COMPLETE — AWAITING BUILD CHECKPOINT

### Tasks
- [x] 1.1: SaltyApplication subclass + manifest
- [x] 1.2: AccountButton (gradient circle, press-scale animation)
- [x] 1.3: TopBar (three-slot Row, appear animation)
- [x] 1.4: AccountHubSheet (all 11 sections, 2-step delete)
- [x] 1.5: LoginScreen refactor (email expand animation)
- [x] 1.6: FTUX region selection (blocking dialog, grouped list)
- [x] 1.7: AppViewModel additions (auth state, FTUX, foreground refresh)
- [x] 1.8: MainActivity rewrite (auth state machine, wiring)
- [x] 1.9: Delete SettingsScreen, remove gear icon from MapScreen
- [x] 1.10: Final wiring + bug fix (RefreshFailure handling)

### Known Issues / Revisit Later
- FTUX region thumbnails use placeholder text (Coil not yet added as dependency)
- Hilt DI deferred — using direct ViewModel construction for now (works, can add Hilt later)
- RegionStore/DatasetStore extraction deferred — AppViewModel still monolithic (Phase 2 will address)
- Offline mode indicator not yet implemented (placeholder row in AccountHub)

### Worktree
- Branch: `worktree-agent-aebef4ae`
- Path: `.claude/worktrees/agent-aebef4ae`
- 10 commits, 1714 lines added, 374 removed

### Build Checkpoint (USER: test these)
- [ ] App builds and runs
- [ ] Auth flow works (login → map → signout → login)
- [ ] "Continue with email" expands the form on tap
- [ ] Map loads regions and datasets as before
- [ ] Top bar shows with gradient account button (top-right)
- [ ] Account button has press-scale animation
- [ ] Account hub opens as bottom sheet with all sections
- [ ] Units section in account hub works (temperature, depth, distance, speed)
- [ ] Sign out from account hub returns to login
- [ ] Delete account shows two confirmation dialogs
- [ ] FTUX shows on first launch (no saved region)
- [ ] Selecting a region in FTUX dismisses dialog and loads map

---

## Wave 2: Phases 2 + 7 + 8 (parallel)

### Status: WAITING (needs Wave 1)

### Phase 2: Rendering Config Pipeline
- [ ] DatasetRenderConfig type
- [ ] Wire DatasetStore through config
- [ ] Inline FilterGradientBar (replaces FilterRangeSheet)
- [ ] Wire layer controls to config
- [ ] Status banners (stale, composite, low coverage)

### Phase 7: Measurement Tool
- [ ] MeasurementTypes
- [ ] MeasureMode + MeasurementManager
- [ ] MeasurementLayer (map)
- [ ] MeasureModeOverlay (UI)

### Phase 8: Announcements
- [ ] Announcement type
- [ ] AnnouncementService
- [ ] AnnouncementSheetView

---

## Wave 3: Phases 3 + 4 (parallel)

### Status: WAITING (needs Wave 2)

### Phase 3: Presets
- [ ] PresetTypes + COGStatistics
- [ ] COGStatisticsService
- [ ] QuickActionsBar + PresetChips
- [ ] Wire to DatasetStore

### Phase 4: Colorscale + Variables
- [ ] ColorscalePickerSheet (3-column grid rewrite)
- [ ] Sweep gradient chip
- [ ] Missing colorscales (wind, waveHeight, wavePeriod)
- [ ] Variable selector chips
- [ ] Expanded/collapsed dataset control

---

## Wave 4: Phase 5 — Overlays
### Status: WAITING (needs Wave 3)

## Wave 5: Phase 6 — Waypoints
### Status: WAITING (needs Wave 1)

## Wave 6: Phases 9 + 10 (parallel)
### Status: WAITING (needs Wave 5)

---

## Critical Bug: ANR on Region/Dataset Selection

**Root cause:** Cascading Compose recompositions on main thread.

**Problem chain:**
1. `selectDataset()` makes 5+ individual `mutableStateOf` mutations → each triggers separate recomposition
2. Each recomposition re-fires `DatasetLayersEffect` (7 sync Mapbox ops) + `GlobalLayersEffect` (10 sync layer updates with NO change guard)
3. After Zarr load completes, `loadingState = Ready` triggers another cascade
4. Total: 20-30+ synchronous Mapbox operations on main thread = 47 second freeze

**iOS doesn't have this because:**
- `@Observable` batches property changes into single SwiftUI update
- GlobalLayers has change guard (`if lastVisibility == visibility { return }`)
- Separate stores minimize cascading

**Fix needed:**
1. Batch state mutations (use Compose `snapshotFlow` or single state object)
2. Add change guard to GlobalLayers.update()
3. Debounce DatasetLayersEffect
4. Extract RegionStore + DatasetStore (Phase 1.2 task)

## Global Notes
- iOS is always the source of truth — re-read Swift files when in doubt
- Phase specs live in `docs/plans/phases/phase-N-*.md`
- Each wave merges before the next starts
- Build locally between waves to catch issues early
