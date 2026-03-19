# Migration Plans

> Detailed implementation plans for iOS → Android migration.

**Source of Truth:** `/Users/ryan/Developer/salty-ios`

---

## Plan Index

| Phase | File | Status | Description |
|-------|------|--------|-------------|
| 0 | [00-foundation-parity.md](./00-foundation-parity.md) | Pending | Complete preferences, dataset types, rendering config |
| 1 | 01-map-layers.md | Pending | BreaksLayer, NumbersLayer, Zarr visual layer |
| 2 | 02-core-features.md | Pending | Waypoints, Crews, Timeline, Saved Maps |
| 3 | 03-polish.md | Pending | Offline, Tracks, AI Reports, Satellites |

---

## How to Use

1. **Before starting work:** Read the relevant plan file
2. **Find iOS implementation:** Each task references exact iOS source files
3. **Port verbatim:** Same type names, same properties, same behavior
4. **Mark complete:** Update status in this README as phases complete

---

## Progress Tracking

### Phase 0: Foundation Parity
- [ ] 0.1 Preferences System (AppPreferencesDataStore expansion)
- [ ] 0.2 DatasetType Completion (phytoplankton, capabilities, defaults)
- [ ] 0.3 Dataset Variable System
- [ ] 0.4 Rendering Configuration (ScaleMode, DomainStrategy, RenderingConfig)
- [ ] 0.5 Preset System
- [ ] 0.6 Map Configuration (share links)
- [ ] 0.7 Dataset Field Configuration

### Phase 1: Map Layers
- [ ] 1.1 BreaksLayer (thermal fronts)
- [ ] 1.2 NumbersLayer (grid values)
- [ ] 1.3 Zarr Visual Layer (optional - depends on performance needs)

### Phase 2: Core Features
- [ ] 2.1 WaypointStore + disk persistence
- [ ] 2.2 CrewStore + realtime subscriptions
- [ ] 2.3 Timeline scrubbing
- [ ] 2.4 Saved Maps

### Phase 3: Polish
- [ ] 3.1 Offline downloads
- [ ] 3.2 Track recording
- [ ] 3.3 AI Reports
- [ ] 3.4 Satellite tracking
