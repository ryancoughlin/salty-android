package com.example.saltyoffshore.viewmodel

import android.graphics.Bitmap
import android.util.Log
import com.example.saltyoffshore.data.DatasetRenderConfig
import com.example.saltyoffshore.data.MapConfiguration
import com.example.saltyoffshore.data.RegionMetadata
import com.example.saltyoffshore.data.SavedMap
import com.example.saltyoffshore.data.SavedMapService
import com.example.saltyoffshore.data.Dataset
import com.example.saltyoffshore.data.TimeEntry
import com.example.saltyoffshore.data.sharelink.ShareLinkCameraView
import com.example.saltyoffshore.data.sharelink.ShareLinkDatasetConfig
import com.example.saltyoffshore.data.sharelink.ShareLinkPayload
import com.example.saltyoffshore.data.sharelink.ShareLinkService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "SavedMapsStore"

data class SavedMapsState(
    val savedMaps: List<SavedMap> = emptyList(),
    val isLoadingSavedMaps: Boolean = false,
    val isSavingMap: Boolean = false,
    val shareLinkUrl: String? = null,
    val shareLinkSnapshot: Bitmap? = null,
    val isCreatingShareLink: Boolean = false,
)

class SavedMapsStore(
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(SavedMapsState())
    val state: StateFlow<SavedMapsState> = _state.asStateFlow()

    private fun updateState(transform: SavedMapsState.() -> SavedMapsState) {
        _state.update { it.transform() }
    }

    fun loadSavedMaps() {
        scope.launch {
            updateState { copy(isLoadingSavedMaps = true) }
            try {
                val maps = SavedMapService.fetchAllVisibleMaps()
                updateState { copy(savedMaps = maps) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load saved maps: ${e.message}")
            } finally {
                updateState { copy(isLoadingSavedMaps = false) }
            }
        }
    }

    fun saveCurrentMap(
        name: String,
        mapConfig: MapConfiguration,
        regionName: String?,
        datasetName: String?,
        onSuccess: (SavedMap) -> Unit,
    ) {
        scope.launch {
            updateState { copy(isSavingMap = true) }
            try {
                val map = SavedMapService.createSavedMap(name, mapConfig, regionName, datasetName)
                updateState { copy(savedMaps = listOf(map) + savedMaps) }
                onSuccess(map)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save map: ${e.message}")
                throw e
            } finally {
                updateState { copy(isSavingMap = false) }
            }
        }
    }

    fun deleteSavedMap(mapId: String) {
        scope.launch {
            try {
                SavedMapService.deleteSavedMap(mapId)
                updateState { copy(savedMaps = savedMaps.filter { it.id != mapId }) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete saved map: ${e.message}")
            }
        }
    }

    fun shareMapWithCrew(mapId: String, crewId: String, sharedByName: String?) {
        scope.launch {
            try {
                val updated = SavedMapService.shareMapWithCrew(mapId, crewId, sharedByName)
                updateState { copy(savedMaps = savedMaps.map { if (it.id == mapId) updated else it }) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to share map with crew: ${e.message}")
            }
        }
    }

    fun unshareMap(mapId: String) {
        scope.launch {
            try {
                val updated = SavedMapService.unshareMap(mapId)
                updateState { copy(savedMaps = savedMaps.map { if (it.id == mapId) updated else it }) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unshare map: ${e.message}")
            }
        }
    }

    fun createShareLink(
        region: RegionMetadata,
        dataset: Dataset,
        entry: TimeEntry,
        config: DatasetRenderConfig?,
        zoom: Double,
        latitude: Double,
        longitude: Double,
        selectedDepth: Int?,
        onError: (String) -> Unit = {},
    ) {
        updateState { copy(isCreatingShareLink = true) }
        scope.launch(Dispatchers.IO) {
            try {
                val payload = ShareLinkPayload(
                    entryId = entry.id,
                    regionId = region.id,
                    view = ShareLinkCameraView.from(
                        longitude = longitude,
                        latitude = latitude,
                        zoom = zoom
                    ),
                    primaryConfig = config?.let { cfg ->
                        ShareLinkDatasetConfig(
                            datasetId = dataset.id,
                            colorscaleId = cfg.colorscale?.id,
                            customRange = cfg.customRange?.let { listOf(it.start, it.endInclusive) },
                            filterMode = cfg.filterMode.rawValue,
                            visualEnabled = cfg.visualEnabled,
                            visualOpacity = cfg.visualOpacity,
                            contourEnabled = cfg.contourEnabled,
                            contourOpacity = cfg.contourOpacity,
                            contourColor = String.format("#%06X", 0xFFFFFF and cfg.contourColor.toInt()),
                            dynamicContourColoring = cfg.dynamicContourColoring,
                            arrowsEnabled = cfg.arrowsEnabled,
                            arrowsOpacity = cfg.arrowsOpacity,
                            breaksEnabled = cfg.breaksEnabled,
                            breaksOpacity = cfg.breaksOpacity,
                            numbersEnabled = cfg.numbersEnabled,
                            numbersOpacity = cfg.numbersOpacity,
                            particlesEnabled = cfg.particlesEnabled,
                            selectedDepth = selectedDepth
                        )
                    }
                )

                val response = ShareLinkService.createShareLink(payload)
                updateState { copy(shareLinkUrl = response.url, isCreatingShareLink = false) }
                Log.d(TAG, "Share link created: ${response.url}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create share link", e)
                updateState { copy(isCreatingShareLink = false) }
                onError("Failed to create share link")
            }
        }
    }

    fun dismissShareLink() {
        updateState { copy(shareLinkUrl = null, shareLinkSnapshot = null) }
    }

    fun setMapSnapshot(bitmap: Bitmap?) {
        updateState { copy(shareLinkSnapshot = bitmap) }
    }

    fun clearMaps() {
        updateState { copy(savedMaps = emptyList(), isLoadingSavedMaps = false, isSavingMap = false) }
    }
}
