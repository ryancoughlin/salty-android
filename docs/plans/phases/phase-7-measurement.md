# Phase 7: Measurement Tool

> **iOS is the source of truth.**

## Requirement
On-map distance measurement tool that lets users tap points to create multi-segment lines showing distance and bearing between points.

## UX Description
- **Enter**: Toolbar button or tapping an existing measurement activates measure mode. Sheets dismiss on entry.
- **Add points**: Tap map to place points. Each tap adds a vertex. Lines connect consecutive points with distance labels at midpoints.
- **Overlay**: Status pill shows total distance (or "Tap map to add points" hint). Action toolbar has Undo, Clear, and Done buttons.
- **Rendering**: Black polyline (3px) with white circle vertices (8px radius, black stroke). Distance labels in black pills at segment midpoints using `MarineUnits.formatDistance`.
- **Active point**: Last point in active measurement shows a pulsing blue indicator.
- **Multiple measurements**: Finishing a measurement (Done or starting new) preserves it on map. All completed measurements remain visible.
- **Session only**: No persistence. Measurements cleared when app closes.

## Acceptance Criteria
1. Tapping map in measure mode adds points connected by polylines
2. Distance labels display at midpoints in user's preferred units (nm or mi)
3. Undo removes the last point; if no points remain, active measurement clears
4. Clear removes all measurements (active + completed)
5. Done finishes active measurement (requires 2+ points) and exits measure mode
6. Completed measurements persist on map until Clear or app restart
7. Tapping an existing measurement line or point re-enters measure mode
8. Status pill shows total distance across all segments

## iOS Reference Files
- `Features/Measurement/Types/MeasurementTypes.swift` -- MeasurementPoint, MeasurementSegment, MapMeasurement
- `Features/Measurement/MeasurementManager.swift` -- State management for active/completed measurements
- `Features/Measurement/MeasureMode.swift` -- Mode toggle state
- `Features/Measurement/MapContent/MeasurementLayer.swift` -- Polyline + circles + label rendering
- `Features/Measurement/Views/MeasureModeOverlay.swift` -- Toolbar overlay with undo/clear/done

## Key Types to Port

```
MeasurementPoint: id (UUID), coordinate (LatLng)

MeasurementSegment: start (LatLng), end (LatLng)
  - computed: distanceMeters, midpoint, bearingDegrees, bearingFormatted

MapMeasurement: id (UUID), points ([MeasurementPoint])
  - computed: segments, totalDistanceMeters, coordinates, hasSegments

MeasurementEntrySource: toolbar, quickButton, tapExisting

MeasureMode: isActive (Boolean)

MeasurementManager: activeMeasurement (MapMeasurement?),
                    completedMeasurements ([MapMeasurement])
  - actions: addPoint, undoLastPoint, finishMeasurement, startNewMeasurement,
             clearAllMeasurements, deleteMeasurement
```

## Tasks
1. ✅ Port measurement types (`MeasurementPoint`, `MeasurementSegment`, `MapMeasurement`)
2. ✅ Port `MeasurementManager` as `MeasurementState` (merged mode + session into one class)
3. ✅ Port `MeasureMode` toggle into `AppViewModel` + `RightSideToolbar` button
4. ✅ Build `MeasurementMapEffect` -- GeoJSON LineLayer + CircleLayer + SymbolLayer via Mapbox Turf
5. ✅ Build `MeasureModeOverlay` -- accent status pill + undo/clear/done with M3 surface hierarchy
6. ✅ Wire map tap handler to route taps to measurement state when mode active

## Android Files Created
- `data/measurement/MeasurementTypes.kt` — data classes + `formatDistance()`
- `data/measurement/MeasurementState.kt` — merged mode + session state with Compose `mutableStateOf`
- `ui/measurement/MeasurementMapEffect.kt` — 3 GeoJSON sources + 3 layers (line, circle, symbol)
- `ui/measurement/MeasureModeOverlay.kt` — M3 Surface with accent pill + icon actions + primary Done
