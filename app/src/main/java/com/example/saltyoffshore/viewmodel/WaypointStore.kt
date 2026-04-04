package com.example.saltyoffshore.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.saltyoffshore.auth.AuthManager
import com.example.saltyoffshore.auth.SupabaseClientProvider
import com.example.saltyoffshore.data.waypoint.GPXImportOptions
import com.example.saltyoffshore.data.waypoint.GPXImportService
import com.example.saltyoffshore.data.waypoint.LoadingState
import com.example.saltyoffshore.data.waypoint.PendingWaypointQueue
import com.example.saltyoffshore.data.waypoint.SharedWaypoint
import com.example.saltyoffshore.data.waypoint.Waypoint
import com.example.saltyoffshore.data.waypoint.WaypointCategory
import com.example.saltyoffshore.data.waypoint.WaypointFormState
import com.example.saltyoffshore.data.waypoint.WaypointSection
import com.example.saltyoffshore.data.waypoint.WaypointSelectionSource
import com.example.saltyoffshore.data.waypoint.WaypointSharingService
import com.example.saltyoffshore.data.waypoint.WaypointSheet
import com.example.saltyoffshore.data.waypoint.WaypointSortOption
import com.example.saltyoffshore.data.waypoint.WaypointStorage
import com.example.saltyoffshore.data.waypoint.WaypointSymbol
import com.example.saltyoffshore.data.waypoint.WaypointSyncService
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "WaypointStore"

data class WaypointState(
    val waypoints: List<Waypoint> = emptyList(),
    val crewWaypoints: List<SharedWaypoint> = emptyList(),
    val waypointLoadingState: LoadingState = LoadingState.IDLE,
    val selectedWaypointId: String? = null,
    val activeWaypointSheet: WaypointSheet? = null,
    val waypointFormState: WaypointFormState = WaypointFormState(),
    val waypointSortOption: WaypointSortOption = WaypointSortOption.DATE_CREATED,
    val importResult: String? = null,
) {
    val ownedWaypointIds: Set<String>
        get() = waypoints.map { it.id }.toSet()

    val allWaypoints: List<Waypoint>
        get() {
            val ownedIds = waypoints.map { it.id }.toSet()
            val crewOnly = crewWaypoints
                .filter { it.waypoint.id !in ownedIds }
                .map { it.waypoint }
            return waypoints + crewOnly
        }

    val groupedWaypoints: List<WaypointSection>
        get() {
            val sorted = sortWaypoints(allWaypoints)
            return when (waypointSortOption) {
                WaypointSortOption.DATE_CREATED -> groupByCreationDate(sorted)
                WaypointSortOption.SYMBOL -> groupBySymbol(sorted)
            }
        }

    private fun sortWaypoints(waypoints: List<Waypoint>): List<Waypoint> {
        return when (waypointSortOption) {
            WaypointSortOption.DATE_CREATED ->
                waypoints.sortedByDescending { it.createdAt }
            WaypointSortOption.SYMBOL ->
                waypoints.sortedWith(
                    compareBy<Waypoint> { it.symbol.rawValue }.thenByDescending { it.createdAt }
                )
        }
    }

    private fun groupByCreationDate(waypoints: List<Waypoint>): List<WaypointSection> {
        val formatter = DateTimeFormatter.ofPattern("MMMM yyyy")
        val grouped = waypoints.groupBy { wp ->
            try {
                val instant = Instant.parse(wp.createdAt)
                formatter.format(instant.atZone(ZoneOffset.UTC))
            } catch (_: Exception) {
                "Unknown"
            }
        }
        return grouped.map { (monthYear, wps) ->
            WaypointSection(id = monthYear, title = monthYear, waypoints = wps)
        }
    }

    private fun groupBySymbol(waypoints: List<Waypoint>): List<WaypointSection> {
        val grouped = waypoints.groupBy { it.symbol }
        val categoryOrder = listOf(
            WaypointCategory.GARMIN, WaypointCategory.FISH, WaypointCategory.STRUCTURE,
            WaypointCategory.NAVIGATION, WaypointCategory.ENVIRONMENT, WaypointCategory.OTHER
        )
        val sortedSymbols = WaypointSymbol.sortedByCategory(grouped.keys.toList(), categoryOrder)

        return sortedSymbols.map { symbol ->
            val wps = grouped[symbol] ?: emptyList()
            val title = "${symbol.rawValue} (${wps.size})"
            WaypointSection(id = symbol.rawValue, title = title, waypoints = wps)
        }
    }
}

