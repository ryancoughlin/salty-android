package com.example.saltyoffshore.config

/**
 * Data class representing map padding values.
 * Equivalent to iOS UIEdgeInsets.
 * Uses start/end instead of left/right for RTL support.
 */
data class MapPadding(
    val top: Int,
    val start: Int,
    val bottom: Int,
    val end: Int
)

