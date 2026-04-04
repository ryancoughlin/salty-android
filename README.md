# SaltyOffshore Android

Marine navigation app with real-time ocean data, weather forecasts, and navigation tools. Android port of the [iOS app](https://github.com/ryancoughlin/salty-ios).

## Setup

### Prerequisites

- Android Studio (latest stable)
- JDK 11+
- Android SDK 36

### Mapbox Token

Create `app/src/main/res/values/mapbox_access_token.xml` (gitignored):

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources xmlns:tools="http://schemas.android.com/tools">
    <string name="mapbox_access_token" translatable="false" tools:ignore="UnusedResources">YOUR_MAPBOX_PUBLIC_TOKEN</string>
</resources>
```

Get a public token (`pk.`) at [account.mapbox.com/access-tokens](https://account.mapbox.com/access-tokens/).

### Supabase

Auth and data sync use Supabase. The project URL and anon key are configured in `SaltyApplication.kt`.

## Build

```bash
./gradlew assembleDebug      # Build
./gradlew installDebug       # Install on device
./gradlew test               # Run tests
```

## Architecture

```
UI (Composables)
    ↕ StateFlow
ViewModel
    ↕ suspend functions
Services
    ↕ Ktor
Data (data classes)
```

## Tech Stack

- **UI:** Jetpack Compose + Material 3
- **Maps:** Mapbox SDK
- **Networking:** Ktor
- **Serialization:** kotlinx.serialization
- **Auth/Data:** Supabase Kotlin SDK
- **Persistence:** DataStore, Room
