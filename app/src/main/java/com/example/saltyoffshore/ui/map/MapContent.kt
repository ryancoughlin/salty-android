package com.example.saltyoffshore.ui.map

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.example.saltyoffshore.config.AppConstants
import com.example.saltyoffshore.config.CrosshairConstants
import com.example.saltyoffshore.data.CurrentValue
import com.example.saltyoffshore.data.Dataset
import com.example.saltyoffshore.data.DatasetRenderingSnapshot
import com.example.saltyoffshore.data.DatasetType
import com.example.saltyoffshore.data.DistanceUnits
import com.example.saltyoffshore.data.GlobalLayerVisibility
import com.example.saltyoffshore.data.LoranRegionConfig
import com.example.saltyoffshore.data.RegionListItem
import com.example.saltyoffshore.data.RegionMetadata
import com.example.saltyoffshore.data.RegionStatus
import com.example.saltyoffshore.data.Station
import com.example.saltyoffshore.data.TimeEntry
import com.example.saltyoffshore.data.Tournament
import com.example.saltyoffshore.data.VisualLayerSource
import com.example.saltyoffshore.data.measurement.MapMeasurement
import com.example.saltyoffshore.data.waypoint.Waypoint
import com.example.saltyoffshore.managers.CrosshairFeatureQueryManager
import com.example.saltyoffshore.ui.components.RegionAnnotationView
import com.example.saltyoffshore.ui.map.globallayers.GlobalLayers
import com.example.saltyoffshore.ui.map.layers.DatasetLayers
import com.example.saltyoffshore.ui.map.satellite.SatelliteLayersEffect
import com.example.saltyoffshore.ui.map.waypoint.WaypointAnnotationLayer
import com.example.saltyoffshore.ui.measurement.MeasurementMapEffect
import com.example.saltyoffshore.viewmodel.SatelliteStore
import com.example.saltyoffshore.viewmodel.SatelliteTrackingMode
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraBoundsOptions
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.RenderedQueryGeometry
import com.mapbox.maps.RenderedQueryOptions
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.ViewAnnotation
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.extension.compose.style.projection.generated.Projection
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.viewannotation.geometry
import com.mapbox.maps.viewannotation.viewAnnotationOptions

private const val TAG = "MapContent"

/**
 * Standalone map composable that owns the MapboxMap and all map-internal effects.
 * Matches iOS MapboxMapView_V2.swift — isolated from sheet state recomposition.
 *
 * Takes explicit params only. No ViewModel reference crosses this boundary.
 */
