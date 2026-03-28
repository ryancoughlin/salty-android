package com.example.saltyoffshore.ui.map.satellite

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import com.example.saltyoffshore.data.satellite.RegionalPass
import com.example.saltyoffshore.data.satellite.SatelliteTrack
import com.example.saltyoffshore.viewmodel.SatelliteMode
import com.example.saltyoffshore.viewmodel.SatelliteStore
import com.example.saltyoffshore.viewmodel.SatelliteTrackingMode
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.gestures.gestures

private const val TAG = "SatelliteLayers"

/**
 * Router composable for satellite map layers.
 * Uses subscribeStyleLoaded + getStyle pattern to survive style reloads
 * (theme/projection changes destroy all custom sources/layers).
 *
 * iOS ref: SatelliteLayers.swift
 */
@Composable
fun SatelliteLayersEffect(
    mapView: MapView,
    trackingMode: SatelliteTrackingMode,
    store: SatelliteStore
) {
    // Keep references to latest data for style-loaded callback
    val currentMode by rememberUpdatedState(trackingMode.mode)
    val currentIsActive by rememberUpdatedState(trackingMode.isActive)
    val currentTracks by rememberUpdatedState(store.tracks)
    val currentPasses by rememberUpdatedState(store.passes)
    val currentSelectedTrackId by rememberUpdatedState(trackingMode.selectedTrackId)
    val currentSelectedPassId by rememberUpdatedState(trackingMode.selectedPassId)

    // Render function: applies layers to current style
    fun render() {
        if (!currentIsActive) return
        val style = mapView.mapboxMap.style ?: return

        when (currentMode) {
            SatelliteMode.TRACKER -> {
                // Remove coverage layers first
                removeLayers(style, COVERAGE_PASS_LAYER_IDS, COVERAGE_PASS_SOURCE_IDS)
                if (currentTracks.isNotEmpty()) {
                    renderTrackerLayers(style, currentTracks, currentSelectedTrackId)
                }
            }
            SatelliteMode.COVERAGE -> {
                // Remove tracker layers first
                removeLayers(style, SATELLITE_TRACK_LAYER_IDS, SATELLITE_TRACK_SOURCE_IDS)
                if (currentPasses.isNotEmpty()) {
                    renderCoverageLayers(style, currentPasses, currentSelectedPassId)
                }
            }
        }
    }

    // Subscribe to style reloads (theme/projection change wipes layers)
    DisposableEffect(mapView) {
        val cancelable = mapView.mapboxMap.subscribeStyleLoaded { _ ->
            Log.d(TAG, "Style reloaded — re-adding satellite layers")
            render()
        }

        // Also render immediately if style already loaded
        mapView.mapboxMap.getStyle { _ ->
            Log.d(TAG, "Style available — rendering satellite layers")
            render()
        }

        onDispose {
            cancelable.cancel()
            // Clean up all satellite layers on dispose
            mapView.mapboxMap.style?.let { style ->
                removeLayers(style, SATELLITE_TRACK_LAYER_IDS, SATELLITE_TRACK_SOURCE_IDS)
                removeLayers(style, COVERAGE_PASS_LAYER_IDS, COVERAGE_PASS_SOURCE_IDS)
            }
        }
    }

    // Re-render when data or selection changes
    LaunchedEffect(
        trackingMode.isActive,
        trackingMode.mode,
        store.tracks,
        store.passes,
        trackingMode.selectedTrackId,
        trackingMode.selectedPassId
    ) {
        if (!trackingMode.isActive) {
            // Clean up when deactivated
            mapView.mapboxMap.style?.let { style ->
                removeLayers(style, SATELLITE_TRACK_LAYER_IDS, SATELLITE_TRACK_SOURCE_IDS)
                removeLayers(style, COVERAGE_PASS_LAYER_IDS, COVERAGE_PASS_SOURCE_IDS)
            }
            return@LaunchedEffect
        }
        mapView.mapboxMap.getStyle { _ -> render() }
    }

    // Pin tap listener for coverage mode
    if (trackingMode.isActive && trackingMode.mode == SatelliteMode.COVERAGE) {
        val currentOnPassTap by rememberUpdatedState<(String) -> Unit> { id ->
            trackingMode.selectPass(id)
        }

        DisposableEffect(Unit) {
            val clickListener = com.mapbox.maps.plugin.gestures.OnMapClickListener { point ->
                val mapboxMap = mapView.mapboxMap
                val screenPoint = mapboxMap.pixelForCoordinate(point)
                val queryGeometry = com.mapbox.maps.RenderedQueryGeometry(screenPoint)
                val options = com.mapbox.maps.RenderedQueryOptions(listOf("coverage-pins-circle"), null)

                mapboxMap.queryRenderedFeatures(queryGeometry, options) { result ->
                    val tappedId = result.value?.firstOrNull()
                        ?.queriedFeature?.feature?.getStringProperty("id")
                    if (tappedId != null) {
                        currentOnPassTap(tappedId)
                    }
                }
                false
            }

            mapView.gestures.addOnMapClickListener(clickListener)
            onDispose { mapView.gestures.removeOnMapClickListener(clickListener) }
        }
    }
}

// MARK: - Cleanup

private fun removeLayers(
    style: com.mapbox.maps.Style,
    layerIds: List<String>,
    sourceIds: List<String>
) {
    layerIds.forEach { id ->
        if (style.styleLayerExists(id)) style.removeStyleLayer(id)
    }
    sourceIds.forEach { id ->
        if (style.styleSourceExists(id)) style.removeStyleSource(id)
    }
}
