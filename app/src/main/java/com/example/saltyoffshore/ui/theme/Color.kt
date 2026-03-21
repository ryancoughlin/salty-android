package com.example.saltyoffshore.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme

// ── Accent (same in both modes) ──────────────────────────────────────────────
val SaltyAccent = Color(0xFF3B909C)

// ── Surfaces ─────────────────────────────────────────────────────────────────
// 4-level hierarchy: Sunken → Base → Raised → Overlay. Never skip levels.

// Light
val SunkenLight = Color(0xFFD4D4D4)
val BaseLight = Color(0xFFE5E4E2)
val RaisedLight = Color(0xFFF5F5F5)
val OverlayLight = Color(0xFFFFFFFF)

// Dark
val SunkenDark = Color(0xFF171717)
val BaseDark = Color(0xFF262626)
val RaisedDark = Color(0xFF323232)
val OverlayDark = Color(0xFF3D3D3D)

// ── Text ─────────────────────────────────────────────────────────────────────
val TextPrimaryLight = Color(0xFF171717)
val TextPrimaryDark = Color(0xFFFAFAFA)

val TextSecondaryLight = Color(0xFF737373)
val TextSecondaryDark = Color(0xFFA3A3A3)

// ── UI Colors ────────────────────────────────────────────────────────────────
val BorderSubtleLight = Color(0x40262626) // #262626 @ 25% alpha
val BorderSubtleDark = Color(0x1AFAFEFF)  // #FEFEFF @ 10% alpha

val IconButtonLight = Color(0xFF262626)
val IconButtonDark = Color(0xFFA3A3A3)

val ButtonTextLight = Color(0xFFD4EAFF)
val ButtonTextDark = Color(0xFF042C2D)

val GlassTextLight = Color(0xFF171717)
val GlassTextDark = Color(0xFFFEFEFF)

// ── Neutrals ─────────────────────────────────────────────────────────────────
val Neutral100 = Color(0xFFF5F5F5)
val Neutral800 = Color(0xFF262626)
val Neutral900 = Color(0xFF171717)

// ── Semantic accessors ───────────────────────────────────────────────────────
// Use these in Composables to get the correct color for current theme.

object SaltyColors {
    val sunken: Color @Composable get() = if (isSystemInDarkTheme()) SunkenDark else SunkenLight
    val base: Color @Composable get() = if (isSystemInDarkTheme()) BaseDark else BaseLight
    val raised: Color @Composable get() = if (isSystemInDarkTheme()) RaisedDark else RaisedLight
    val overlay: Color @Composable get() = if (isSystemInDarkTheme()) OverlayDark else OverlayLight

    val textPrimary: Color @Composable get() = if (isSystemInDarkTheme()) TextPrimaryDark else TextPrimaryLight
    val textSecondary: Color @Composable get() = if (isSystemInDarkTheme()) TextSecondaryDark else TextSecondaryLight

    val accent: Color = SaltyAccent

    val borderSubtle: Color @Composable get() = if (isSystemInDarkTheme()) BorderSubtleDark else BorderSubtleLight
    val iconButton: Color @Composable get() = if (isSystemInDarkTheme()) IconButtonDark else IconButtonLight
    val buttonText: Color @Composable get() = if (isSystemInDarkTheme()) ButtonTextDark else ButtonTextLight
    val glassText: Color @Composable get() = if (isSystemInDarkTheme()) GlassTextDark else GlassTextLight
}
