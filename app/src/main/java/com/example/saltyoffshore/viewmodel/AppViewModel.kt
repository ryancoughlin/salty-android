package com.example.saltyoffshore.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.saltyoffshore.auth.AuthManager
import com.example.saltyoffshore.auth.SupabaseClientProvider
import com.example.saltyoffshore.data.AppStatus
import com.example.saltyoffshore.data.Colorscale
import com.example.saltyoffshore.data.CurrentValue
import com.example.saltyoffshore.data.Dataset
import com.example.saltyoffshore.data.DatasetRenderingSnapshot
import com.example.saltyoffshore.data.DatasetType
import com.example.saltyoffshore.data.DepthFilterState
import com.example.saltyoffshore.data.DepthUnits
import com.example.saltyoffshore.data.DistanceUnits
import com.example.saltyoffshore.data.RegionMetadata
import com.example.saltyoffshore.data.SaltyApi
import com.example.saltyoffshore.data.ScaleMode
import com.example.saltyoffshore.data.SpeedUnits
import com.example.saltyoffshore.data.TemperatureUnits
import com.example.saltyoffshore.data.TimeEntry
import com.example.saltyoffshore.data.VisualLayerSource
import com.example.saltyoffshore.data.scaleMode
import com.example.saltyoffshore.data.zarrVariable
import com.example.saltyoffshore.preferences.AppPreferencesDataStore
import com.example.saltyoffshore.repository.UserPreferencesRepository
import com.example.saltyoffshore.zarr.TimeEntry as ZarrTimeEntry
import com.example.saltyoffshore.zarr.ZarrManager
import com.example.saltyoffshore.zarr.ZarrVisualLayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "AppViewModel"

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val context get() = getApplication<Application>()
    private val preferencesRepository = UserPreferencesRepository(SupabaseClientProvider.client)

    // --- Single UI state ---
    private val _state = MutableStateFlow(MapScreenState())
    val state: StateFlow<MapScreenState> = _state.asStateFlow()

    private fun updateState(transform: MapScreenState.() -> MapScreenState) {
        _state.update { it.transform() }
    }

    // --- Non-UI singletons (NOT in state) ---

    val globalLayerManager = GlobalLayerManager(context, viewModelScope)

    private val zarrManager = ZarrManager(coroutineScope = viewModelScope)

    var repaint: (() -> Unit)? = null
        set(value) {
            field = value
            zarrManager.repaint = value
        }

    // =========================================================================
    // Init
    // =========================================================================

    init {
        loadRegionsAndRestoreSelection()
        loadUserPreferences()
    }

    private fun loadUserPreferences() {
        val userId = AuthManager.currentUserId ?: return
        viewModelScope.launch {
            val prefs = preferencesRepository.fetchPreferences(userId)
            updateState { copy(userPreferences = prefs) }
            Log.d(TAG, "Loaded user preferences: ${prefs != null}")
        }
    }

    private fun loadRegionsAndRestoreSelection() {
        viewModelScope.launch {
            try {
                val response = SaltyApi.getRegions()
                val groups = response.groups
                val regionList = groups.flatMap { it.regions }
                Log.d(TAG, "Loaded ${regionList.size} regions")

                val preferredId = AppPreferencesDataStore.getPreferredRegionId(context).first()

                updateState {
                    copy(
                        regionGroups = groups,
                        regions = regionList,
                        preferredRegionId = preferredId,
                    )
                }

                // Restore persisted region selection
                val savedRegionId = AppPreferencesDataStore.getSelectedRegionId(context).first()
                if (savedRegionId != null && regionList.any { it.id == savedRegionId }) {
                    Log.d(TAG, "Restoring saved region: $savedRegionId")
                    onRegionSelected(savedRegionId)
                }

                updateState { copy(hasCompletedInitialLoad = true) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load regions", e)
                updateState { copy(hasCompletedInitialLoad = true) }
            }
        }
    }

    // =========================================================================
    // Region Selection
    // =========================================================================

    fun onRegionSelected(regionId: String) {
        updateState { copy(appStatus = AppStatus.Loading) }
        Log.d(TAG, "Region selected: $regionId")

        viewModelScope.launch {
            AppPreferencesDataStore.setSelectedRegionId(context, regionId)

            try {
                val region = SaltyApi.fetchRegion(regionId)
                Log.d(TAG, "Loaded region: ${region.name} with ${region.datasets.size} datasets")
                updateState { copy(selectedRegion = region) }
                handleDatasetSetup(region)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load region", e)
                updateState { copy(appStatus = AppStatus.Error("Failed to load region: ${e.message}")) }
            }
        }
    }

    private fun handleDatasetSetup(region: RegionMetadata) {
        if (region.status == "coming_soon") {
            updateState {
                copy(
                    appStatus = AppStatus.ComingSoon,
                    selectedDataset = null,
                    selectedEntry = null,
                    depthFilterState = DepthFilterState(),
                )
            }
            return
        }

        val firstDataset = region.activeDatasets.firstOrNull()
        if (firstDataset == null) {
            Log.w(TAG, "No active datasets for region ${region.name}")
            updateState { copy(appStatus = AppStatus.Idle) }
            return
        }

        Log.d(TAG, "Selected dataset: ${firstDataset.name} (${firstDataset.type})")

        val depths = firstDataset.availableDepths ?: listOf(0)
        val newDepthFilter = DepthFilterState(selectedDepth = 0, availableDepths = depths)

        val entry = firstDataset.mostRecentEntry
        val newSnapshot = buildSnapshotForEntry(entry, firstDataset, _state.value.renderingSnapshot)

        // ONE atomic state update
        updateState {
            copy(
                selectedDataset = firstDataset,
                depthFilterState = newDepthFilter,
                selectedEntry = entry,
                renderingSnapshot = newSnapshot,
                appStatus = AppStatus.Idle,
            )
        }

        if (entry != null) {
            Log.d(TAG, "Selected entry: ${entry.timestamp}")
        }

        loadZarrForDataset(firstDataset)
    }

    // =========================================================================
    // Entry / Dataset Selection
    // =========================================================================

    fun selectEntry(entry: TimeEntry) {
        Log.d(TAG, "Entry selected: ${entry.timestamp}")
        val dataset = _state.value.selectedDataset
        val newSnapshot = buildSnapshotForEntry(entry, dataset, _state.value.renderingSnapshot)

        updateState {
            copy(
                selectedEntry = entry,
                renderingSnapshot = newSnapshot,
            )
        }

        zarrManager.showFrame(entry.id)
    }

    fun selectDataset(dataset: Dataset) {
        Log.d(TAG, "Dataset selected: ${dataset.name}")

        val depths = dataset.availableDepths ?: listOf(0)
        val newDepthFilter = DepthFilterState(selectedDepth = 0, availableDepths = depths)
        val entry = dataset.mostRecentEntry
        val newSnapshot = buildSnapshotForEntry(entry, dataset, _state.value.renderingSnapshot)

        updateState {
            copy(
                selectedDataset = dataset,
                selectedEntry = entry,
                depthFilterState = newDepthFilter,
                renderingSnapshot = newSnapshot,
            )
        }

        loadZarrForDataset(dataset)
    }

    // =========================================================================
    // Zarr Loading
    // =========================================================================

    private fun loadZarrForDataset(dataset: Dataset) {
        val zarrUrl = dataset.zarrUrl
        if (zarrUrl == null) {
            Log.d(TAG, "Dataset ${dataset.name} has no zarrUrl, using COG fallback")
            updateState { copy(visualSource = VisualLayerSource.None) }
            return
        }

        val shaderHost = zarrManager.shaderHost
        val host: ZarrVisualLayer
        if (shaderHost == null) {
            host = ZarrVisualLayer()
            zarrManager.setShaderHost(host)
        } else {
            host = shaderHost
        }
        updateState { copy(visualSource = VisualLayerSource.Zarr(host)) }

        val datasetType = DatasetType.fromRawValue(dataset.type)
        val entries = dataset.entries.map { entry ->
            ZarrTimeEntry(
                id = entry.id,
                timestamp = parseTimestamp(entry.timestamp),
                depth = entry.depth
            )
        }

        val colorscale = datasetType?.defaultColorscale ?: com.example.saltyoffshore.data.Colorscale.VIRIDIS
        val scaleMode = datasetType?.scaleMode ?: ScaleMode.LINEAR
        val variableName = datasetType?.zarrVariable ?: "sea_surface_temperature"

        val rangeKey = datasetType?.rangeKey ?: "value"
        val currentEntry = _state.value.selectedEntry
        val rangeData = currentEntry?.ranges?.get(rangeKey)
        val dataRange = if (rangeData?.min != null && rangeData.max != null) {
            rangeData.min.toFloat()..rangeData.max.toFloat()
        } else {
            0f..100f
        }

        zarrManager.load(
            zarrUrl = zarrUrl,
            variableName = variableName,
            entries = entries,
            depths = dataset.availableDepths ?: listOf(0),
            dataRange = dataRange,
            initialEntryId = currentEntry?.id,
            colorscale = colorscale,
            scaleMode = scaleMode
        )

        Log.d(TAG, "Started Zarr load for ${dataset.name} with variable: $variableName")
    }

    private fun parseTimestamp(timestamp: String): Long {
        return try {
            java.time.Instant.parse(timestamp).epochSecond
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse timestamp: $timestamp", e)
            0L
        }
    }

    // =========================================================================
    // Clear Selection
    // =========================================================================

    fun clearSelection() {
        updateState {
            copy(
                selectedRegion = null,
                selectedDataset = null,
                selectedEntry = null,
                renderingSnapshot = DatasetRenderingSnapshot.default(),
                depthFilterState = DepthFilterState(),
                appStatus = AppStatus.Idle,
                visualSource = VisualLayerSource.None,
            )
        }
        zarrManager.removeAll()

        viewModelScope.launch {
            AppPreferencesDataStore.setSelectedRegionId(context, null)
        }
    }

    // =========================================================================
    // Depth Selection
    // =========================================================================

    fun onDepthSelected(depth: Int) {
        Log.d(TAG, "Depth selected: ${depth}m")

        val dataset = _state.value.selectedDataset ?: return
        val entriesAtDepth = dataset.entries.filter { it.depth == depth }
        val entry = entriesAtDepth.maxByOrNull { it.timestamp }
        val newSnapshot = buildSnapshotForEntry(entry, dataset, _state.value.renderingSnapshot)

        updateState {
            copy(
                depthFilterState = depthFilterState.copy(selectedDepth = depth),
                selectedEntry = entry,
                renderingSnapshot = newSnapshot,
            )
        }
    }

    // =========================================================================
    // Rendering Snapshot Toggles
    // =========================================================================

    fun toggleVisualLayer() {
        updateState { copy(renderingSnapshot = renderingSnapshot.copy(visualEnabled = !renderingSnapshot.visualEnabled)) }
    }

    fun toggleContourLayer() {
        updateState { copy(renderingSnapshot = renderingSnapshot.copy(contourEnabled = !renderingSnapshot.contourEnabled)) }
    }

    fun toggleArrowsLayer() {
        updateState { copy(renderingSnapshot = renderingSnapshot.copy(arrowsEnabled = !renderingSnapshot.arrowsEnabled)) }
    }

    fun toggleBreaksLayer() {
        updateState { copy(renderingSnapshot = renderingSnapshot.copy(breaksEnabled = !renderingSnapshot.breaksEnabled)) }
    }

    fun toggleNumbersLayer() {
        updateState { copy(renderingSnapshot = renderingSnapshot.copy(numbersEnabled = !renderingSnapshot.numbersEnabled)) }
    }

    fun updateVisualOpacity(opacity: Double) {
        updateState { copy(renderingSnapshot = renderingSnapshot.copy(visualOpacity = opacity)) }
    }

    fun updateContourOpacity(opacity: Double) {
        updateState { copy(renderingSnapshot = renderingSnapshot.copy(contourOpacity = opacity)) }
    }

    fun updateArrowsOpacity(opacity: Double) {
        updateState { copy(renderingSnapshot = renderingSnapshot.copy(arrowsOpacity = opacity)) }
    }

    fun updateBreaksOpacity(opacity: Double) {
        updateState { copy(renderingSnapshot = renderingSnapshot.copy(breaksOpacity = opacity)) }
    }

    fun updateNumbersOpacity(opacity: Double) {
        updateState { copy(renderingSnapshot = renderingSnapshot.copy(numbersOpacity = opacity)) }
    }

    fun updateDataRange(min: Double, max: Double) {
        updateState { copy(renderingSnapshot = renderingSnapshot.copy(dataMin = min, dataMax = max)) }
    }

    // =========================================================================
    // Crosshair Updates
    // =========================================================================

    fun updateCurrentValue(value: CurrentValue) {
        updateState { copy(currentValue = value) }
    }

    fun updateCameraState(zoom: Double, latitude: Double) {
        updateState { copy(currentZoom = zoom, currentLatitude = latitude) }
    }

    // =========================================================================
    // User Preferences
    // =========================================================================

    fun updateDepthUnits(units: DepthUnits) {
        val userId = AuthManager.currentUserId ?: return
        viewModelScope.launch {
            if (preferencesRepository.updateField(userId, "depth_units", units.rawValue)) {
                updateState { copy(userPreferences = userPreferences?.copy(depthUnits = units.rawValue)) }
                Log.d(TAG, "Updated depth units to ${units.displayName}")
            }
        }
    }

    fun updateDistanceUnits(units: DistanceUnits) {
        val userId = AuthManager.currentUserId ?: return
        viewModelScope.launch {
            if (preferencesRepository.updateField(userId, "distance_units", units.rawValue)) {
                updateState { copy(userPreferences = userPreferences?.copy(distanceUnits = units.rawValue)) }
                Log.d(TAG, "Updated distance units to ${units.displayName}")
            }
        }
    }

    fun updateSpeedUnits(units: SpeedUnits) {
        val userId = AuthManager.currentUserId ?: return
        viewModelScope.launch {
            if (preferencesRepository.updateField(userId, "speed_units", units.rawValue)) {
                updateState { copy(userPreferences = userPreferences?.copy(speedUnits = units.rawValue)) }
                Log.d(TAG, "Updated speed units to ${units.displayName}")
            }
        }
    }

    fun updateTemperatureUnits(units: TemperatureUnits) {
        val userId = AuthManager.currentUserId ?: return
        viewModelScope.launch {
            if (preferencesRepository.updateField(userId, "temperature_units", units.rawValue)) {
                updateState { copy(userPreferences = userPreferences?.copy(temperatureUnits = units.rawValue)) }
                Log.d(TAG, "Updated temperature units to ${units.displayName}")
            }
        }
    }

    // =========================================================================
    // FTUX Region Selection
    // =========================================================================

    fun selectRegionAsFTUX(regionId: String) {
        updateState { copy(ftuxLoadingRegionId = regionId) }
        viewModelScope.launch {
            AppPreferencesDataStore.setPreferredRegionId(context, regionId)
            updateState { copy(preferredRegionId = regionId) }
            onRegionSelected(regionId)
            updateState { copy(ftuxLoadingRegionId = null) }
        }
    }

    // =========================================================================
    // Foreground Refresh
    // =========================================================================

    fun refreshData() {
        viewModelScope.launch {
            try {
                val response = SaltyApi.getRegions()
                val groups = response.groups
                val regionList = groups.flatMap { it.regions }
                Log.d(TAG, "Refreshed ${regionList.size} regions")

                updateState {
                    copy(
                        regionGroups = groups,
                        regions = regionList,
                    )
                }

                _state.value.selectedRegion?.let { region ->
                    val refreshedRegion = SaltyApi.fetchRegion(region.id)
                    updateState { copy(selectedRegion = refreshedRegion) }
                    handleDatasetSetup(refreshedRegion)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to refresh data", e)
            }
        }
    }

    // =========================================================================
    // Sign Out
    // =========================================================================

    fun handleSignOut() {
        updateState { copy(userPreferences = null) }
        clearSelection()
        Log.d(TAG, "Sign out: app state cleared")
    }

    fun signOut() {
        viewModelScope.launch {
            AuthManager.signOut()
            handleSignOut()
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private fun buildSnapshotForEntry(
        entry: TimeEntry?,
        dataset: Dataset?,
        current: DatasetRenderingSnapshot
    ): DatasetRenderingSnapshot {
        if (entry == null || dataset == null) return current
        val datasetType = DatasetType.fromRawValue(dataset.type)
        val rangeKey = datasetType?.rangeKey ?: "value"
        val rangeData = entry.ranges?.get(rangeKey)
        return if (rangeData?.min != null && rangeData.max != null) {
            Log.d(TAG, "Updated data range: ${rangeData.min} - ${rangeData.max}")
            current.copy(dataMin = rangeData.min, dataMax = rangeData.max)
        } else {
            current
        }
    }
}
