package com.example.saltyoffshore.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ── Accent (same in both modes) ──────────────────────────────────────────────
val SaltyAccent = Color(0xFF3B909C)

// ── Raw Color Values ────────────────────────────────────────────────────────
// Exact hex values from the iOS asset catalog.
// Feed into both M3 ColorScheme (Theme.kt) and SaltyColorTokens below.

// Surfaces
val SunkenLight = Color(0xFFD4D4D4)
val BaseLight = Color(0xFFE5E4E2)
val RaisedLight = Color(0xFFF5F5F5)
val OverlayLight = Color(0xFFFFFFFF)

val SunkenDark = Color(0xFF171717)
val BaseDark = Color(0xFF262626)
val RaisedDark = Color(0xFF323232)
val OverlayDark = Color(0xFF3D3D3D)

// Text
val TextPrimaryLight = Color(0xFF171717)
val TextPrimaryDark = Color(0xFFFAFAFA)

val TextSecondaryLight = Color(0xFF737373)
val TextSecondaryDark = Color(0xFFA3A3A3)

// UI Colors
val BorderSubtleLight = Color(0x40262626) // #262626 @ 25% alpha
val BorderSubtleDark = Color(0x1AFAFEFF)  // #FEFEFF @ 10% alpha

val IconButtonLight = Color(0xFF262626)
val IconButtonDark = Color(0xFFA3A3A3)

val ButtonTextLight = Color(0xFFD4EAFF)
val ButtonTextDark = Color(0xFF042C2D)

val GlassTextLight = Color(0xFF171717)
val GlassTextDark = Color(0xFFFEFEFF)

// Neutrals (same in both modes)
val Neutral100 = Color(0xFFF5F5F5)
val Neutral800 = Color(0xFF262626)
val Neutral900 = Color(0xFF171717)

// ── SaltyColorTokens ────────────────────────────────────────────────────────
// Immutable data class holding all Salty color tokens for a single theme mode.
// Provided via CompositionLocal so the theme sets it once, everything reads it.

@Immutable
data class SaltyColorTokens(
    // Surfaces (aliases — same values as M3 ColorScheme surface containers)
    val sunken: Color,
    val base: Color,
    val raised: Color,
    val overlay: Color,
    // Text
    val textPrimary: Color,
    val textSecondary: Color,
    // Accent
    val accent: Color,
    // UI
    val borderSubtle: Color,
    val iconButton: Color,
    val buttonText: Color,
    val glassText: Color,
    // Semantic
    val nowIndicator: Color,
    val success: Color,
    // Neutrals
    val neutral100: Color,
    val neutral800: Color,
    val neutral900: Color,
)

val LightSaltyColorTokens = SaltyColorTokens(
    sunken = SunkenLight,
    base = BaseLight,
    raised = RaisedLight,
    overlay = OverlayLight,
    textPrimary = TextPrimaryLight,
    textSecondary = TextSecondaryLight,
    accent = SaltyAccent,
    borderSubtle = BorderSubtleLight,
    iconButton = IconButtonLight,
    buttonText = ButtonTextLight,
    glassText = GlassTextLight,
    nowIndicator = Color(0xFFFFB333),
    success = Color(0xFF34C759),
    neutral100 = Neutral100,
    neutral800 = Neutral800,
    neutral900 = Neutral900,
)

val DarkSaltyColorTokens = SaltyColorTokens(
    sunken = SunkenDark,
    base = BaseDark,
    raised = RaisedDark,
    overlay = OverlayDark,
    textPrimary = TextPrimaryDark,
    textSecondary = TextSecondaryDark,
    accent = SaltyAccent,
    borderSubtle = BorderSubtleDark,
    iconButton = IconButtonDark,
    buttonText = ButtonTextDark,
    glassText = GlassTextDark,
    nowIndicator = Color(0xFFFFB333),
    success = Color(0xFF30D158),
    neutral100 = Neutral100,
    neutral800 = Neutral800,
    neutral900 = Neutral900,
)

val LocalSaltyColors = staticCompositionLocalOf { LightSaltyColorTokens }

// ── SaltyColors (backward-compatible accessor) ──────────────────────────────
// 141 call sites use SaltyColors.raised, SaltyColors.textPrimary, etc.
// This object delegates to the CompositionLocal so existing code keeps working.
// New code can use SaltyTheme.colors.raised instead — same result.

object SaltyColors {
    val sunken: Color @Composable get() = LocalSaltyColors.current.sunken
    val base: Color @Composable get() = LocalSaltyColors.current.base
    val raised: Color @Composable get() = LocalSaltyColors.current.raised
    val overlay: Color @Composable get() = LocalSaltyColors.current.overlay

    val textPrimary: Color @Composable get() = LocalSaltyColors.current.textPrimary
    val textSecondary: Color @Composable get() = LocalSaltyColors.current.textSecondary

    val accent: Color = SaltyAccent

    val borderSubtle: Color @Composable get() = LocalSaltyColors.current.borderSubtle
    val iconButton: Color @Composable get() = LocalSaltyColors.current.iconButton
    val buttonText: Color @Composable get() = LocalSaltyColors.current.buttonText
    val glassText: Color @Composable get() = LocalSaltyColors.current.glassText

    val nowIndicator: Color @Composable get() = LocalSaltyColors.current.nowIndicator
    val success: Color @Composable get() = LocalSaltyColors.current.success
}
