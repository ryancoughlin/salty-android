package com.example.saltyoffshore.data

/**
 * Manual time/depth override for any dataset entry.
 * Matches iOS EntryOverride exactly.
 */
data class EntryOverride(
    val entryId: String,
    val timestamp: String? = null,
    val depth: Double? = null
)
