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

## What "Simple" Means Here

- Fewer files > more files
- Fewer layers > more layers
- Fewer concepts to learn > more flexibility
- Direct API calls > wrapper services (unless reused 3+ times)
- Built-in Compose/Material components > custom implementations
- Great UX with minimal code > comprehensive code with average UX
