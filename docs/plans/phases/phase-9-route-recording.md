# Phase 9: Route Recording

> **iOS is the source of truth.**

## Requirement
Record GPS tracks during fishing trips with live stats, motion state classification, depth sampling, ocean data capture, and post-trip analysis with interactive detail view.

## UX Description
- **Start**: User taps record button. GPS capture begins at 10-second intervals. Background location enabled. Live Activity shows on lock screen (Android: foreground notification).
- **Live recording UI**: Recording mode overlay shows elapsed time, speed, distance, depth, mark count. FAB buttons: "Drop Mark" (creates waypoint at current location) and "Stop". Speed histogram updates live. Pause/resume supported.
- **Motion classification**: Each point classified as idle (<3 kts), trolling (3-11 kts), running (>=11 kts), or fighting (course reversal >120 deg).
- **Ocean data**: SST, chlorophyll, currents, MLD, FSLE sampled every 5 seconds from TiTiler and attached to track points.
- **Stop**: Saves track with pre-computed `TrackStats` (single pass). Shows trip completion view.
- **Track list**: Saved tracks shown in list with name, date, duration, distance. Tap opens detail view.
- **Detail view**: Embedded map with color-coded polyline (activity/SST/chlor/MLD/FSLE modes), stats section, activity breakdown bar, depth profile chart with interactive scrubber, linked waypoints, ocean data backfill, export as JSON.

## Acceptance Criteria
1. Recording captures GPS points every 10 seconds with speed, heading, course, depth, ocean data
2. Motion state correctly classifies idle/trolling/running based on speed thresholds
3. Fighting detection identifies course reversals >120 degrees
4. Live stats (speed, distance, elapsed time, depth) update every second during recording
5. Drop Mark creates a waypoint linked to the active track via `trackId`
6. Pause/resume stops GPS capture while preserving existing points
7. Track saves with pre-computed `TrackStats` for O(1) stat access
8. Track detail view shows color-coded polyline with activity/ocean data modes
9. Depth profile chart is interactive -- scrubbing moves map camera to that point
10. Track export produces valid JSON importable on another device

## iOS Reference Files
- `Features/Routes/Types/TrackTypes.swift` -- Track, TrackPoint, TrackStats, OceanData, MotionState, TrackColorMode
- `Features/Routes/ViewModels/TrackViewModel.swift` -- Recording orchestration, live stats, track management
- `Features/Routes/Services/TrackRecordingService.swift` -- GPS capture actor, depth/ocean data integration
- `Features/Routes/Views/RecordingControlsFAB.swift` -- Drop Mark + Stop floating buttons
- `Features/Routes/Views/TrackDetailView.swift` -- Post-trip analysis with embedded map
- `Features/Routes/Views/RecordingModeView.swift` -- Live recording overlay
- `Features/Routes/Models/SpeedBracket.swift` -- Speed histogram categories
- `Features/Routes/Services/TrackStorage.swift` -- Local JSON persistence

## Key Types to Port

```
Track: id (UUID), name (String), createdBy (UUID), crewId (UUID?),
       startedAt (Date), endedAt (Date?), points ([TrackPoint]), stats (TrackStats?)
  - computed: isRecording, duration, distanceNauticalMiles, formattedDuration,
              maxSpeedKnots, averageSpeedKnots, time by motion state

TrackPoint: latitude, longitude, timestamp, speedKnots, headingDegrees,
            courseDegrees (Double?), motionState (MotionState), depthFeet (Double?),
            oceanData (OceanData?)

MotionState: idle, fighting, trolling, running
  - init from speed only, or speed + course + previousCourse

TrackStats: distanceNauticalMiles, maxSpeedKnots, averageSpeedKnots,
            timeIdleSeconds, timeFightingSeconds, timeTrollingSeconds,
            timeRunningSeconds, minDepthFeet, maxDepthFeet, pointCount,
            distanceByState, hasOceanData

OceanData: sst, chlorophyll, currentSpeed, currentDirection, mld, fsle,
           salinity, sampledAt, needsBackfill

TrackColorMode: activity, sst, chlorophyll, mld, fsle

TrackError: sealed class with notAuthenticated, noLocationPermission,
            recordingAlreadyInProgress, noActiveRecording, etc.
```

## Tasks
1. Port `TrackPoint`, `MotionState`, `OceanData`, `TrackStats` data classes
2. Port `Track` data class with computed properties and stats
3. Implement `TrackRecordingService` -- GPS capture with coroutines, depth/ocean sampling
4. Implement `TrackViewModel` -- recording lifecycle, live stats timer, track management
5. Build foreground service for background GPS recording (Android-specific)
6. Build recording mode overlay -- stats grid, speed histogram, pause/resume
7. Build `RecordingControlsFAB` -- Drop Mark + Stop floating action buttons
8. Build `TrackDetailView` -- embedded Mapbox map with color-coded polyline, stats, chart
9. Build depth profile chart with interactive scrubber linked to map camera
10. Implement `TrackStorage` for local JSON persistence
11. Implement ocean data backfill manager for post-trip data loading
12. Build track list view with track cards
