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
