# Salty Android Design System

> Ported verbatim from iOS. This is a marine data instrument, not a lifestyle app.

---

## Core Principles

1. **Information first** — Data is the UI. Typography, spacing, and layout serve the numbers.
2. **Four surfaces, no more** — Sunken → Base → Raised → Overlay. Each level adds one step of depth.
3. **Contrast over color** — Use weight, size, and opacity for hierarchy. Color reserved for status/brand.
4. **Density with breathing room** — Pack data tightly within components, generous space between sections.

---

## Typography

**Font Family:** Spline Sans (text) and Spline Sans Mono (numbers only)

Access via `SaltyType` object in `Type.kt`.

| Token | Weight | Size | Use |
|-------|--------|------|-----|
| `SaltyType.headingLarge` | SemiBold | 28sp | Screen titles, sheet titles |
| `SaltyType.heading` | Medium | 20sp | Section headings, card titles |
| `SaltyType.body` | Medium | 16sp | Primary body text |
| `SaltyType.bodySmall` | Medium | 14sp | Buttons, control labels |
| `SaltyType.caption` | Medium | 12sp | Supporting text, metadata |
| `SaltyType.captionSmall` | Normal | 10sp | Tertiary info, badges, timestamps |
| `SaltyType.mono(size)` | Normal | Any | Numbers ONLY (not labels) |

**Mono Rule:** Fixed-width for numeric values to prevent layout reflow when values update.

**Three font sizes per screen max.** If you need four, information architecture is wrong.

---

## Spacing

Access via `Spacing` object in `Theme.kt`.

| Token | Value | Use |
|-------|-------|-----|
| `Spacing.small` | 4dp | Icon-to-label, dot separators, tight internal gaps |
| `Spacing.medium` | 12dp | Standard internal component padding |
| `Spacing.large` | 16dp | Screen edge insets, section internal padding |
| `Spacing.extraLarge` | 24dp | Between distinct sections on a screen |

---

## Layout Constants

Access via `SaltyLayout` object in `Theme.kt`.

| Token | Value | Use |
|-------|-------|-----|
| `SaltyLayout.padding` | 16dp | Screen edge padding |
| `SaltyLayout.controlCornerRadius` | 6dp | Buttons, pickers, chips |
| `SaltyLayout.cardCornerRadius` | 12dp | Cards, wells, containers |
| `SaltyLayout.topBarElementHeight` | 44dp | Top bar controls |
| `SaltyLayout.layerControlHeight` | 64dp | Layer control strip |

---

## Surface Hierarchy

**4 levels only. Never skip levels.**

Access via `SaltyColors` object in `Color.kt`.

| Level | Light | Dark | Use |
|-------|-------|------|-----|
| `SaltyColors.sunken` | #D4D4D4 | #171717 | Wells, inputs, recessed containers |
| `SaltyColors.base` | #E5E4E2 | #262626 | App background, navigation |
| `SaltyColors.raised` | #F5F5F5 | #323232 | Cards, panels, list rows |
| `SaltyColors.overlay` | #FFFFFF | #3D3D3D | Sheets, modals, bottom sheets |

**Rule:** Content on base uses raised. Content on raised uses overlay.

---

## Named Colors

| Color | Light | Dark | Use |
|-------|-------|------|-----|
| `SaltyColors.accent` | #3B909C | #3B909C | Primary actions, highlights (same both modes) |
| `SaltyColors.textPrimary` | #171717 | #FAFAFA | Main text |
| `SaltyColors.textSecondary` | #737373 | #A3A3A3 | Supporting text, metadata |
| `SaltyColors.borderSubtle` | #262626 @25% | #FEFEFF @10% | Dividers, card borders |
| `SaltyColors.iconButton` | #262626 | #A3A3A3 | Icon button tint |
| `SaltyColors.buttonText` | #D4EAFF | #042C2D | Text on accent buttons |
| `SaltyColors.glassText` | #171717 | #FEFEFF | Text on glass surfaces |

**Neutrals** (non-semantic, use sparingly):
- `Neutral100` = #F5F5F5
- `Neutral800` = #262626
- `Neutral900` = #171717

---

## Control Sizes

Access via `SaltyControlSize` enum in `Theme.kt`.

| Size | Icon | Horiz Padding | Height | Preview |
|------|------|---------------|--------|---------|
| `Regular` | 13dp | 10dp | 28dp | 20x12dp |
| `Compact` | 10dp | 6dp | 22dp | 16x10dp |

---

## Material 3 Mapping

The theme maps iOS surfaces to Material 3 semantic slots:

| Material 3 Slot | Salty Surface |
|-----------------|---------------|
| `background` | Base |
| `surface` | Raised |
| `surfaceVariant` | Sunken |
| `surfaceContainerHighest` | Overlay |
| `primary` | Accent |
| `onBackground` / `onSurface` | TextPrimary |
| `onSurfaceVariant` | TextSecondary |
| `outline` | BorderSubtle |

---

## Key Rules

1. **No hardcoded hex values in composables** — All colors come from `SaltyColors` or `MaterialTheme.colorScheme`
2. **Surface-first thinking** — Always ask "what surface level should this live on?"
3. **Mono for numbers only** — Never use mono for labels, units, or static text
4. **Type hierarchy via weight and size, not color** — Color is reserved for status/brand
5. **Icons are stroke-only** — 1.5dp line weight, geometric, dashed = inactive
6. **No dynamic color** — We use our own brand colors, not Material You wallpaper colors
7. **Dark mode first** — Fishing at night/dawn. Design dark, verify light.

---

## Logo Assets

| Asset | Resource | Use |
|-------|----------|-----|
| SaltyMark | `R.drawable.salty_mark` | Square logo mark (login header, about) |
| Logo Light | `R.drawable.logo_light` | Full logo for light backgrounds |
| Logo Dark | `R.drawable.logo_dark` | Full logo for dark backgrounds |

All assets available at mdpi, xhdpi, xxhdpi densities.

---

## File Locations

| File | Contains |
|------|----------|
| `ui/theme/Color.kt` | All color definitions + `SaltyColors` accessor |
| `ui/theme/Type.kt` | Typography tokens + `SaltyType` accessor |
| `ui/theme/Theme.kt` | `Spacing`, `SaltyLayout`, `SaltyControlSize`, Material 3 wiring |
| `ui/theme/ColorScales.kt` | Data visualization colormaps |
