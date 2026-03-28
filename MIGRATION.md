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

---

## Feature Parity Status

| Feature | Status | Notes |
|---------|--------|-------|
| Authentication (login/signup/reset) | ✅ Complete | |
| Account Hub + Profile | ✅ Complete | |
| Region Selection (FTUX + settings) | ✅ Complete | |
| Map + Mapbox | ✅ Complete | |
| Global Layers (bathymetry, stations, shipping, MPAs, reefs, tournaments, GPS/LORAN) | ✅ Complete | |
| Dataset Visualization (contours, breaks, arrows, particles, numbers) | ✅ Complete | |
| Overlay Datasets | ✅ Complete | |
| Colorscale + Variable Selector | ✅ Complete | |
| Presets / Quick Actions | ✅ Complete | |
| Zarr Pipeline | ✅ Complete | |
| Entry Gallery / Timeline | ✅ Complete | |
| Waypoints (CRUD, symbols, GPS formats, GPX import/export) | ✅ Complete | |
| Waypoint Crew Sharing + Offline Queue | ✅ Complete | |
| Station Details (observations, currents, forecasts, charts) | ✅ Complete | |
| Satellite Tracking | ✅ Complete | |
| Measurement Tool | ✅ Complete | |
| Share Links | ✅ Complete | |
| Notifications (loading/error capsules) | ✅ Complete | |
| Announcements | 🔄 In Progress | Completing — extract service + sheet |
| Dataset Guide | 🔄 In Progress | Building from scratch |
| Notification Settings + Primer | 🔄 In Progress | UI layer, no FCM yet |
| Crew Management UI (create/join/invite/detail) | ⬜ Not Started | |
| Saved Maps (save/load/share configs) | ⬜ Not Started | |
| Routes/Tracks (recording, playback, detail) | ⬜ Not Started | Largest missing feature |
| Playback Mode (historical data animation) | ⬜ Not Started | |
| Eddy Feature Layer (SSH pulsing) | ⬜ Not Started | |
| Waypoint Notification Overlay (direction indicator) | ⬜ Not Started | |
| Realtime Waypoint Subscriptions | ⬜ Not Started | |
| Google Sign-In | ⬜ Not Started | Android equivalent of Apple Sign-In |
| Subscription/Premium (billing) | ⬜ Not Started | |
| Guided Onboarding (tooltip journey) | ⬜ Not Started | |
| FADs Layer | ⬜ Not Started | |
| Offline Downloads | ⬜ Not Started | |
| Weather Overlays (wind/pressure/wave heatmaps) | ⬜ LAST | Complex shader work, do after everything else |
| AI Reports | ⬜ LAST | Do after everything else |

---

## Implementation Order

1. ✅ Foundation (auth, map, datasets, layers) — DONE
2. ✅ Waypoints + Crew Sharing — DONE
3. ✅ Stations + Satellite + Measurement — DONE
4. 🔄 Small features (announcements, dataset guide, notification settings) — IN PROGRESS
5. ⬜ Crew Management UI (create/join/invite screens)
6. ⬜ Saved Maps
7. ⬜ Routes/Tracks (recording + playback)
8. ⬜ Playback Mode (historical data)
9. ⬜ Remaining layers (eddies, FADs)
10. ⬜ Polish (onboarding, Google Sign-In, subscription)
11. ⬜ Weather Overlays — **MUST BE LAST** (complex Metal→GL shader port)
12. ⬜ AI Reports — **MUST BE LAST**
