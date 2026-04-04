package com.example.saltyoffshore.viewmodel

import android.util.Log
import androidx.compose.runtime.Stable
import com.example.saltyoffshore.data.COGStatisticsResponse
import com.example.saltyoffshore.data.COGStatisticsService
import com.example.saltyoffshore.data.Colorscale
import com.example.saltyoffshore.data.CurrentValue
import com.example.saltyoffshore.data.Dataset
import com.example.saltyoffshore.data.DatasetPreset
import com.example.saltyoffshore.data.DatasetRenderConfig
import com.example.saltyoffshore.data.DatasetRenderingSnapshot
import com.example.saltyoffshore.data.DatasetType
import com.example.saltyoffshore.data.DatasetVariable
import com.example.saltyoffshore.data.DepthFilterState
import com.example.saltyoffshore.data.LoadOperation
import com.example.saltyoffshore.data.PresetConfiguration
import com.example.saltyoffshore.data.RegionMetadata
import com.example.saltyoffshore.data.RenderingConfig
import com.example.saltyoffshore.data.ResamplingMethod
import com.example.saltyoffshore.data.SaltyApi
import com.example.saltyoffshore.data.TimeEntry
import com.example.saltyoffshore.data.VisualLayerSource
import com.example.saltyoffshore.data.renderingConfig
import com.example.saltyoffshore.data.scaleMode
import com.example.saltyoffshore.data.zarrVariable
import com.example.saltyoffshore.ui.components.notification.UnifiedNotificationManager
import com.example.saltyoffshore.zarr.ColormapTextureFactory
import com.example.saltyoffshore.zarr.ZarrManager
import com.example.saltyoffshore.zarr.ZarrVisualLayer
import com.example.saltyoffshore.zarr.TimeEntry as ZarrTimeEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "DatasetStore"
private const val DEBOUNCE_MS = 600L

// MARK: - State

@Stable
data class DatasetState(
    val selectedDataset: Dataset? = null,
    val selectedEntry: TimeEntry? = null,
    val primaryConfig: DatasetRenderConfig? = null,
    val renderingSnapshot: DatasetRenderingSnapshot = DatasetRenderingSnapshot.default(),
    val depthFilterState: DepthFilterState = DepthFilterState(),
    val visualSource: VisualLayerSource = VisualLayerSource.None,
    val cogStatistics: COGStatisticsResponse? = null,
    val dynamicPresets: List<DatasetPreset> = emptyList(),
    val isLoadingPresets: Boolean = false,
    val primaryValue: CurrentValue = CurrentValue(),
    val isDatasetControlCollapsed: Boolean = false,
) {
    val isDataLayerActive: Boolean
        get() = selectedDataset != null && selectedEntry != null

    val currentDatasetType: DatasetType?
        get() = selectedDataset?.let { DatasetType.fromRawValue(it.type) }

    val allPresets: List<DatasetPreset>
        get() {
            val datasetType = selectedDataset?.let { DatasetType.fromRawValue(it.type) }
                ?: return emptyList()
            val config = PresetConfiguration.configuration(datasetType) ?: return emptyList()
            return config.staticPresets + dynamicPresets
        }
}

// MARK: - DatasetStore

/**
 * Owns the entire dataset rendering pipeline: selection, config, Zarr, depth, presets.
 * Matches iOS DatasetStore.swift.
 *
 * Store pattern: View -> DatasetStore -> Service (ZarrManager, SaltyApi).
 */
