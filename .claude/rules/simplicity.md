---
paths:
  - "**/*.kt"
  - "**/*.xml"
---

# Simplicity and Modern Android

Every line of code must justify its existence. When in doubt, delete it.

## Writing Code

- Use the latest stable Android APIs and Kotlin idioms. No deprecated patterns.
- One way to do things. If two approaches exist, pick the simpler one and delete the other.
- Straight-line code: do the thing, return the result. Minimize branching.
- No wrapper classes, no interfaces with one implementation, no "just in case" abstractions.
- No defensive null chains unless the data genuinely arrives nullable from an external boundary.
- Coroutines and Flow, never callbacks. Suspend functions, never listeners.
- Compose state hoisting: ViewModel owns state, Composables render it. No in-between layers.

## Reviewing Code

After completing any implementation, self-review against these questions:

1. Can any class be deleted and its logic inlined?
2. Can any function be removed by using a stdlib or Compose API directly?
3. Are there parameters, properties, or imports that nothing uses?
4. Is there a simpler Compose API that does the same thing (e.g., `LazyColumn` items instead of manual `forEach`)?
5. Could two similar blocks be collapsed without adding abstraction?

If the answer to any of these is yes, fix it before presenting the work.

## Material Design as the Translation Layer

The iOS app defines *what* the experience is. Material Design 3 defines *how* to express it on Android.

- Translate iOS interactions into their Material 3 equivalent — don't copy iOS visual patterns literally.
- Use Material 3 components as-is: `TopAppBar`, `BottomSheetScaffold`, `NavigationBar`, `ModalBottomSheet`. Don't build custom versions of things Material already provides.
- Use Material 3 tokens for color, typography, and spacing — `MaterialTheme.colorScheme`, `MaterialTheme.typography`. Never hardcode values.
- Respect Android navigation idioms: bottom nav, back gestures, predictive back. Match the iOS *journey*, not the iOS *mechanism*.
- Dynamic color and dark theme support through Material 3's color system, not manual color switching.

## What "Simple" Means Here

- Fewer files > more files
- Fewer layers > more layers
- Fewer concepts to learn > more flexibility
- Direct API calls > wrapper services (unless reused 3+ times)
- Built-in Compose/Material components > custom implementations
- Great UX with minimal code > comprehensive code with average UX
