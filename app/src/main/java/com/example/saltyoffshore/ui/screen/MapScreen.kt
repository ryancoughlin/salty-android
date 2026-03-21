package com.example.saltyoffshore.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.saltyoffshore.ui.controls.LayersControlSheet
import com.example.saltyoffshore.ui.controls.RightSideToolbar
import com.example.saltyoffshore.config.AppConstants
import com.example.saltyoffshore.data.AppStatus
import com.example.saltyoffshore.data.RegionStatus
import com.example.saltyoffshore.ui.components.CrosshairOverlay
import com.example.saltyoffshore.ui.components.DatasetSelectorSheet
import com.example.saltyoffshore.ui.components.DepthSelector
import com.example.saltyoffshore.ui.components.DatasetFilterSheet
import com.example.saltyoffshore.ui.components.RegionAnnotationView
import com.example.saltyoffshore.ui.components.SaltyDatasetControl
import com.example.saltyoffshore.data.Dataset
import com.example.saltyoffshore.data.GlobalLayerVisibility
import com.example.saltyoffshore.data.LoranRegionConfig
import com.example.saltyoffshore.data.RegionMetadata
import com.example.saltyoffshore.data.Station
import com.example.saltyoffshore.data.TimeEntry
import com.example.saltyoffshore.data.Tournament
import com.example.saltyoffshore.data.VisualLayerSource
import com.example.saltyoffshore.config.CrosshairConstants
import com.example.saltyoffshore.managers.CrosshairFeatureQueryManager
import android.util.Log
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.ui.platform.LocalDensity
import com.example.saltyoffshore.data.DatasetType
import com.example.saltyoffshore.ui.map.RegionBoundsEffect
import com.example.saltyoffshore.ui.map.layers.DatasetLayers
import com.example.saltyoffshore.ui.map.globallayers.GlobalLayers
import com.example.saltyoffshore.ui.theme.SaltyColors
import com.example.saltyoffshore.ui.theme.SaltyLayout
import com.example.saltyoffshore.ui.theme.Spacing
import com.example.saltyoffshore.viewmodel.AppViewModel
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.ViewAnnotation
import com.mapbox.maps.extension.compose.style.MapStyle
import com.example.saltyoffshore.data.DatasetRenderingSnapshot
import com.mapbox.maps.extension.compose.style.projection.generated.Projection
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.viewannotation.geometry
import com.mapbox.maps.viewannotation.viewAnnotationOptions

