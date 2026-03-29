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

### Layer Z-Ordering for Multiple Custom Layers

**Problem:** When stacking multiple `CustomLayer`s (e.g. overlay datasets), using `addStyleCustomLayer` with `LayerPosition(above=anchorId)` silently fails if the anchor layer doesn't exist on the map yet. The layer gets added at the default position (bottom of custom layers), making overlays invisible behind earlier layers. Additionally, once a layer is added at the wrong position, it stays there — `addStyleCustomLayer` with `styleLayerExists` guard never repositions it.

**Solution:** Two-part fix:

1. **Guard against phantom anchors:** Before adding a new layer, verify the anchor layer actually exists with `style.styleLayerExists(anchorId)`. If it doesn't, defer to the next render cycle.
2. **Use `moveStyleLayer` for repositioning:** On every render pass, call `style.moveStyleLayer(layerId, LayerPosition(above, null, null))` for already-existing layers to ensure correct z-order even if layers loaded out of activation order.
3. **Only advance the `previousLayerId` chain** when the layer actually exists on the map, not just because it was iterated.

```kotlin
// WRONG — references layer that may not exist yet
style.addStyleCustomLayer(layerId, renderer, LayerPosition("nonexistent-layer", null, null))

// CORRECT — verify anchor exists, use moveStyleLayer for existing layers
if (style.styleLayerExists(layerId)) {
    style.moveStyleLayer(layerId, LayerPosition(anchorId, null, null))
} else if (anchorId == null || style.styleLayerExists(anchorId)) {
    style.addStyleCustomLayer(layerId, renderer, LayerPosition(anchorId, null, null))
} else {
    // Defer — anchor not on map yet, next render pass will pick this up
}
```

Docs: https://docs.mapbox.com/android/maps/guides/styles/work-with-layers/

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
