package com.example.saltyoffshore.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.saltyoffshore.auth.AuthManager
import com.example.saltyoffshore.auth.SupabaseClientProvider
import com.example.saltyoffshore.data.AnnouncementDisplayState
import com.example.saltyoffshore.data.AnnouncementService
import com.example.saltyoffshore.data.SaltyApi
import com.example.saltyoffshore.data.measurement.MeasurementState
import com.example.saltyoffshore.data.network.NetworkMonitor
import com.example.saltyoffshore.data.waypoint.WaypointSharingService
import com.example.saltyoffshore.preferences.AppPreferencesDataStore
import com.example.saltyoffshore.repository.UserPreferencesRepository
import com.example.saltyoffshore.ui.components.notification.UnifiedNotificationManager
import com.example.saltyoffshore.zarr.ZarrManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "AppViewModel"

/**
 * Thin orchestrator — creates stores, wires them together, handles cross-cutting concerns.
 *
 * Domain state lives in individual stores (RegionStore, DatasetStore, etc.).
 * AppViewModel only owns app-level state (announcements) and auth lifecycle.
 *
 * Matches iOS pattern where SaltyOffshoreApp creates and wires its @Observable stores.
 */
class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val context get() = getApplication<Application>()
    private val preferencesRepository = UserPreferencesRepository(SupabaseClientProvider.client)

    // ── Non-store objects (created first — stores depend on some of these) ──

    val notificationManager = UnifiedNotificationManager()
    val measurementState = MeasurementState()
    val satelliteTrackingMode = SatelliteTrackingMode()
    val satelliteStore = SatelliteStore(SaltyApi.client)
    val globalLayerManager = GlobalLayerManager(context, viewModelScope)
    private val zarrManager = ZarrManager(coroutineScope = viewModelScope)

    // ── Stores (matching iOS store separation) ──

    val regionStore = RegionStore(context, viewModelScope, notificationManager, preferencesRepository)
    val datasetStore = DatasetStore(viewModelScope, zarrManager, notificationManager)
    val waypointStore = WaypointStore(context, viewModelScope)
    val crewStore = CrewStore(viewModelScope)
    val userPreferencesStore = UserPreferencesStore(context, viewModelScope)
    val savedMapsStore = SavedMapsStore(viewModelScope)
    val stationStore = StationStore(context, viewModelScope)

    // ── App-level state (small — announcements only) ──

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    private fun updateState(transform: AppState.() -> AppState) {
        _state.update { it.transform() }
    }

    // ── Init ──

    init {
        // Wire inter-store callbacks (iOS: .task(id: dataVersion) cascade)
        regionStore.onRegionLoaded = { region ->
            datasetStore.handleRegionChange(region)
        }

        satelliteTrackingMode.selectedRegionIdProvider = { regionStore.state.value.selectedRegion?.id }

        // Wire crew waypoints → waypoint store
        viewModelScope.launch {
            crewStore.state.collect { crewState ->
                waypointStore.setCrewWaypoints(crewState.crewWaypoints)
            }
        }

        // Cold start
        regionStore.loadRegions()
        userPreferencesStore.loadUserPreferences()
        checkForAnnouncements()
    }

    // ── Auth lifecycle ──

    fun handleAuthReady() {
        waypointStore.syncPendingChanges()
        crewStore.loadCrews()
        savedMapsStore.loadSavedMaps()
    }

    private fun handleSignOut() {
        crewStore.handleSignOut()
        waypointStore.clearCrewWaypoints()
        savedMapsStore.clearMaps()
    }

    fun signOut() {
        handleSignOut()
        viewModelScope.launch(Dispatchers.IO) {
            AuthManager.signOut()
            AppPreferencesDataStore.setPreferredRegionId(context, null)
            AppPreferencesDataStore.setSelectedRegionId(context, null)
            AppPreferencesDataStore.setRegionBounds(context, null)
            userPreferencesStore.clearPreferences()
            regionStore.clear()
            datasetStore.clearSelection()
            Log.d(TAG, "User signed out")
        }
    }

    fun deleteAccount() {
        val userId = AuthManager.currentUserId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                preferencesRepository.updateField(userId, "deleted", "true")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark account for deletion", e)
            }
            withContext(Dispatchers.Main) {
                signOut()
            }
            Log.d(TAG, "Account deletion requested for user $userId")
        }
    }

    // ── Announcements ──

    private fun checkForAnnouncements() {
        viewModelScope.launch {
            val displayState = AnnouncementService.checkForAnnouncements(context)
            updateState { copy(announcementDisplayState = displayState) }
        }
    }

    fun markAnnouncementAsSeen() {
        val version = _state.value.announcement?.version ?: return
        viewModelScope.launch {
            AnnouncementService.markAsSeen(context, version)
        }
        updateState { copy(announcementDisplayState = AnnouncementDisplayState.Hidden, showAnnouncementSheet = false) }
    }

    fun setShowAnnouncementSheet(show: Boolean) {
        updateState { copy(showAnnouncementSheet = show) }
    }

    // ── Network observation ──

    fun observeNetworkState() {
        viewModelScope.launch {
            var wasOnline = NetworkMonitor.isOnline.value
            NetworkMonitor.isOnline.collect { isOnline ->
                if (!wasOnline && isOnline) {
                    Log.d(TAG, "Network restored — syncing pending changes")
                    waypointStore.syncPendingChanges()
                    val userId = AuthManager.currentUserId
                    if (userId != null) {
                        viewModelScope.launch(Dispatchers.IO) {
                            WaypointSharingService.syncPendingShares(
                                SupabaseClientProvider.client, context, userId
                            )
                        }
                    }
                }
                wasOnline = isOnline
            }
        }
    }
}
