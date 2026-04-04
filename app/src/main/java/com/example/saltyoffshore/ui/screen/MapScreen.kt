package com.example.saltyoffshore.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.saltyoffshore.ui.controls.LayersControlSheet
import com.example.saltyoffshore.ui.controls.MapToolBar
import com.example.saltyoffshore.ui.sharelink.ShareLinkSheet
import com.example.saltyoffshore.ui.components.AnnouncementSheetView
import com.example.saltyoffshore.ui.components.CrosshairOverlay
import com.example.saltyoffshore.ui.components.TopBar
import com.example.saltyoffshore.ui.components.DatasetSelectorSheet
import com.example.saltyoffshore.ui.components.DepthSelector
import com.example.saltyoffshore.ui.components.DatasetFilterSheet
import com.example.saltyoffshore.data.DatasetConfiguration
import com.example.saltyoffshore.data.DatasetType
import com.example.saltyoffshore.data.TemperatureUnits
import com.example.saltyoffshore.data.waypoint.WaypointSheet
import com.example.saltyoffshore.data.waypoint.WaypointSource
import com.example.saltyoffshore.data.coordinate.GPSFormat
import com.example.saltyoffshore.ui.waypoint.WaypointDetailSheet
import com.example.saltyoffshore.ui.waypoint.WaypointFormSheet
import com.example.saltyoffshore.ui.waypoint.WaypointManagementSheet
import androidx.compose.material3.MaterialTheme
import com.example.saltyoffshore.ui.theme.Spacing
import com.example.saltyoffshore.ui.station.StationDetailsView
import com.example.saltyoffshore.ui.satellite.SatelliteModeView
import com.example.saltyoffshore.ui.map.MapContent
import com.example.saltyoffshore.ui.map.MapControlsOverlay
import com.example.saltyoffshore.viewmodel.AppViewModel
import com.example.saltyoffshore.viewmodel.StationDetailViewModel
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.platform.LocalContext
import com.example.saltyoffshore.ui.crew.CrewListSheet
import com.example.saltyoffshore.ui.crew.CreateCrewSheet
import com.example.saltyoffshore.ui.crew.JoinCrewSheet
import com.example.saltyoffshore.ui.crew.ShareWaypointSheet
import com.example.saltyoffshore.ui.crew.CrewChipsOverlay
import com.example.saltyoffshore.ui.savedmaps.SavedMapsListSheet
import com.example.saltyoffshore.ui.savedmaps.SaveMapSheet
import com.example.saltyoffshore.auth.AuthManager
import com.example.saltyoffshore.ui.map.MapSheet
import com.example.saltyoffshore.ui.map.MapSheetCoordinator

