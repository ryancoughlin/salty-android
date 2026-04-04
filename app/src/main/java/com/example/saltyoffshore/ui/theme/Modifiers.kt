package com.example.saltyoffshore.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ── Surface Modifiers ───────────────────────────────────────────────────────
// Direct ports of iOS Theme.swift View extensions.
// Each builds on M3 primitives — colors from MaterialTheme.colorScheme,
// shapes from MaterialTheme.shapes.
//
// iOS                          → Android
// .card()                      → Modifier.card()
// .well()                      → Modifier.well()
// .instrumentCard()            → Modifier.instrumentCard()
// .elevation()                 → Modifier.saltyElevation()     (in Shadow.kt)
// .controlContainer(size:)     → Modifier.controlContainer()
// .listSection()               → Modifier.listSection()
//
// How it works:
//   Text("Hello", modifier = Modifier.card())
//
// This is exactly like SwiftUI's Text("Hello").card() — same idea, same result.

/** Standard raised card — default container for grouped data.
 *  iOS: Color.raised background + 12dp corners. No shadow, no border. */
@Composable
fun Modifier.card(): Modifier {
    val shape = MaterialTheme.shapes.extraLarge // 12dp
    return this
        .clip(shape)
        .background(MaterialTheme.colorScheme.surfaceContainerHigh, shape)
}

/** Sunken content well — inset area for charts, scales, stat groups.
 *  iOS: Color.sunken background + 12dp corners. */
@Composable
fun Modifier.well(): Modifier {
    val shape = MaterialTheme.shapes.extraLarge // 12dp
    return this
        .clip(shape)
        .background(MaterialTheme.colorScheme.surfaceContainerLow, shape)
}

/** Instrument panel card — Garmin/Simrad marine display aesthetic.
 *  iOS: Color.raised + BorderSubtle stroke + floating shadow + 12dp corners. */
@Composable
fun Modifier.instrumentCard(): Modifier {
    val shape = MaterialTheme.shapes.extraLarge // 12dp
    return this
        .saltyShadow(SaltyShadow.floating, shape)
        .clip(shape)
        .background(MaterialTheme.colorScheme.surfaceContainerHigh, shape)
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
}

/** Layer card — selectable card for layer/overlay lists.
 *  iOS: raised bg when selected, clear when not, always has BorderSubtle stroke. */
@Composable
fun Modifier.layerCard(selected: Boolean): Modifier {
    val shape = MaterialTheme.shapes.large // 8dp
    val bg = if (selected) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent
    return this
        .clip(shape)
        .background(bg, shape)
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
}

/** Control container — pickers, toggles, buttons.
 *  iOS: gray 15% opacity bg + controlCornerRadius (6dp). */
@Composable
fun Modifier.controlContainer(size: SaltyControlSize = SaltyControlSize.Regular): Modifier {
    val shape = MaterialTheme.shapes.medium // 6dp
    return this
        .clip(shape)
        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), shape)
        .height(size.height)
        .padding(horizontal = size.horizontalPadding)
}

/** List section — sunken background for grouped rows.
 *  iOS: Color.sunken bg + vertical padding. */
@Composable
fun Modifier.listSection(): Modifier {
    return this
        .background(MaterialTheme.colorScheme.surfaceContainerLow)
        .padding(vertical = Spacing.medium)
}
