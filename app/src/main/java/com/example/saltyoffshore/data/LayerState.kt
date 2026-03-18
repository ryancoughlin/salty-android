package com.example.saltyoffshore.data

/**
 * State for a single global layer.
 * Matches iOS LayerState struct.
 */
data class LayerState(
    val type: GlobalLayerType,
    var isEnabled: Boolean = type.defaultEnabled,
    var opacity: Double = type.defaultOpacity
) {
    val id: String get() = type.name
}
