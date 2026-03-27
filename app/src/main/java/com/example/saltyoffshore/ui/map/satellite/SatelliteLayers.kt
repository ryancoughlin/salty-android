package com.example.saltyoffshore.ui.map.satellite

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.example.saltyoffshore.viewmodel.SatelliteMode
import com.example.saltyoffshore.viewmodel.SatelliteStore
import com.example.saltyoffshore.viewmodel.SatelliteTrackingMode
import com.mapbox.maps.MapView

/**
 * Router composable for satellite map layers.
 * Switches between tracker and coverage layers based on mode.
 * Cleans up all satellite sources/layers when mode is deactivated.
 *
 * iOS ref: SatelliteLayers.swift
 */
@Composable
fun SatelliteLayersEffect(
    mapView: MapView,
    trackingMode: SatelliteTrackingMode,
    store: SatelliteStore
) {
    // Cleanup when satellite mode is deactivated
    LaunchedEffect(trackingMode.isActive) {
        if (!trackingMode.isActive) {
            removeSatelliteLayers(mapView)
        }
    }

    if (!trackingMode.isActive) return

    when (trackingMode.mode) {
        SatelliteMode.TRACKER -> {
            // Clean coverage layers when switching to tracker
            LaunchedEffect(Unit) { removeCoverageLayers(mapView) }

            if (store.tracks.isNotEmpty()) {
                SatelliteTrackLayerEffect(
                    mapView = mapView,
                    tracks = store.tracks,
                    selectedId = trackingMode.selectedTrackId
                )
            }
        }

        SatelliteMode.COVERAGE -> {
            // Clean tracker layers when switching to coverage
            LaunchedEffect(Unit) { removeTrackerLayers(mapView) }

            if (store.passes.isNotEmpty()) {
                CoveragePassLayerEffect(
                    mapView = mapView,
                    passes = store.passes,
                    selectedId = trackingMode.selectedPassId,
                    onPassTap = { id -> trackingMode.selectPass(id) }
                )
            }
        }
    }
}

// MARK: - Cleanup

private fun removeSatelliteLayers(mapView: MapView) {
    removeTrackerLayers(mapView)
    removeCoverageLayers(mapView)
}

private fun removeTrackerLayers(mapView: MapView) {
    val style = mapView.mapboxMap.style ?: return
    SATELLITE_TRACK_LAYER_IDS.forEach { id ->
        if (style.styleLayerExists(id)) style.removeStyleLayer(id)
    }
    SATELLITE_TRACK_SOURCE_IDS.forEach { id ->
        if (style.styleSourceExists(id)) style.removeStyleSource(id)
    }
}

private fun removeCoverageLayers(mapView: MapView) {
    val style = mapView.mapboxMap.style ?: return
    COVERAGE_PASS_LAYER_IDS.forEach { id ->
        if (style.styleLayerExists(id)) style.removeStyleLayer(id)
    }
    COVERAGE_PASS_SOURCE_IDS.forEach { id ->
        if (style.styleSourceExists(id)) style.removeStyleSource(id)
    }
}
