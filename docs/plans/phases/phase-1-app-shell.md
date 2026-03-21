# Phase 1: App Shell + DI + Navigation

> **iOS is the source of truth.** Always read the Swift files before writing Android code. We won't get everything on the first pass -- refer back constantly.

## Requirement

The user must be able to launch the app, authenticate (sign in / sign up / reset password), see the map screen with a top bar, access a settings hub via bottom sheet, and complete first-time region selection -- matching iOS state machine and screen flow exactly.

## UX Description

### Cold Launch (unauthenticated)

1. App opens to `LoginScreen` -- dark background (`Color.base`) with a subtle dot-grid pattern filling the screen behind content.
2. Centered vertically in upper third: Salty logo mark (72x72), then heading text "Built for pros. Designed for everyone." in `headingLarge`, multiline centered.
3. Below (80dp gap): Auth section containing:
   - **Apple Sign-In button** (iOS only -- skip for Android, replace with Google Sign-In later; for now just email).
   - "or" divider -- horizontal line, caption text "or", horizontal line.
   - **"Continue with email" button** (secondary variant, envelope icon). Tapping it animates open (0.2s ease-in-out) the email/password form below it.
   - Email/password form: two text fields with validation (red border + caption error on submit if empty/invalid). "Forgot Password?" link right-aligned below password field opens `ResetPasswordSheet`.
   - "Sign In" primary button (shows loading spinner when `AuthManager.isLoading`). Disabled when loading or form invalid.
4. Footer: "Don't have an account? **Sign Up**" -- tapping "Sign Up" opens `SignUpScreen` as a bottom sheet.
5. Sheets available from login: `ResetPasswordSheet` (email field + send button), `SignUpSheet` (first name, last name, email, password + create account button).

### Auth State Machine

The app listens to Supabase auth state changes. Three events matter:

| Event | Action |
|-------|--------|
| `signedIn` / session restored | Set `isAuthenticated = true`, load crews + saved maps in parallel |
| `signedOut` | Clear crews, saved maps, pending shares. Set `isAuthenticated = false` |
| `passwordRecovery` | Show `UpdatePasswordSheet` |

When `isAuthenticated` flips to `true`, the login screen is replaced (no animation, instant swap) with the authenticated content (map + top bar).

### Authenticated Content (Map Screen)

Full-screen map with overlaid UI. No navigation bars -- everything floats.

**Top bar** floats at top with 16dp horizontal padding. Three slots, left to right:

| Slot | Content | Behavior |
|------|---------|----------|
| **Left** | Crew chips overlay (future) or recording pill (future). Empty for Phase 1. | `maxWidth = Infinity`, left-aligned |
| **Center** | Notification center (future). Empty for Phase 1. | Fixed size, centered |
| **Right** | Account button -- circular gradient (blue-to-teal) with `person.circle.fill` icon (white, 26dp). Has press-scale animation (0.92x on press, spring return). | `maxWidth = Infinity`, right-aligned |

If an announcement button should appear, it sits to the left of the account button with 12dp spacing (future phase).

The top bar animates in on first appear: fades from 0 opacity + offset -20dp to full opacity + offset 0, using spring animation (response 0.4, damping 0.85). The top bar hides (same animation, reversed) when dataset control is expanded or a special mode is active (future phases).

### Account Hub (Settings Sheet)

Tapping the account button opens a modal bottom sheet with detents at medium and large (starts at large). Sheet has a drag indicator and allows background interaction.

The sheet contains a `NavigationStack` wrapping a `List` with these sections top to bottom:

1. **Welcome** -- Personal message from founders. "Share Feedback" secondary button (opens mailto).
2. **Account Settings** -- Navigation rows with icon + title + chevron:
   - "Preferred Region" (map icon) -> Region selection view
   - "Edit Profile" (person icon) -> Edit profile view
   - "Offline Mode" (radio waves icon) -> Offline management view
   - "Subscription" (star icon) -> Subscription management view
3. **Notifications** -- Single navigation row: "Notification Settings" (bell icon).
4. **Units** -- Unit preference rows (temperature, depth, distance, speed) with segmented pickers. Changes sync to Supabase `user_preferences` table.
5. **Map Theme** -- Theme selection row.
6. **Dataset Information** -- Dataset guide navigation row.
7. **About Salty** -- Four button rows:
   - "Take the Tour" (sparkles icon)
   - "Privacy Policy" (hand.raised icon) -> opens URL
   - "Terms of Use" (doc icon) -> opens URL
   - "Get Help" (question icon) -> opens mailto
