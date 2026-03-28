package com.example.saltyoffshore.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Text
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.example.saltyoffshore.ui.controls.MapToolBar
import com.example.saltyoffshore.ui.sharelink.ShareLinkSheet
import com.example.saltyoffshore.ui.controls.RightSideToolbar
import com.example.saltyoffshore.config.AppConstants
import com.example.saltyoffshore.data.RegionStatus
import com.example.saltyoffshore.ui.components.CrosshairOverlay
import com.example.saltyoffshore.ui.components.TopBar
import com.example.saltyoffshore.ui.components.DatasetSelectorSheet
import com.example.saltyoffshore.ui.components.DepthSelector
import com.example.saltyoffshore.ui.components.DatasetFilterSheet
import com.example.saltyoffshore.ui.components.QuickActionsBar
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.example.saltyoffshore.data.CurrentValue
import com.example.saltyoffshore.data.DatasetConfiguration
import com.example.saltyoffshore.data.DatasetType
import com.example.saltyoffshore.data.displayVariables
import com.example.saltyoffshore.data.hasMultipleVariables
import com.example.saltyoffshore.data.primaryVariable
import com.example.saltyoffshore.data.TemperatureUnits
import com.example.saltyoffshore.data.waypoint.SharedWaypoint
import com.example.saltyoffshore.data.waypoint.Waypoint
import com.example.saltyoffshore.data.waypoint.WaypointSelectionSource
import com.example.saltyoffshore.data.waypoint.WaypointSheet
import com.example.saltyoffshore.data.waypoint.WaypointSource
import com.example.saltyoffshore.data.coordinate.GPSFormat
import com.example.saltyoffshore.ui.waypoint.WaypointDetailSheet
import com.example.saltyoffshore.ui.waypoint.WaypointFormSheet
import com.example.saltyoffshore.ui.waypoint.WaypointManagementSheet
import com.example.saltyoffshore.ui.measurement.MeasurementMapEffect
import com.example.saltyoffshore.ui.measurement.MeasureModeOverlay
import com.example.saltyoffshore.ui.map.RegionBoundsEffect
import com.example.saltyoffshore.ui.map.layers.DatasetLayers
import com.example.saltyoffshore.ui.map.globallayers.GlobalLayers
import com.example.saltyoffshore.ui.map.waypoint.WaypointAnnotationLayer
import com.example.saltyoffshore.ui.map.waypoint.SharedWaypointAnnotationLayer
import androidx.compose.material3.MaterialTheme
import com.example.saltyoffshore.ui.theme.Spacing
import com.example.saltyoffshore.ui.station.StationDetailsView
import com.example.saltyoffshore.ui.satellite.SatelliteModeView
import com.example.saltyoffshore.ui.map.satellite.SatelliteLayersEffect
import com.example.saltyoffshore.viewmodel.AppViewModel
import com.example.saltyoffshore.viewmodel.StationDetailViewModel
import com.mapbox.maps.MapView
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.plugin.gestures.gestures
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
import androidx.compose.material3.ExperimentalMaterial3Api
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

    // Filter sheet state (fixed-position panel, not ModalBottomSheet)
    var showFilterSheet by remember { mutableStateOf(false) }

    // Waypoint management sheet state
    var showWaypointSheet by remember { mutableStateOf(false) }

    // Tools menu sheet state
    var showToolsSheet by remember { mutableStateOf(false) }

    // MapView ref for satellite layers (needs style reload subscription)
    var satelliteMapViewRef by remember { mutableStateOf<MapView?>(null) }

    // Special mode: hides normal controls when in satellite, measurement, etc.
    val isInSpecialMode = viewModel.satelliteTrackingMode.isActive || viewModel.measurementState.isActive

    // GPX file picker
    val context = LocalContext.current
    val gpxPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importGPX(it, context) }
    }

    // Snackbar for import feedback
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel.importResult) {
        viewModel.importResult?.let { message ->
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
            viewModel.clearImportResult()
        }
    }

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

    // Load waypoints from disk on first composition
    LaunchedEffect(Unit) {
        viewModel.loadWaypoints()
    }

    // ── Satellite mode viewport animations (iOS: MapModeModifier.swift) ──

    // Fly to globe on enter, back to region on exit
    LaunchedEffect(viewModel.satelliteTrackingMode.isActive) {
        if (viewModel.satelliteTrackingMode.isActive) {
            mapViewportState.flyTo(
                cameraOptions = CameraOptions.Builder()
                    .center(Point.fromLngLat(-40.0, 20.0))  // Atlantic midpoint
                    .zoom(0.0)
                    .bearing(0.0)
                    .pitch(0.0)
                    .build(),
                animationOptions = MapAnimationOptions.Builder().duration(1500L).build()
            )
        } else {
            // Fly back to region on exit
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
                    animationOptions = MapAnimationOptions.Builder().duration(1000L).build()
                )
            }
        }
    }

    // Fly to selected track (zoom 2.0)
    LaunchedEffect(viewModel.satelliteTrackingMode.selectedTrackId) {
        val id = viewModel.satelliteTrackingMode.selectedTrackId ?: return@LaunchedEffect
        val track = viewModel.satelliteStore.tracks.firstOrNull { it.id == id } ?: return@LaunchedEffect
        val (lat, lon) = track.center
        mapViewportState.flyTo(
            cameraOptions = CameraOptions.Builder()
                .center(Point.fromLngLat(lon, lat))
                .zoom(2.0)
                .bearing(0.0)
                .pitch(0.0)
                .build(),
            animationOptions = MapAnimationOptions.Builder().duration(1000L).build()
        )
    }

    // Fly to selected pass (zoom 3.0)
    LaunchedEffect(viewModel.satelliteTrackingMode.selectedPassId) {
        val id = viewModel.satelliteTrackingMode.selectedPassId ?: return@LaunchedEffect
        val pass = viewModel.satelliteStore.passes.firstOrNull { it.id == id } ?: return@LaunchedEffect
        val (lat, lon) = pass.center
        mapViewportState.flyTo(
            cameraOptions = CameraOptions.Builder()
                .center(Point.fromLngLat(lon, lat))
                .zoom(3.0)
                .bearing(0.0)
                .pitch(0.0)
                .build(),
            animationOptions = MapAnimationOptions.Builder().duration(800L).build()
        )
    }

    // ── Map style: switch projection & theme for satellite mode ──
    val mapStyleUri = if (viewModel.satelliteTrackingMode.isActive) {
        AppConstants.darkMapStyleURI
    } else {
        AppConstants.lightMapStyleURI
    }
    val mapProjection = if (viewModel.satelliteTrackingMode.isActive) {
        Projection.GLOBE
    } else {
        Projection.MERCATOR
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = mapViewportState,
            style = { MapStyle(style = mapStyleUri, projection = mapProjection) }
        ) {
            // Wire up repaint callback for Zarr frame updates
            ZarrRepaintEffect(viewModel = viewModel)

            // Wire map snapshot capture for share links
            MapEffect(Unit) { mapView ->
                viewModel.captureMapSnapshot = {
                    mapView.snapshot { bitmap ->
                        viewModel.setMapSnapshot(bitmap)
                    }
                }
            }

            // Zoom constraints: cap at 4.0 in satellite mode (iOS: cameraBounds)
            MapEffect(viewModel.satelliteTrackingMode.isActive) { mapView ->
                val bounds = if (viewModel.satelliteTrackingMode.isActive) {
                    com.mapbox.maps.CameraBoundsOptions.Builder()
                        .minZoom(0.0).maxZoom(4.0).build()
                } else {
                    com.mapbox.maps.CameraBoundsOptions.Builder()
                        .minZoom(1.0).maxZoom(24.0).build()
                }
                mapView.mapboxMap.setBounds(bounds)
            }

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

            // Load stations when layer is enabled (deferred — matches iOS)
            val stationsEnabled = viewModel.globalLayerManager.isEnabled(
                com.example.saltyoffshore.data.GlobalLayerType.STATIONS
            )
            LaunchedEffect(stationsEnabled) {
                if (stationsEnabled) viewModel.loadStationsIfNeeded()
            }

            // Global overlay layers (bathymetry, shipping lanes, etc.)
            GlobalLayersEffect(
                visibility = viewModel.globalLayerManager.visibility,
                loranConfig = viewModel.globalLayerManager.selectedLoranConfig,
                selectedTournament = viewModel.globalLayerManager.selectedTournament,
                stations = viewModel.stations,
                onStationTap = { stationId ->
                    Log.d(TAG, "Station tapped: $stationId")
                    viewModel.openStationDetail(stationId)
                }
            )

            // Crosshair feature query on camera changes
            CrosshairQueryEffect(
                isDataLayerActive = viewModel.isDataLayerActive,
                datasetType = viewModel.currentDatasetType,
                onPrimaryValueChanged = { viewModel.updatePrimaryValue(it) },
                onCameraChanged = { zoom, lat, lon -> viewModel.updateCameraState(zoom, lat, lon) }
            )

            // Region annotations — hide during satellite mode + hide the active region
            if (!viewModel.satelliteTrackingMode.isActive) viewModel.regions
                .filter { it.id != viewModel.selectedRegion?.id }
                .forEach { region ->
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

            // Waypoint annotation layers
            WaypointLayersEffect(
                waypoints = viewModel.waypoints,
                crewWaypoints = viewModel.crewWaypoints,
                selectedWaypointId = viewModel.selectedWaypointId,
                ownedWaypointIds = viewModel.ownedWaypointIds,
                onWaypointTap = { id ->
                    viewModel.selectWaypoint(id, WaypointSelectionSource.MAP_TAP)
                }
            )

            // Long-press to create waypoint + open form
            MapEffect(Unit) { mapView ->
                mapView.gestures.addOnMapLongClickListener { point ->
                    val waypoint = viewModel.createWaypoint(
                        latitude = point.latitude(),
                        longitude = point.longitude()
                    )
                    viewModel.openWaypointForm(waypoint)
                    true
                }
            }

            // Measurement layers (lines, points, distance labels)
            MeasurementMapEffect(
                measurements = viewModel.measurementState.allMeasurements,
                distanceUnits = viewModel.currentDistanceUnits
            )

            // Tap-to-measure: intercept map clicks when measure mode active
            // Registered once (Unit key) — isActive check gates behavior at runtime
            MapEffect(Unit) { mapView ->
                mapView.gestures.addOnMapClickListener { point ->
                    if (viewModel.measurementState.isActive) {
                        viewModel.measurementState.addPoint(point)
                        true
                    } else {
                        false
                    }
                }
            }

            // Satellite tracking layers — uses subscribeStyleLoaded for style reload survival
            MapEffect(Unit) { mapView ->
                // SatelliteLayersEffect is a composable, but we need the MapView ref.
                // Store it for the composable below.
                satelliteMapViewRef = mapView
            }
            satelliteMapViewRef?.let { mv ->
                SatelliteLayersEffect(
                    mapView = mv,
                    trackingMode = viewModel.satelliteTrackingMode,
                    store = viewModel.satelliteStore
                )
            }
        }

        // Crosshair overlay
        CrosshairOverlay(
            primaryValue = viewModel.primaryValue,
            temperatureUnits = TemperatureUnits.fromRawValue(viewModel.userPreferences?.temperatureUnits)
                ?: TemperatureUnits.FAHRENHEIT,
            zoom = viewModel.currentZoom,
            latitude = viewModel.currentLatitude,
            isDataLayerActive = viewModel.isDataLayerActive
        )

        // Top bar: left (crew/future) | center (loading/error capsules) | right (announcement + account)
        TopBar(
            isVisible = !isInSpecialMode,
            notifications = viewModel.notificationManager.notifications,
            showAnnouncement = viewModel.isAnnouncementVisible,
            onAnnouncementTap = { viewModel.showAnnouncementSheet = true },
            onAccountTap = onSettingsClick,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp)
        )

        // Snackbar host (import feedback)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )

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

        // Bottom controls column — matches iOS MapControlsContainer VStack
        // Hidden in special modes (satellite, measurement replaces with its own overlay)
        if (viewModel.selectedDataset != null && !viewModel.satelliteTrackingMode.isActive) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                // Right toolbar (right-aligned, above quick actions + bottom panel)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = Spacing.large),
                    horizontalArrangement = Arrangement.End
                ) {
                    RightSideToolbar(
                        onFilterClick = { showFilterSheet = true },
                        onLayersClick = { showLayersSheet = true },
                        onToolsClick = { showToolsSheet = true },
                        onMeasureClick = {
                            if (viewModel.measurementState.isActive) {
                                viewModel.measurementState.exit()
                            } else {
                                viewModel.measurementState.enter()
                            }
                        }
                    )
                }

                Spacer(Modifier.height(Spacing.medium))

                // Measurement mode overlay (replaces dataset control when active)
                if (viewModel.measurementState.isActive) {
                    MeasureModeOverlay(
                        totalDistanceMeters = viewModel.measurementState.totalDistanceMeters,
                        hasMeasurements = viewModel.measurementState.hasMeasurements,
                        canUndo = viewModel.measurementState.canUndo,
                        distanceUnits = viewModel.currentDistanceUnits,
                        onUndo = { viewModel.measurementState.undoLastPoint() },
                        onClear = { viewModel.measurementState.clearAll() },
                        onDone = { viewModel.measurementState.exit() }
                    )
                }

                // Quick actions bar (presets, variables, depth)
                if (!viewModel.measurementState.isActive) {
                    val dataset = viewModel.selectedDataset!!
                    val datasetType = DatasetType.fromRawValue(dataset.type) ?: DatasetType.SST
                    val entry = viewModel.selectedEntry
                    val rangeKey = datasetType.rangeKey
                    val rangeData = entry?.ranges?.get(rangeKey)
                    val valueRange = if (rangeData?.min != null && rangeData.max != null) {
                        rangeData.min..rangeData.max
                    } else {
                        0.0..1.0
                    }
                    val config = viewModel.primaryConfig

                    QuickActionsBar(
                        datasetType = datasetType,
                        variables = datasetType.displayVariables,
                        selectedVariable = config?.selectedVariable(dataset) ?: datasetType.primaryVariable,
                        onVariableSelected = { viewModel.selectVariable(it) },
                        availableDepths = dataset.availableDepths ?: listOf(0),
                        selectedDepth = viewModel.depthFilterState.selectedDepth,
                        onDepthSelected = { viewModel.onDepthSelected(it) },
                        allPresets = viewModel.allPresets,
                        selectedPreset = config?.selectedPreset,
                        currentValue = viewModel.primaryValue.value,
                        valueRange = valueRange,
                        isLoadingPresets = viewModel.isLoadingPresets,
                        onPresetSelected = { viewModel.applyPreset(it) }
                    )
                }

                // Bottom panel
                if (!viewModel.measurementState.isActive) SaltyDatasetControl(
                    dataset = viewModel.selectedDataset!!,
                    entry = viewModel.selectedEntry,
                    snapshot = viewModel.renderingSnapshot,
                    primaryValue = viewModel.primaryValue,
                    isExpanded = viewModel.isDatasetControlCollapsed,
                    primaryConfig = viewModel.primaryConfig,
                    onConfigChanged = { viewModel.updatePrimaryConfig(it) },
                    onEntrySelected = { viewModel.selectEntry(it) },
                    onExpandToggle = {
                        viewModel.isDatasetControlCollapsed = !viewModel.isDatasetControlCollapsed
                    },
                    onChange = { showDatasetSheet = true }
                )
            }
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
                selectedEntry = viewModel.selectedEntry,
                isPremium = true, // TODO: Wire up subscription status
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
                datasetType = datasetType,
                apiUnit = DatasetConfiguration.forDatasetType(datasetType).unit,
                decimalPlaces = DatasetConfiguration.forDatasetType(datasetType).decimalPlaces,
                onConfigChanged = { newConfig ->
                    viewModel.updatePrimaryConfig(newConfig)
                },
                onDragRangeChanged = { min, max ->
                    viewModel.setFilterRangeDirect(min, max)
                },
                onDismiss = { showFilterSheet = false }
            )
        }

        // Waypoint detail sheet
        viewModel.activeWaypointSheet?.let { sheet ->
            when (sheet) {
                is WaypointSheet.Details -> {
                    val waypoint = viewModel.allWaypoints.find { it.id == sheet.waypointId }
                    if (waypoint != null) {
                        WaypointDetailSheet(
                            waypoint = waypoint,
                            source = WaypointSource.Own,
                            gpsFormat = GPSFormat.DMM, // TODO: wire from user preferences
                            onDismiss = { viewModel.dismissWaypointSheet() },
                            onEdit = { viewModel.openWaypointForm(it) },
                            onDelete = { viewModel.deleteWaypoint(it) },
                            onShareToCrew = { /* TODO: wire sharing */ },
                            onShareGPX = { /* TODO: wire GPX export */ },
                            onNotesChanged = { notes ->
                                val updated = waypoint.copy(notes = notes.ifEmpty { null })
                                viewModel.saveWaypoint(updated)
                            }
                        )
                    }
                }
                is WaypointSheet.Form -> {
                    WaypointFormSheet(
                        waypoint = sheet.waypoint,
                        isNewWaypoint = sheet.waypoint.name == null,
                        gpsFormat = GPSFormat.DMM, // TODO: wire from user preferences
                        formState = viewModel.waypointFormState,
                        onFormStateChange = { viewModel.waypointFormState = it },
                        onSave = {
                            val updated = viewModel.waypointFormState.buildWaypoint(
                                from = sheet.waypoint,
                                format = GPSFormat.DMM
                            )
                            if (updated != null) {
                                viewModel.saveWaypoint(updated)
                                viewModel.dismissWaypointSheet()
                            }
                        },
                        onCancel = { viewModel.dismissWaypointSheet() },
                        onDismiss = { viewModel.dismissWaypointSheet() }
                    )
                }
            }
        }

        // Waypoint management sheet
        if (showWaypointSheet) {
            WaypointManagementSheet(
                sections = viewModel.groupedWaypoints,
                sortOption = viewModel.waypointSortOption,
                selectedWaypointId = viewModel.selectedWaypointId,
                onSortOptionChanged = { viewModel.updateWaypointSortOption(it) },
                onWaypointTap = { id ->
                    showWaypointSheet = false
                    viewModel.openWaypointDetails(id)
                },
                onWaypointDelete = { viewModel.deleteWaypoint(it) },
                onImportGPX = { gpxPickerLauncher.launch(arrayOf("*/*")) },
                onDismiss = { showWaypointSheet = false }
            )
        }

        // Announcement sheet
        if (viewModel.showAnnouncementSheet) {
            viewModel.announcement?.let { ann ->
                androidx.compose.material3.ModalBottomSheet(
                    onDismissRequest = { viewModel.markAnnouncementAsSeen() },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
                    containerColor = MaterialTheme.colorScheme.surface,
                    dragHandle = null
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp)
                    ) {
                        // Header with close button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Announcement",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.weight(1f))
                            androidx.compose.material3.IconButton(
                                onClick = { viewModel.markAnnouncementAsSeen() }
                            ) {
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        // Title
                        Text(
                            text = ann.title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        // Message
                        Text(
                            text = ann.formattedMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(Modifier.height(24.dp))
                        // OK button — primary filled (not tonal)
                        androidx.compose.material3.Button(
                            onClick = { viewModel.markAnnouncementAsSeen() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                        ) {
                            Text("OK")
                        }
                    }
                }
            }
        }

        // Tools menu sheet
        if (showToolsSheet) {
            androidx.compose.material3.ModalBottomSheet(
                onDismissRequest = { showToolsSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                MapToolBar(
                    onAddWaypoint = {
                        showToolsSheet = false
                        // TODO: Open waypoint creation at map center
                    },
                    onSatellites = {
                        showToolsSheet = false
                        showLayersSheet = false
                        showDatasetSheet = false
                        viewModel.satelliteTrackingMode.enter()
                    },
                    onMyLocation = {
                        showToolsSheet = false
                        // TODO: Fly to user location
                    },
                    onShare = {
                        showToolsSheet = false
                        viewModel.createShareLink()
                    },
                    onWaypoints = {
                        showToolsSheet = false
                        showWaypointSheet = true
                    },
                    onDatasetGuide = {
                        showToolsSheet = false
                        // TODO: Open dataset guide
                    },
                    onDismiss = { showToolsSheet = false }
                )
            }
        }

        // Share link preview sheet — full-screen style matching iOS
        viewModel.shareLinkUrl?.let { url ->
            androidx.compose.material3.ModalBottomSheet(
                onDismissRequest = { viewModel.dismissShareLink() },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = null
            ) {
                ShareLinkSheet(
                    url = url,
                    mapSnapshot = viewModel.shareLinkSnapshot,
                    regionName = viewModel.selectedRegion?.name ?: "Unknown",
                    datasetName = viewModel.selectedDataset?.let {
                        DatasetType.fromRawValue(it.type)?.shortName ?: it.type
                    } ?: "Unknown",
                    timestamp = viewModel.selectedEntry?.timestamp ?: "",
                    latitude = viewModel.currentLatitude,
                    longitude = viewModel.currentLongitude,
                    onDismiss = { viewModel.dismissShareLink() }
                )
            }
        }

        // Station detail sheet
        viewModel.selectedStationId?.let { stationId ->
            val stationDetailViewModel: StationDetailViewModel = viewModel(
                key = "stationDetail",
                factory = androidx.lifecycle.ViewModelProvider.NewInstanceFactory()
            )
            androidx.compose.material3.ModalBottomSheet(
                onDismissRequest = { viewModel.dismissStationDetail() },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                StationDetailsView(
                    stationId = stationId,
                    viewModel = stationDetailViewModel
                )
            }
        }

        // Satellite mode overlay (full-screen, on top of map)
        if (viewModel.satelliteTrackingMode.isActive) {
            SatelliteModeView(
                trackingMode = viewModel.satelliteTrackingMode,
                store = viewModel.satelliteStore,
                onDismiss = { viewModel.satelliteTrackingMode.exit() }
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
 * Manager owns primaryValue state and notifies via callback.
 */
@Composable
private fun CrosshairQueryEffect(
    isDataLayerActive: Boolean,
    datasetType: DatasetType?,
    onPrimaryValueChanged: (CurrentValue) -> Unit,
    onCameraChanged: (zoom: Double, latitude: Double, longitude: Double) -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val yOffsetPx = with(density) { CrosshairConstants.yOffset.toPx() }

    MapEffect(key1 = isDataLayerActive, key2 = datasetType) { mapView ->
        val mapboxMap = mapView.mapboxMap

        val queryManager = CrosshairFeatureQueryManager(mapboxMap, scope)
        queryManager.configure(datasetType)
        queryManager.onPrimaryValueChanged = { onPrimaryValueChanged(it) }

        val cancelable = mapboxMap.subscribeCameraChanged { _ ->
            val cameraState = mapboxMap.cameraState
            val zoom = cameraState.zoom
            val center = cameraState.center
            val latitude = center.latitude()
            val longitude = center.longitude()

            onCameraChanged(zoom, latitude, longitude)

            if (isDataLayerActive) {
                val screenCenterX = mapView.width / 2.0
                val screenCenterY = mapView.height / 2.0 + yOffsetPx
                val screenPoint = ScreenCoordinate(screenCenterX, screenCenterY)
                queryManager.queryCenterFeatures(screenPoint, zoom)
            }
        }

        try {
            kotlinx.coroutines.awaitCancellation()
        } finally {
            queryManager.reset()
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
    stations: List<Station>,
    onStationTap: (String) -> Unit = {}
) {
    var globalLayers by remember { mutableStateOf<GlobalLayers?>(null) }
    var mapboxMapRef by remember { mutableStateOf<com.mapbox.maps.MapboxMap?>(null) }

    // Use rememberUpdatedState to always have latest values in callbacks
    val currentVisibility by androidx.compose.runtime.rememberUpdatedState(visibility)
    val currentLoranConfig by androidx.compose.runtime.rememberUpdatedState(loranConfig)
    val currentTournament by androidx.compose.runtime.rememberUpdatedState(selectedTournament)
    val currentStations by androidx.compose.runtime.rememberUpdatedState(stations)
    val currentOnStationTap by androidx.compose.runtime.rememberUpdatedState(onStationTap)

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

        // Station marker tap detection via queryRenderedFeatures
        mapView.mapboxMap.let { map ->
            mapView.gestures.addOnMapClickListener { point ->
                val screenPoint = map.pixelForCoordinate(point)
                val geometry = com.mapbox.maps.RenderedQueryGeometry(screenPoint)
                val options = com.mapbox.maps.RenderedQueryOptions(
                    listOf(com.example.saltyoffshore.config.MapLayers.Global.STATIONS_LAYER),
                    null
                )
                map.queryRenderedFeatures(geometry, options) { expected ->
                    expected.value?.firstOrNull()?.let { feature ->
                        val stationId = feature.queriedFeature.feature.getStringProperty("id")
                        if (stationId != null) {
                            currentOnStationTap(stationId)
                        }
                    }
                }
                false // Don't consume — let other listeners also handle
            }
        }

        // Subscribe to style loaded events for initial render
        mapView.mapboxMap.subscribeStyleLoaded { _ ->
            Log.d(TAG, "GlobalLayersEffect: Style loaded event received")
            if (globalLayers == null) {
                globalLayers = GlobalLayers(mapView.context, mapView.mapboxMap)
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
                globalLayers = GlobalLayers(mapView.context, mapView.mapboxMap)
            }
            globalLayers?.update(
                visibility = currentVisibility,
                loranConfig = currentLoranConfig,
                selectedTournament = currentTournament,
                stations = currentStations
            )
        }
    }

    // Update layers when visibility or stations change (after initial setup)
    LaunchedEffect(visibility, loranConfig, selectedTournament, stations) {
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

/**
 * Effect that manages waypoint annotation layers (own + shared).
 * Uses imperative GeoJSON source + SymbolLayer pattern matching StationsLayer.
 */
@Composable
private fun WaypointLayersEffect(
    waypoints: List<Waypoint>,
    crewWaypoints: List<SharedWaypoint>,
    selectedWaypointId: String?,
    ownedWaypointIds: Set<String>,
    onWaypointTap: (String) -> Unit
) {
    var ownLayer by remember { mutableStateOf<WaypointAnnotationLayer?>(null) }
    var sharedLayer by remember { mutableStateOf<SharedWaypointAnnotationLayer?>(null) }
    var mapboxMapRef by remember { mutableStateOf<com.mapbox.maps.MapboxMap?>(null) }

    // Use rememberUpdatedState for latest values in callbacks
    val currentWaypoints by androidx.compose.runtime.rememberUpdatedState(waypoints)
    val currentCrewWaypoints by androidx.compose.runtime.rememberUpdatedState(crewWaypoints)
    val currentSelectedId by androidx.compose.runtime.rememberUpdatedState(selectedWaypointId)
    val currentOwnedIds by androidx.compose.runtime.rememberUpdatedState(ownedWaypointIds)

    // Clean up on dispose
    DisposableEffect(Unit) {
        onDispose {
            ownLayer?.removeFromMap()
            ownLayer = null
            sharedLayer?.removeFromMap()
            sharedLayer = null
        }
    }

    // Get map reference and do initial render
    MapEffect(Unit) { mapView ->
        mapboxMapRef = mapView.mapboxMap

        mapView.mapboxMap.getStyle { _ ->
            if (ownLayer == null) {
                ownLayer = WaypointAnnotationLayer(mapView.mapboxMap)
            }
            if (sharedLayer == null) {
                sharedLayer = SharedWaypointAnnotationLayer(mapView.mapboxMap)
            }

            ownLayer?.update(
                waypoints = currentWaypoints,
                selectedWaypointId = currentSelectedId,
                activeCrewId = null // TODO: wire crew state
            )
            sharedLayer?.update(
                sharedWaypoints = currentCrewWaypoints,
                activeCrewId = null, // TODO: wire crew state
                ownedWaypointIds = currentOwnedIds,
                selectedWaypointId = currentSelectedId
            )
        }

        // TODO: Handle tap on waypoint features — wire when gesture API is confirmed
        // Needs: addOnMapClickListener + queryRenderedFeatures for waypoint layer tap detection
    }

    // Update layers when data changes
    LaunchedEffect(waypoints, crewWaypoints, selectedWaypointId) {
        val map = mapboxMapRef ?: return@LaunchedEffect
        map.getStyle { _ ->
            ownLayer?.update(
                waypoints = currentWaypoints,
                selectedWaypointId = currentSelectedId,
                activeCrewId = null
            )
            sharedLayer?.update(
                sharedWaypoints = currentCrewWaypoints,
                activeCrewId = null,
                ownedWaypointIds = currentOwnedIds,
                selectedWaypointId = currentSelectedId
            )
        }
    }
}
