package com.example.saltyoffshore.ui.map.satellite

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.example.saltyoffshore.viewmodel.SatelliteMode
import com.example.saltyoffshore.viewmodel.SatelliteStore
import com.example.saltyoffshore.viewmodel.SatelliteTrackingMode
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.plugin.gestures.gestures

private const val TAG = "SatelliteLayers"

/**
 * Satellite map layers — lives inside MapboxMap { } composable.
 * Uses MapEffect to get mapView directly (no nullable state ref).
 * Subscribes to style reload events so layers survive theme/projection changes.
 *
 * Pattern matches GlobalLayersEffect exactly.
 *
 * iOS ref: SatelliteLayers.swift
 */
@Composable
fun SatelliteLayersEffect(
    trackingMode: SatelliteTrackingMode,
    store: SatelliteStore
) {
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    // Keep latest values for async callbacks
    val currentIsActive by rememberUpdatedState(trackingMode.isActive)
    val currentMode by rememberUpdatedState(trackingMode.mode)
    val currentTracks by rememberUpdatedState(store.tracks)
    val currentPasses by rememberUpdatedState(store.passes)
    val currentSelectedTrackId by rememberUpdatedState(trackingMode.selectedTrackId)
    val currentSelectedPassId by rememberUpdatedState(trackingMode.selectedPassId)

    fun render(mapboxMap: MapboxMap) {
        val style = mapboxMap.style ?: return

        if (!currentIsActive) {
            removeLayers(style, SATELLITE_TRACK_LAYER_IDS, SATELLITE_TRACK_SOURCE_IDS)
            removeLayers(style, COVERAGE_PASS_LAYER_IDS, COVERAGE_PASS_SOURCE_IDS)
            return
        }

        when (currentMode) {
            SatelliteMode.TRACKER -> {
                removeLayers(style, COVERAGE_PASS_LAYER_IDS, COVERAGE_PASS_SOURCE_IDS)
                if (currentTracks.isNotEmpty()) {
                    renderTrackerLayers(style, currentTracks, currentSelectedTrackId)
                }
            }
            SatelliteMode.COVERAGE -> {
                removeLayers(style, SATELLITE_TRACK_LAYER_IDS, SATELLITE_TRACK_SOURCE_IDS)
                if (currentPasses.isNotEmpty()) {
                    renderCoverageLayers(style, currentPasses, currentSelectedPassId)
                }
            }
        }
    }

    // Get MapView + subscribe to style reloads (fires inside MapboxMap composable)
    MapEffect(Unit) { mapView ->
        Log.d(TAG, "MapEffect: got mapView")
        mapViewRef = mapView

        // Re-add layers after every style reload (theme/projection change)
        mapView.mapboxMap.subscribeStyleLoaded { _ ->
            Log.d(TAG, "Style reloaded — re-rendering satellite layers")
            render(mapView.mapboxMap)
        }

        // Also render immediately if style already loaded
        mapView.mapboxMap.getStyle { _ ->
            Log.d(TAG, "Style available — initial satellite layer render")
            render(mapView.mapboxMap)
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
        val mapView = mapViewRef ?: return@LaunchedEffect
        // Call render directly — style is guaranteed loaded since MapEffect already ran
        render(mapView.mapboxMap)
    }

    // Clean up on dispose
    DisposableEffect(Unit) {
        onDispose {
            mapViewRef?.mapboxMap?.style?.let { style ->
                removeLayers(style, SATELLITE_TRACK_LAYER_IDS, SATELLITE_TRACK_SOURCE_IDS)
                removeLayers(style, COVERAGE_PASS_LAYER_IDS, COVERAGE_PASS_SOURCE_IDS)
            }
        }
    }

    // Pin tap listener for coverage mode
    val mv = mapViewRef
    if (mv != null && trackingMode.isActive && trackingMode.mode == SatelliteMode.COVERAGE) {
        val currentOnPassTap by rememberUpdatedState<(String) -> Unit> { id ->
            trackingMode.selectPass(id)
        }

        DisposableEffect(Unit) {
            val clickListener = com.mapbox.maps.plugin.gestures.OnMapClickListener { point ->
                val mapboxMap = mv.mapboxMap
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

            mv.gestures.addOnMapClickListener(clickListener)
            onDispose { mv.gestures.removeOnMapClickListener(clickListener) }
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
