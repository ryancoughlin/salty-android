# Salty Android

> Safety-critical ocean data tools for offshore fishermen navigating remote, dangerous conditions.

---

## iOS Migration Context

**This project is a 1:1 port of the iOS app.**

- **Source of truth**: `/Users/ryan/Developer/salty-ios`
- **Goal**: Same types, same names, same behavior
- **Migration plan**: See `MIGRATION.md` for detailed task breakdown

When implementing any feature:
1. Find the iOS implementation first
2. Port it verbatim (adjusted for Kotlin/Android idioms)
3. Use the same type names, property names, and API contracts
4. Match the iOS behavior exactly

### iOS → Android Translation

| iOS | Android |
|-----|---------|
| `@Observable class` | `ViewModel` + `MutableStateFlow` |
| `@Environment(Type.self)` | Parameter passing or Hilt `@Inject` |
| `actor` | `object` + `Mutex` |
| `async/await` | `suspend fun` |
| `.task {}` | `LaunchedEffect` |
| `UserDefaults` | `DataStore<Preferences>` |
| `Codable` | `@Serializable` |
| `struct` (data) | `data class` |
| `enum` with associated values | `sealed class` |

---

## User Persona

Commercial fishing vessels making safety-critical decisions on spotty satellite internet. Multi-day expeditions in remote locations where crashes or data loss could mean disaster.

**UX Requirements:** Instant loading (< 1s), state persistence across restarts, fresh data for browsed regions, offline capability for preferred region, seamless timeline scrubbing with zero delay.

---

## Core Architecture

- Never write defensively or have fallbacks unless explicitly stated.

### Kotlin/Compose Architecture

```
UI Layer (Composables)
    ↕️ StateFlow/State
ViewModel (AndroidX ViewModel)
    ↕️ suspend functions
Services (suspend functions / coroutines)
    ↕️ Ktor/Network
Data (data classes)
```

### Layer Responsibilities

| Layer | Type | Purpose | Examples |
|---|---|---|---|
| **Composable** | `@Composable` function | Rendering + local state + LaunchedEffect | `MapScreen`, `SaltyDatasetControl` |
| **ViewModel** | `ViewModel` | State holder + business logic + coroutine scope | `SaltyViewModel` |
| **Service** | Singleton/Object | Network I/O, caching | `SaltyApi`, `COGService` |
| **Model** | `data class` | Pure data, no behavior | `Region`, `TimeEntry`, `Dataset` |

### State Management

- Use `mutableStateOf` with `by` delegation for Compose state
- ViewModel owns all shared state, Composables read via parameter passing
- Single source of truth per feature
- Derived state via computed properties (`val isActive: Boolean get() = ...`)

---

## Compose Recomposition Pitfall

```kotlin
// ❌ BAD: Recomposes on ANY state change
@Composable
fun MapScreen(viewModel: SaltyViewModel) {
    val everything = viewModel.someFrequentlyChangingState // triggers full recompose
    ExpensiveChild(...)
}

// ✅ GOOD: Only recompose what needs it
@Composable
fun MapScreen(viewModel: SaltyViewModel) {
    val stableData = viewModel.selectedRegion // only what this composable needs
    ExpensiveChild(data = stableData)
}
```

---

## Styling

- Use `Theme.kt` and `Color.kt` for consistent theming
- Follow Material 3 design patterns
- Dark mode first (fishing at night/dawn)

## Testing

**Golden Rule:** "If I delete this code, would this test fail?" If no → delete the test.

**Never test:** Tautologies, language guarantees, framework internals, implementation details.

**Always test:**
1. Critical user paths (region selection, offline caching, waypoint sharing)
2. Data loss scenarios (preference persistence, state restoration)
3. Safety-critical calculations (distance, contour generation, coordinate transforms)
4. Production edge cases (race conditions, network failures, offline transitions)

## Building Project

Do NOT build the project to test work.

## Debugging with Supabase

Use Supabase MCP to query backend tables:

| Table | Contains |
|-------|----------|
| `forecast_entries` | Latest forecast model runs (GFS, HRRR, etc.) |
| `user_preferences` | User settings, crew preferences, preferred regions |
| `shared_waypoints` | Crew waypoint sharing (realtime) |
| `crews` / `crew_members` | Crew/group membership |
| `saved_maps` | Saved map configurations |
| `device_tokens` | Push notification FCM tokens |
| `announcement` | App announcements |

## Git Workflow

- **`main`** — release trunk, never work here directly
- **Feature branches** — short-lived branches off main

**Commits:** No co-author lines. Atomic and frequent. One logical change per commit.

## Zarr Data Formats

All Zarr implementations MUST match exact structures in iOS `docs/DATA_STRUCTURES.md`.

**Ocean data:** 4D `(time, depth, y, x)` Web Mercator, chunks `[1, 1, 512, 512]`
**Forecast data:** 3D `(time, lat, lon)` EPSG:4326, chunks vary by model

No fallbacks. No defensive coding around data structure.

## Tech Stack

- **UI:** Jetpack Compose + Material 3
- **Maps:** Mapbox SDK for Android
- **Networking:** Ktor Client
- **Serialization:** kotlinx.serialization
- **State:** AndroidX ViewModel + Compose State
- **Coroutines:** kotlinx.coroutines for async
- **Persistence:** DataStore (preferences), Room (structured data)
- **Auth:** Supabase Kotlin SDK
