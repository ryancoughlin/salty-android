package com.example.saltyoffshore.data

/**
 * State for breaks (temperature gradients) layer rendering.
 * Mirrors iOS BreaksLayerState exactly.
 */
data class BreaksLayerState(
    val opacity: Double,
    val strengthFilter: Set<String>,
    val selectedBreakId: String?,
    val sourceLayer: String,
    val sourceId: String,
    val layerId: String
) {
    val showStrong: Boolean
        get() = "strong" in strengthFilter

    val showModerate: Boolean
        get() = "moderate" in strengthFilter

    val showWeak: Boolean
        get() = "weak" in strengthFilter

    companion object {
        val ALL_STRENGTHS = setOf("strong", "moderate", "weak")

        fun default(
            sourceId: String,
            layerId: String
        ) = BreaksLayerState(
            opacity = 1.0,
            strengthFilter = ALL_STRENGTHS,
            selectedBreakId = null,
            sourceLayer = "breaks",
            sourceId = sourceId,
            layerId = layerId
        )
    }
}
