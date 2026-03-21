package com.example.saltyoffshore.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

// ── Spacing ──────────────────────────────────────────────────────────────────
// Mirrors iOS Spacing enum: small/medium/large/extraLarge
object Spacing {
    val small = 4.dp
    val medium = 12.dp
    val large = 16.dp
    val extraLarge = 24.dp
}

// ── Layout Constants ─────────────────────────────────────────────────────────
// Mirrors iOS Theme.Layout
object SaltyLayout {
    val padding = 16.dp
    val layerControlHeight = 64.dp
    val topBarElementHeight = 44.dp
    val controlCornerRadius = 6.dp
    val cardCornerRadius = 12.dp
}

// ── Control Sizes ────────────────────────────────────────────────────────────
// Mirrors iOS ControlSize enum
enum class SaltyControlSize(
    val iconSize: Int,
    val horizontalPadding: Int,
    val height: Int,
    val previewWidth: Int,
    val previewHeight: Int
) {
    Regular(iconSize = 13, horizontalPadding = 10, height = 28, previewWidth = 20, previewHeight = 12),
    Compact(iconSize = 10, horizontalPadding = 6, height = 22, previewWidth = 16, previewHeight = 10)
}

// ── Material 3 Color Schemes ─────────────────────────────────────────────────
// Mapped from iOS surface hierarchy to Material 3 semantic slots.

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
    outlineVariant = BorderSubtleDark
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
    outlineVariant = BorderSubtleLight
)

@Composable
fun SaltyOffshoreTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
