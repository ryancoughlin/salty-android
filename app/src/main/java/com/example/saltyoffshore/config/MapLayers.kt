package com.example.saltyoffshore.config

/**
 * Layer and source IDs for Mapbox, organized by type.
 * Mirrors iOS layer structure for consistency.
 */
object MapLayers {

    // MARK: - Region Layers (bounds, selection)
    object Region {
        const val OUTLINE_SOURCE = "region-outline-source"
        const val OUTLINE_LAYER = "region-outline-layer"
    }

    // MARK: - Global Layers (always visible, world-wide data)
    object Global {
        const val SST_SOURCE = "global-sst-source"
        const val SST_LAYER = "global-sst-layer"
        const val CURRENTS_SOURCE = "global-currents-source"
        const val CURRENTS_LAYER = "global-currents-layer"

        // Shaded Relief
        const val SHADED_RELIEF_SOURCE = "shaded-relief-source"
        const val SHADED_RELIEF_LAYER = "shaded-relief-layer"

        // Bathymetry
        const val BATHYMETRY_SOURCE = "bathymetry-source"
        const val BATHYMETRY_LINES_LAYER = "bathymetry-lines"
        const val BATHYMETRY_LABELS_LAYER = "bathymetry-labels"
        const val BATHYMETRY_MARKED_LINES_LAYER = "bathymetry-marked-lines"
        const val BATHYMETRY_MARKED_LABELS_LAYER = "bathymetry-marked-labels"

        // Shaded Bathymetry
        const val SHADED_BATHYMETRY_SOURCE = "shaded-bathymetry-source"
        const val SHADED_BATHYMETRY_LAYER = "shaded-bathymetry-layer"

        // Shipping Lanes
        const val SHIPPING_LANES_SOURCE = "shipping-lanes-source"
        const val SHIPPING_LANES_FILL_LAYER = "shipping-lanes-fill"
        const val SHIPPING_LANES_LINES_LAYER = "shipping-lanes-lines"

        // Marine Protected Areas
        const val MPA_SOURCE = "mpa-source"
        const val MPA_FILL_LAYER = "mpa-fill"
        const val MPA_PATTERN_LAYER = "mpa-pattern"
        const val MPA_OUTLINE_LAYER = "mpa-outline"
        const val MPA_LABELS_LAYER = "mpa-labels"

        // GPS Grid
        const val GPS_GRID_SOURCE = "coordinate-grid-source"
        const val GPS_GRID_COARSE_LINES_LAYER = "coordinate-grid-coarse-lines"
        const val GPS_GRID_COARSE_LABELS_LAYER = "coordinate-grid-coarse-labels"
        const val GPS_GRID_FINE_LINES_LAYER = "coordinate-grid-fine-lines"
        const val GPS_GRID_FINE_LABELS_LAYER = "coordinate-grid-fine-labels"

        // LORAN Grid
        const val LORAN_GRID_SOURCE = "loran-grid-source"
        const val LORAN_GRID_MAJOR_LAYER = "loran-grid-major"
        const val LORAN_GRID_MINOR_LAYER = "loran-grid-minor"
        const val LORAN_GRID_LABELS_MAJOR_LAYER = "loran-grid-labels-major"
        const val LORAN_GRID_LABELS_MINOR_LAYER = "loran-grid-labels-minor"

        // Artificial Reefs
        const val ARTIFICIAL_REEFS_SOURCE = "artificial-reefs-source"
        const val ARTIFICIAL_REEFS_LAYER = "artificial-reefs-symbols"

        // Stations (Buoys)
        const val STATIONS_SOURCE = "stations-source"
        const val STATIONS_LAYER = "stations-layer"
        const val STATIONS_CLUSTER_LAYER = "stations-cluster-layer"
        const val STATIONS_COUNT_LAYER = "stations-count-layer"
    }

    // MARK: - Dataset Layers (region-specific data)
    object Dataset {
        const val COG_SOURCE = "dataset-cog-source"
        const val COG_LAYER = "dataset-cog-layer"
        const val PMTILES_SOURCE = "dataset-pmtiles-source"
        const val CONTOURS_LAYER = "dataset-contours-layer"
        const val DATA_LAYER = "dataset-data-layer"
        const val FEATURES_LAYER = "dataset-features-layer"
    }

    // MARK: - Overlay Layers (user markers, annotations)
    object Overlay {
        const val MARKERS_SOURCE = "overlay-markers-source"
        const val MARKERS_LAYER = "overlay-markers-layer"
    }
}
