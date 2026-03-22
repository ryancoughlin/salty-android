package com.example.saltyoffshore.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

// ── Salty Motion Tokens ──────────────────────────────────────────────────────
// Centralizes spring specs to match iOS animation configs.
// M3 Expressive's MotionScheme.expressive() handles component internals
// (sheets, switches, buttons). These tokens are for custom animations.
//
// | Token        | Damping | Stiffness  | iOS Equivalent              |
// |-------------|---------|------------|-----------------------------|
// | springDefault| 0.80    | MediumLow  | response 0.4, damping 0.8   |
// | springSnappy | 0.85    | MediumLow  | response 0.4, damping 0.85  |
// | springBouncy | 0.70    | MediumLow  | response 0.35, damping 0.7  |
// | springMedium | 0.80    | Medium     | response 0.3, damping 0.8   |
// | springQuick  | 0.80    | 3000       | response 0.2, damping 0.8   |

object SaltyMotion {

    /** General-purpose spring — toolbar animations, state transitions */
    fun <T> springDefault() = spring<T>(
        dampingRatio = 0.8f,
        stiffness = Spring.StiffnessMediumLow,
    )

    /** Slightly damped — top bar entrance, controlled reveals */
    fun <T> springSnappy() = spring<T>(
        dampingRatio = 0.85f,
        stiffness = Spring.StiffnessMediumLow,
    )

    /** Noticeable bounce — account button, playful interactions */
    fun <T> springBouncy() = spring<T>(
        dampingRatio = 0.7f,
        stiffness = Spring.StiffnessMediumLow,
    )

    /** Medium response — dataset control expand/collapse */
    fun <T> springMedium() = spring<T>(
        dampingRatio = 0.8f,
        stiffness = Spring.StiffnessMedium,
    )

    /** Fast response — depth selector, quick interactions */
    fun <T> springQuick() = spring<T>(
        dampingRatio = 0.8f,
        stiffness = 3000f, // Between Medium (1500) and High (10000)
    )

    /** Slow, smooth — region annotations, map-level elements */
    fun <T> springSlow() = spring<T>(
        dampingRatio = 0.8f,
        stiffness = Spring.StiffnessLow,
    )
}
