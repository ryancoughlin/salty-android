package com.example.saltyoffshore.ui.map.globallayers

import android.content.Context
import android.util.Log
import com.example.saltyoffshore.data.DepthUnits
import com.example.saltyoffshore.data.GlobalLayerType
import com.example.saltyoffshore.data.GlobalLayerVisibility
import com.example.saltyoffshore.data.LoranRegionConfig
import com.example.saltyoffshore.data.Station
import com.example.saltyoffshore.data.Tournament
import com.mapbox.maps.MapboxMap

private const val TAG = "GlobalLayers"

/**
 * Orchestrates rendering of all global map layers.
 *
 * Performance pattern: Uses visibility snapshot to prevent expensive rebuilds.
 * - Snapshot is a value type, Equatable check prevents rebuilds when unchanged
 * - Map only rebuilds when visibility actually changes
 *
 * Matches iOS GlobalLayers.
 */
class GlobalLayers(
    private val context: Context,
    private val mapboxMap: MapboxMap
) {
    // Layer instances
    private var shadedReliefLayer: ShadedReliefLayer? = null
    private var shadedBathymetryLayer: ShadedBathymetryLayer? = null
    private var bathymetryLayer: BathymetryLayer? = null
    private var shippingLanesLayer: ShippingLanesLayer? = null
    private var marineProtectedAreasLayer: MarineProtectedAreasLayer? = null
    private var gpsGridLayer: GPSGridLayer? = null
    private var loranGridLayer: LORANGridLayer? = null
    private var stationsLayer: StationsLayer? = null
    private var artificialReefsLayer: ArtificialReefsLayer? = null
    private var tournamentsLayer: TournamentsLayer? = null

    // Cached state for change detection
    private var lastVisibility: GlobalLayerVisibility? = null
    private var lastLoranConfig: LoranRegionConfig? = null
    private var lastTournament: Tournament? = null
    private var lastStations: List<Station> = emptyList()

    /**
     * Update all global layers based on visibility snapshot.
     */
    fun update(
        visibility: GlobalLayerVisibility,
        depthUnits: DepthUnits = DepthUnits.FATHOMS,
        loranConfig: LoranRegionConfig? = null,
        selectedTournament: Tournament? = null,
        stations: List<Station> = emptyList()
    ) {
        // Dedup: skip if nothing changed since last update
        if (visibility == lastVisibility && loranConfig == lastLoranConfig &&
            selectedTournament == lastTournament && stations == lastStations) {
            Log.d(TAG, "update() skipped — no changes")
            return
        }

        Log.d(TAG, "update() called with ${visibility.enabledLayers.size} enabled layers")
        Log.d(TAG, "  Enabled: ${visibility.enabledLayers.map { it.name }}")

        // Shaded Relief (read-only, always included in visibility)
        updateShadedRelief(visibility)

        // Shaded Bathymetry
        updateShadedBathymetry(visibility)

        // Bathymetry Contours
        updateBathymetry(visibility, depthUnits)

        // Shipping Lanes
        updateShippingLanes(visibility)

        // Marine Protected Areas
        updateMarineProtectedAreas(visibility)

        // GPS Grid
        updateGPSGrid(visibility)

        // LORAN Grid
        updateLORANGrid(visibility, loranConfig)

        // Stations
        updateStations(visibility, stations)

        // Artificial Reefs
        updateArtificialReefs(visibility)

        // Tournaments
        updateTournaments(visibility, selectedTournament)

        // Cache state
        lastVisibility = visibility
        lastLoranConfig = loranConfig
        lastTournament = selectedTournament
        lastStations = stations
    }

    // MARK: - Individual Layer Updates

    private fun updateShadedRelief(visibility: GlobalLayerVisibility) {
        val enabled = visibility.enabledLayers.contains(GlobalLayerType.SHADED_RELIEF)
        Log.d(TAG, "updateShadedRelief: enabled=$enabled, existing=${shadedReliefLayer != null}")

        if (enabled) {
            if (shadedReliefLayer == null) {
                Log.d(TAG, "Creating ShadedReliefLayer")
                shadedReliefLayer = ShadedReliefLayer(mapboxMap)
                shadedReliefLayer?.addToMap()
                Log.d(TAG, "ShadedReliefLayer added to map")
            }
        } else {
            shadedReliefLayer?.removeFromMap()
            shadedReliefLayer = null
        }
    }

    private fun updateShadedBathymetry(visibility: GlobalLayerVisibility) {
        val enabled = visibility.enabledLayers.contains(GlobalLayerType.SHADED_BATHYMETRY)
        val opacity = visibility.opacities[GlobalLayerType.SHADED_BATHYMETRY] ?: 1.0

        if (enabled) {
            if (shadedBathymetryLayer == null) {
                shadedBathymetryLayer = ShadedBathymetryLayer(mapboxMap, opacity)
                shadedBathymetryLayer?.addToMap()
            } else {
                shadedBathymetryLayer?.updateOpacity(opacity)
            }
        } else {
            shadedBathymetryLayer?.removeFromMap()
            shadedBathymetryLayer = null
        }
    }

    private fun updateBathymetry(visibility: GlobalLayerVisibility, depthUnits: DepthUnits) {
        val enabled = visibility.enabledLayers.contains(GlobalLayerType.BATHYMETRY)
        val opacity = visibility.opacities[GlobalLayerType.BATHYMETRY] ?: 0.2

        if (enabled) {
            if (bathymetryLayer == null) {
                bathymetryLayer = BathymetryLayer(mapboxMap, depthUnits, opacity)
                bathymetryLayer?.addToMap()
            } else {
                bathymetryLayer?.updateOpacity(opacity)
            }
        } else {
            bathymetryLayer?.removeFromMap()
            bathymetryLayer = null
        }
    }

    private fun updateShippingLanes(visibility: GlobalLayerVisibility) {
        val enabled = visibility.enabledLayers.contains(GlobalLayerType.SHIPPING_LANES)
        val opacity = visibility.opacities[GlobalLayerType.SHIPPING_LANES] ?: 0.4

        if (enabled) {
            if (shippingLanesLayer == null) {
                shippingLanesLayer = ShippingLanesLayer(mapboxMap, opacity)
                shippingLanesLayer?.addToMap()
            } else {
                shippingLanesLayer?.updateOpacity(opacity)
            }
        } else {
            shippingLanesLayer?.removeFromMap()
            shippingLanesLayer = null
        }
    }

    private fun updateMarineProtectedAreas(visibility: GlobalLayerVisibility) {
        val enabled = visibility.enabledLayers.contains(GlobalLayerType.MARINE_PROTECTED_AREAS)
        val opacity = visibility.opacities[GlobalLayerType.MARINE_PROTECTED_AREAS] ?: 0.7

        if (enabled) {
            if (marineProtectedAreasLayer == null) {
                marineProtectedAreasLayer = MarineProtectedAreasLayer(mapboxMap, opacity)
                marineProtectedAreasLayer?.addToMap()
            } else {
                marineProtectedAreasLayer?.updateOpacity(opacity)
            }
        } else {
            marineProtectedAreasLayer?.removeFromMap()
            marineProtectedAreasLayer = null
        }
    }

    private fun updateGPSGrid(visibility: GlobalLayerVisibility) {
        val enabled = visibility.enabledLayers.contains(GlobalLayerType.GPS_GRID_LINES)
        val opacity = visibility.opacities[GlobalLayerType.GPS_GRID_LINES] ?: 0.2

        if (enabled) {
            if (gpsGridLayer == null) {
                gpsGridLayer = GPSGridLayer(mapboxMap, opacity)
                gpsGridLayer?.addToMap()
            } else {
                gpsGridLayer?.updateOpacity(opacity)
            }
        } else {
            gpsGridLayer?.removeFromMap()
            gpsGridLayer = null
        }
    }

    private fun updateLORANGrid(visibility: GlobalLayerVisibility, loranConfig: LoranRegionConfig?) {
        val enabled = visibility.enabledLayers.contains(GlobalLayerType.LORAN_GRID_LINES)
        val opacity = visibility.opacities[GlobalLayerType.LORAN_GRID_LINES] ?: 0.2

        // Need to recreate if config changed
        val configChanged = loranConfig != lastLoranConfig

        if (enabled && loranConfig != null) {
            if (loranGridLayer == null || configChanged) {
                loranGridLayer?.removeFromMap()
                loranGridLayer = LORANGridLayer(mapboxMap, opacity, loranConfig)
                loranGridLayer?.addToMap()
            } else {
                loranGridLayer?.updateOpacity(opacity)
            }
        } else {
            loranGridLayer?.removeFromMap()
            loranGridLayer = null
        }
    }

    private fun updateStations(visibility: GlobalLayerVisibility, stations: List<Station>) {
        val enabled = visibility.enabledLayers.contains(GlobalLayerType.STATIONS)
        val opacity = visibility.opacities[GlobalLayerType.STATIONS] ?: 1.0

        if (enabled && stations.isNotEmpty()) {
            if (stationsLayer == null) {
                stationsLayer = StationsLayer(context, mapboxMap, opacity)
                stationsLayer?.addToMap(stations)
            } else if (stations != lastStations) {
                stationsLayer?.updateStations(stations)
            }
            stationsLayer?.updateOpacity(opacity)
        } else {
            stationsLayer?.removeFromMap()
            stationsLayer = null
        }
    }

    private fun updateArtificialReefs(visibility: GlobalLayerVisibility) {
        val enabled = visibility.enabledLayers.contains(GlobalLayerType.ARTIFICIAL_REEFS)
        val opacity = visibility.opacities[GlobalLayerType.ARTIFICIAL_REEFS] ?: 1.0

        if (enabled) {
            if (artificialReefsLayer == null) {
                artificialReefsLayer = ArtificialReefsLayer(mapboxMap, opacity)
                artificialReefsLayer?.addToMap()
            } else {
                artificialReefsLayer?.updateOpacity(opacity)
            }
        } else {
            artificialReefsLayer?.removeFromMap()
            artificialReefsLayer = null
        }
    }

    private fun updateTournaments(visibility: GlobalLayerVisibility, selectedTournament: Tournament?) {
        val enabled = visibility.enabledLayers.contains(GlobalLayerType.TOURNAMENTS)
        val opacity = visibility.opacities[GlobalLayerType.TOURNAMENTS] ?: 1.0

        // Need to recreate if tournament changed
        val tournamentChanged = selectedTournament?.id != lastTournament?.id

        if (enabled && selectedTournament != null && selectedTournament.hasValidBoundary) {
            if (tournamentsLayer == null || tournamentChanged) {
                tournamentsLayer?.removeFromMap()
                tournamentsLayer = TournamentsLayer(mapboxMap, selectedTournament, opacity)
                tournamentsLayer?.addToMap()
            } else {
                tournamentsLayer?.updateOpacity(opacity)
            }
        } else {
            tournamentsLayer?.removeFromMap()
            tournamentsLayer = null
        }
    }

    /**
     * Remove all global layers from map.
     */
    fun removeAll() {
        shadedReliefLayer?.removeFromMap()
        shadedReliefLayer = null

        shadedBathymetryLayer?.removeFromMap()
        shadedBathymetryLayer = null

        bathymetryLayer?.removeFromMap()
        bathymetryLayer = null

        shippingLanesLayer?.removeFromMap()
        shippingLanesLayer = null

        marineProtectedAreasLayer?.removeFromMap()
        marineProtectedAreasLayer = null

        gpsGridLayer?.removeFromMap()
        gpsGridLayer = null

        loranGridLayer?.removeFromMap()
        loranGridLayer = null

        stationsLayer?.removeFromMap()
        stationsLayer = null

        artificialReefsLayer?.removeFromMap()
        artificialReefsLayer = null

        tournamentsLayer?.removeFromMap()
        tournamentsLayer = null

        lastVisibility = null
        lastLoranConfig = null
        lastTournament = null
        lastStations = emptyList()
    }
}
