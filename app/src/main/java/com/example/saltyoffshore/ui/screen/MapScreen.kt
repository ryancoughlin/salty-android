package com.example.saltyoffshore.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import com.example.saltyoffshore.data.Dataset
import androidx.compose.ui.platform.LocalContext
import com.example.saltyoffshore.data.DatasetConfiguration
import com.example.saltyoffshore.data.DatasetType
import com.example.saltyoffshore.data.TemperatureUnits
import com.example.saltyoffshore.data.waypoint.Waypoint
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

    // Load waypoints from disk on first composition
    LaunchedEffect(Unit) {
        viewModel.loadWaypoints()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ── Map (isolated composable — matches iOS MapView.swift / MapboxMapView_V2) ──
        // MapContent takes explicit params only. Sheet state changes in MapScreen
        // do NOT recompose MapContent — this is the key performance boundary.
        MapContent(
            selectedRegion = viewModel.selectedRegion,
            regions = viewModel.regions,
            selectedDataset = viewModel.selectedDataset,
            selectedEntry = viewModel.selectedEntry,
            renderingSnapshot = viewModel.renderingSnapshot,
            visualSource = viewModel.visualSource,
            isDataLayerActive = viewModel.isDataLayerActive,
            currentDatasetType = viewModel.currentDatasetType,
            globalLayerVisibility = viewModel.globalLayerManager.visibility,
            loranConfig = viewModel.globalLayerManager.selectedLoranConfig,
            selectedTournament = viewModel.globalLayerManager.selectedTournament,
            stations = viewModel.stations,
            isStationsEnabled = viewModel.globalLayerManager.isEnabled(
                com.example.saltyoffshore.data.GlobalLayerType.STATIONS
            ),
            waypoints = viewModel.waypoints,
            measurements = viewModel.measurementState.allMeasurements,
            measurementIsActive = viewModel.measurementState.isActive,
            distanceUnits = viewModel.currentDistanceUnits,
            satelliteTrackingMode = viewModel.satelliteTrackingMode,
            satelliteStore = viewModel.satelliteStore,
            onCameraChanged = { zoom, lat, lon ->
                cameraZoom = zoom
                cameraLatitude = lat
                cameraLongitude = lon
                viewModel.updateCameraState(zoom, lat, lon)
            },
            onPrimaryValueChanged = { viewModel.updatePrimaryValue(it) },
            onRegionSelected = { viewModel.onRegionSelected(it) },
            onStationTap = { viewModel.openStationDetail(it) },
            onWaypointTap = { viewModel.openWaypointDetails(it) },
            onWaypointLongPress = { point ->
                val waypoint = viewModel.createWaypoint(
                    latitude = point.latitude(),
                    longitude = point.longitude()
                )
                viewModel.openWaypointForm(waypoint)
            },
            onMeasurementTap = { viewModel.measurementState.addPoint(it) },
            onStationsNeedLoad = { viewModel.loadStationsIfNeeded() },
            onCaptureMapSnapshotReady = { capture -> viewModel.captureMapSnapshot = capture },
            onMapSnapshotTaken = { viewModel.setMapSnapshot(it) },
            onRepaintReady = { repaint -> viewModel.repaint = repaint },
            modifier = Modifier.fillMaxSize(),
        )

        // Crosshair overlay
        CrosshairOverlay(
            primaryValue = viewModel.primaryValue,
            temperatureUnits = TemperatureUnits.fromRawValue(viewModel.userPreferences?.temperatureUnits)
                ?: TemperatureUnits.FAHRENHEIT,
            zoomProvider = { cameraZoom },
            latitudeProvider = { cameraLatitude },
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

        // Crew filter chips
        if (viewModel.crews.isNotEmpty()) {
            CrewChipsOverlay(
                crews = viewModel.crews,
                activeCrewId = viewModel.activeCrewId,
                unreadCounts = emptyMap(),
                onSelectCrew = { viewModel.activeCrewId = it },
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
        if (viewModel.depthFilterState.hasSelection) {
            DepthSelector(
                depthFilter = viewModel.depthFilterState,
                onDepthSelected = { viewModel.onDepthSelected(it) },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = Spacing.large)
            )
        }

        // Bottom controls — extracted composable (matches iOS MapControlsOverlay)
        // Own recomposition scope: sheet state changes don't touch this.
        if (viewModel.selectedDataset != null && !viewModel.satelliteTrackingMode.isActive) {
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
        if (coordinator.activeSheet is MapSheet.Layers && viewModel.selectedDataset != null && viewModel.primaryConfig != null) {
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
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                onDismiss = { coordinator.dismissSheet() }
            )
        }

        // Dataset selector sheet
        if (coordinator.activeSheet is MapSheet.DatasetSelector && viewModel.selectedRegion != null) {
            DatasetSelectorSheet(
                datasets = viewModel.selectedRegion!!.activeDatasets,
                selectedDataset = viewModel.selectedDataset,
                selectedEntry = viewModel.selectedEntry,
                isPremium = true, // TODO: Wire up subscription status
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                onDatasetSelected = { dataset ->
                    viewModel.selectDataset(dataset)
                },
                onDismiss = { coordinator.dismissSheet() }
            )
        }

        // Dataset filter sheet
        if (coordinator.activeSheet is MapSheet.DatasetFilter && viewModel.primaryConfig != null && viewModel.selectedDataset != null) {
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
                onDismiss = { coordinator.dismissSheet() }
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
                            onShareToCrew = { waypoint ->
                                coordinator.openSheet(MapSheet.ShareWaypoint(waypoint))
                            },
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
        if (coordinator.activeSheet is MapSheet.WaypointManagement) {
            WaypointManagementSheet(
                sections = viewModel.groupedWaypoints,
                sortOption = viewModel.waypointSortOption,
                selectedWaypointId = viewModel.selectedWaypointId,
                onSortOptionChanged = { viewModel.updateWaypointSortOption(it) },
                onWaypointTap = { id ->
                    coordinator.dismissSheet()
                    viewModel.openWaypointDetails(id)
                },
                onWaypointDelete = { viewModel.deleteWaypoint(it) },
                onImportGPX = { gpxPickerLauncher.launch(arrayOf("*/*")) },
                onDismiss = { coordinator.dismissSheet() }
            )
        }

        // Announcement sheet
        if (viewModel.showAnnouncementSheet) {
            viewModel.announcement?.let { ann ->
                AnnouncementSheetView(
                    announcement = ann,
                    onDismiss = { viewModel.markAnnouncementAsSeen() }
                )
            }
        }

        // Tools menu sheet
        if (coordinator.activeSheet is MapSheet.Tools) {
            androidx.compose.material3.ModalBottomSheet(
                onDismissRequest = { coordinator.dismissSheet() },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
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
                        viewModel.createShareLink()
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
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
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
                    latitude = cameraLatitude,
                    longitude = cameraLongitude,
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

        // Crew list sheet
        if (coordinator.activeSheet is MapSheet.CrewList) {
            CrewListSheet(
                crews = viewModel.crews,
                crewWaypoints = viewModel.crewWaypoints,
                savedMaps = viewModel.savedMaps,
                selectedCrew = viewModel.selectedCrew,
                selectedCrewMembers = viewModel.selectedCrewMembers,
                isCreator = viewModel.selectedCrew?.let { viewModel.isCreator(it) } ?: false,
                hasDisplayName = viewModel.hasDisplayName,
                onSelectCrew = viewModel::selectCrew,
                onCreateCrew = { name, onSuccess -> viewModel.createCrew(name, onSuccess) },
                onJoinCrew = { code, onSuccess, onError -> viewModel.joinCrew(code, onSuccess, onError) },
                onLeaveCrew = { crew, onComplete -> viewModel.leaveCrew(crew, onComplete) },
                onDeleteCrew = { crew, onComplete -> viewModel.deleteCrew(crew, onComplete) },
                onRemoveMember = { crewId, memberId -> viewModel.removeMember(crewId, memberId) },
                onUpdateCrewName = { crewId, newName, onSuccess -> viewModel.updateCrewName(crewId, newName, onSuccess) },
                onSaveName = { firstName, lastName -> viewModel.saveName(firstName, lastName) },
                onWaypointTap = { sharedWaypoint ->
                    coordinator.dismissSheet()
                    viewModel.selectWaypoint(sharedWaypoint.waypoint.id)
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
                onDismiss = { coordinator.dismissSheet() },
                onCrewCreated = { crew ->
                    coordinator.dismissSheet()
                    viewModel.loadCrews()
                },
                onSaveName = { firstName, lastName -> viewModel.saveName(firstName, lastName) },
                hasDisplayName = viewModel.hasDisplayName,
            )
        }

        // Join crew sheet
        if (coordinator.activeSheet is MapSheet.JoinCrew) {
            JoinCrewSheet(
                onDismiss = { coordinator.dismissSheet() },
                onCrewJoined = { crew ->
                    coordinator.dismissSheet()
                    viewModel.loadCrews()
                },
                onSaveName = { firstName, lastName -> viewModel.saveName(firstName, lastName) },
                hasDisplayName = viewModel.hasDisplayName,
            )
        }

        // Save map sheet
        if (coordinator.activeSheet is MapSheet.SaveMap) {
            SaveMapSheet(
                crews = viewModel.crews,
                regionName = viewModel.selectedRegion?.name,
                datasetName = viewModel.selectedDataset?.type,
                isSaving = viewModel.isSavingMap,
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
                savedMaps = viewModel.savedMaps,
                crews = viewModel.crews,
                currentUserId = AuthManager.currentUserId,
                isLoading = viewModel.isLoadingSavedMaps,
                onLoadMap = { savedMap ->
                    coordinator.dismissSheet()
                    // TODO: apply saved map configuration
                },
                onDeleteMap = { viewModel.deleteSavedMap(it) },
                onShareToCrew = { mapId, crewId, name -> viewModel.shareMapWithCrew(mapId, crewId, name) },
                onUnshare = { viewModel.unshareMap(it) },
                onDismiss = { coordinator.dismissSheet() },
            )
        }

        // Share waypoint to crew sheet
        (coordinator.activeSheet as? MapSheet.ShareWaypoint)?.let { sheet ->
            ShareWaypointSheet(
                waypoint = sheet.waypoint,
                crews = viewModel.crews,
                onShare = { crewIds ->
                    viewModel.shareWaypointToCrews(sheet.waypoint, crewIds)
                    coordinator.dismissSheet()
                },
                onDismiss = { coordinator.dismissSheet() },
            )
        }
    }
}

// Private helper composables (DatasetLayersEffect, ZarrRepaintEffect, CrosshairQueryEffect,
// GlobalLayersEffect, WaypointLayersEffect) have been moved to MapContent.kt.