private const val TAG = "MapScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: AppViewModel = viewModel(),
    onSettingsClick: () -> Unit = {}
) {
    // Sheet state for layers control
    var showLayersSheet by remember { mutableStateOf(false) }
    val layersSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Sheet state for dataset selector
    var showDatasetSheet by remember { mutableStateOf(false) }
    val datasetSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Sheet state for filter range
    var showFilterSheet by remember { mutableStateOf(false) }
    val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(-60.0, 30.0))
            zoom(AppConstants.mapInitialWorldZoom)
            bearing(0.0)
            pitch(0.0)
        }
    }

    // Fly to region when selected — key on ID only, not full object
    LaunchedEffect(viewModel.selectedRegion?.id) {
        viewModel.selectedRegion?.let { region ->
            val bounds = region.bounds
            val centerLon = (bounds[0][0] + bounds[1][0]) / 2.0
            val centerLat = (bounds[0][1] + bounds[1][1]) / 2.0

            mapViewportState.flyTo(
                cameraOptions = CameraOptions.Builder()
                    .center(Point.fromLngLat(centerLon, centerLat))
                    .zoom(AppConstants.mapDefaultZoom)
                    .bearing(0.0)
                    .pitch(0.0)
                    .build(),
                animationOptions = MapAnimationOptions.Builder()
                    .duration(AppConstants.mapDefaultAnimationDuration)
                    .build()
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = mapViewportState,
            style = { MapStyle(style = AppConstants.lightMapStyleURI, projection = Projection.MERCATOR) }
        ) {
            // Wire up repaint callback for Zarr frame updates
            ZarrRepaintEffect(viewModel = viewModel)

            // Region bounds outline
            RegionBoundsEffect(region = viewModel.selectedRegion)

            // Dataset visualization layers (Zarr GPU, Contours, Currents, etc.)
            DatasetLayersEffect(
                dataset = viewModel.selectedDataset,
                entry = viewModel.selectedEntry,
                region = viewModel.selectedRegion,
                snapshot = viewModel.renderingSnapshot,
                visualSource = viewModel.visualSource
            )

            // Global overlay layers (bathymetry, shipping lanes, etc.)
            GlobalLayersEffect(
                visibility = viewModel.globalLayerManager.visibility,
                loranConfig = viewModel.globalLayerManager.selectedLoranConfig,
                selectedTournament = viewModel.globalLayerManager.selectedTournament,
                stations = emptyList() // TODO: Wire up stations from API
            )

            // Crosshair feature query on camera changes
            CrosshairQueryEffect(
                isDataLayerActive = viewModel.isDataLayerActive,
                datasetType = viewModel.currentDatasetType,
                onValueChanged = { viewModel.updateCurrentValue(it) },
                onCameraChanged = { zoom, lat -> viewModel.updateCameraState(zoom, lat) }
            )

            // Region annotations
            viewModel.regions.forEach { region ->
                ViewAnnotation(
                    options = viewAnnotationOptions {
                        geometry(Point.fromLngLat(region.centerLon, region.centerLat))
                        allowOverlap(true)
                    }
                ) {
                    RegionAnnotationView(
                        region = region,
                        isComingSoon = region.status == RegionStatus.COMING_SOON,
                        onClick = { viewModel.onRegionSelected(region.id) }
                    )
                }
            }
        }

        // Crosshair overlay
        CrosshairOverlay(
            currentValue = viewModel.currentValue,
            zoom = viewModel.currentZoom,
            latitude = viewModel.currentLatitude,
            isDataLayerActive = viewModel.isDataLayerActive
        )

        // Settings button (top-right)
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = Spacing.large)
                .size(SaltyLayout.topBarElementHeight)
                .background(SaltyColors.raised.copy(alpha = 0.9f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = SaltyColors.iconButton
            )
        }

        // Loading overlay
        if (viewModel.appStatus is AppStatus.Loading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = SaltyColors.accent)
            }
        }

        // Depth selector (right side)
        if (viewModel.depthFilterState.hasSelection) {
            DepthSelector(
                depthFilter = viewModel.depthFilterState,
                onDepthSelected = { viewModel.onDepthSelected(it) },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = Spacing.large)
            )
        }

        // Dataset control (bottom)
        if (viewModel.selectedDataset != null) {
            SaltyDatasetControl(
                dataset = viewModel.selectedDataset!!,
                entry = viewModel.selectedEntry,
                snapshot = viewModel.renderingSnapshot,
                onEntrySelected = { viewModel.selectEntry(it) },
                onChange = { showDatasetSheet = true },
                onFilter = { showFilterSheet = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            )
        }

        // Right side toolbar (layers button)
        if (viewModel.selectedDataset != null) {
            RightSideToolbar(
                onLayersClick = { showLayersSheet = true },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = Spacing.large)
                    .padding(top = 100.dp) // Below depth selector area
            )
        }

        // Layers control sheet
        if (showLayersSheet && viewModel.selectedDataset != null && viewModel.primaryConfig != null) {
            LayersControlSheet(
                // Dataset layer props
                dataset = viewModel.selectedDataset!!,
                config = viewModel.primaryConfig!!,
                onConfigChanged = { viewModel.updatePrimaryConfig(it) },
                isPrimary = true,
                // Overlay layer props
                layersByCategory = viewModel.globalLayerManager.layersByCategory,
                onOverlayToggle = { viewModel.globalLayerManager.toggleEnabled(it) },
                onOverlayOpacity = { type, opacity -> viewModel.globalLayerManager.setOpacity(type, opacity) },
                selectedLoranConfig = viewModel.globalLayerManager.selectedLoranConfig,
                onLoranConfigChange = { viewModel.globalLayerManager.setLoranRegion(it) },
                tournaments = viewModel.globalLayerManager.allTournaments,
                selectedTournament = viewModel.globalLayerManager.selectedTournament,
                onTournamentSelect = { viewModel.globalLayerManager.selectTournament(it) },
                onTournamentDeselect = { viewModel.globalLayerManager.deselectTournament() },
                // Sheet props
                sheetState = layersSheetState,
                onDismiss = { showLayersSheet = false }
            )
        }

        // Dataset selector sheet
        if (showDatasetSheet && viewModel.selectedRegion != null) {
            DatasetSelectorSheet(
                datasets = viewModel.selectedRegion!!.activeDatasets,
                selectedDataset = viewModel.selectedDataset,
                sheetState = datasetSheetState,
                onDatasetSelected = { dataset ->
                    viewModel.selectDataset(dataset)
                },
                onDismiss = { showDatasetSheet = false }
            )
        }

        // Dataset filter sheet
        if (showFilterSheet && viewModel.primaryConfig != null && viewModel.selectedDataset != null) {
            val config = viewModel.primaryConfig!!
            val dataset = viewModel.selectedDataset!!
            val datasetType = DatasetType.fromRawValue(dataset.type) ?: DatasetType.SST
            val entry = viewModel.selectedEntry
            val rangeKey = datasetType.rangeKey
            val rangeData = entry?.ranges?.get(rangeKey)
            val dataRange = if (rangeData?.min != null && rangeData.max != null) {
                rangeData.min..rangeData.max
            } else {
                viewModel.renderingSnapshot.dataMin..viewModel.renderingSnapshot.dataMax
            }

            DatasetFilterSheet(
                config = config,
                dataRange = dataRange,
                unit = rangeData?.unit ?: "°F",
                onConfigChanged = { newConfig ->
                    viewModel.updatePrimaryConfig(newConfig)
                },
                onDragRangeChanged = { min, max ->
                    viewModel.repaint?.invoke()
                },
                onDismiss = { showFilterSheet = false }
            )
        }
    }
}

