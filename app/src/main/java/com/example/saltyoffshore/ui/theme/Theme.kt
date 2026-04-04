package com.example.saltyoffshore.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ── Spacing ──────────────────────────────────────────────────────────────────
// Mirrors iOS Spacing enum exactly.
object Spacing {
    val small = 4.dp
    val medium = 12.dp
    val large = 16.dp
    val extraLarge = 24.dp
    val xxLarge = 42.dp
}

// ── Layout Constants ─────────────────────────────────────────────────────────
// Mirrors iOS Theme.Layout. Corner radii live in Shape.kt.
object SaltyLayout {
    val padding = 16.dp
    val layerControlHeight = 64.dp
    val topBarElementHeight = 44.dp
}

// ── Control Sizes ────────────────────────────────────────────────────────────
// Mirrors iOS ControlSize enum. All values in Dp for direct use in Compose.
enum class SaltyControlSize(
    val height: Dp,
    val iconSize: Dp,
    val horizontalPadding: Dp,
    val previewWidth: Dp,
    val previewHeight: Dp,
) {
    Regular(height = 28.dp, iconSize = 13.dp, horizontalPadding = 10.dp, previewWidth = 20.dp, previewHeight = 12.dp),
    Compact(height = 22.dp, iconSize = 10.dp, horizontalPadding = 6.dp, previewWidth = 16.dp, previewHeight = 10.dp),
}

// ── Material 3 Color Schemes ─────────────────────────────────────────────────
// Maps iOS 4-level surface hierarchy into M3 semantic slots.
// SaltyColors (via CompositionLocal) provides the same values with iOS names.

private val DarkColorScheme = darkColorScheme(
    primary = SaltyAccent,
    onPrimary = ButtonTextDark,
    secondary = TextSecondaryDark,
    onSecondary = TextPrimaryDark,
    background = BaseDark,
    onBackground = TextPrimaryDark,
    surface = RaisedDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SunkenDark,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainer = BaseDark,
    surfaceContainerHigh = RaisedDark,
    surfaceContainerHighest = OverlayDark,
    surfaceContainerLow = SunkenDark,
    outline = BorderSubtleDark,
    outlineVariant = BorderSubtleDark,
)

private val LightColorScheme = lightColorScheme(
    primary = SaltyAccent,
    onPrimary = ButtonTextLight,
    secondary = TextSecondaryLight,
    onSecondary = TextPrimaryLight,
    background = BaseLight,
    onBackground = TextPrimaryLight,
    surface = RaisedLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SunkenLight,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainer = BaseLight,
    surfaceContainerHigh = RaisedLight,
    surfaceContainerHighest = OverlayLight,
    surfaceContainerLow = SunkenLight,
    outline = BorderSubtleLight,
    outlineVariant = BorderSubtleLight,
)

// ── SaltyTheme ──────────────────────────────────────────────────────────────
// The single entry point. Wraps MaterialExpressiveTheme and provides
// SaltyColors via CompositionLocal.
//
// Usage:
//   MaterialTheme.colorScheme.primary     → M3 tokens (auto-themes M3 components)
//   SaltyTheme.colors.raised              → iOS-parity naming for custom composables
//   MaterialTheme.typography.bodyLarge     → text styles
//   MaterialTheme.shapes.extraLarge       → card corners (12dp)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SaltyOffshoreTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val saltyColors = if (darkTheme) DarkSaltyColorTokens else LightSaltyColorTokens

    CompositionLocalProvider(LocalSaltyColors provides saltyColors) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = SaltyShapes,
            motionScheme = MotionScheme.expressive(),
            content = content,
        )
    }
}

object SaltyTheme {
    val colors: SaltyColorTokens
        @Composable get() = LocalSaltyColors.current
}
