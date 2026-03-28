# iOS PARITY IS THE ONLY GOAL

**This Android app must be identical to the iOS app.** Nothing more, nothing less.

**Source of truth:** `/Users/ryan/Developer/salty-ios`

---

## The Rules

### 1. Read iOS First — Always

Before writing ANY Android code, you MUST:

1. **Find the iOS file(s)** that implement this feature
2. **Read every line** — understand the types, properties, methods, behavior
3. **Port exactly** — same names, same logic, same UX, same UI

**Match iOS on:**
- Type names and property names
- Method signatures and business logic
- User experience and interaction patterns
- Visual design and layout
- Error handling and edge cases

**DO NOT:**
- Invent patterns that don't exist in iOS
- Add "improvements" or skip features
- Guess what iOS does — read it
- Create partial implementations

### 2. End-to-End or Nothing

**Every feature must work completely.** No stubs. No TODOs. No "we'll add the UI later."

When you implement a feature:
- **UI exists and is wired up**
- **ViewModel has the state and actions**
- **Service calls are complete**
- **Data flows from API to screen**

If you can't finish it end-to-end, stop and ask.

### 3. Use Context7 for Documentation

**Before using ANY library API, query Context7 for current documentation.**

This applies to:
- Mapbox SDK (maps, layers, expressions, gestures)
- Jetpack Compose (effects, state, layouts)
- Ktor (networking, serialization)
- Supabase (auth, database, realtime)
- Any other dependency

**Pattern:**
```
1. Resolve library ID: mcp__context7__resolve-library-id
2. Query docs: mcp__context7__query-docs
3. Then write code
```

Do not rely on training data. Documentation changes. Query Context7.

### 4. Self-Improvement

After any debugging session or correction, update `.claude/rules/lessons.md` with:
- What went wrong
- The fix
- The pattern to use going forward

---

## iOS Reference

| iOS Location | Contains |
|--------------|----------|
| `SaltyOffshore/` | Main app code |
| `SaltyOffshore/Map/` | Map views, layers, controls |
| `SaltyOffshore/Models/` | Data types, enums, API contracts |
| `SaltyOffshore/Services/` | Network, caching, data processing |
| `SaltyOffshore/Stores/` | State management (Observable classes) |
| `docs/DATA_STRUCTURES.md` | Zarr format specifications |

### iOS → Android Translation

| iOS | Android |
|-----|---------|
| `@Observable class` | `ViewModel` + `MutableStateFlow` |
| `@Environment` | Parameter passing or Hilt |
| `actor` | `object` + `Mutex` |
| `async/await` | `suspend fun` |
| `.task {}` | `LaunchedEffect` |
| `UserDefaults` | `DataStore<Preferences>` |
| `Codable` | `@Serializable` |
| `struct` (data) | `data class` |
| `enum` + associated values | `sealed class` |

---

## Working Principles

1. **iOS first** — Read iOS before writing Android. No exceptions.
2. **End-to-end** — Complete features only. No partial work.
3. **Context7 always** — Query docs before using library APIs.
4. **Plan mode** — Enter planning for tasks with 3+ steps.
5. **Parallel agents** — Spawn multiple agents for research and implementation.
6. **Verify before done** — Prove it works before marking complete.
7. **Autonomous fixing** — Given a bug, just fix it.
8. **Ask when unclear** — If it could go multiple ways, ask.

---

## Simplicity Mandate

**Every line of code must justify its existence.**

1. **Delete dead code immediately.** Unused functions, commented blocks — remove on sight.
2. **Write lean code.** No defensive `?.let` chains "just in case."
3. **Minimize branching.** Straight-line code: do the thing, return the result.
4. **Kill abstractions.** Interfaces with one implementation — delete them.
5. **One path.** Pick one way to do something, delete the other.
6. **Modern Kotlin only.** Coroutines, not callbacks.

---

## Architecture

```
UI Layer (Composables)
    ↕ StateFlow/State
ViewModel (AndroidX ViewModel)
    ↕ suspend functions
Services (suspend functions)
    ↕ Ktor/Network
Data (data classes)
```

| Layer | Type | Purpose |
|---|---|---|
| **Composable** | `@Composable` | Rendering + local state + LaunchedEffect |
| **ViewModel** | `ViewModel` | State holder + business logic |
| **Service** | Object | Network I/O, caching |
| **Model** | `data class` | Pure data, no behavior |

---

## Tech Stack

- **UI:** Jetpack Compose + Material 3
- **Maps:** Mapbox SDK for Android
- **Networking:** Ktor Client
- **Serialization:** kotlinx.serialization
- **State:** AndroidX ViewModel + Compose State
- **Persistence:** DataStore, Room
- **Auth:** Supabase Kotlin SDK

---

## Design

Dark mode first. Glanceable, not readable.

- One primary action per view
- Data gets color; chrome stays neutral
- Loading shows skeleton, not spinner
- Same interaction = same visual treatment

---

## Build Commands

```bash
./gradlew assembleDebug      # Build
./gradlew installDebug       # Install on device
./gradlew test               # Run tests
```

Do NOT build to test during development.

---

## Git

- **`main`** — release trunk
- **Feature branches** — short-lived, off main
- **Commits** — atomic, frequent, no co-author lines

---

## Supabase Tables

| Table | Contains |
|-------|----------|
| `forecast_entries` | Forecast model runs |
| `user_preferences` | User settings |
| `shared_waypoints` | Crew waypoint sharing |
| `crews` / `crew_members` | Crew membership |
| `saved_maps` | Saved map configs |
| `device_tokens` | Push notification tokens |
