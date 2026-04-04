package com.example.saltyoffshore.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ── Salty Shadow ─────────────────────────────────────────────────────────────
// Mirrors iOS Theme.Shadow (ShadowStyle).
//
// iOS shadows use (opacity, blur radius, y offset).
// Compose shadow() uses (elevation, shape, ambientColor, spotColor).
//
// The mapping: elevation ≈ blur/2. Compose renders the shadow from the shape's
// outline, so we pass the shape through. Colors use black at the iOS alpha.
//
// | iOS Token        | opacity | blur | y  | → Compose elevation |
// |-----------------|---------|------|----|---------------------|
// | Shadow.floating  | 0.15    | 8dp  | 4  | 4.dp                |
// | Shadow.inline    | 0.08    | 2dp  | 1  | 1.dp                |

data class SaltyShadowStyle(
    val elevation: Dp,
    val ambientAlpha: Float,
    val spotAlpha: Float,
)

object SaltyShadow {
    /** Floating UI over map — capsules, controls, buttons.
     *  iOS: opacity 0.15, radius 8, y 4 */
    val floating = SaltyShadowStyle(
        elevation = 4.dp,
        ambientAlpha = 0.08f,
        spotAlpha = 0.15f,
    )

    /** Subtle inline elements — segmented controls, chips.
     *  iOS: opacity 0.08, radius 2, y 1 */
    val inline = SaltyShadowStyle(
        elevation = 1.dp,
        ambientAlpha = 0.04f,
        spotAlpha = 0.08f,
    )
}

// ── Modifier Extensions ─────────────────────────────────────────────────────
// Usage: Modifier.saltyShadow(SaltyShadow.floating)
// The shape defaults to card corner radius. Override for pills, circles, etc.

fun Modifier.saltyShadow(
    style: SaltyShadowStyle,
    shape: Shape = RoundedCornerShape(12.dp),
): Modifier = shadow(
    elevation = style.elevation,
    shape = shape,
    ambientColor = Color.Black.copy(alpha = style.ambientAlpha),
    spotColor = Color.Black.copy(alpha = style.spotAlpha),
)

/** Convenience: floating shadow. Mirrors iOS .elevation() modifier. */
fun Modifier.saltyElevation(
    shape: Shape = RoundedCornerShape(12.dp),
): Modifier = saltyShadow(SaltyShadow.floating, shape)
