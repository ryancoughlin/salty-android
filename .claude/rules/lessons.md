# Lessons Learned

Patterns and fixes discovered during development. Claude should update this file after any correction or debugging session.

---

## Mapbox Android

### Expression Syntax in Layer DSL

**Problem:** Data-driven expressions don't work when created inline inside the layer DSL block.

```kotlin
// WRONG - expression created inside DSL doesn't work
symbolLayer(layerId, sourceId) {
    iconSize(get { literal("iconSize") })  // Silent failure
}
```

**Solution:** Create expression functions outside the DSL, then pass the result.

```kotlin
// CORRECT - expression created outside, result passed in
private fun iconSizeExpression(): Expression = get { literal("iconSize") }

symbolLayer(layerId, sourceId) {
    iconSize(iconSizeExpression())  // Works
}
```

### Clustering Setup

**Pattern:** GeoJSON source with clustering requires three layers:
1. `circleLayer` for cluster circles (filter: `has { literal("point_count") }`)
2. `symbolLayer` for cluster count text (same filter)
3. `symbolLayer` for individual points (filter: `not { has { literal("point_count") } }`)

```kotlin
geoJsonSource(sourceId) {
    featureCollection(features)
    cluster(true)
    clusterRadius(30)
    clusterMaxZoom(11)
    clusterMinPoints(6)
}
```

### Icon Registration

**Problem:** Icons must be registered with the style before use in layers.

**Solution:** Load from drawable resources and register with exact name used in feature properties.

```kotlin
val drawable = context.getDrawable(resId) ?: return
style.addImage(iconName, drawable.toBitmap())
```

### Drawable Naming

**Convention:** Android drawable names are lowercase with underscores. iOS image names are PascalCase.

| iOS | Android |
|-----|---------|
| `RedCircle` | `redcircle` |
| `Marker-Bluefin` | `marker_bluefintuna` |

---

## Style Callbacks and Threading

**Problem:** Mapbox style callbacks may fire on background threads, causing crashes when updating UI state.

**Solution:** Dispatch to main thread using Handler.

```kotlin
val mainHandler = Handler(Looper.getMainLooper())
mapView.mapboxMap.subscribeStyleLoaded { _ ->
    mainHandler.post { render(mapView.mapboxMap) }
}
```

---

## Tap Detection on Map Features

**Pattern:** Use `queryRenderedFeatures` with layer filter to detect taps on specific features.

```kotlin
mapView.gestures.addOnMapClickListener { point ->
    val screenPoint = mapView.mapboxMap.pixelForCoordinate(point)
    val options = RenderedQueryOptions(listOf(layerId), null)
    mapView.mapboxMap.queryRenderedFeatures(
        RenderedQueryGeometry(screenPoint),
        options
    ) { result ->
        result.value?.firstOrNull()?.queriedFeature?.feature
            ?.getStringProperty("id")?.let { id ->
                onTap(id)
            }
    }
    false
}
```

---

## Supabase Kotlin SDK

### Realtime: decodeRecord doesn't exist on PostgresAction

**Problem:** `PostgresAction.Insert` does not have a `decodeRecord<T>()` method in supabase-kt. The Context7 docs show it but the actual SDK exposes `record` as a `JsonObject`.

**Solution:** Decode manually using your own Json instance:

```kotlin
// WRONG — decodeRecord is not a method on PostgresAction.Insert
val item = action.decodeRecord<MyType>()

// CORRECT — decode from the record JsonObject
val item = json.decodeFromJsonElement(MyType.serializer(), action.record)
```

### Realtime: Channel cleanup

**Pattern:** Use `channel.unsubscribe()` to tear down, not `realtime.removeChannel()`.

### RPC calls

**Pattern:** Use `supabase.postgrest.rpc(functionName, jsonParams)` with `buildJsonObject {}`.

```kotlin
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc

supabase.postgrest.rpc("mark_waypoint_read", buildJsonObject {
    put("waypoint_id", JsonPrimitive(id))
    put("user_id", JsonPrimitive(userId))
})
```

---

## kotlinx.serialization

### SetSerializer / ListSerializer inline type parameter

**Problem:** `kotlinx.serialization.builtins.serializer<String>()` is a reified inline function that doesn't resolve when called from a non-inline context.

**Solution:** Use `encodeToString` with a concrete collection — the serializer is inferred:

```kotlin
// WRONG — unresolved reference
json.encodeToString(SetSerializer(serializer<String>()), set)

// CORRECT — encode a List, decode as Set (JSON arrays work for both)
json.encodeToString(set.toList())
// and decode:
json.decodeFromString<Set<String>>(raw)
```

---

## Compose Performance Patterns

### Lambda State Providers for High-Frequency State

**Problem:** `mutableStateOf` properties that change every frame (camera zoom/lat/lng) cause full recomposition of the composable that reads them.

**Solution:** Pass lambdas instead of values. The child composable reads the lambda inside its own scope — recomposition is limited to that child.

```kotlin
// WRONG — parent recomposes every frame
CrosshairOverlay(zoom = viewModel.currentZoom)

// CORRECT — parent never recomposes for camera moves
CrosshairOverlay(zoomProvider = { cameraZoom })

// Inside CrosshairOverlay:
val zoom = zoomProvider()  // read HERE, in child scope
```

### MapContent Isolation (matches iOS MapView.swift)

**Pattern:** The map composable must be isolated from sheet state. Extract it into a standalone `@Composable` that takes explicit params — no ViewModel reference.

- `MapContent.kt` = iOS `MapboxMapView_V2.swift`
- Takes explicit params only (region, dataset, waypoints, callbacks)
- Sheet state changes in `MapScreen` do NOT recompose `MapContent`

### MapEffect Keys — Don't Use Full Objects

**Problem:** Using a data class as a MapEffect key tears down/restarts the effect on every property change (e.g., opacity slider drag creates new snapshot).

**Solution:** Key on identifiers only. Use `rememberUpdatedState` for the changing data.

```kotlin
// WRONG — tears down MapEffect on every opacity change
MapEffect(regionId, entry?.id, snapshot, visualSource) { ... }

// CORRECT — only tears down on region/entry change
val currentSnapshot by rememberUpdatedState(snapshot)
MapEffect(regionId, entry?.id) { mapView ->
    datasetLayers?.render(snapshot = currentSnapshot)
}
// Separate LaunchedEffect for incremental updates
LaunchedEffect(snapshot) { datasetLayers?.render(snapshot = currentSnapshot) }
```

### derivedStateOf for Computed Properties

**Problem:** Plain `get()` recomputes on every recomposition, even when inputs haven't changed.

**Solution:** Use `derivedStateOf` — it caches the result and only recomputes when observed inputs change.

```kotlin
// WRONG — recomputes filter/groupBy on every recompose
val layersByCategory: List<...>
    get() { _layers.filter { ... }.groupBy { ... } }

// CORRECT — only recomputes when _layers changes
val layersByCategory by derivedStateOf { _layers.filter { ... }.groupBy { ... } }
```

### key() for ForEach in Compose

**Problem:** `forEach` in composable scope without `key {}` means Compose can't track identity — all items get torn down and recreated on recompose.

**Solution:** Wrap each item in `key(stableId)`.

```kotlin
regions.forEach { region ->
    key(region.id) {
        ViewAnnotation(...) { RegionAnnotationView(region = region) }
    }
}
```
