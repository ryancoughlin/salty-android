package com.example.saltyoffshore.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.saltyoffshore.auth.AuthManager
import com.example.saltyoffshore.auth.SupabaseClientProvider
import com.example.saltyoffshore.data.AppStatus
import com.example.saltyoffshore.data.Colorscale
import com.example.saltyoffshore.data.CurrentValue
import com.example.saltyoffshore.data.Dataset
import com.example.saltyoffshore.data.DatasetRenderConfig
import com.example.saltyoffshore.data.DatasetRenderingSnapshot
import com.example.saltyoffshore.data.DatasetType
import com.example.saltyoffshore.data.DepthFilterState
import com.example.saltyoffshore.data.DepthUnits
import com.example.saltyoffshore.data.DistanceUnits
import com.example.saltyoffshore.data.RegionGroup
import com.example.saltyoffshore.data.RegionListItem
import com.example.saltyoffshore.data.RegionMetadata
import com.example.saltyoffshore.data.SaltyApi
import com.example.saltyoffshore.data.ScaleMode
import com.example.saltyoffshore.data.SpeedUnits
import com.example.saltyoffshore.data.TemperatureUnits
import com.example.saltyoffshore.data.TimeEntry
import com.example.saltyoffshore.data.GlobalLayerType
import com.example.saltyoffshore.data.LoranRegionConfig
import com.example.saltyoffshore.data.Tournament
import com.example.saltyoffshore.data.UserPreferences
import com.example.saltyoffshore.data.VisualLayerSource
import com.example.saltyoffshore.data.scaleMode
import com.example.saltyoffshore.data.zarrVariable
import com.example.saltyoffshore.preferences.AppPreferencesDataStore
import com.example.saltyoffshore.repository.UserPreferencesRepository
import com.example.saltyoffshore.zarr.TimeEntry as ZarrTimeEntry
import com.example.saltyoffshore.zarr.ZarrManager
import com.example.saltyoffshore.zarr.ZarrVisualLayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "AppViewModel"

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val context get() = getApplication<Application>()
    private val preferencesRepository = UserPreferencesRepository(SupabaseClientProvider.client)

    // Global layer manager
    val globalLayerManager = GlobalLayerManager(context, viewModelScope)

    // Zarr manager for GPU rendering
    private val zarrManager = ZarrManager(coroutineScope = viewModelScope)

    // Visual layer source (Zarr GPU or None)
    var visualSource by mutableStateOf<VisualLayerSource>(VisualLayerSource.None)
        private set

    // Repaint callback — set by map when loaded
    var repaint: (() -> Unit)? = null
        set(value) {
            field = value
            zarrManager.repaint = value
        }

    // User preferences
    var userPreferences by mutableStateOf<UserPreferences?>(null)
        private set

    // App state
    var appStatus by mutableStateOf<AppStatus>(AppStatus.Idle)
        private set

    // Region list (from /regions)
    var regions by mutableStateOf<List<RegionListItem>>(emptyList())
        private set

    // Region groups (for FTUX grouped display)
    var regionGroups by mutableStateOf<List<RegionGroup>>(emptyList())
        private set

    // FTUX state
    var preferredRegionId by mutableStateOf<String?>(null)
        private set

    var hasCompletedInitialLoad by mutableStateOf(false)
        private set

    var ftuxLoadingRegionId by mutableStateOf<String?>(null)
        private set

    // Selected region metadata (from /region/{id})
    var selectedRegion by mutableStateOf<RegionMetadata?>(null)
        private set

    // Currently selected dataset
    var selectedDataset by mutableStateOf<Dataset?>(null)
        private set

    // Currently selected time entry
    var selectedEntry by mutableStateOf<TimeEntry?>(null)
        private set

    // Layer rendering config (source of truth for layer toggles/opacity)
    var primaryConfig by mutableStateOf<DatasetRenderConfig?>(null)
        private set

    // Layer rendering state (derived from config for map rendering)
    var renderingSnapshot by mutableStateOf(DatasetRenderingSnapshot.default())
        private set

    // Depth filter state
    var depthFilterState by mutableStateOf(DepthFilterState())
        private set

    // Crosshair state
    var currentValue by mutableStateOf<CurrentValue>(CurrentValue.None)
        private set

    var currentZoom by mutableStateOf(4.0)
        private set

    var currentLatitude by mutableStateOf(30.0)
        private set

    init {
        loadRegionsAndRestoreSelection()
        loadUserPreferences()
    }

    private fun loadUserPreferences() {
        val userId = AuthManager.currentUserId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val prefs = preferencesRepository.fetchPreferences(userId)
            withContext(Dispatchers.Main) {
                userPreferences = prefs
            }
            Log.d(TAG, "Loaded user preferences: ${prefs != null}")
        }
    }

    private fun loadRegionsAndRestoreSelection() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = SaltyApi.getRegions()
                val regionList = response.groups.flatMap { it.regions }
                val savedPreferredId = AppPreferencesDataStore.getPreferredRegionId(context).first()

                withContext(Dispatchers.Main) {
                    regions = regionList
                    regionGroups = response.groups
                    preferredRegionId = savedPreferredId
                }
                Log.d(TAG, "Loaded ${regionList.size} regions")

                // Restore persisted region selection
                val savedRegionId = AppPreferencesDataStore.getSelectedRegionId(context).first()
                if (savedRegionId != null && regionList.any { it.id == savedRegionId }) {
                    Log.d(TAG, "Restoring saved region: $savedRegionId")
                    withContext(Dispatchers.Main) {
                        onRegionSelected(savedRegionId)
                    }
                }

                withContext(Dispatchers.Main) {
                    hasCompletedInitialLoad = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load regions", e)
                withContext(Dispatchers.Main) {
                    hasCompletedInitialLoad = true
                }
            }
        }
    }

    fun onFTUXRegionSelected(regionId: String) {
        ftuxLoadingRegionId = regionId

        viewModelScope.launch(Dispatchers.IO) {
            // Save as preferred region (local + Supabase)
            AppPreferencesDataStore.setPreferredRegionId(context, regionId)
            val userId = AuthManager.currentUserId
            if (userId != null) {
                try {
                    preferencesRepository.updateField(userId, "preferred_region_id", regionId)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync preferred region to Supabase", e)
                }
            }

            withContext(Dispatchers.Main) {
                preferredRegionId = regionId
                ftuxLoadingRegionId = null
                onRegionSelected(regionId)
            }
        }
    }

    fun onRegionSelected(regionId: String) {
        appStatus = AppStatus.Loading
        Log.d(TAG, "Region selected: $regionId")

        viewModelScope.launch(Dispatchers.IO) {
            // Persist selection
            AppPreferencesDataStore.setSelectedRegionId(context, regionId)

            try {
                val region = SaltyApi.fetchRegion(regionId)
                withContext(Dispatchers.Main) {
                    selectedRegion = region
                    Log.d(TAG, "Loaded region: ${region.name} with ${region.datasets.size} datasets")

                    handleDatasetSetup(region)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load region", e)
                withContext(Dispatchers.Main) {
                    appStatus = AppStatus.Error("Failed to load region: ${e.message}")
                }
            }
        }
    }

    private fun handleDatasetSetup(region: RegionMetadata) {
        // Check status
        if (region.status == "coming_soon") {
            appStatus = AppStatus.ComingSoon
            selectedDataset = null
            selectedEntry = null
            depthFilterState = DepthFilterState()
            return
        }

        // Select first active dataset (mirrors iOS: region.activeDatasets.first)
        val firstDataset = region.activeDatasets.firstOrNull()
        if (firstDataset == null) {
            Log.w(TAG, "No active datasets for region ${region.name}")
            appStatus = AppStatus.Idle
            return
        }

        selectedDataset = firstDataset
        Log.d(TAG, "Selected dataset: ${firstDataset.name} (${firstDataset.type})")

        // Initialize render config from dataset type defaults
        val datasetType = DatasetType.fromRawValue(firstDataset.type) ?: DatasetType.SST
        primaryConfig = DatasetRenderConfig.primaryDefaults(datasetType, firstDataset.id)

        // Update depth filter state from dataset
        updateDepthFilterForDataset(firstDataset)

        // Select most recent entry (mirrors iOS: dataset.mostRecentEntry)
        selectedEntry = firstDataset.mostRecentEntry
        selectedEntry?.let { entry ->
            Log.d(TAG, "Selected entry: ${entry.timestamp}")
            updateRenderingSnapshotForEntry(entry, firstDataset)
        }

        // Load Zarr data if available
        loadZarrForDataset(firstDataset)

        appStatus = AppStatus.Idle
    }

    fun selectEntry(entry: TimeEntry) {
        selectedEntry = entry
        Log.d(TAG, "Entry selected: ${entry.timestamp}")
        selectedDataset?.let { dataset ->
            updateRenderingSnapshotForEntry(entry, dataset)
        }

        // Show frame in Zarr renderer
        zarrManager.showFrame(entry.id)
    }

    fun selectDataset(dataset: Dataset) {
        selectedDataset = dataset
        selectedEntry = dataset.mostRecentEntry
        Log.d(TAG, "Dataset selected: ${dataset.name}")

        // Initialize render config from dataset type defaults
        val datasetType = DatasetType.fromRawValue(dataset.type) ?: DatasetType.SST
        primaryConfig = DatasetRenderConfig.primaryDefaults(datasetType, dataset.id)

        // Update depth filter state from dataset
        updateDepthFilterForDataset(dataset)

        selectedEntry?.let { entry ->
            updateRenderingSnapshotForEntry(entry, dataset)
        }

        // Load Zarr data if available
        loadZarrForDataset(dataset)
    }

    // MARK: - Zarr Loading

    private fun loadZarrForDataset(dataset: Dataset) {
        val zarrUrl = dataset.zarrUrl
        if (zarrUrl == null) {
            Log.d(TAG, "Dataset ${dataset.name} has no zarrUrl, using COG fallback")
            visualSource = VisualLayerSource.None
            return
        }

        // Get shader host from manager
        val shaderHost = zarrManager.shaderHost
        if (shaderHost == null) {
            // Create and set shader host on first load
            val newHost = ZarrVisualLayer()
            zarrManager.setShaderHost(newHost)
            visualSource = VisualLayerSource.Zarr(newHost)
        } else {
            visualSource = VisualLayerSource.Zarr(shaderHost)
        }

        val datasetType = DatasetType.fromRawValue(dataset.type)

        // Build TimeEntry list for Zarr loading
        val entries = dataset.entries.map { entry ->
            ZarrTimeEntry(
                id = entry.id,
                timestamp = parseTimestamp(entry.timestamp),
                depth = entry.depth
            )
        }

        val colorscale = datasetType?.defaultColorscale ?: Colorscale.VIRIDIS
        val scaleMode = datasetType?.scaleMode ?: ScaleMode.LINEAR
        val variableName = datasetType?.zarrVariable ?: "sea_surface_temperature"

        // Get data range from first entry or use defaults
        val rangeKey = datasetType?.rangeKey ?: "value"
        val rangeData = selectedEntry?.ranges?.get(rangeKey)
        val dataRange = if (rangeData?.min != null && rangeData.max != null) {
            rangeData.min.toFloat()..rangeData.max.toFloat()
        } else {
            0f..100f
        }

        // Load Zarr data
        zarrManager.load(
            zarrUrl = zarrUrl,
            variableName = variableName,
            entries = entries,
            depths = dataset.availableDepths ?: listOf(0),
            dataRange = dataRange,
            initialEntryId = selectedEntry?.id,
            colorscale = colorscale,
            scaleMode = scaleMode
        )

        Log.d(TAG, "Started Zarr load for ${dataset.name} with variable: $variableName")
    }

    /**
     * Parse ISO8601 timestamp string to Unix seconds.
     */
    private fun parseTimestamp(timestamp: String): Long {
        return try {
            java.time.Instant.parse(timestamp).epochSecond
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse timestamp: $timestamp", e)
            0L
        }
    }

    fun clearSelection() {
        selectedRegion = null
        selectedDataset = null
        selectedEntry = null
        renderingSnapshot = DatasetRenderingSnapshot.default()
        depthFilterState = DepthFilterState()
        appStatus = AppStatus.Idle
        visualSource = VisualLayerSource.None
        zarrManager.removeAll()

        viewModelScope.launch(Dispatchers.IO) {
            AppPreferencesDataStore.setSelectedRegionId(context, null)
        }
    }

    // MARK: - Depth Selection

    private fun updateDepthFilterForDataset(dataset: Dataset) {
        val depths = dataset.availableDepths ?: listOf(0)
        depthFilterState = DepthFilterState(
            selectedDepth = 0,
            availableDepths = depths
        )
        Log.d(TAG, "Updated depth filter: ${depths.size} depths available")
    }

    fun onDepthSelected(depth: Int) {
        depthFilterState = depthFilterState.copy(selectedDepth = depth)
        Log.d(TAG, "Depth selected: ${depth}m")

        // Filter entries for selected depth and select most recent (mirrors iOS)
        selectedDataset?.let { dataset ->
            val entriesAtDepth = dataset.entries.filter { it.depth == depth }
            selectedEntry = entriesAtDepth.maxByOrNull { it.timestamp }
            selectedEntry?.let { entry ->
                updateRenderingSnapshotForEntry(entry, dataset)
            }
        }
    }

    // MARK: - Config-Based Rendering Updates

    fun updatePrimaryConfig(config: DatasetRenderConfig) {
        primaryConfig = config
        // Derive snapshot from config for backward compatibility
        val dataRange = renderingSnapshot.dataMin..renderingSnapshot.dataMax
        renderingSnapshot = config.snapshot(dataRange)
    }

    // MARK: - Rendering Snapshot Updates (legacy — will be removed in Task 2.4)

    fun toggleVisualLayer() {
        renderingSnapshot = renderingSnapshot.copy(
            visualEnabled = !renderingSnapshot.visualEnabled
        )
    }

    fun toggleContourLayer() {
        renderingSnapshot = renderingSnapshot.copy(
            contourEnabled = !renderingSnapshot.contourEnabled
        )
    }

    fun toggleArrowsLayer() {
        renderingSnapshot = renderingSnapshot.copy(
            arrowsEnabled = !renderingSnapshot.arrowsEnabled
        )
    }

    fun toggleBreaksLayer() {
        renderingSnapshot = renderingSnapshot.copy(
            breaksEnabled = !renderingSnapshot.breaksEnabled
        )
    }

    fun toggleNumbersLayer() {
        renderingSnapshot = renderingSnapshot.copy(
            numbersEnabled = !renderingSnapshot.numbersEnabled
        )
    }

    fun updateVisualOpacity(opacity: Double) {
        renderingSnapshot = renderingSnapshot.copy(visualOpacity = opacity)
    }

    fun updateContourOpacity(opacity: Double) {
        renderingSnapshot = renderingSnapshot.copy(contourOpacity = opacity)
    }

    fun updateArrowsOpacity(opacity: Double) {
        renderingSnapshot = renderingSnapshot.copy(arrowsOpacity = opacity)
    }

    fun updateBreaksOpacity(opacity: Double) {
        renderingSnapshot = renderingSnapshot.copy(breaksOpacity = opacity)
    }

    fun updateNumbersOpacity(opacity: Double) {
        renderingSnapshot = renderingSnapshot.copy(numbersOpacity = opacity)
    }

    fun updateDataRange(min: Double, max: Double) {
        renderingSnapshot = renderingSnapshot.copy(
            dataMin = min,
            dataMax = max
        )
    }

    private fun updateRenderingSnapshotForEntry(entry: TimeEntry, dataset: Dataset) {
        val datasetType = DatasetType.fromRawValue(dataset.type)
        val rangeKey = datasetType?.rangeKey ?: "value"
        val rangeData = entry.ranges?.get(rangeKey)

        if (rangeData?.min != null && rangeData.max != null) {
            renderingSnapshot = renderingSnapshot.copy(
                dataMin = rangeData.min,
                dataMax = rangeData.max
            )
            Log.d(TAG, "Updated data range: ${rangeData.min} - ${rangeData.max}")
        }
    }

    // MARK: - Crosshair Updates

    fun updateCurrentValue(value: CurrentValue) {
        currentValue = value
    }

    fun updateCameraState(zoom: Double, latitude: Double) {
        currentZoom = zoom
        currentLatitude = latitude
    }

    val isDataLayerActive: Boolean
        get() = selectedDataset != null && selectedEntry != null

    val currentDatasetType: DatasetType?
        get() = selectedDataset?.let { DatasetType.fromRawValue(it.type) }

    // MARK: - User Preferences Updates

    fun updateDepthUnits(units: DepthUnits) {
        val userId = AuthManager.currentUserId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            if (preferencesRepository.updateField(userId, "depth_units", units.rawValue)) {
                withContext(Dispatchers.Main) {
                    userPreferences = userPreferences?.copy(depthUnits = units.rawValue)
                }
                Log.d(TAG, "Updated depth units to ${units.displayName}")
            }
        }
    }

    fun updateDistanceUnits(units: DistanceUnits) {
        val userId = AuthManager.currentUserId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            if (preferencesRepository.updateField(userId, "distance_units", units.rawValue)) {
                withContext(Dispatchers.Main) {
                    userPreferences = userPreferences?.copy(distanceUnits = units.rawValue)
                }
                Log.d(TAG, "Updated distance units to ${units.displayName}")
            }
        }
    }

    fun updateSpeedUnits(units: SpeedUnits) {
        val userId = AuthManager.currentUserId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            if (preferencesRepository.updateField(userId, "speed_units", units.rawValue)) {
                withContext(Dispatchers.Main) {
                    userPreferences = userPreferences?.copy(speedUnits = units.rawValue)
                }
                Log.d(TAG, "Updated speed units to ${units.displayName}")
            }
        }
    }

    fun updateTemperatureUnits(units: TemperatureUnits) {
        val userId = AuthManager.currentUserId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            if (preferencesRepository.updateField(userId, "temperature_units", units.rawValue)) {
                withContext(Dispatchers.Main) {
                    userPreferences = userPreferences?.copy(temperatureUnits = units.rawValue)
                }
                Log.d(TAG, "Updated temperature units to ${units.displayName}")
            }
        }
    }

    // MARK: - Sign Out

    fun signOut() {
        viewModelScope.launch(Dispatchers.IO) {
            AuthManager.signOut()
            AppPreferencesDataStore.setPreferredRegionId(context, null)
            withContext(Dispatchers.Main) {
                userPreferences = null
                preferredRegionId = null
                hasCompletedInitialLoad = false
                clearSelection()
            }
            Log.d(TAG, "User signed out")
        }
    }
}