@Composable
fun MapContent(
    // Region & dataset
    selectedRegion: RegionMetadata?,
    regions: List<RegionListItem>,
    selectedDataset: Dataset?,
    selectedEntry: TimeEntry?,
    renderingSnapshot: DatasetRenderingSnapshot,
    visualSource: VisualLayerSource,
    // Data layer state
    isDataLayerActive: Boolean,
    currentDatasetType: DatasetType?,
    // Global layers
    globalLayerVisibility: GlobalLayerVisibility,
    loranConfig: LoranRegionConfig?,
    selectedTournament: Tournament?,
    stations: List<Station>,
    isStationsEnabled: Boolean,
    // Waypoints
    waypoints: List<Waypoint>,
    // Measurement
    measurements: List<MapMeasurement>,
    measurementIsActive: Boolean,
    distanceUnits: DistanceUnits,
    // Satellite mode
    satelliteTrackingMode: SatelliteTrackingMode,
    satelliteStore: SatelliteStore,
    // Callbacks
    onCameraChanged: (zoom: Double, latitude: Double, longitude: Double) -> Unit,
    onPrimaryValueChanged: (CurrentValue) -> Unit,
    onRegionSelected: (String) -> Unit,
    onStationTap: (String) -> Unit,
    onWaypointTap: (String) -> Unit,
    onWaypointLongPress: (Point) -> Unit,
    onMeasurementTap: (Point) -> Unit,
    onStationsNeedLoad: () -> Unit,
    // Snapshot/repaint callbacks — MapContent calls these with its closures
    onCaptureMapSnapshotReady: ((() -> Unit)) -> Unit,
    onMapSnapshotTaken: (Bitmap?) -> Unit,
    onRepaintReady: ((() -> Unit)) -> Unit,
    modifier: Modifier = Modifier,
) {
    // ── Viewport state ──
    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(-60.0, 30.0))
            zoom(AppConstants.mapInitialWorldZoom)
            bearing(0.0)
            pitch(0.0)
        }
    }

    // Fly to region when selected — key on ID only, not full object
    LaunchedEffect(selectedRegion?.id) {
        selectedRegion?.let { region ->
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

    // ── Satellite mode viewport animations (iOS: MapModeModifier.swift) ──

    // Fly to globe on enter, back to region on exit
    LaunchedEffect(satelliteTrackingMode.isActive) {
        if (satelliteTrackingMode.isActive) {
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
            selectedRegion?.let { region ->
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
    LaunchedEffect(satelliteTrackingMode.selectedTrackId) {
        val id = satelliteTrackingMode.selectedTrackId ?: return@LaunchedEffect
        val track = satelliteStore.tracks.firstOrNull { it.id == id } ?: return@LaunchedEffect
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
    LaunchedEffect(satelliteTrackingMode.selectedPassId) {
        val id = satelliteTrackingMode.selectedPassId ?: return@LaunchedEffect
        val pass = satelliteStore.passes.firstOrNull { it.id == id } ?: return@LaunchedEffect
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
    val mapStyleUri = if (satelliteTrackingMode.isActive) {
        AppConstants.darkMapStyleURI
    } else {
        AppConstants.lightMapStyleURI
    }
    val mapProjection = if (satelliteTrackingMode.isActive) {
        Projection.GLOBE
    } else {
        Projection.MERCATOR
    }

    MapboxMap(
        modifier = modifier,
        mapViewportState = mapViewportState,
        style = { MapStyle(style = mapStyleUri, projection = mapProjection) }
    ) {
        // Wire up repaint callback for Zarr frame updates
        ZarrRepaintEffect(onRepaintReady = onRepaintReady)

        // Wire map snapshot capture for share links
        MapEffect(Unit) { mapView ->
            onCaptureMapSnapshotReady {
                mapView.snapshot { bitmap ->
                    onMapSnapshotTaken(bitmap)
                }
            }
        }

        // Zoom constraints: cap at 4.0 in satellite mode (iOS: cameraBounds)
        MapEffect(satelliteTrackingMode.isActive) { mapView ->
            val bounds = if (satelliteTrackingMode.isActive) {
                CameraBoundsOptions.Builder()
                    .minZoom(0.0).maxZoom(4.0).build()
            } else {
                CameraBoundsOptions.Builder()
                    .minZoom(1.0).maxZoom(24.0).build()
            }
            mapView.mapboxMap.setBounds(bounds)
        }

        // Region bounds outline
        RegionBoundsEffect(region = selectedRegion)

        // Dataset visualization layers (Zarr GPU, Contours, Currents, etc.)
        DatasetLayersEffect(
            dataset = selectedDataset,
            entry = selectedEntry,
            region = selectedRegion,
            snapshot = renderingSnapshot,
            visualSource = visualSource
        )

        // Load stations when layer is enabled (deferred — matches iOS)
        LaunchedEffect(isStationsEnabled) {
            if (isStationsEnabled) onStationsNeedLoad()
        }

        // Global overlay layers (bathymetry, shipping lanes, etc.)
        GlobalLayersEffect(
            visibility = globalLayerVisibility,
            loranConfig = loranConfig,
            selectedTournament = selectedTournament,
            stations = stations,
            onStationTap = { stationId ->
                Log.d(TAG, "Station tapped: $stationId")
                onStationTap(stationId)
            }
        )

        // Crosshair feature query on camera changes
        CrosshairQueryEffect(
            isDataLayerActive = isDataLayerActive,
            datasetType = currentDatasetType,
            onPrimaryValueChanged = onPrimaryValueChanged,
            onCameraChanged = onCameraChanged
        )

        // Region annotations — hide during satellite mode + hide the active region
        // key() gives each ViewAnnotation a stable identity so Compose can diff
        // the list instead of tearing down / recreating all annotations on every recompose.
        if (!satelliteTrackingMode.isActive) regions
            .filter { it.id != selectedRegion?.id }
            .forEach { region ->
                key(region.id) {
                    ViewAnnotation(
                        options = viewAnnotationOptions {
                            geometry(Point.fromLngLat(region.centerLon, region.centerLat))
                            allowOverlap(true)
                        }
                    ) {
                        RegionAnnotationView(
                            region = region,
                            isComingSoon = region.status == RegionStatus.COMING_SOON,
                            onClick = { onRegionSelected(region.id) }
                        )
                    }
                }
            }

        // Waypoint annotation layer with tap handling
        WaypointLayersEffect(
            waypoints = waypoints,
            onWaypointTap = onWaypointTap
        )

        // Long-press to create waypoint + open form
        MapEffect(Unit) { mapView ->
            mapView.gestures.addOnMapLongClickListener { point ->
                onWaypointLongPress(point)
                true
            }
        }

        // Measurement layers (lines, points, distance labels)
        MeasurementMapEffect(
            measurements = measurements,
            distanceUnits = distanceUnits
        )

        // Tap-to-measure: intercept map clicks when measure mode active
        // Registered once (Unit key) — isActive check gates behavior at runtime
        val currentMeasurementIsActive by rememberUpdatedState(measurementIsActive)
        val currentOnMeasurementTap by rememberUpdatedState(onMeasurementTap)
        MapEffect(Unit) { mapView ->
            mapView.gestures.addOnMapClickListener { point ->
                if (currentMeasurementIsActive) {
                    currentOnMeasurementTap(point)
                    true
                } else {
                    false
                }
            }
        }

        // Satellite tracking layers — uses MapEffect internally for style reload survival
        SatelliteLayersEffect(
            trackingMode = satelliteTrackingMode,
            store = satelliteStore
        )
    }
}

// ── Private helper composables (map-internal) ──

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
    val regionId = region?.id
    var datasetLayers by remember { mutableStateOf<DatasetLayers?>(null) }

    // Use rememberUpdatedState so we always have latest snapshot/visualSource
    // without tearing down the MapEffect (which is expensive — cancels coroutine,
    // re-enters MapView). This matches the GlobalLayersEffect pattern.
    val currentDataset by rememberUpdatedState(dataset)
    val currentEntry by rememberUpdatedState(entry)
    val currentRegion by rememberUpdatedState(region)
    val currentSnapshot by rememberUpdatedState(snapshot)
    val currentVisualSource by rememberUpdatedState(visualSource)

    // Clean up old layers when region changes
    DisposableEffect(regionId) {
        onDispose {
            datasetLayers?.removeAllLayers()
            datasetLayers = null
        }
    }

    // Key only on region + entry. NOT on snapshot — snapshot changes on every
    // opacity drag / contour toggle and would tear down the MapEffect each time.
    MapEffect(regionId, entry?.id) { mapView ->
        val mapboxMap = mapView.mapboxMap

        if (datasetLayers == null) {
            datasetLayers = DatasetLayers(mapboxMap)
        }

        datasetLayers?.render(
            dataset = currentDataset,
            entry = currentEntry,
            region = currentRegion,
            snapshot = currentSnapshot,
            visualSource = currentVisualSource
        )
    }

    // Re-render when snapshot or visualSource changes (incremental update, no teardown)
    LaunchedEffect(snapshot, visualSource) {
        datasetLayers?.render(
            dataset = currentDataset,
            entry = currentEntry,
            region = currentRegion,
            snapshot = currentSnapshot,
            visualSource = currentVisualSource
        )
    }
}

/**
 * Effect that wires up the repaint callback for Zarr frame updates.
 * Must be called once when map is ready.
 */
@Composable
private fun ZarrRepaintEffect(
    onRepaintReady: ((() -> Unit)) -> Unit
) {
    MapEffect(Unit) { mapView ->
        onRepaintReady {
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
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    var globalLayers by remember { mutableStateOf<GlobalLayers?>(null) }
    var mapboxMapRef by remember { mutableStateOf<com.mapbox.maps.MapboxMap?>(null) }

    // Use rememberUpdatedState to always have latest values in callbacks
    val currentVisibility by rememberUpdatedState(visibility)
    val currentLoranConfig by rememberUpdatedState(loranConfig)
    val currentTournament by rememberUpdatedState(selectedTournament)
    val currentStations by rememberUpdatedState(stations)
    val currentOnStationTap by rememberUpdatedState(onStationTap)

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
                val geometry = RenderedQueryGeometry(screenPoint)
                val options = RenderedQueryOptions(
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
        // Dispatch to main thread — style callbacks may fire on background threads
        // (see lessons.md: "Style Callbacks and Threading")
        mapView.mapboxMap.subscribeStyleLoaded { _ ->
            mainHandler.post {
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
        }

        // Also try immediately if style is already loaded
        mapView.mapboxMap.getStyle { _ ->
            mainHandler.post {
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
 * Simple waypoint layer effect: icons + text + tap handling.
 */
@Composable
private fun WaypointLayersEffect(
    waypoints: List<Waypoint>,
    onWaypointTap: (String) -> Unit
) {
    val context = LocalContext.current
    var layer by remember { mutableStateOf<WaypointAnnotationLayer?>(null) }
    var mapboxMapRef by remember { mutableStateOf<com.mapbox.maps.MapboxMap?>(null) }
    val currentWaypoints by rememberUpdatedState(waypoints)
    val currentOnWaypointTap by rememberUpdatedState(onWaypointTap)
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    DisposableEffect(Unit) {
        onDispose {
            layer?.removeFromMap()
            layer = null
        }
    }

    fun render(mapboxMap: com.mapbox.maps.MapboxMap) {
        if (layer == null) layer = WaypointAnnotationLayer(mapboxMap, context)
        layer?.update(currentWaypoints)
    }

    MapEffect(Unit) { mapView ->
        mapboxMapRef = mapView.mapboxMap

        // Tap detection via queryRenderedFeatures
        mapView.gestures.addOnMapClickListener { point ->
            val screenPoint = mapView.mapboxMap.pixelForCoordinate(point)
            val geometry = RenderedQueryGeometry(screenPoint)
            val options = RenderedQueryOptions(
                listOf(com.example.saltyoffshore.config.MapLayers.Waypoint.OWN_LAYER),
                null
            )
            mapView.mapboxMap.queryRenderedFeatures(geometry, options) { expected ->
                expected.value?.firstOrNull()?.let { feature ->
                    feature.queriedFeature.feature.getStringProperty("id")?.let { id ->
                        currentOnWaypointTap(id)
                    }
                }
            }
            false
        }

        mapView.mapboxMap.subscribeStyleLoaded { _ ->
            mainHandler.post { render(mapView.mapboxMap) }
        }

        mapView.mapboxMap.getStyle { _ -> render(mapView.mapboxMap) }
    }

    LaunchedEffect(waypoints) {
        val map = mapboxMapRef ?: return@LaunchedEffect
        map.getStyle { _ -> render(map) }
    }
}
