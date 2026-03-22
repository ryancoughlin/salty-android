# STOP — Requirements Before Code

Before ANY code exploration, file reading, or implementation:

1. **State the requirement** — What does the user need to accomplish?
2. **Describe the UX** — What will the user see, tap, experience?
3. **List acceptance criteria** — How do we know it works?

Only AFTER stating these clearly should you explore code or propose solutions.

If you cannot articulate the requirement in one sentence, ASK the user.

---

# Salty Android

> Safety-critical ocean data tools for offshore fishermen navigating remote, dangerous conditions.

## UX Philosophy

**Glanceable, not readable.** Think Android Auto, not Settings. Widgets, not walls of text.

**Undo over confirm.** Let them do it, let them reverse it. No "Are you sure?" dialogs.

**Show state, not spinners.** User always knows what the app is doing.

**Every interruption costs.** Modals, alerts, toasts — justify every one.

---

## Simplicity Mandate

**Every line of code must justify its existence.**

1. **Delete dead code immediately.** Unused functions, unreachable branches, commented-out blocks — remove on sight.
2. **Write lean code.** No defensive null checks or `?.let` chains "just in case." Trust the types.
3. **Minimize branching.** Flatten logic. Straight-line code: do the thing, return the result.
4. **No imperative observation.** Let `LaunchedEffect(key)` + `StateFlow` do the work. No `DisposableEffect` workarounds, no RxJava.
5. **Kill unnecessary abstractions.** Interfaces with one implementation, wrappers that just forward calls — delete them.
6. **One path through the code.** Pick one way to do something and delete the other.
7. **Modern Kotlin only.** Coroutines, not callbacks. Declarative, never imperative.

---

## Working Principles

1. **Plan mode default** — Enter planning for any task with 3+ steps or architectural decisions.
2. **Parallel research** — Spawn multiple Explore agents to investigate different parts of the codebase simultaneously. Never search sequentially.
3. **Parallel execution** — When the plan has independent tasks, spawn multiple agents to implement them at once. One agent per file/feature.
4. **Verification before done** — Never mark complete without proving it works.
5. **Autonomous bug fixing** — When given a bug, just fix it. Zero hand-holding.
6. **Self-improvement** — After any correction, add to `.claude/rules/lessons.md`.
7. **Start with user impact** — What will the user experience? Let that drive the technical approach.
8. **Ask when ambiguous** — If a request could go multiple directions, ask.

---

## iOS-First Workflow (MANDATORY)

**Every conversation starts with iOS.** This is not optional.

**Source of truth**: `/Users/ryan/Developer/salty-ios`

Before writing ANY Android code:
1. **Read the iOS implementation** — Find the exact Swift file(s) that implement this feature
2. **Understand the types** — What structs, enums, classes exist? What are their properties?
3. **Understand the behavior** — What does the code do? What's the user experience?
4. **Port verbatim** — Same type names, same property names, same API contracts, same behavior

**DO NOT:**
- Invent new patterns that don't exist in iOS
- Add "improvements" or "enhancements" beyond what iOS does
- Skip reading iOS because "I think I know what it should do"
- Create types/views that don't have iOS equivalents

**When in doubt:** Open the iOS file and read it.

### Quick Reference

| iOS Location | Contains |
|--------------|----------|
| `SaltyOffshore/` | Main app code (Views, Stores, Services, Models) |
| `SaltyOffshore/Map/` | Map views, layers, controls |
| `SaltyOffshore/Models/` | Data types, enums, API contracts |
| `SaltyOffshore/Services/` | Network, caching, data processing |
| `docs/DATA_STRUCTURES.md` | Zarr format specifications |

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

## Architecture

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

### Compose Recomposition Pitfall

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

## Design

All styles from `Theme.kt`. Dark mode first (fishing at night/dawn).

**Visual hierarchy:**
- One primary action per view
- Max 3 type styles per screen
- Data gets color; chrome stays neutral

**Information density:**
- One number per glance — don't stack values
- Labels only when meaning isn't obvious
- Progressive disclosure — summary first, detail on tap

**Consistency:**
- All spacing from Theme scale (no magic numbers)
- Material Icons only, one weight per context
- Same interaction = same visual treatment

**State:**
- Every control shows its current state
- Selected vs unselected must be obvious
- Loading shows skeleton, not spinner

**Data visualization:**
- Consistent color mapping across app (warm water = warm colors)
- Legend always accessible, never blocking data

---

## Testing

Test user flows, not units. If a user can't break it, don't test it.

**What to test:**
- User journeys: select overlay → scrub timeline → see data update
- Data flow: API response → ViewModel → View renders correctly
- Persistence: state survives app restart
- Safety-critical calculations (distance, contour generation, coordinate transforms)
- Production edge cases (race conditions, network failures, offline transitions)

**What NOT to test:**
- Internal state changes
- UI layout or styling
- Private functions
- Mocks of internal code

**Pattern:**
- Given (setup) → When (action) → Then (outcome)
- 5 flow tests > 20 unit tests

**Golden Rule:** "If I delete this code, would this test fail?" If no → delete the test.

---

## Build & Test Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Run all tests
./gradlew test

# Run single test class
./gradlew test --tests "com.example.saltyoffshore.SomeTestClass"

# Install on connected device
./gradlew installDebug
```

Do NOT build the project to test work during development.

---

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

---

## Git Workflow

- **`main`** — release trunk, never work here directly
- **Feature branches** — short-lived branches off main

**Commits:** No co-author lines. Atomic and frequent. One logical change per commit.

---

## Zarr Data Formats

All Zarr implementations MUST match exact structures in iOS `docs/DATA_STRUCTURES.md`.

**Ocean data:** 4D `(time, depth, y, x)` Web Mercator, chunks `[1, 1, 512, 512]`
**Forecast data:** 3D `(time, lat, lon)` EPSG:4326, chunks vary by model

No fallbacks. No defensive coding around data structure.

---

## Tech Stack

- **UI:** Jetpack Compose + Material 3
- **Maps:** Mapbox SDK for Android
- **Networking:** Ktor Client
- **Serialization:** kotlinx.serialization
- **State:** AndroidX ViewModel + Compose State
- **Coroutines:** kotlinx.coroutines for async
- **Persistence:** DataStore (preferences), Room (structured data)
- **Auth:** Supabase Kotlin SDK