8. **Sign Out** -- Destructive red button. Calls `AuthManager.signOut()`.
9. **Delete Account** -- Secondary button with trash icon. Two-step confirmation: first alert ("Are you sure?"), then final alert requiring user to type "DELETE".
10. **Diagnostics** -- Tile server and map cache diagnostic rows.
11. **Version footer** -- "Version X.Y.Z" caption + "Build N" sub-caption, centered.

Sheet title is "Settings" (large title). "Done" button in trailing toolbar dismisses.

Each section uses a "sunken" card style: `Color.sunken` background with 12dp corner radius.

### First-Time User Experience (FTUX)

Shown as a blocking full-screen cover when `AppPreferences.preferredRegionId == nil`. Cannot be dismissed by gesture (`interactiveDismissDisabled`).

1. **Loading state**: Logo (80x80), "Welcome to Salty Offshore" heading, "Loading available regions..." body text, large spinner. Shown while `regionStore.regions` is empty.
2. **Region list state**: Scrollable list with parallax header.
   - **Header**: Logo (72x72), "Welcome to Salty Offshore" heading, "Choose your home waters for offline access and personalized data" body, "You can change regions or explore others anytime" caption. Has parallax scale effect on scroll.
   - **Suggested section** (if location available): "SUGGESTED FOR YOU" header, single region row with star icon and "Nearest to your location" subtitle. Uses `LocationManager` to find nearest region.
   - **All regions**: Grouped by `group` field. Each group has uppercased header. Regions shown as rows: 80x60 thumbnail, region name, chevron. Tapping a row shows loading spinner on that row, calls `regionStore.browseToRegion(id, setAsPreferred: true)`, and the sheet auto-dismisses when `preferredRegionId` becomes non-nil.

### Scene Lifecycle

When app returns to foreground (`scenePhase == .active`):
- Refresh region data
- Refresh dataset data for selected region
- Check for announcements
- Resume background downloads

## Acceptance Criteria

1. User launches app with no session and sees the login screen with dot-grid background, logo, heading, and "Continue with email" button.
2. User taps "Continue with email" and the email/password form animates open.
3. User enters invalid email and taps Sign In -- sees red border and "Please enter a valid email" error.
4. User enters valid credentials and taps Sign In -- sees loading state, then lands on map screen.
5. User taps the gradient account button in the top-right corner -- Settings sheet opens at large detent.
6. Settings sheet contains all sections: Welcome, Account Settings (4 rows), Notifications, Units, Map Theme, Dataset Info, About Salty (4 rows), Sign Out, Delete Account, Diagnostics, Version.
7. User taps "Sign Out" in settings -- returns to login screen. App state (crews, saved maps) is cleared.
8. User signs up with new account via Sign Up sheet and is authenticated.
9. User with `preferredRegionId == nil` sees FTUX region selection full-screen cover.
10. User selects a region in FTUX -- region loads, FTUX dismisses, map shows selected region.
11. Auth state changes (session expiry, sign out from another device) correctly route to login.
12. Password reset flow: user taps "Forgot Password?", enters email, receives reset link, deep link opens UpdatePasswordSheet.
13. Top bar animates in on first appear and the account button has press-scale feedback.
14. Delete account flow: two confirmation dialogs, second requires typing "DELETE".

## Current Android State

**What exists:**
- `MainActivity.kt` -- Basic activity with `if/else` toggling between `MapScreen` and `SettingsScreen`. No auth gating, no DI container, no state machine.
- `AppViewModel.kt` -- God-object ViewModel owning regions, datasets, entries, rendering, zarr, preferences, depth, crosshair. 480 lines. No auth state.
- `AuthManager.kt` -- Singleton object with `signIn`, `signUp`, `resetPassword`, `signOut`. Has `hasStoredSession` and `currentUserId`. Works.
- `LoginScreen.kt` -- Exists with email/password form, dot-grid background. Basic but functional.
- `SignUpScreen.kt` -- Exists.
- `SettingsScreen.kt` -- Basic settings screen (not a sheet). Only has unit preferences and sign out. Not matching iOS AccountHub structure.
- `MapScreen.kt` -- Full map screen with dataset controls, crosshair, layers. Has a gear icon in top-right (not matching iOS account button).

