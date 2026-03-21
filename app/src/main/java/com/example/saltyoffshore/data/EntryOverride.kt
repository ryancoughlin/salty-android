package com.example.saltyoffshore.data

/**
 * Unified override for overlay entry selection.
 * Replaces separate sstSelectedEntry and selectedDepth fields.
 * Matches iOS EntryOverride exactly.
 */
data class EntryOverride(
    /** Manual timestamp selection (null = use primary entry's time) */
    val timestamp: String? = null,
    /** Depth layer selection (default surface) */
    val depth: Int = 0
)
