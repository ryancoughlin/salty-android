package com.example.saltyoffshore.data

/**
 * Observable value container for crosshair/overlay feature queries.
 * Matches iOS CurrentValue exactly.
 */
data class CurrentValue(
    val value: Double? = null,
    val apiUnit: DatasetUnit? = null,
    val datasetType: DatasetType? = null
) {
    val shouldShow: Boolean get() = value != null
}