**What's missing:**
- No `AppContainer` / DI container -- everything threaded through single ViewModel.
- No auth state machine (no listener for Supabase auth events).
- No `isAuthenticated` state gating at the Activity level.
- No FTUX region selection flow.
- No bottom sheet for settings (uses full-screen navigation).
- No TopBar component (three-slot layout).
- No AccountButton component (gradient circle with scale animation).
- No account deletion flow.
- No scene lifecycle handling (foreground refresh).

## iOS Reference Files

| File | What to port |
|------|-------------|
| `/Users/ryan/Developer/salty-ios/SaltyOffshore/SaltyOffshoreApp.swift` | Auth state machine (`listenForAuthChanges`), `isAuthenticated` gating, lifecycle (`scenePhase`), `handleSignOut` |
| `/Users/ryan/Developer/salty-ios/SaltyOffshore/ContentView.swift` | Top bar layout, sheet presentations (account, announcement, crew, FTUX), fullScreenCover for FTUX, `shouldHideTopUI`, appear animation |
| `/Users/ryan/Developer/salty-ios/SaltyOffshore/Core/AppContainer.swift` | DI container pattern, sync init, child ownership graph |
| `/Users/ryan/Developer/salty-ios/SaltyOffshore/Core/MapboxContainer.swift` | Map DI container, separation of app vs map state |
| `/Users/ryan/Developer/salty-ios/SaltyOffshore/Auth/LoginView.swift` | Login layout, "Continue with email" expand pattern, validation UX, header/auth/footer sections |
| `/Users/ryan/Developer/salty-ios/SaltyOffshore/Auth/SignUpView.swift` | Sign up sheet |
| `/Users/ryan/Developer/salty-ios/SaltyOffshore/Auth/ResetPasswordView.swift` | Reset password sheet |
| `/Users/ryan/Developer/salty-ios/SaltyOffshore/Auth/UpdatePasswordView.swift` | Update password sheet (deep link target) |
| `/Users/ryan/Developer/salty-ios/SaltyOffshore/Views/Common/TopBar.swift` | Three-slot layout (left/center/right), spacing, alignment |
| `/Users/ryan/Developer/salty-ios/SaltyOffshore/Views/Account/AccountButton.swift` | Gradient circle, press-scale animation |
| `/Users/ryan/Developer/salty-ios/SaltyOffshore/Views/Account/AccountHub.swift` | Full settings list, section order, delete account flow |
| `/Users/ryan/Developer/salty-ios/SaltyOffshore/Views/Account/WelcomeSectionView.swift` | Founders message + feedback button |
| `/Users/ryan/Developer/salty-ios/SaltyOffshore/Views/Account/AccountSettingsSectionView.swift` | Four navigation rows with icons |
| `/Users/ryan/Developer/salty-ios/SaltyOffshore/Views/Account/AboutSectionView.swift` | Tour, Privacy, Terms, Help rows |
| `/Users/ryan/Developer/salty-ios/SaltyOffshore/Views/Account/SignOutSection.swift` | Destructive sign out button |
| `/Users/ryan/Developer/salty-ios/SaltyOffshore/Views/Onboarding/FTUXRegionSelectionView.swift` | Region selection full-screen, parallax header, suggested region, loading state |

## Data Flow

### Auth State Machine

```
App launch
  -> SupabaseClient.auth.sessionStatus (Flow)
    -> SessionStatus.Authenticated -> isAuthenticated = true
       -> parallel: loadCrews(), loadSavedMaps()
    -> SessionStatus.NotAuthenticated -> isAuthenticated = false
    -> SessionStatus.LoadingFromStorage -> show loading (optional)

Sign out tap
  -> AuthManager.signOut()
  -> Supabase emits NotAuthenticated
  -> handleSignOut(): clear CrewStore, SavedMapViewModel, pending shares
  -> isAuthenticated = false
  -> UI swaps to LoginScreen
```

iOS types involved: `AuthManager.shared`, `SupabaseManager.shared.client.auth.authStateChanges`, events: `.initialSession`, `.signedIn`, `.signedOut`, `.passwordRecovery`.

