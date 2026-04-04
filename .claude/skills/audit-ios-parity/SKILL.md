---
name: audit-ios-parity
description: Audit Android code against the iOS implementation for parity. Use when implementing features, porting iOS code, reviewing changes, or before committing.
allowed-tools: Read Grep Glob
---

# iOS Parity Audit

Audit: $ARGUMENTS

## Step 1: Find the iOS Implementation

Look in `/Users/ryan/Developer/salty-ios/SaltyOffshore/` for the matching iOS files.

Translation guide:
- Android ViewModel → iOS `@Observable class`
- Android Service → iOS class in `Services/`
- Android Composable → iOS SwiftUI `View`
- Android `data class` → iOS `struct` with `Codable`

## Step 2: Read Both Versions

Read every line of the iOS file(s) and the Android file(s). Do not skim.

## Step 3: Compare

Check these match **exactly**:

| Check | What to Compare |
|-------|----------------|
| **Type names** | Same names (adjusted for platform conventions) |
| **Properties** | Same fields, same types, same defaults |
| **Methods** | Same signatures, same logic, same return types |
| **Business logic** | Same validation, same calculations, same branching |
| **Error handling** | Same error cases, same user-facing messages |
| **UX flow** | Same user journey, same interactions |
| **UI structure** | Same layout hierarchy, same spacing, same colors |

## Step 4: Check Completeness

- [ ] UI exists and renders
- [ ] ViewModel holds state via StateFlow
- [ ] Service calls are wired end-to-end
- [ ] Data flows from API response to screen
- [ ] No TODOs, stubs, or placeholder implementations

## Step 5: Report

Format your output as:

### Parity Score

| iOS File | Android File | Match |
|----------|-------------|-------|
| ... | ... | Full / Partial / Missing |

### Deviations Found
List each difference: what iOS does vs what Android does.

### Missing Implementations
Features in iOS that have no Android equivalent yet.

### Recommended Fixes
Specific, actionable steps to close each gap.
