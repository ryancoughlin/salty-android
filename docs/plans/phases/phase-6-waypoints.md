# Phase 6: Waypoints

> **iOS is the source of truth.**

## Requirement
Full waypoint CRUD with map annotations, crew sharing, GPX import/export, and per-waypoint ocean conditions and weather forecasts.

## UX Description
- **Create**: Long-press map to drop a waypoint. Default name auto-generated ("WPT-001"). Symbol picker shows Garmin-compatible shapes (circles, flags, squares, triangles in 4 colors) plus legacy fish/structure icons.
- **View on map**: Waypoints render as `PointAnnotation` icons with text labels. Clustering kicks in at zoom < 11 with 6+ points. Own waypoints hidden when crew mode active.
- **Details sheet**: Tap a waypoint on map to open detail sheet showing symbol icon, name, distance from user, crew name (if shared), coordinates (tap to copy), inline editable notes, and tabbed Conditions/Weather sections.
- **Management list**: Full-screen list grouped by date or symbol. Search bar filters by name/notes. Overflow menu: Select mode, Import GPX, Import Settings, Delete All. Edit mode enables multi-select for batch share/delete. Swipe-to-delete on individual rows.
- **Crew sharing**: Share owned waypoints to crews via Supabase realtime. Crew waypoints appear on map and in list with crew badge. Any crew member can edit/delete shared waypoints.
- **GPX import/export**: Import `.gpx` files via file picker. Export individual waypoints as GPX via share sheet.

## Acceptance Criteria
1. Long-press on map creates a waypoint at that coordinate with auto-generated name
2. Waypoint annotations render on map with correct symbol icon, label, and clustering
3. Tapping a waypoint annotation opens detail sheet with symbol, name, coordinates, distance, notes
4. Coordinates display respects user's GPS format preference; tap copies to clipboard
5. Inline notes save on focus loss with optimistic UI update
6. Waypoint management list supports search, sort by date/symbol, swipe-delete
7. GPX file import creates waypoints; duplicate detection prevents re-import
8. Share-as-GPX exports valid GPX file via Android share sheet
9. Crew waypoints appear on map and in list; edit/delete routes through Supabase
10. Waypoint symbol picker shows all Garmin + legacy symbols organized by category

## iOS Reference Files
- `Features/Waypoints/Types/WaypointTypes.swift` -- Waypoint, WaypointSymbol, WaypointCategory enums
- `Features/Waypoints/WaypointStore.swift` -- CRUD, sorting, crew waypoint management
- `Features/Waypoints/MapContent/WaypointLayer.swift` -- Map annotation rendering + clustering
- `Features/Waypoints/Views/WaypointDetailsView.swift` -- Detail sheet with tabs
- `Features/Waypoints/Views/WaypointManagementView.swift` -- List management
- `Features/Waypoints/Import/Services/GPXImportService.swift` -- GPX parsing
- `Features/Waypoints/Services/GPXExportService.swift` -- GPX export
- `Features/Waypoints/Services/WaypointStorage.swift` -- Local persistence
- `Features/Waypoints/Services/WaypointConditionsService.swift` -- Ocean conditions API

## Key Types to Port

```
Waypoint: id (UUID), name (String?), notes (String?), symbol (WaypointSymbol),
          latitude (Double), longitude (Double), createdAt (Date),
          trackId (UUID?), depthFeet (Double?)

WaypointSymbol: enum (~50 cases) with imageName, category, shortName
  - Garmin: circles/flags/squares/triangles in 4 colors, dot, fishingArea1-9
  - Legacy: fish species, structure types, navigation, environment

WaypointCategory: garmin, fish, structure, navigation, environment, other

WaypointSection: id (String), title (String), waypoints ([Waypoint])

WaypointSortOption: dateCreated, symbol

WaypointPresentationMode: navigation, sheet

WaypointSelectionSource: mapTap, managementSheet, managementNav, notification
```

## Tasks
1. Port `WaypointSymbol` enum with all cases, `imageName`, `category`, decoder migration
2. Port `Waypoint` data class with serialization
3. Implement `WaypointStore` ViewModel -- CRUD, sorting, grouping, crew waypoints
4. Implement `WaypointStorage` using local JSON file persistence
5. Build `WaypointLayer` -- Mapbox `PointAnnotation` with clustering
6. Build `WaypointDetailsView` -- header, coordinates, notes, conditions/weather tabs
7. Build `WaypointManagementView` -- grouped list, search, sort, multi-select
8. Build waypoint form (create/edit) with symbol picker
9. Implement GPX import service with file picker integration
10. Implement GPX export via Android share intent
11. Wire crew waypoint sharing through Supabase realtime
