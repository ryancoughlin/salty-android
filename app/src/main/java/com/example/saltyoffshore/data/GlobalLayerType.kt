package com.example.saltyoffshore.data

import com.example.saltyoffshore.R

/**
 * All global layer types available in the app.
 * Matches iOS GlobalLayerType enum exactly.
 */
enum class GlobalLayerType(
    val displayName: String,
    val iconRes: Int,
    val category: OverlayCategory,
    val defaultEnabled: Boolean,
    val defaultOpacity: Double,
    val supportsOpacity: Boolean
) {
    SHADED_RELIEF(
        displayName = "Shaded Relief",
        iconRes = R.drawable.ic_mountain,
        category = OverlayCategory.MAP_LAYERS,
        defaultEnabled = true,
        defaultOpacity = 1.0,
        supportsOpacity = true
    ),
    BATHYMETRY(
        displayName = "Bathymetry",
        iconRes = R.drawable.ic_water_waves,
        category = OverlayCategory.MAP_LAYERS,
        defaultEnabled = true,
        defaultOpacity = 0.2,
        supportsOpacity = true
    ),
    SHADED_BATHYMETRY(
        displayName = "Shaded Bathymetry",
        iconRes = R.drawable.ic_water_waves,
        category = OverlayCategory.MAP_LAYERS,
        defaultEnabled = false,
        defaultOpacity = 1.0,
        supportsOpacity = true
    ),
    SHIPPING_LANES(
        displayName = "Shipping Lanes",
        iconRes = R.drawable.ic_ferry,
        category = OverlayCategory.MAP_LAYERS,
        defaultEnabled = false,
        defaultOpacity = 0.4,
        supportsOpacity = true
    ),
    MARINE_PROTECTED_AREAS(
        displayName = "Marine Protected Areas",
        iconRes = R.drawable.ic_shield,
        category = OverlayCategory.MAP_LAYERS,
        defaultEnabled = false,
        defaultOpacity = 0.7,
        supportsOpacity = true
    ),
    STATIONS(
        displayName = "Offshore Buoys",
        iconRes = R.drawable.ic_antenna,
        category = OverlayCategory.MARKERS,
        defaultEnabled = true,
        defaultOpacity = 1.0,
        supportsOpacity = false
    ),
    GPS_GRID_LINES(
        displayName = "GPS Grid Lines",
        iconRes = R.drawable.ic_grid,
        category = OverlayCategory.MAP_LAYERS,
        defaultEnabled = false,
        defaultOpacity = 0.2,
        supportsOpacity = true
    ),
    LORAN_GRID_LINES(
        displayName = "LORAN Grid Lines",
        iconRes = R.drawable.ic_grid,
        category = OverlayCategory.MAP_LAYERS,
        defaultEnabled = false,
        defaultOpacity = 0.2,
        supportsOpacity = true
    ),
    WAYPOINTS(
        displayName = "Waypoints",
        iconRes = R.drawable.ic_mappin,
        category = OverlayCategory.MARKERS,
        defaultEnabled = true,
        defaultOpacity = 1.0,
        supportsOpacity = false
    ),
    TOURNAMENTS(
        displayName = "Tournament Boundaries",
        iconRes = R.drawable.ic_trophy,
        category = OverlayCategory.MARKERS,
        defaultEnabled = false,
        defaultOpacity = 1.0,
        supportsOpacity = true
    ),
    ARTIFICIAL_REEFS(
        displayName = "Artificial Reefs",
        iconRes = R.drawable.ic_hexagon,
        category = OverlayCategory.MARKERS,
        defaultEnabled = false,
        defaultOpacity = 1.0,
        supportsOpacity = false
    ),
    WIND(
        displayName = "Weather Forecast",
        iconRes = R.drawable.ic_wind,
        category = OverlayCategory.CONDITIONS,
        defaultEnabled = false,
        defaultOpacity = 1.0,
        supportsOpacity = true
    ),
    FADS(
        displayName = "FADs",
        iconRes = R.drawable.ic_mappin_circle,
        category = OverlayCategory.MARKERS,
        defaultEnabled = true,
        defaultOpacity = 1.0,
        supportsOpacity = false
    );

    companion object {
        /** Layer types shown in the UI (excludes hidden layers) */
        val uiVisibleTypes: List<GlobalLayerType>
            get() = entries.filter { it != SHADED_RELIEF && it != SHADED_BATHYMETRY }
    }
}
