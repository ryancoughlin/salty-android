package com.example.saltyoffshore.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ── Salty Shapes ─────────────────────────────────────────────────────────────
// Maps the existing radius vocabulary to M3 shape slots.
// Mirrors iOS Theme.swift: controlCornerRadius (6) and cardCornerRadius (12).
//
// | Slot             | Radius | Use                                  |
// |-----------------|--------|--------------------------------------|
// | extraSmall       | 2.dp   | Drag handles, thin bars              |
// | small            | 4.dp   | Gradient pointers, minimal rounding  |
// | medium           | 6.dp   | Controls (buttons, toggles, pickers) |
// | large            | 8.dp   | Text fields, smaller cards           |
// | extraLarge       | 12.dp  | Cards, wells, instrument panels      |

val SaltyShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(12.dp),
)