/**
 * Effect that manages dataset visualization layers.
 * Uses MapEffect to get MapboxMap reference and renders layers via DatasetLayers.
 *
 * IMPORTANT: DatasetLayers must be remembered to persist layer state across renders.
 * Otherwise toggling layers won't work (fresh instance has null layer references).
 */
@Composable
private fun DatasetLayersEffect(
    dataset: Dataset?,
    entry: TimeEntry?,
    region: RegionMetadata?,
    snapshot: DatasetRenderingSnapshot,
    visualSource: VisualLayerSource
) {
    // Remember layer manager - recreate only when region changes
    val regionId = region?.id
    var datasetLayers by remember { mutableStateOf<DatasetLayers?>(null) }

    // Clean up old layers when region changes
    DisposableEffect(regionId) {
        onDispose {
            datasetLayers?.removeAllLayers()
            datasetLayers = null
        }
    }

    // Render layers when any input changes
    MapEffect(regionId, entry?.id, snapshot, visualSource) { mapView ->
        val mapboxMap = mapView.mapboxMap

        // Create layer manager if needed (first render or region changed)
        if (datasetLayers == null) {
            datasetLayers = DatasetLayers(mapboxMap)
        }

        // Render layers
        datasetLayers?.render(
            dataset = dataset,
            entry = entry,
            region = region,
            snapshot = snapshot,
            visualSource = visualSource
        )
    }
}

/**
 * Effect that wires up the repaint callback for Zarr frame updates.
 * Must be called once when map is ready.
 */
@Composable
private fun ZarrRepaintEffect(viewModel: AppViewModel) {
    MapEffect(Unit) { mapView ->
        viewModel.repaint = {
            mapView.mapboxMap.triggerRepaint()
        }
    }
}

/**
 * Effect that queries features at crosshair position when camera changes.
 */
