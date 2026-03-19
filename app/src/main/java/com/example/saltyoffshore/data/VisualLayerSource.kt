package com.example.saltyoffshore.data

import com.example.saltyoffshore.zarr.ZarrVisualLayer

/**
 * Visual layer source for dataset rendering.
 * Matches iOS `VisualLayerSource` exactly.
 *
 * Zarr GPU rendering is the only path — no fallbacks.
 */
sealed class VisualLayerSource {
    /**
     * Zarr GPU shader rendering.
     */
    data class Zarr(val renderer: ZarrVisualLayer) : VisualLayerSource()

    /**
     * No visual layer (dataset not loaded yet).
     */
    data object None : VisualLayerSource()
}