### Account Button -> Settings Sheet

```
User taps AccountButton
  -> showingAccountSheet = true
  -> ModalBottomSheet(detents: [.medium, .large], selection: large)
    -> AccountHub() composable
      -> List of sections, each with own state
      -> "Sign Out" -> AuthManager.signOut() -> auth flow above
      -> "Done" -> dismiss()
```

### FTUX Flow

```
ContentView appears
  -> Check AppPreferences.preferredRegionId
  -> if nil: show FTUXRegionSelectionView as fullScreenCover
    -> RegionStore.regions loaded via API
    -> LocationManager.requestPermission() (non-blocking)
    -> User taps region row
      -> loadingRegionId = region.id (shows spinner on row)
      -> regionStore.browseToRegion(id, setAsPreferred: true)
      -> preferredRegionId set -> fullScreenCover binding flips -> dismisses
```

## Tasks

### 1. Create AppContainer (DI)

**Create:** `app/src/main/java/com/example/saltyoffshore/core/AppContainer.kt`

Kotlin `object` or class created in `Application.onCreate()`. Holds references to all shared state: `AppViewModel` (slimmed), `RegionStore` (new -- extracted from AppViewModel), `DatasetStore` (new -- extracted from AppViewModel). For Phase 1, focus on the auth-adjacent pieces: `AuthManager` reference, `isAuthenticated` StateFlow.

Read iOS: `AppContainer.swift` lines 30-100 for the ownership graph.

### 2. Auth State Machine in MainActivity

**Modify:** `app/src/main/java/com/example/saltyoffshore/MainActivity.kt`

Replace the `if/else` navigation with an auth state machine. Collect `SupabaseClient.auth.sessionStatus` as a Flow. When `Authenticated`, show `AuthenticatedContent`. When `NotAuthenticated`, show `LoginScreen`. Wire `handleSignOut()` to clear app state.

Read iOS: `SaltyOffshoreApp.swift` lines 233-248 (`mainContent`), lines 350-427 (`listenForAuthChanges`, `handleAuthReady`, `handleSignOut`).

### 3. TopBar Component

**Create:** `app/src/main/java/com/example/saltyoffshore/ui/components/TopBar.kt`

Three-slot `Row`: left (`Modifier.weight(1f)`, left-aligned), center (fixed), right (`Modifier.weight(1f)`, right-aligned). For Phase 1, left and center are empty spacers. Right contains `AccountButton`.

Add appear animation: `AnimatedVisibility` with fade + slide from top (-20dp), spring spec (stiffness ~medium, damping ~0.85).

Hide when `shouldHideTopUI` is true (wired in later phases).

Read iOS: `TopBar.swift` full file.

### 4. AccountButton Component

**Create:** `app/src/main/java/com/example/saltyoffshore/ui/components/AccountButton.kt`

Circular button with linear gradient (blue `0x1A99E6` to teal `0x33CCCC`, top-leading to bottom-trailing). White person icon (26dp). White border overlay at 0.3 opacity. Drop shadow. Press scale animation (0.92x, spring).

Read iOS: `AccountButton.swift` full file.

### 5. AccountHub Bottom Sheet

**Create:** `app/src/main/java/com/example/saltyoffshore/ui/screen/AccountHubSheet.kt`

`ModalBottomSheet` with `rememberSheetState(skipPartiallyExpanded = false)`. Contains scrollable column (or LazyColumn) with all sections matching iOS order:

- `WelcomeSection` -- founders message + feedback button
- `AccountSettingsSection` -- 4 navigation rows (Preferred Region, Edit Profile, Offline Mode, Subscription). For Phase 1, rows navigate to placeholder screens.
- `NotificationsSection` -- 1 navigation row (placeholder)
- `UnitsSection` -- existing unit pickers (migrate from SettingsScreen)
- `MapThemeSection` -- placeholder
- `DatasetInfoSection` -- placeholder
- `AboutSection` -- 4 button rows (Tour, Privacy Policy, Terms, Help)
- Sign Out button (destructive)
- Delete Account button (secondary, with two-step alert confirmation)
- `DiagnosticsSection` -- placeholder
- Version footer

Read iOS: `AccountHub.swift` lines 135-191 (`mainContent`).

### 6. Refactor LoginScreen