@Composable
private fun CrosshairQueryEffect(
    isDataLayerActive: Boolean,
    datasetType: DatasetType?,
    onValueChanged: (com.example.saltyoffshore.data.CurrentValue) -> Unit,
    onCameraChanged: (zoom: Double, latitude: Double) -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val yOffsetPx = with(density) { CrosshairConstants.yOffset.toPx() }

    MapEffect(key1 = isDataLayerActive, key2 = datasetType) { mapView ->
        val mapboxMap = mapView.mapboxMap

        // Create query manager
        val queryManager = CrosshairFeatureQueryManager(mapboxMap, scope)

        // Subscribe to camera changes
        val cancelable = mapboxMap.subscribeCameraChanged { _ ->
            val cameraState = mapboxMap.cameraState
            val zoom = cameraState.zoom
            val center = cameraState.center
            val latitude = center.latitude()

            // Update camera state
            onCameraChanged(zoom, latitude)

            // Calculate crosshair screen position (center + yOffset)
            val screenCenterX = mapView.width / 2.0
            val screenCenterY = mapView.height / 2.0 + yOffsetPx

            if (isDataLayerActive) {
                val screenPoint = ScreenCoordinate(screenCenterX, screenCenterY)
                queryManager.queryAtPoint(screenPoint, zoom, datasetType, onValueChanged)
            }
        }

        // Suspend until cancelled (keeps MapEffect alive), then cleanup
        try {
            kotlinx.coroutines.awaitCancellation()
        } finally {
            cancelable.cancel()
        }
    }
}

/**
 * Effect that manages global overlay layers.
 * Uses MapEffect to get MapboxMap reference and renders layers via GlobalLayers.
 *
 * Important: Uses getStyle callback to ensure style is loaded before adding layers.
 * Uses rememberUpdatedState to avoid stale closure captures in callbacks.
 */
@Composable
private fun GlobalLayersEffect(
    visibility: GlobalLayerVisibility,
    loranConfig: LoranRegionConfig?,
    selectedTournament: Tournament?,
    stations: List<Station>
) {
    var globalLayers by remember { mutableStateOf<GlobalLayers?>(null) }
    var mapboxMapRef by remember { mutableStateOf<com.mapbox.maps.MapboxMap?>(null) }

    // Use rememberUpdatedState to always have latest values in callbacks
    val currentVisibility by androidx.compose.runtime.rememberUpdatedState(visibility)
    val currentLoranConfig by androidx.compose.runtime.rememberUpdatedState(loranConfig)
    val currentTournament by androidx.compose.runtime.rememberUpdatedState(selectedTournament)
    val currentStations by androidx.compose.runtime.rememberUpdatedState(stations)

    // Clean up on dispose
    DisposableEffect(Unit) {
        onDispose {
            globalLayers?.removeAll()
            globalLayers = null
        }
    }

    // Get map reference once and subscribe to style loaded
    MapEffect(Unit) { mapView ->
        Log.d(TAG, "GlobalLayersEffect: MapEffect triggered")
        mapboxMapRef = mapView.mapboxMap

        // Subscribe to style loaded events for initial render
        mapView.mapboxMap.subscribeStyleLoaded { _ ->
            Log.d(TAG, "GlobalLayersEffect: Style loaded event received")
            if (globalLayers == null) {
                globalLayers = GlobalLayers(mapView.mapboxMap)
            }
            globalLayers?.update(
                visibility = currentVisibility,
                loranConfig = currentLoranConfig,
                selectedTournament = currentTournament,
                stations = currentStations
            )
        }

        // Also try immediately if style is already loaded
        mapView.mapboxMap.getStyle { _ ->
            Log.d(TAG, "GlobalLayersEffect: getStyle callback - style available")
            if (globalLayers == null) {
                globalLayers = GlobalLayers(mapView.mapboxMap)
            }
            globalLayers?.update(
                visibility = currentVisibility,
                loranConfig = currentLoranConfig,
                selectedTournament = currentTournament,
                stations = currentStations
            )
        }
    }

    // Update layers when visibility changes (after initial setup)
    LaunchedEffect(visibility, loranConfig, selectedTournament) {
        val map = mapboxMapRef ?: return@LaunchedEffect
        map.getStyle { _ ->
            globalLayers?.update(
                visibility = currentVisibility,
                loranConfig = currentLoranConfig,
                selectedTournament = currentTournament,
                stations = currentStations
            )
        }
    }
}
