package com.example.saltyoffshore.data

/**
 * Represents the current queried value at crosshair position.
 * Matches iOS CurrentValue exactly.
 */
sealed class CurrentValue {
    /**
     * No value available (no data layer active).
     */
    data object None : CurrentValue()

    /**
     * Query in progress.
     */
    data object Loading : CurrentValue()

    /**
     * Position is over land (not water).
     */
    data object Land : CurrentValue()

    /**
     * Position is over water but no data available at this point.
     */
    data object NoData : CurrentValue()

    /**
     * Successfully queried value.
     */
    data class Value(
        val value: Double,
        val formattedValue: String,
        val unit: DatasetUnit
    ) : CurrentValue()

    val displayText: String
        get() = when (this) {
            is None -> ""
            is Loading -> "..."
            is Land -> "Land"
            is NoData -> "No Data"
            is Value -> "$formattedValue ${unit.symbol}"
        }

    val isOverWater: Boolean
        get() = this !is Land
}