private const val TAG = "MapScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: AppViewModel = viewModel(),
    onSettingsClick: () -> Unit = {}
) {
    // Single coordinator for all map sheets (replaces per-sheet booleans)
    val coordinator = remember { MapSheetCoordinator() }

    // Hoisted sheet states — live above conditional blocks so they survive
    // show/hide transitions. One shared state for all coordinator-managed sheets
    // (only one is active at a time), separate states for viewModel-driven sheets.
    val coordinatorSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val shareLinkSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val stationSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val waypointSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val announcementSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    // ── Collect per-store state (replaces monolithic AppState) ──
    val appState by viewModel.state.collectAsState()
    val regionState by viewModel.regionStore.state.collectAsState()
    val datasetState by viewModel.datasetStore.state.collectAsState()
    val waypointState by viewModel.waypointStore.state.collectAsState()
    val crewState by viewModel.crewStore.state.collectAsState()
    val prefsState by viewModel.userPreferencesStore.state.collectAsState()
    val savedMapsState by viewModel.savedMapsStore.state.collectAsState()
    val stationState by viewModel.stationStore.state.collectAsState()

    // Camera state — local to MapScreen, NOT in ViewModel.
    // Only CrosshairOverlay reads these (via lambdas), so camera moves at 60fps
    // only recompose the overlay, not the entire MapScreen.
    // iOS equivalent: CrosshairFeatureQueryManager owns camera state privately.
    var cameraZoom by remember { mutableDoubleStateOf(4.0) }
    var cameraLatitude by remember { mutableDoubleStateOf(30.0) }
    var cameraLongitude by remember { mutableDoubleStateOf(-60.0) }

    // Special mode: hides normal controls when in satellite, measurement, etc.
    val isInSpecialMode = viewModel.satelliteTrackingMode.isActive || viewModel.measurementState.isActive

    // GPX file picker
    val context = LocalContext.current
    val gpxPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.waypointStore.importGPX(it, context) }
    }

    // Snackbar for import feedback
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(waypointState.importResult) {
        waypointState.importResult?.let { message ->
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
            viewModel.waypointStore.clearImportResult()
        }
    }

    // Load waypoints from disk on first composition
    LaunchedEffect(Unit) {
        viewModel.waypointStore.loadWaypoints()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ── Map (isolated composable — matches iOS MapView.swift / MapboxMapView_V2) ──
        // MapContent takes explicit params only. Sheet state changes in MapScreen
        // do NOT recompose MapContent — this is the key performance boundary.
        MapContent(
            selectedRegion = regionState.selectedRegion,
            regions = regionState.regions,
            selectedDataset = datasetState.selectedDataset,
            selectedEntry = datasetState.selectedEntry,
            renderingSnapshot = datasetState.renderingSnapshot,
            visualSource = datasetState.visualSource,
            isDataLayerActive = datasetState.isDataLayerActive,
            currentDatasetType = datasetState.currentDatasetType,
            globalLayerVisibility = viewModel.globalLayerManager.visibility,
            loranConfig = viewModel.globalLayerManager.selectedLoranConfig,
            selectedTournament = viewModel.globalLayerManager.selectedTournament,
            stations = stationState.stations,
            isStationsEnabled = viewModel.globalLayerManager.isEnabled(
                com.example.saltyoffshore.data.GlobalLayerType.STATIONS
            ),
            waypoints = waypointState.waypoints,
            measurements = viewModel.measurementState.allMeasurements,
            measurementIsActive = viewModel.measurementState.isActive,
            distanceUnits = prefsState.currentDistanceUnits,
            satelliteTrackingMode = viewModel.satelliteTrackingMode,
            satelliteStore = viewModel.satelliteStore,
            onCameraChanged = { zoom, lat, lon ->
                cameraZoom = zoom
                cameraLatitude = lat
                cameraLongitude = lon
                viewModel.datasetStore.updateCameraState(zoom, lat, lon)
            },
            onPrimaryValueChanged = { viewModel.datasetStore.updatePrimaryValue(it) },
            onRegionSelected = { viewModel.regionStore.onRegionSelected(it) },
            onStationTap = { viewModel.stationStore.openStationDetail(it) },
            onWaypointTap = { viewModel.waypointStore.openWaypointDetails(it) },
            onWaypointLongPress = { point ->
                val waypoint = viewModel.waypointStore.createWaypoint(
                    latitude = point.latitude(),
                    longitude = point.longitude()
                )
                viewModel.waypointStore.openWaypointForm(waypoint)
            },
            onMeasurementTap = { viewModel.measurementState.addPoint(it) },
            onStationsNeedLoad = { viewModel.stationStore.loadStationsIfNeeded() },
            onCaptureMapSnapshotReady = { capture -> viewModel.datasetStore.captureMapSnapshot = capture },
            onMapSnapshotTaken = { viewModel.savedMapsStore.setMapSnapshot(it) },
            onRepaintReady = { repaint -> viewModel.datasetStore.repaint = repaint },
            modifier = Modifier.fillMaxSize(),
        )

        // Crosshair overlay
        CrosshairOverlay(
            primaryValue = datasetState.primaryValue,
            temperatureUnits = TemperatureUnits.fromRawValue(prefsState.userPreferences?.temperatureUnits)
                ?: TemperatureUnits.FAHRENHEIT,
            zoomProvider = { cameraZoom },
            latitudeProvider = { cameraLatitude },
            isDataLayerActive = datasetState.isDataLayerActive
        )

        // Top bar: left (crew/future) | center (loading/error capsules) | right (announcement + account)
        TopBar(
            isVisible = !isInSpecialMode,
            notifications = viewModel.notificationManager.notifications,
            showAnnouncement = appState.isAnnouncementVisible,
            onAnnouncementTap = { viewModel.setShowAnnouncementSheet(true) },
            onAccountTap = onSettingsClick,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp)
        )

        // Crew filter chips
        if (crewState.crews.isNotEmpty()) {
            CrewChipsOverlay(
                crews = crewState.crews,
                activeCrewId = crewState.activeCrewId,
                unreadCounts = emptyMap(),
                onSelectCrew = { viewModel.crewStore.setActiveCrewId(it) },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 100.dp)
            )
        }

        // Snackbar host (import feedback)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )

        // Depth selector (right side)
        if (datasetState.depthFilterState.hasSelection) {
            DepthSelector(
                depthFilter = datasetState.depthFilterState,
                onDepthSelected = { viewModel.datasetStore.onDepthSelected(it) },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = Spacing.large)
            )
        }

        // Bottom controls — extracted composable (matches iOS MapControlsOverlay)
        // Own recomposition scope: sheet state changes don't touch this.
        if (datasetState.selectedDataset != null && !viewModel.satelliteTrackingMode.isActive) {
            MapControlsOverlay(
                viewModel = viewModel,
                coordinator = coordinator,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            )
        }

        // Layers control sheet
        if (coordinator.activeSheet is MapSheet.Layers && datasetState.selectedDataset != null && datasetState.primaryConfig != null) {
            LayersControlSheet(
                // Dataset layer props
                dataset = datasetState.selectedDataset!!,
                config = datasetState.primaryConfig!!,
                onConfigChanged = { viewModel.datasetStore.updatePrimaryConfig(it) },
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
                sheetState = coordinatorSheetState,
                onDismiss = { coordinator.dismissSheet() }
            )
        }

        // Dataset selector sheet
        if (coordinator.activeSheet is MapSheet.DatasetSelector && regionState.selectedRegion != null) {
            DatasetSelectorSheet(
                datasets = regionState.selectedRegion!!.activeDatasets,
                selectedDataset = datasetState.selectedDataset,
                selectedEntry = datasetState.selectedEntry,
                isPremium = true, // TODO: Wire up subscription status
                sheetState = coordinatorSheetState,
                onDatasetSelected = { dataset ->
                    viewModel.datasetStore.selectDataset(dataset)
                },
                onDismiss = { coordinator.dismissSheet() }
            )
        }

        // Dataset filter sheet
        if (coordinator.activeSheet is MapSheet.DatasetFilter && datasetState.primaryConfig != null && datasetState.selectedDataset != null) {
            val config = datasetState.primaryConfig!!
            val dataset = datasetState.selectedDataset!!
            val datasetType = DatasetType.fromRawValue(dataset.type) ?: DatasetType.SST
            val entry = datasetState.selectedEntry
            val rangeKey = datasetType.rangeKey
            val rangeData = entry?.ranges?.get(rangeKey)
            val dataRange = if (rangeData?.min != null && rangeData.max != null) {
                rangeData.min..rangeData.max
            } else {
                datasetState.renderingSnapshot.dataMin..datasetState.renderingSnapshot.dataMax
            }

            DatasetFilterSheet(
                config = config,
                dataRange = dataRange,
                datasetType = datasetType,
                apiUnit = DatasetConfiguration.forDatasetType(datasetType).unit,
                decimalPlaces = DatasetConfiguration.forDatasetType(datasetType).decimalPlaces,
                onConfigChanged = { newConfig ->
                    viewModel.datasetStore.updatePrimaryConfig(newConfig)
                },
                onDragRangeChanged = { min, max ->
                    viewModel.datasetStore.setFilterRangeDirect(min, max)
                },
                onDismiss = { coordinator.dismissSheet() }
            )
        }

        // Waypoint detail sheet
        waypointState.activeWaypointSheet?.let { sheet ->
            when (sheet) {
                is WaypointSheet.Details -> {
                    val waypoint = waypointState.allWaypoints.find { it.id == sheet.waypointId }
                    if (waypoint != null) {
                        WaypointDetailSheet(
                            sheetState = waypointSheetState,
                            waypoint = waypoint,
                            source = WaypointSource.Own,
                            gpsFormat = GPSFormat.DMM, // TODO: wire from user preferences
                            onDismiss = { viewModel.waypointStore.dismissWaypointSheet() },
                            onEdit = { viewModel.waypointStore.openWaypointForm(it) },
                            onDelete = { viewModel.waypointStore.deleteWaypoint(it) },
                            onShareToCrew = { waypoint ->
                                coordinator.openSheet(MapSheet.ShareWaypoint(waypoint))
                            },
                            onShareGPX = { /* TODO: wire GPX export */ },
                            onNotesChanged = { notes ->
                                val updated = waypoint.copy(notes = notes.ifEmpty { null })
                                viewModel.waypointStore.saveWaypoint(updated)
                            }
                        )
                    }
                }
                is WaypointSheet.Form -> {
                    WaypointFormSheet(
                        sheetState = waypointSheetState,
                        waypoint = sheet.waypoint,
                        isNewWaypoint = sheet.waypoint.name == null,
                        gpsFormat = GPSFormat.DMM, // TODO: wire from user preferences
                        formState = waypointState.waypointFormState,
                        onFormStateChange = { viewModel.waypointStore.updateWaypointFormState(it) },
                        onSave = {
                            val updated = waypointState.waypointFormState.buildWaypoint(
                                from = sheet.waypoint,
                                format = GPSFormat.DMM
                            )
                            if (updated != null) {
                                viewModel.waypointStore.saveWaypoint(updated)
                                viewModel.waypointStore.dismissWaypointSheet()
                            }
                        },
                        onCancel = { viewModel.waypointStore.dismissWaypointSheet() },
                        onDismiss = { viewModel.waypointStore.dismissWaypointSheet() }
                    )
                }
            }
        }

        // Waypoint management sheet
        if (coordinator.activeSheet is MapSheet.WaypointManagement) {
            WaypointManagementSheet(
                sheetState = coordinatorSheetState,
                sections = waypointState.groupedWaypoints,
                sortOption = waypointState.waypointSortOption,
                selectedWaypointId = waypointState.selectedWaypointId,
                onSortOptionChanged = { viewModel.waypointStore.updateWaypointSortOption(it) },
                onWaypointTap = { id ->
                    coordinator.dismissSheet()
                    viewModel.waypointStore.openWaypointDetails(id)
                },
                onWaypointDelete = { viewModel.waypointStore.deleteWaypoint(it) },
                onImportGPX = { gpxPickerLauncher.launch(arrayOf("*/*")) },
                onDismiss = { coordinator.dismissSheet() }
            )
        }

        // Announcement sheet
        if (appState.showAnnouncementSheet) {
            appState.announcement?.let { ann ->
                AnnouncementSheetView(
                    announcement = ann,
                    onDismiss = { viewModel.markAnnouncementAsSeen() },
                    sheetState = announcementSheetState,
                )
            }
        }

        // Tools menu sheet
        if (coordinator.activeSheet is MapSheet.Tools) {
            androidx.compose.material3.ModalBottomSheet(
                onDismissRequest = { coordinator.dismissSheet() },
                sheetState = coordinatorSheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                MapToolBar(
                    onAddWaypoint = {
                        coordinator.dismissSheet()
                        // TODO: Open waypoint creation at map center
                    },
                    onSatellites = {
                        coordinator.dismissSheet()
                        viewModel.satelliteTrackingMode.enter()
                    },
                    onMyLocation = {
                        coordinator.dismissSheet()
                        // TODO: Fly to user location
                    },
                    onShare = {
                        coordinator.dismissSheet()
                        val rs = viewModel.regionStore.state.value
                        val ds = viewModel.datasetStore.state.value
                        val region = rs.selectedRegion ?: return@MapToolBar
                        val dataset = ds.selectedDataset ?: return@MapToolBar
                        val entry = ds.selectedEntry ?: return@MapToolBar
                        viewModel.datasetStore.captureMapSnapshot?.invoke()
                        viewModel.savedMapsStore.createShareLink(
                            region = region,
                            dataset = dataset,
                            entry = entry,
                            config = ds.primaryConfig,
                            zoom = cameraZoom,
                            latitude = cameraLatitude,
                            longitude = cameraLongitude,
                            selectedDepth = ds.depthFilterState.selectedDepth,
                            onError = { viewModel.notificationManager.updateError(it) }
                        )
                    },
                    onWaypoints = {
                        coordinator.openSheet(MapSheet.WaypointManagement)
                    },
                    onCrews = {
                        coordinator.openSheet(MapSheet.CrewList)
                    },
                    onSavedMaps = {
                        coordinator.openSheet(MapSheet.SavedMaps)
                    },
                    onSaveMap = {
                        coordinator.openSheet(MapSheet.SaveMap)
                    },
                    onCreateCrew = {
                        coordinator.openSheet(MapSheet.CreateCrew)
                    },
                    onJoinCrew = {
                        coordinator.openSheet(MapSheet.JoinCrew)
                    },
                    onDatasetGuide = {
                        coordinator.openSheet(MapSheet.DatasetGuide)
                    },
                    onDismiss = { coordinator.dismissSheet() }
                )
            }
        }

        // Dataset guide sheet
        if (coordinator.activeSheet is MapSheet.DatasetGuide) {
            androidx.compose.material3.ModalBottomSheet(
                onDismissRequest = { coordinator.dismissSheet() },
                sheetState = coordinatorSheetState,
                containerColor = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Dataset Guide",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        androidx.compose.material3.TextButton(
                            onClick = { coordinator.dismissSheet() }
                        ) {
                            Text("Done", color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    com.example.saltyoffshore.ui.components.DatasetGuideView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(600.dp)
                    )
                }
            }
        }

        // Share link preview sheet — full-screen style matching iOS
        savedMapsState.shareLinkUrl?.let { url ->
            androidx.compose.material3.ModalBottomSheet(
                onDismissRequest = { viewModel.savedMapsStore.dismissShareLink() },
                sheetState = shareLinkSheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = null
            ) {
                ShareLinkSheet(
                    url = url,
                    mapSnapshot = savedMapsState.shareLinkSnapshot,
                    regionName = regionState.selectedRegion?.name ?: "Unknown",
                    datasetName = datasetState.selectedDataset?.let {
                        DatasetType.fromRawValue(it.type)?.shortName ?: it.type
                    } ?: "Unknown",
                    timestamp = datasetState.selectedEntry?.timestamp ?: "",
                    latitude = cameraLatitude,
                    longitude = cameraLongitude,
                    onDismiss = { viewModel.savedMapsStore.dismissShareLink() }
                )
            }
        }

        // Station detail sheet
        stationState.selectedStationId?.let { stationId ->
            val stationDetailViewModel: StationDetailViewModel = viewModel(
                key = "stationDetail",
                factory = androidx.lifecycle.ViewModelProvider.NewInstanceFactory()
            )
            androidx.compose.material3.ModalBottomSheet(
                onDismissRequest = { viewModel.stationStore.dismissStationDetail() },
                sheetState = stationSheetState,
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

        // Crew list sheet
        if (coordinator.activeSheet is MapSheet.CrewList) {
            CrewListSheet(
                sheetState = coordinatorSheetState,
                crews = crewState.crews,
                crewWaypoints = crewState.crewWaypoints,
                savedMaps = savedMapsState.savedMaps,
                selectedCrew = crewState.selectedCrew,
                selectedCrewMembers = crewState.selectedCrewMembers,
                isCreator = crewState.selectedCrew?.let { viewModel.crewStore.isCreator(it) } ?: false,
                hasDisplayName = prefsState.hasDisplayName,
                onSelectCrew = viewModel.crewStore::selectCrew,
                onCreateCrew = { name, onSuccess -> viewModel.crewStore.createCrew(name, onSuccess) },
                onJoinCrew = { code, onSuccess, onError -> viewModel.crewStore.joinCrew(code, onSuccess, onError) },
                onLeaveCrew = { crew, onComplete -> viewModel.crewStore.leaveCrew(crew, onComplete) },
                onDeleteCrew = { crew, onComplete -> viewModel.crewStore.deleteCrew(crew, onComplete) },
                onRemoveMember = { crewId, memberId -> viewModel.crewStore.removeMember(crewId, memberId) },
                onUpdateCrewName = { crewId, newName, onSuccess -> viewModel.crewStore.updateCrewName(crewId, newName, onSuccess) },
                onSaveName = { firstName, lastName ->
                    viewModel.userPreferencesStore.saveName(firstName, lastName)
                },
                onWaypointTap = { sharedWaypoint ->
                    coordinator.dismissSheet()
                    viewModel.waypointStore.selectWaypoint(sharedWaypoint.waypoint.id)
                },
                onLoadMap = { savedMap ->
                    coordinator.dismissSheet()
                    // TODO: apply saved map configuration
                },
                onDismiss = { coordinator.dismissSheet() },
            )
        }

        // Create crew sheet
        if (coordinator.activeSheet is MapSheet.CreateCrew) {
            CreateCrewSheet(
                sheetState = coordinatorSheetState,
                onDismiss = { coordinator.dismissSheet() },
                onCrewCreated = { crew ->
                    coordinator.dismissSheet()
                    viewModel.crewStore.loadCrews()
                },
                onSaveName = { firstName, lastName ->
                    viewModel.userPreferencesStore.saveName(firstName, lastName)
                },
                hasDisplayName = prefsState.hasDisplayName,
            )
        }

        // Join crew sheet
        if (coordinator.activeSheet is MapSheet.JoinCrew) {
            JoinCrewSheet(
                sheetState = coordinatorSheetState,
                onDismiss = { coordinator.dismissSheet() },
                onCrewJoined = { crew ->
                    coordinator.dismissSheet()
                    viewModel.crewStore.loadCrews()
                },
                onSaveName = { firstName, lastName ->
                    viewModel.userPreferencesStore.saveName(firstName, lastName)
                },
                hasDisplayName = prefsState.hasDisplayName,
            )
        }

        // Save map sheet
        if (coordinator.activeSheet is MapSheet.SaveMap) {
            SaveMapSheet(
                sheetState = coordinatorSheetState,
                crews = crewState.crews,
                regionName = regionState.selectedRegion?.name,
                datasetName = datasetState.selectedDataset?.type,
                isSaving = savedMapsState.isSavingMap,
                onSave = { name, crewId ->
                    // TODO: build MapConfiguration from current state
                    coordinator.dismissSheet()
                },
                onDismiss = { coordinator.dismissSheet() },
            )
        }

        // Saved maps list sheet
        if (coordinator.activeSheet is MapSheet.SavedMaps) {
            SavedMapsListSheet(
                sheetState = coordinatorSheetState,
                savedMaps = savedMapsState.savedMaps,
                crews = crewState.crews,
                currentUserId = AuthManager.currentUserId,
                isLoading = savedMapsState.isLoadingSavedMaps,
                onLoadMap = { savedMap ->
                    coordinator.dismissSheet()
                    // TODO: apply saved map configuration
                },
                onDeleteMap = { viewModel.savedMapsStore.deleteSavedMap(it) },
                onShareToCrew = { mapId, crewId, name -> viewModel.savedMapsStore.shareMapWithCrew(mapId, crewId, name) },
                onUnshare = { viewModel.savedMapsStore.unshareMap(it) },
                onDismiss = { coordinator.dismissSheet() },
            )
        }

        // Share waypoint to crew sheet
        (coordinator.activeSheet as? MapSheet.ShareWaypoint)?.let { sheet ->
            ShareWaypointSheet(
                sheetState = coordinatorSheetState,
                waypoint = sheet.waypoint,
                crews = crewState.crews,
                onShare = { crewIds ->
                    viewModel.crewStore.shareWaypointToCrews(sheet.waypoint, crewIds)
                    coordinator.dismissSheet()
                },
                onDismiss = { coordinator.dismissSheet() },
            )
        }
    }
}

// Private helper composables (DatasetLayersEffect, ZarrRepaintEffect, CrosshairQueryEffect,
// GlobalLayersEffect, WaypointLayersEffect) have been moved to MapContent.kt.