class WaypointStore(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(WaypointState())
    val state: StateFlow<WaypointState> = _state.asStateFlow()

    private fun updateState(transform: WaypointState.() -> WaypointState) {
        _state.update { it.transform() }
    }

    private var hasLoadedFromDisk = false

    /** Callback for crew waypoint mutations — CrewStore can listen to this. */
    var onCrewWaypointChanged: ((SharedWaypoint) -> Unit)? = null

    // MARK: - Loading

    fun loadWaypoints() {
        if (hasLoadedFromDisk) return
        hasLoadedFromDisk = true
        updateState { copy(waypointLoadingState = LoadingState.LOADING) }

        scope.launch(Dispatchers.IO) {
            val loaded = WaypointStorage.load(context)
            updateState { copy(waypoints = loaded, waypointLoadingState = LoadingState.LOADED) }
            Log.d(TAG, "Loaded ${loaded.size} waypoints from disk")

            syncWithRemote()
        }
    }

    // MARK: - CRUD

    fun createWaypoint(
        latitude: Double,
        longitude: Double,
        name: String? = null,
        symbol: WaypointSymbol = WaypointSymbol.DOT
    ): Waypoint {
        val waypoint = Waypoint(
            id = UUID.randomUUID().toString(),
            name = name ?: generateDefaultName(),
            symbol = symbol,
            latitude = latitude,
            longitude = longitude,
            createdAt = Instant.now().toString()
        )

        updateState { copy(waypoints = waypoints + waypoint) }
        persistWaypoints()
        syncUpsert(waypoint)

        Log.d(TAG, "Created waypoint: ${waypoint.name}")
        return waypoint
    }

    fun saveWaypoint(waypoint: Waypoint) {
        val s = _state.value
        val shared = s.crewWaypoints.find { it.waypoint.id == waypoint.id }
        if (shared != null) {
            val updated = shared.copy(waypoint = waypoint)
            updateState {
                copy(crewWaypoints = crewWaypoints.map {
                    if (it.id == shared.id) updated else it
                })
            }
            onCrewWaypointChanged?.invoke(updated)
            scope.launch(Dispatchers.IO) {
                try {
                    WaypointSharingService.updateSharedWaypoint(
                        SupabaseClientProvider.client, shared.id, waypoint
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update shared waypoint: ${e.message}")
                }
            }
            return
        }

        val index = s.waypoints.indexOfFirst { it.id == waypoint.id }
        if (index == -1) {
            Log.w(TAG, "Waypoint not found for save: ${waypoint.id}")
            return
        }
        updateState {
            copy(waypoints = waypoints.toMutableList().also { it[index] = waypoint })
        }
        persistWaypoints()
        syncUpsert(waypoint)
        Log.d(TAG, "Saved waypoint: ${waypoint.name}")
    }

    fun deleteWaypoint(waypoint: Waypoint) {
        val s = _state.value
        val shared = s.crewWaypoints.find { it.waypoint.id == waypoint.id }
        if (shared != null) {
            updateState { copy(crewWaypoints = crewWaypoints.filter { it.id != shared.id }) }
            scope.launch(Dispatchers.IO) {
                try {
                    WaypointSharingService.deleteSharedWaypoint(
                        SupabaseClientProvider.client, shared.id
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to delete shared waypoint: ${e.message}")
                }
            }
            return
        }

        updateState { copy(waypoints = waypoints.filter { it.id != waypoint.id }) }
        persistWaypoints()
        syncDelete(waypoint.id)
        Log.d(TAG, "Deleted waypoint: ${waypoint.name}")
    }

    // MARK: - Selection

    fun selectWaypoint(id: String, source: WaypointSelectionSource? = null) {
        // Force re-trigger by clearing if re-selecting same waypoint
        if (_state.value.selectedWaypointId == id) {
            updateState { copy(selectedWaypointId = null) }
        }
        updateState { copy(selectedWaypointId = id) }
    }

    fun deselectWaypoint() {
        updateState { copy(selectedWaypointId = null) }
    }

    // MARK: - Sheet Management

    fun openWaypointDetails(id: String) {
        updateState { copy(activeWaypointSheet = WaypointSheet.Details(id)) }
    }

    fun openWaypointForm(waypoint: Waypoint) {
        updateState {
            copy(
                waypointFormState = WaypointFormState().setFromWaypoint(
                    waypoint,
                    com.example.saltyoffshore.data.coordinate.GPSFormat.DMM
                ),
                activeWaypointSheet = WaypointSheet.Form(waypoint)
            )
        }
    }

    fun dismissWaypointSheet() {
        updateState { copy(activeWaypointSheet = null) }
    }

    // MARK: - Form & Sort

    fun updateWaypointFormState(formState: WaypointFormState) {
        updateState { copy(waypointFormState = formState) }
    }

    fun updateWaypointSortOption(option: WaypointSortOption) {
        updateState { copy(waypointSortOption = option) }
    }

    // MARK: - GPX Import

    fun importGPX(uri: Uri, context: Context) {
        scope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw Exception("Cannot read file")
                val currentWaypoints = _state.value.waypoints
                val result = GPXImportService.parseAndDeduplicate(
                    inputStream = inputStream,
                    options = GPXImportOptions(),
                    existingWaypoints = currentWaypoints
                )
                inputStream.close()

                val updatedList = currentWaypoints
                    .filter { existing -> result.waypointsToRemove.none { it.id == existing.id } }
                    .plus(result.waypointsToAdd)

                updateState { copy(waypoints = updatedList) }
                WaypointStorage.save(context, updatedList)
                syncUpsertBatch(result.waypointsToAdd)

                val count = result.waypointsToAdd.size
                val message = if (count > 0) {
                    "Imported $count waypoint${if (count != 1) "s" else ""}"
                } else {
                    "All waypoints already exist"
                }
                updateState { copy(importResult = message) }
            } catch (e: Exception) {
                updateState { copy(importResult = "Import failed: ${e.message}") }
            }
        }
    }

    fun clearImportResult() {
        updateState { copy(importResult = null) }
    }

    // MARK: - Sharing

    fun shareWaypointToCrews(waypoint: Waypoint, crewIds: List<String>) {
        val userId = AuthManager.currentUserId ?: return
        scope.launch(Dispatchers.IO) {
            crewIds.forEach { crewId ->
                try {
                    WaypointSharingService.shareWaypoint(
                        SupabaseClientProvider.client, waypoint, crewId, userId
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to share waypoint to crew $crewId: ${e.message}")
                }
            }
        }
    }

    // MARK: - Crew Waypoint State

    fun setCrewWaypoints(crewWaypoints: List<SharedWaypoint>) {
        updateState { copy(crewWaypoints = crewWaypoints) }
    }

    fun upsertCrewWaypoint(sharedWaypoint: SharedWaypoint) {
        updateState {
            val existing = crewWaypoints.indexOfFirst { it.waypoint.id == sharedWaypoint.waypoint.id }
            if (existing >= 0) {
                copy(crewWaypoints = crewWaypoints.toMutableList().also { it[existing] = sharedWaypoint })
            } else {
                copy(crewWaypoints = crewWaypoints + sharedWaypoint)
            }
        }
    }

    fun clearCrewWaypoints() {
        updateState { copy(crewWaypoints = emptyList()) }
    }

    // MARK: - Sync

    fun syncPendingChanges() {
        scope.launch(Dispatchers.IO) {
            syncWithRemote()
        }
    }

    private suspend fun syncWithRemote() {
        try {
            drainPendingQueue()

            val remoteWaypoints = WaypointSyncService.fetchWaypoints()

            val currentWaypoints = _state.value.waypoints
            val remoteIds = remoteWaypoints.map { it.id }.toSet()
            val localOnly = currentWaypoints.filter { it.id !in remoteIds }
            val merged = remoteWaypoints + localOnly

            updateState { copy(waypoints = merged) }
            WaypointStorage.save(context, merged)

            if (localOnly.isNotEmpty()) {
                WaypointSyncService.upsertWaypoints(localOnly)
                Log.d(TAG, "Pushed ${localOnly.size} local-only waypoints to Supabase")
            }

            Log.d(TAG, "Sync complete: ${merged.size} waypoints (${remoteWaypoints.size} remote, ${localOnly.size} local-only)")
        } catch (e: Exception) {
            Log.e(TAG, "Remote waypoint sync failed (using local data): ${e.message}")
        }
    }

    private suspend fun drainPendingQueue() {
        val upsertIds = PendingWaypointQueue.pendingUpsertIds(context)
        val deleteIds = PendingWaypointQueue.pendingDeleteIds(context)

        if (upsertIds.isEmpty() && deleteIds.isEmpty()) return
        Log.d(TAG, "Draining pending queue: ${upsertIds.size} upserts, ${deleteIds.size} deletes")

        for (id in upsertIds) {
            val wp = _state.value.waypoints.find { it.id == id }
            if (wp != null) {
                try {
                    WaypointSyncService.upsertWaypoint(wp)
                    PendingWaypointQueue.completedUpsert(context, id)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to drain upsert $id: ${e.message}")
                }
            } else {
                PendingWaypointQueue.completedUpsert(context, id)
            }
        }

        for (id in deleteIds) {
            try {
                WaypointSyncService.deleteWaypoint(id)
                PendingWaypointQueue.completedDelete(context, id)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to drain delete $id: ${e.message}")
            }
        }
    }

    private fun syncUpsert(waypoint: Waypoint) {
        scope.launch(Dispatchers.IO) {
            PendingWaypointQueue.markForUpsert(context, waypoint.id)
            try {
                WaypointSyncService.upsertWaypoint(waypoint)
                PendingWaypointQueue.completedUpsert(context, waypoint.id)
            } catch (e: Exception) {
                Log.e(TAG, "Waypoint ${waypoint.id} queued for later sync: ${e.message}")
            }
        }
    }

    private fun syncDelete(waypointId: String) {
        scope.launch(Dispatchers.IO) {
            PendingWaypointQueue.markForDelete(context, waypointId)
            try {
                WaypointSyncService.deleteWaypoint(waypointId)
                PendingWaypointQueue.completedDelete(context, waypointId)
            } catch (e: Exception) {
                Log.e(TAG, "Delete for $waypointId queued for later sync: ${e.message}")
            }
        }
    }

    private fun syncUpsertBatch(waypointsToSync: List<Waypoint>) {
        if (waypointsToSync.isEmpty()) return
        scope.launch(Dispatchers.IO) {
            for (wp in waypointsToSync) {
                PendingWaypointQueue.markForUpsert(context, wp.id)
            }
            try {
                WaypointSyncService.upsertWaypoints(waypointsToSync)
                for (wp in waypointsToSync) {
                    PendingWaypointQueue.completedUpsert(context, wp.id)
                }
            } catch (e: Exception) {
                Log.e(TAG, "${waypointsToSync.size} waypoints queued for later sync: ${e.message}")
            }
        }
    }

    private fun persistWaypoints() {
        val snapshot = _state.value.waypoints
        scope.launch(Dispatchers.IO) {
            WaypointStorage.save(context, snapshot)
        }
    }

    // MARK: - Naming

    private fun generateDefaultName(): String {
        val existingNumbers = _state.value.waypoints.mapNotNull { wp ->
            wp.name?.let { name ->
                if (name.startsWith("WPT")) name.removePrefix("WPT").toIntOrNull() else null
            }
        }
        val next = (existingNumbers.maxOrNull() ?: 0) + 1
        return "WPT%03d".format(next)
    }
}