class DatasetStore(
    private val scope: CoroutineScope,
    private val zarrManager: ZarrManager,
    private val notificationManager: UnifiedNotificationManager,
) {
    private val _state = MutableStateFlow(DatasetState())
    val state: StateFlow<DatasetState> = _state.asStateFlow()

    private fun updateState(transform: DatasetState.() -> DatasetState) {
        _state.update { it.transform() }
    }

    // Repaint callback — set by map when loaded
    var repaint: (() -> Unit)? = null
        set(value) {
            field = value
            zarrManager.repaint = value
        }

    // Map snapshot capture callback
    var captureMapSnapshot: (() -> Unit)? = null

    // Non-reactive camera state (updated 60fps, read only for share links)
    var currentZoom: Double = 4.0
        private set
    var currentLatitude: Double = 30.0
        private set
    var currentLongitude: Double = -60.0
        private set

    // Datasets for the current region — set when region changes
    var availableDatasets: List<Dataset> = emptyList()
        private set

    // Current region ID
    var regionId: String? = null
        private set

    // Jobs
    private var datasetLoadJob: Job? = null
    private var cogStatsJob: Job? = null

    // MARK: - Region Change

    /**
     * Handle region data arriving — picks default dataset, fetches entries, starts rendering.
     * Matches iOS DatasetStore.handleRegionChange(_:).
     */
    fun handleRegionChange(region: RegionMetadata) {
        regionId = region.id
        availableDatasets = region.datasets

        // Check unavailable states
        when (region.status) {
            "coming_soon" -> {
                updateState {
                    copy(
                        selectedDataset = null,
                        selectedEntry = null,
                        depthFilterState = DepthFilterState()
                    )
                }
                return
            }
            "maintenance", "unknown" -> {
                updateState { copy(selectedDataset = null) }
                return
            }
        }

        if (region.activeDatasets.isEmpty()) {
            updateState { copy(selectedDataset = null) }
            return
        }

        // Same dataset exists in new region — use it with fresh config
        val current = _state.value.selectedDataset
        if (current != null) {
            val refreshed = region.datasets.firstOrNull { it.id == current.id }
            if (refreshed != null) {
                selectDataset(refreshed)
                return
            }
        }

        // Pick default: prefer GOES19, fallback to SST, then first
        val defaultId = "G19-ABI-L3C-ACSPO-v3.00"
        val dataset = region.activeDatasets.firstOrNull { it.id == defaultId }
            ?: region.activeDatasets.firstOrNull { it.type == "sst" }
            ?: region.activeDatasets.first()
        selectDataset(dataset)
    }

    // MARK: - Dataset Selection

    /**
     * Select a dataset — configures rendering, fetches entries, kicks off Zarr.
     * Matches iOS DatasetStore.selectDataset(_:regionId:).
     */
    fun selectDataset(dataset: Dataset) {
        datasetLoadJob?.cancel()
        cogStatsJob?.cancel()
        zarrManager.removeAll()

        notificationManager.startLoading(LoadOperation.Dataset)

        configureForDataset(dataset)

        // If entries already populated, proceed directly
        val entries = dataset.entries
        if (!entries.isNullOrEmpty()) {
            val mostRecent = dataset.mostRecentEntry
            val snapshot = if (mostRecent != null) {
                computeSnapshot(mostRecent, dataset)
            } else {
                _state.value.renderingSnapshot
            }

            updateState {
                copy(selectedEntry = mostRecent, renderingSnapshot = snapshot)
            }

            updateVisualSource()
            loadZarrForDataset(dataset)
            notificationManager.finishLoading(LoadOperation.Dataset)
            loadCOGStatisticsDebounced()
            return
        }

        // Fetch entries lazily
        val rid = regionId ?: return
        loadEntriesForDataset(dataset, rid)
    }

    // MARK: - Entry Selection

    /**
     * Show entry — updates snapshot, shows Zarr frame, refreshes COG stats.
     * Matches iOS DatasetStore.showEntry(_:).
     */
    fun selectEntry(entry: TimeEntry) {
        val dataset = _state.value.selectedDataset
        val snapshot = if (dataset != null) {
            computeSnapshot(entry, dataset)
        } else {
            _state.value.renderingSnapshot
        }
        updateState { copy(selectedEntry = entry, renderingSnapshot = snapshot) }
        Log.d(TAG, "Entry selected: ${entry.timestamp}")

        zarrManager.showFrame(entry.id)
        loadCOGStatisticsDebounced()
    }

    // MARK: - Layer Toggles

    fun toggleVisualLayer() {
        val config = _state.value.primaryConfig ?: return
        updatePrimaryConfig(config.copy(visualEnabled = !config.visualEnabled))
    }

    fun toggleContourLayer() {
        val config = _state.value.primaryConfig ?: return
        updatePrimaryConfig(config.copy(contourEnabled = !config.contourEnabled))
    }

    fun toggleArrowsLayer() {
        val config = _state.value.primaryConfig ?: return
        updatePrimaryConfig(config.copy(arrowsEnabled = !config.arrowsEnabled))
    }

    fun toggleBreaksLayer() {
        val config = _state.value.primaryConfig ?: return
        updatePrimaryConfig(config.copy(breaksEnabled = !config.breaksEnabled))
    }

    fun toggleNumbersLayer() {
        val config = _state.value.primaryConfig ?: return
        updatePrimaryConfig(config.copy(numbersEnabled = !config.numbersEnabled))
    }

    // MARK: - Opacity Controls

    fun updateVisualOpacity(opacity: Double) {
        updateState { copy(renderingSnapshot = renderingSnapshot.copy(visualOpacity = opacity)) }
        zarrManager.setUniforms(opacity = opacity.toFloat())
        repaint?.invoke()
    }

    fun updateContourOpacity(opacity: Double) {
        val config = _state.value.primaryConfig ?: return
        updatePrimaryConfig(config.copy(contourOpacity = opacity))
    }

    fun updateArrowsOpacity(opacity: Double) {
        val config = _state.value.primaryConfig ?: return
        updatePrimaryConfig(config.copy(arrowsOpacity = opacity))
    }

    fun updateBreaksOpacity(opacity: Double) {
        val config = _state.value.primaryConfig ?: return
        updatePrimaryConfig(config.copy(breaksOpacity = opacity))
    }

    fun updateNumbersOpacity(opacity: Double) {
        val config = _state.value.primaryConfig ?: return
        updatePrimaryConfig(config.copy(numbersOpacity = opacity))
    }

    // MARK: - Config Update

    /**
     * Explicit entry point for changing primaryConfig.
     * Syncs shader + rebuilds snapshot so the map reflects the new settings.
     * Matches iOS DatasetStore.updatePrimaryConfig(_:animated:).
     */
    fun updatePrimaryConfig(config: DatasetRenderConfig) {
        val s = _state.value
        val dataRange = s.renderingSnapshot.dataMin..s.renderingSnapshot.dataMax
        updateState { copy(primaryConfig = config, renderingSnapshot = config.snapshot(dataRange)) }
        syncConfigToShader(config)
    }

    /**
     * Update data range from entry (used when rendering snapshot needs external update).
     */
    fun updateDataRange(min: Double, max: Double) {
        updateState { copy(renderingSnapshot = renderingSnapshot.copy(dataMin = min, dataMax = max)) }
    }

    /**
     * Direct filter range update — sets shader uniforms without full config rebuild.
     * Used for real-time slider dragging.
     */
    fun setFilterRangeDirect(min: Float, max: Float) {
        val config = _state.value.primaryConfig ?: return
        val rc = resolveRenderingConfig(config)
        val filterActive = min < max
        zarrManager.setUniforms(
            opacity = config.visualOpacity.toFloat(),
            filterMin = if (filterActive) min else 0f,
            filterMax = if (filterActive) max else 0f,
            filterMode = config.filterMode.ordinal,
            scaleMode = rc.scaleMode.rawValue,
            blendFactor = 1.0f
        )
        repaint?.invoke()
    }

    // MARK: - Depth Selection

    /**
     * Handle depth selection for primary dataset.
     * Matches iOS DatasetStore.selectDepth(_:entry:).
     */
    fun onDepthSelected(depth: Int) {
        Log.d(TAG, "Depth selected: ${depth}m")

        val dataset = _state.value.selectedDataset ?: return
        if (depth == _state.value.depthFilterState.selectedDepth) return

        val entriesAtDepth = (dataset.entries ?: emptyList()).filter { it.depth == depth }
        val bestEntry = entriesAtDepth.maxByOrNull { it.timestamp }

        val snapshot = if (bestEntry != null) {
            computeSnapshot(bestEntry, dataset)
        } else {
            _state.value.renderingSnapshot
        }

        updateState {
            copy(
                depthFilterState = depthFilterState.copy(selectedDepth = depth),
                selectedEntry = bestEntry,
                renderingSnapshot = snapshot
            )
        }

        zarrManager.updateDepth(depth)
    }

    // MARK: - Variable Selection

    /**
     * Select variable for the primary dataset — clears filter, reloads Zarr.
     * Matches iOS DatasetStore.selectVariable(_:entry:).
     */
    fun selectVariable(variable: DatasetVariable) {
        val config = _state.value.primaryConfig ?: return
        val dataset = _state.value.selectedDataset ?: return
        if (variable.id == config.selectedVariable(dataset).id) return

        datasetLoadJob?.cancel()

        val newConfig = config.clearFilter().copy(selectedVariableId = variable.id)
        updatePrimaryConfig(newConfig)

        // Clear GPU frames — same Zarr URL but different variable
        zarrManager.clearForDatasetSwitch()

        loadZarrForDataset(dataset)
    }

    // MARK: - Preset Application

    /**
     * Apply preset filter to current dataset.
     * Matches iOS DatasetStore.applyPreset(_:currentValue:entry:).
     */
    fun applyPreset(preset: DatasetPreset) {
        val s = _state.value
        val config = s.primaryConfig ?: return

        // Toggle off if same preset
        if (config.selectedPreset?.id == preset.id) {
            updatePrimaryConfig(config.clearFilter())
            return
        }

        val currentValue = s.primaryValue.value
        val datasetType = s.selectedDataset?.let { DatasetType.fromRawValue(it.type) } ?: return
        val entry = s.selectedEntry
        val rangeKey = datasetType.rangeKey
        val rangeData = entry?.ranges?.get(rangeKey)
        val valueRange = if (rangeData?.min != null && rangeData.max != null) {
            rangeData.min..rangeData.max
        } else {
            0.0..1.0
        }

        val range = preset.calculateRange(currentValue, valueRange) ?: return

        updatePrimaryConfig(config.copy(customRange = range, selectedPreset = preset))
    }

    // MARK: - Crosshair / Camera

    fun updatePrimaryValue(value: CurrentValue) {
        updateState { copy(primaryValue = value) }
    }

    fun updateCameraState(zoom: Double, latitude: Double, longitude: Double) {
        currentZoom = zoom
        currentLatitude = latitude
        currentLongitude = longitude
    }

    // MARK: - Dataset Control UI

    fun toggleDatasetControl() {
        updateState { copy(isDatasetControlCollapsed = !isDatasetControlCollapsed) }
    }

    fun setDatasetControlCollapsed(collapsed: Boolean) {
        updateState { copy(isDatasetControlCollapsed = collapsed) }
    }

    // MARK: - Clear Selection

    /**
     * Clear dataset state — used during region navigation transitions.
     * Matches iOS DatasetStore.clearSelection().
     */
    fun clearSelection() {
        datasetLoadJob?.cancel()
        cogStatsJob?.cancel()
        updateState {
            copy(
                selectedDataset = null,
                selectedEntry = null,
                renderingSnapshot = DatasetRenderingSnapshot.default(),
                depthFilterState = DepthFilterState(),
                visualSource = VisualLayerSource.None,
                cogStatistics = null,
                dynamicPresets = emptyList(),
                isLoadingPresets = false
            )
        }
        zarrManager.removeAll()
    }

    // MARK: - Private: Entry Loading

    private fun loadEntriesForDataset(dataset: Dataset, regionId: String) {
        datasetLoadJob = scope.launch(Dispatchers.IO) {
            try {
                val datasetWithEntries = SaltyApi.fetchDatasetEntries(regionId, dataset.id)
                Log.d(TAG, "Loaded ${datasetWithEntries.entries?.size ?: 0} entries for ${dataset.name}")

                // Only update if this is still the selected dataset
                if (_state.value.selectedDataset?.id != dataset.id) return@launch

                val mostRecent = datasetWithEntries.mostRecentEntry
                val snapshot = if (mostRecent != null) {
                    computeSnapshot(mostRecent, datasetWithEntries)
                } else {
                    _state.value.renderingSnapshot
                }

                updateState {
                    copy(
                        selectedDataset = datasetWithEntries,
                        selectedEntry = mostRecent,
                        renderingSnapshot = snapshot
                    )
                }

                if (mostRecent != null) {
                    Log.d(TAG, "Selected entry: ${mostRecent.timestamp}")
                }

                updateVisualSource()
                loadZarrForDataset(datasetWithEntries)
                notificationManager.finishLoading(LoadOperation.Dataset)
                loadCOGStatisticsDebounced()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load entries for ${dataset.name}", e)
                notificationManager.finishLoading(LoadOperation.Dataset)
                notificationManager.updateError("Failed to load data: ${e.message}")
            }
        }
    }

    // MARK: - Private: Zarr Loading

    private fun loadZarrForDataset(dataset: Dataset) {
        val zarrUrl = dataset.zarrUrl
        if (zarrUrl == null) {
            Log.d(TAG, "Dataset ${dataset.name} has no zarrUrl, using COG fallback")
            updateState { copy(visualSource = VisualLayerSource.None) }
            return
        }

        // Get or create shader host
        val shaderHost = zarrManager.shaderHost
        if (shaderHost == null) {
            val newHost = ZarrVisualLayer()
            zarrManager.setShaderHost(newHost)
            updateState { copy(visualSource = VisualLayerSource.Zarr(newHost)) }
        } else {
            updateState { copy(visualSource = VisualLayerSource.Zarr(shaderHost)) }
        }

        val datasetType = DatasetType.fromRawValue(dataset.type)
        val config = _state.value.primaryConfig
        val rc = if (config != null) resolveRenderingConfig(config)
        else datasetType?.renderingConfig ?: DatasetType.SST.renderingConfig

        val entries = (dataset.entries ?: emptyList()).map { entry ->
            ZarrTimeEntry(
                id = entry.id,
                timestamp = parseTimestamp(entry.timestamp),
                depth = entry.depth
            )
        }

        val variableName = datasetType?.zarrVariable ?: "sea_surface_temperature"

        val rangeKey = datasetType?.rangeKey ?: "value"
        val selectedEntry = _state.value.selectedEntry
        val rangeData = selectedEntry?.ranges?.get(rangeKey)
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
            initialEntryId = selectedEntry?.id,
            colorscale = rc.colorscale,
            scaleMode = rc.scaleMode
        )

        // Apply full config after load starts
        if (config != null) syncConfigToShader(config)

        Log.d(TAG, "Started Zarr load for ${dataset.name} with variable: $variableName")
    }

    // MARK: - Private: Config Sync

    private fun syncConfigToShader(config: DatasetRenderConfig) {
        val rc = resolveRenderingConfig(config)

        val colorscale = config.colorscale ?: rc.colorscale
        val distribution = if (config.colorscale == null) rc.colormapDistribution
        else ColormapTextureFactory.StopDistribution.Uniform
        zarrManager.setColorscale(colorscale, distribution)

        val filterMin: Float
        val filterMax: Float
        if (config.customRange != null) {
            filterMin = config.customRange.start.toFloat()
            filterMax = config.customRange.endInclusive.toFloat()
        } else {
            filterMin = 0f
            filterMax = 0f
        }

        zarrManager.setUniforms(
            opacity = config.visualOpacity.toFloat(),
            filterMin = filterMin,
            filterMax = filterMax,
            filterMode = config.filterMode.ordinal,
            scaleMode = rc.scaleMode.rawValue,
            blendFactor = 1.0f
        )

        repaint?.invoke()
    }

    private fun resolveRenderingConfig(config: DatasetRenderConfig): RenderingConfig {
        val dataset = _state.value.selectedDataset ?: return DatasetType.SST.renderingConfig
        val datasetType = DatasetType.fromRawValue(dataset.type) ?: return DatasetType.SST.renderingConfig
        val variable = config.selectedVariable(dataset)
        return datasetType.renderingConfig(variable)
    }

    // MARK: - Private: Visual Source

    private fun updateVisualSource() {
        val dataset = _state.value.selectedDataset
        if (dataset?.zarrUrl != null) {
            val host = zarrManager.shaderHost
            if (host != null) {
                updateState { copy(visualSource = VisualLayerSource.Zarr(host)) }
            }
        } else {
            updateState { copy(visualSource = VisualLayerSource.None) }
        }
    }

    // MARK: - Private: Snapshot Computation

    /**
     * Compute rendering snapshot for a given entry + dataset.
     * Pure function — no side effects.
     */
    private fun computeSnapshot(entry: TimeEntry, dataset: Dataset): DatasetRenderingSnapshot {
        val datasetType = DatasetType.fromRawValue(dataset.type)
        val rangeKey = datasetType?.rangeKey ?: "value"
        val rangeData = entry.ranges?.get(rangeKey)

        return if (rangeData?.min != null && rangeData.max != null) {
            Log.d(TAG, "Updated data range: ${rangeData.min} - ${rangeData.max}")
            _state.value.renderingSnapshot.copy(dataMin = rangeData.min, dataMax = rangeData.max)
        } else {
            _state.value.renderingSnapshot
        }
    }

    // MARK: - Private: Dataset Configuration

    private fun configureForDataset(dataset: Dataset) {
        val datasetType = DatasetType.fromRawValue(dataset.type) ?: DatasetType.SST
        val config = DatasetRenderConfig.primaryDefaults(datasetType, dataset.id)
        val depths = dataset.availableDepths ?: listOf(0)

        updateState {
            copy(
                selectedDataset = dataset,
                primaryConfig = config,
                depthFilterState = DepthFilterState(selectedDepth = depths.firstOrNull() ?: 0, availableDepths = depths)
            )
        }

        Log.d(TAG, "Configured for dataset: ${dataset.name}, ${depths.size} depths")
    }

    // MARK: - Private: COG Statistics (Dynamic Presets)

    private fun loadCOGStatisticsDebounced() {
        cogStatsJob?.cancel()

        val s = _state.value
        val cogUrl = s.selectedEntry?.layers?.cog ?: run {
            updateState { copy(cogStatistics = null, dynamicPresets = emptyList(), isLoadingPresets = false) }
            return
        }

        val datasetType = s.selectedDataset?.let { DatasetType.fromRawValue(it.type) } ?: return
        val presetConfig = PresetConfiguration.configuration(datasetType)

        if (presetConfig?.supportsDynamicPresets != true) {
            updateState { copy(cogStatistics = null, dynamicPresets = emptyList(), isLoadingPresets = false) }
            return
        }

        updateState { copy(isLoadingPresets = true) }

        cogStatsJob = scope.launch {
            delay(DEBOUNCE_MS)

            try {
                val response = withContext(Dispatchers.IO) {
                    COGStatisticsService.fetchStatistics(cogUrl)
                }
                ensureActive()

                val stats = response.primaryBandStatistics()
                val builtPresets = if (stats != null) {
                    presetConfig.dynamicBuilder?.invoke(stats) ?: emptyList()
                } else {
                    emptyList()
                }

                updateState { copy(cogStatistics = response, dynamicPresets = builtPresets, isLoadingPresets = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "COG stats fetch failed: ${e.message}")
                updateState { copy(cogStatistics = null, dynamicPresets = emptyList(), isLoadingPresets = false) }
            }
        }
    }

    // MARK: - Private: Timestamp Parsing

    private fun parseTimestamp(timestamp: String): Long {
        return try {
            java.time.Instant.parse(timestamp).epochSecond
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse timestamp: $timestamp", e)
            0L
        }
    }
}
