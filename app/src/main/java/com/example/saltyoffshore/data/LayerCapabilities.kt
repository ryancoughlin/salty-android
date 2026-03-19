package com.example.saltyoffshore.data

/**
 * Data-driven layer capabilities for datasets.
 * Defines what visual features a dataset type supports.
 * Matches iOS LayerCapabilities OptionSet exactly.
 */
data class LayerCapabilities(
    /** Supports visual layer rendering (COG/Zarr) */
    val hasVisualLayer: Boolean = false,
    /** Supports contour lines */
    val hasContours: Boolean = false,
    /** Supports directional arrows (vectors) */
    val hasArrows: Boolean = false,
    /** Supports particle animation */
    val hasParticles: Boolean = false,
    /** Supports SSH feature detection (eddys, fronts) */
    val hasSshFeatures: Boolean = false,
    /** Supports numeric value display on map */
    val hasNumbers: Boolean = false,
    /** Supports depth layer selection */
    val hasDepthSelection: Boolean = false,
    /** Supports manual entry selection (satellite passes) */
    val hasEntrySelection: Boolean = false
)
