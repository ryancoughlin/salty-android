# Waypoint Feature Migration

> Phase 1 implementation complete. Ported from iOS `WaypointStore.swift` and related files.

---

## Status: Phase 1 Complete

Full waypoint CRUD, map annotations, crew sharing, GPX import/export, conditions + weather charts.

**Deferred:**
- Supabase realtime subscriptions (initial load only)
- Track recording integration (DropWaypointSheet)
- AI analysis (WaypointAIAnalysisTypes)

---

## Files Created (30+)

### Data Layer

| File | Purpose |
|------|---------|
| `data/waypoint/Waypoint.kt` | Core model |
| `data/waypoint/WaypointSymbol.kt` | 52 symbols with custom serializer |
| `data/waypoint/SharedWaypoint.kt` | Crew sharing model |
| `data/waypoint/WaypointTypes.kt` | Source, Sheet, Section, Sort, Filter types |
| `data/waypoint/WaypointFormState.kt` | Form state with coordinate parsing |
| `data/waypoint/WaypointStorage.kt` | JSON persistence (Mutex-guarded) |
| `data/waypoint/Crew.kt` | Crew model |
| `data/waypoint/WaypointSharingService.kt` | Supabase CRUD |
| `data/waypoint/OfflineShareQueue.kt` | Offline share persistence |
| `data/waypoint/WaypointConditionsService.kt` | Ocean conditions API |
| `data/waypoint/WaypointWeatherService.kt` | Weather/wave forecast API |
| `data/waypoint/GPXImportService.kt` | GPX XML parser |
| `data/waypoint/GPXExportService.kt` | GPX XML generator |
| `data/waypoint/GPXImportOptions.kt` | Import configuration |
| `data/waypoint/LoadingState.kt` | Loading state enum |
| `data/coordinate/GPSFormat.kt` | DMM/DMS/DD format |
| `data/coordinate/CoordinateTypes.kt` | Input field configs |
| `data/coordinate/CoordinateFormatter.kt` | Parse/format utility |

### UI Layer

| File | Purpose |
|------|---------|
| `ui/map/waypoint/WaypointAnnotationLayer.kt` | Own waypoint map pins |
| `ui/map/waypoint/SharedWaypointAnnotationLayer.kt` | Crew waypoint pins |
| `ui/map/waypoint/WaypointIconRegistrar.kt` | Symbol image registration |
| `ui/waypoint/WaypointDetailSheet.kt` | Detail view with tabs |
| `ui/waypoint/WaypointFormSheet.kt` | Create/edit form |
| `ui/waypoint/WaypointManagementSheet.kt` | List + search + sort |
| `ui/waypoint/ShareWaypointSheet.kt` | Crew picker |
| `ui/waypoint/components/SymbolChipPicker.kt` | Symbol grid |
| `ui/waypoint/components/CoordinateInputView.kt` | GPS fields |
| `ui/waypoint/components/WaypointConditionsContent.kt` | Conditions tab |
| `ui/waypoint/components/WaypointWeatherContent.kt` | Weather tab |

### Charts

| File | Purpose |
|------|---------|
| `ui/waypoint/chart/ChartColorScales.kt` | Color interpolation |
| `ui/waypoint/chart/ChartConstants.kt` | Layout constants |
| `ui/waypoint/chart/WindForecastChart.kt` | Wind bar chart |
| `ui/waypoint/chart/WeatherForecastChart.kt` | Precip + temp composite |
| `ui/waypoint/chart/WaveForecastChart.kt` | Wave bar chart |
| `ui/waypoint/chart/WaypointConditionChart.kt` | 7-day history |

### Modified Files

| File | Change |
|------|--------|
| `viewmodel/AppViewModel.kt` | Waypoint state + CRUD |
| `ui/screen/MapScreen.kt` | Annotation layers + long-press |
| `config/MapLayers.kt` | Waypoint source/layer IDs |
| `gradle/libs.versions.toml` | Added Vico v3.0.3 |
| `app/build.gradle.kts` | Added Vico dependency |
| `AndroidManifest.xml` | Added FileProvider |
| `res/xml/file_paths.xml` | FileProvider paths |

### Assets

- 52 waypoint symbol PNGs in `res/drawable/`

---

## Architecture Decisions

| Decision | Rationale |
|----------|-----------|
| Canvas-based charts (not Vico) | Per-bar custom colors not supported by chart libs |
| Single AppViewModel | Avoids multi-ViewModel coordination overhead |
| ModalBottomSheet for all waypoint UI | Matches iOS `.sheet` behavior |
| GeoJSON + SymbolLayer for map annotations | Matches existing codebase pattern |
