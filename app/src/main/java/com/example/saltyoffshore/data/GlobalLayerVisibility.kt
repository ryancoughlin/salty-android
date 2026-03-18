package com.example.saltyoffshore.data

/**
 * Snapshot of layer visibility state for efficient map rendering.
 *
 * Value-type snapshot pattern prevents expensive recomposition:
 * - Equatable check prevents rebuilds when values unchanged
 * - Map only rebuilds when visibility actually changes
 *
 * Matches iOS GlobalLayerVisibility struct.
 */
data class GlobalLayerVisibility(
    val enabledLayers: Set<GlobalLayerType>,
    val opacities: Map<GlobalLayerType, Double>
) {
    companion object {
        /** Default visibility with default enabled layers */
        val default: GlobalLayerVisibility
            get() = GlobalLayerVisibility(
                enabledLayers = GlobalLayerType.entries
                    .filter { it.defaultEnabled }
                    .toSet(),
                opacities = GlobalLayerType.entries
                    .associate { it to it.defaultOpacity }
            )
    }
}
