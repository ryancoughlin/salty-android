package com.example.saltyoffshore.viewmodel

import android.content.Context
import android.util.Log
import com.example.saltyoffshore.auth.AuthManager
import com.example.saltyoffshore.data.LoadOperation
import com.example.saltyoffshore.data.RegionGroup
import com.example.saltyoffshore.data.RegionListItem
import com.example.saltyoffshore.data.RegionMetadata
import com.example.saltyoffshore.data.SaltyApi
import com.example.saltyoffshore.preferences.AppPreferencesDataStore
import com.example.saltyoffshore.repository.UserPreferencesRepository
import com.example.saltyoffshore.ui.components.notification.UnifiedNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "RegionStore"

data class RegionState(
    val regions: List<RegionListItem> = emptyList(),
    val regionGroups: List<RegionGroup> = emptyList(),
    val selectedRegion: RegionMetadata? = null,
    val preferredRegionId: String? = null,
    val ftuxLoadingRegionId: String? = null,
    val hasCompletedInitialLoad: Boolean = false,
)

/**
 * Region state holder — single source of truth for region catalog and active region.
 *
 * Matches iOS `RegionStore.swift`. Owned by AppViewModel, not a ViewModel itself.
 */
class RegionStore(
    private val context: Context,
    private val scope: CoroutineScope,
    private val notificationManager: UnifiedNotificationManager,
    private val preferencesRepository: UserPreferencesRepository,
) {

    private val _state = MutableStateFlow(RegionState())
    val state: StateFlow<RegionState> = _state.asStateFlow()

    private fun updateState(transform: RegionState.() -> RegionState) {
        _state.update { it.transform() }
    }

    /** Callback for when region is loaded -- AppViewModel sets this to trigger DatasetStore. */
    var onRegionLoaded: ((RegionMetadata) -> Unit)? = null

    private var regionLoadJob: Job? = null

    // MARK: - Region List

    /**
     * Load all region groups from API, then restore persisted selection.
     * Called once at startup.
     */
    fun loadRegions() {
        scope.launch(Dispatchers.IO) {
            try {
                val response = SaltyApi.getRegions()
                val regionList = response.groups.flatMap { it.regions }
                val savedPreferredId = AppPreferencesDataStore.getPreferredRegionId(context).first()

                updateState {
                    copy(
                        regions = regionList,
                        regionGroups = response.groups,
                        preferredRegionId = savedPreferredId,
                    )
                }
                Log.d(TAG, "Loaded ${regionList.size} regions")

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

    /** Find a region by ID in the loaded groups. */
    fun findRegion(id: String): RegionListItem? {
        return _state.value.regions.firstOrNull { it.id == id }
    }

    // MARK: - Region Navigation

    /**
     * Browse to a region. Fetches full metadata, persists selection, then notifies
     * AppViewModel via [onRegionLoaded] so DatasetStore can take over.
     *
     * Cancels any in-flight region load -- last caller wins.
     */
    fun onRegionSelected(regionId: String) {
        regionLoadJob?.cancel()
        notificationManager.startLoading(LoadOperation.Region)
        Log.d(TAG, "Region selected: $regionId")

        regionLoadJob = scope.launch(Dispatchers.IO) {
            // Persist selection
            AppPreferencesDataStore.setSelectedRegionId(context, regionId)

            try {
                val region = SaltyApi.fetchRegion(regionId)
                updateState { copy(selectedRegion = region) }
                Log.d(TAG, "Loaded region: ${region.name} with ${region.datasets.size} datasets")

                notificationManager.finishLoading(LoadOperation.Region)
                onRegionLoaded?.invoke(region)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load region", e)
                notificationManager.finishLoading(LoadOperation.Region)
                notificationManager.updateError("Failed to load region: ${e.message}")
            }
        }
    }

    /**
     * FTUX region selection -- saves as preferred region first, then navigates.
     */
    fun onFTUXRegionSelected(regionId: String) {
        updateState { copy(ftuxLoadingRegionId = regionId) }

        scope.launch(Dispatchers.IO) {
            // Save as preferred region (local + Supabase)
            AppPreferencesDataStore.setPreferredRegionId(context, regionId)
            syncPreferredRegionToSupabase(regionId)

            updateState { copy(preferredRegionId = regionId, ftuxLoadingRegionId = null) }
            onRegionSelected(regionId)
        }
    }

    /**
     * Update preferred region (local DataStore + Supabase).
     * Does NOT navigate -- call [onRegionSelected] separately if needed.
     */
    fun updatePreferredRegion(regionId: String) {
        scope.launch(Dispatchers.IO) {
            AppPreferencesDataStore.setPreferredRegionId(context, regionId)
            syncPreferredRegionToSupabase(regionId)
            updateState { copy(preferredRegionId = regionId) }
            Log.d(TAG, "Updated preferred region to $regionId")
        }
    }

    /**
     * Clear region state on sign-out.
     */
    fun clear() {
        regionLoadJob?.cancel()
        updateState {
            copy(
                preferredRegionId = null,
                hasCompletedInitialLoad = false,
            )
        }
    }

    // MARK: - Private

    private suspend fun syncPreferredRegionToSupabase(regionId: String) {
        val userId = AuthManager.currentUserId ?: return
        try {
            preferencesRepository.updateField(userId, "preferred_region_id", regionId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync preferred region to Supabase", e)
        }
    }
}