**Modify:** `app/src/main/java/com/example/saltyoffshore/ui/screen/LoginScreen.kt`

Restructure to match iOS three-section pattern: `HeaderSection` (logo + heading), `AuthSection` (email expand pattern with animation), `FooterSection` (sign up link). Add the "Continue with email" collapsed state -- initially hide form, show secondary button, animate form expansion on tap.

Read iOS: `LoginView.swift` full file.

### 7. FTUX Region Selection

**Create:** `app/src/main/java/com/example/saltyoffshore/ui/screen/FTUXRegionSelectionScreen.kt`

Full-screen dialog (`Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false))`) shown when `AppPreferences.preferredRegionId` is null.

Two states: loading (logo + spinner) and region list (parallax header + grouped list). Region rows: 80x60 thumbnail via `AsyncImage`, region name, chevron or spinner. Tapping sets preferred region and dismisses.

Optional: "Suggested for you" section using device location.

Read iOS: `FTUXRegionSelectionView.swift` full file.

### 8. Scene Lifecycle Handling

**Modify:** `app/src/main/java/com/example/saltyoffshore/MainActivity.kt`

Use `LifecycleEventObserver` to detect `ON_RESUME`. When returning to foreground after initial load: refresh region data, refresh dataset data, check announcements (future).

Read iOS: `SaltyOffshoreApp.swift` lines 320-339 (`onChange(of: scenePhase)`).

### 9. Delete Old SettingsScreen

**Delete:** `app/src/main/java/com/example/saltyoffshore/ui/screen/SettingsScreen.kt`

Migrate unit pickers into `AccountHubSheet`'s units section. Remove the `showSettings` boolean and full-screen settings navigation from `MainActivity`.

### 10. Wire Navigation

**Modify:** `app/src/main/java/com/example/saltyoffshore/MainActivity.kt`

Final wiring: `LoginScreen` (unauth) -> `AuthenticatedContent` (ZStack of MapScreen + TopBar) -> AccountButton tap -> `AccountHubSheet` bottom sheet. FTUX as blocking dialog overlay on authenticated content.

## Android-Native Choices

| iOS Pattern | Android Replacement | Why |
|-------------|-------------------|-----|
| `@State private var app: AppContainer` created in `App.init()` | `AppContainer` singleton or Hilt module created in `Application.onCreate()` | Android `Application` is the process-level singleton. Hilt `@Singleton` scope matches iOS app-lifetime containers. |
| `client.auth.authStateChanges` (AsyncSequence) | `supabase.auth.sessionStatus` (Kotlin Flow) | Supabase Kotlin SDK exposes `sessionStatus: StateFlow<SessionStatus>`. Collect in `LaunchedEffect` or ViewModel. |
| `.sheet(isPresented:)` with `presentationDetents([.medium, .large])` | `ModalBottomSheet` with `SheetState` | Compose Material3 `ModalBottomSheet` supports partial expansion. Use `rememberModalBottomSheetState()`. |
| `.fullScreenCover(isPresented:)` for FTUX | `Dialog(properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false))` or `if` gate at top of composable tree | Android has no `fullScreenCover`. A full-screen dialog or conditional composable achieves the same blocking behavior. |
| `@Environment(Type.self)` for DI | `CompositionLocal` or parameter passing from ViewModel | Compose `CompositionLocal` is closest to SwiftUI `@Environment`. For Phase 1, parameter passing from a single ViewModel is simpler; migrate to CompositionLocals when splitting state. |
| `UINavigationBarAppearance` (Spline Sans fonts) | `MaterialTheme` typography with Spline Sans font family in `Theme.kt` | Android theming is declarative via `MaterialTheme`. Define `SplineSans` as a `FontFamily` and apply in theme. |
| `scenePhase` lifecycle | `LifecycleEventObserver` on `ProcessLifecycleOwner` | Android lifecycle is Activity-based. `ProcessLifecycleOwner` gives app-level foreground/background events matching `scenePhase`. |
| Sign in with Apple | Google Sign-In (Credential Manager) | Platform-native auth. Defer to Phase 1.5 or later -- email auth is the priority. |
| `RevenueCatManager` (subscriptions) | RevenueCat Android SDK | Same vendor, different SDK. Stub the subscription section for Phase 1. |
