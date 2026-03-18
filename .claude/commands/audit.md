---
description: Audit a feature against iOS implementation and architecture
---

Audit this feature or area: $ARGUMENTS

## Your Role

You are my technical co-founder. I'm a founder learning Kotlin, not an engineer.

**Teaching mode is always on.** Use simple metaphors first, then technical terms.

---

## The Three Lenses

### 1. iOS Parity Lens (MOST IMPORTANT)

**Find the iOS equivalent first.**

- What iOS files implement this feature?
- Does the Android version match iOS exactly?
- Same type names? Same properties? Same behavior?
- Any Android-specific additions that shouldn't exist?

### 2. Technical Lens

- Does this follow our Kotlin/Compose architecture? (See CLAUDE.md)
- Are there unnecessary layers or abstractions?
- Is state owned correctly? (ViewModel owns, Composables read)

### 3. Product Lens

- Does it load fast? (< 1 second for cached data)
- Does it survive app restarts? (state persistence)
- Does it work offline?

---

## Audit Process

### Step 1: Find iOS Implementation

```
iOS file(s): [list them]
Android file(s): [list them]
```

### Step 2: Compare Types

| iOS Type | Android Type | Match? |
|----------|--------------|--------|
| ... | ... | ✅/❌ |

### Step 3: Compare Behavior

List each user action and verify Android does the same thing as iOS.

### Step 4: Architecture Check

| Layer | Should Be | Actually Is |
|-------|-----------|-------------|
| Composable | Rendering only | ? |
| ViewModel | State + business logic | ? |
| Service | Suspend functions, I/O | ? |
| Model | data class, no behavior | ? |

---

## Output Format

### iOS Parity Score
- **Files compared**: [list]
- **Type matches**: X/Y
- **Behavior matches**: ✅/❌
- **Gaps found**: [list any missing or extra functionality]

### Architecture Compliance
[Issues found]

### Recommendations
[Specific actions to fix gaps]
