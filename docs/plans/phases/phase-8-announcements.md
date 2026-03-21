# Phase 8: Announcements

> **iOS is the source of truth.**

## Requirement
Display server-side announcements (outages, updates) to users via a map button and dismissable sheet, with version-based "seen" tracking.

## UX Description
- **Check**: On app launch, fetch the singleton announcement row from Supabase `announcement` table.
- **Button**: If an active announcement exists with version > last seen, show an orange megaphone circle button on the map toolbar.
- **Sheet**: Tapping the button opens a bottom sheet with title, markdown-formatted message body, and "OK" dismiss button.
- **Dismiss**: Tapping OK stores the announcement version in preferences so it won't show again until a new version is published.

## Acceptance Criteria
1. App fetches announcement from Supabase `announcement` table on launch
2. Orange megaphone button appears on map when active announcement has unseen version
3. Tapping button opens sheet with announcement title and formatted message
4. Tapping OK dismisses sheet and persists `lastAnnouncementVersion` to DataStore
5. Button disappears after dismissal until a newer version is published
6. Silent failure on network error (announcements are non-critical)

## iOS Reference Files
- `Features/Announcement/Types/Announcement.swift` -- Announcement model, AnnouncementDisplayState
- `Features/Announcement/Services/AnnouncementService.swift` -- Supabase fetch + version tracking
- `Features/Announcement/Views/AnnouncementButton.swift` -- Orange megaphone FAB
- `Features/Announcement/Views/AnnouncementPreviewView.swift` -- Sheet with title, message, OK button

## Key Types to Port

```
Announcement: id (String), isActive (Boolean), title (String),
              message (String), updatedAt (String), version (Int)
  - CodingKeys: is_active, updated_at

AnnouncementDisplayState: sealed class
  - Hidden
  - Visible(announcement: Announcement)

AnnouncementService: displayState, isLoading, error
  - checkForAnnouncements() -- fetch from Supabase
  - markAnnouncementAsSeen() -- persist version to prefs
```

## Tasks
1. Port `Announcement` data class with `@Serializable` and snake_case mapping
2. Port `AnnouncementDisplayState` as sealed class
3. Port `AnnouncementService` -- Supabase query, version comparison, preference storage
4. Build `AnnouncementButton` composable -- orange circle with megaphone icon
5. Build `AnnouncementSheetView` composable -- title, markdown body, OK button
6. Wire into map toolbar: show button when `displayState` is Visible
