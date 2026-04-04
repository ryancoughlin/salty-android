package com.example.saltyoffshore.viewmodel

import android.util.Log
import com.example.saltyoffshore.auth.AuthManager
import com.example.saltyoffshore.auth.SupabaseClientProvider
import com.example.saltyoffshore.data.waypoint.Crew
import com.example.saltyoffshore.data.waypoint.CrewMember
import com.example.saltyoffshore.data.waypoint.CrewService
import com.example.saltyoffshore.data.waypoint.RealtimeWaypointService
import com.example.saltyoffshore.data.waypoint.SharedWaypoint
import com.example.saltyoffshore.data.waypoint.Waypoint
import com.example.saltyoffshore.data.waypoint.WaypointSharingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "CrewStore"

data class CrewState(
    val crews: List<Crew> = emptyList(),
    val crewWaypoints: List<SharedWaypoint> = emptyList(),
    val selectedCrew: Crew? = null,
    val selectedCrewMembers: List<CrewMember> = emptyList(),
    val activeCrewId: String? = null,
    val isLoadingCrews: Boolean = false,
) {
    val crewIds: Set<String> get() = crews.map { it.id }.toSet()

    val activeCrew: Crew?
        get() = activeCrewId?.let { id -> crews.firstOrNull { it.id == id } }
}

class CrewStore(
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(CrewState())
    val state: StateFlow<CrewState> = _state.asStateFlow()

    private fun updateState(transform: CrewState.() -> CrewState) {
        _state.update { it.transform() }
    }

    private var hasLoadedCrews = false

    // MARK: - Crew Operations

    fun loadCrews() {
        if (hasLoadedCrews) return
        scope.launch {
            updateState { copy(isLoadingCrews = true) }
            try {
                val loaded = CrewService.fetchUserCrews()
                updateState { copy(crews = loaded) }
                hasLoadedCrews = true
                loadCrewWaypoints()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load crews: ${e.message}")
            } finally {
                updateState { copy(isLoadingCrews = false) }
            }
        }
    }

    fun selectCrew(crew: Crew?) {
        if (crew?.id == _state.value.selectedCrew?.id) return
        updateState { copy(selectedCrew = crew, selectedCrewMembers = emptyList()) }
        if (crew != null) loadCrewDetails(crew.id)
    }

    private fun loadCrewDetails(crewId: String) {
        scope.launch {
            try {
                val members = CrewService.fetchCrewMembers(crewId)
                updateState { copy(selectedCrewMembers = members) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load crew members: ${e.message}")
            }
        }
    }

    fun createCrew(name: String, onSuccess: (Crew) -> Unit) {
        scope.launch {
            updateState { copy(isLoadingCrews = true) }
            try {
                val crew = CrewService.createCrew(name)
                updateState { copy(crews = listOf(crew) + crews) }
                onSuccess(crew)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create crew: ${e.message}")
                throw e
            } finally {
                updateState { copy(isLoadingCrews = false) }
            }
        }
    }

    fun joinCrew(code: String, onSuccess: (Crew) -> Unit, onError: (Exception) -> Unit) {
        scope.launch {
            updateState { copy(isLoadingCrews = true) }
            try {
                val crew = CrewService.joinCrewByCode(code)
                updateState { copy(crews = listOf(crew) + crews) }
                loadCrewWaypoints()
                onSuccess(crew)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to join crew: ${e.message}")
                onError(e)
            } finally {
                updateState { copy(isLoadingCrews = false) }
            }
        }
    }

    fun leaveCrew(crew: Crew, onComplete: () -> Unit = {}) {
        scope.launch {
            try {
                CrewService.leaveCrew(crew.id)
                updateState {
                    copy(
                        crews = crews.filter { it.id != crew.id },
                        activeCrewId = if (activeCrewId == crew.id) null else activeCrewId,
                        selectedCrew = if (selectedCrew?.id == crew.id) null else selectedCrew
                    )
                }
                loadCrewWaypoints()
                onComplete()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to leave crew: ${e.message}")
            }
        }
    }

    fun deleteCrew(crew: Crew, onComplete: () -> Unit = {}) {
        scope.launch {
            try {
                CrewService.deleteCrew(crew.id)
                updateState {
                    copy(
                        crews = crews.filter { it.id != crew.id },
                        activeCrewId = if (activeCrewId == crew.id) null else activeCrewId,
                        selectedCrew = if (selectedCrew?.id == crew.id) null else selectedCrew
                    )
                }
                loadCrewWaypoints()
                onComplete()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete crew: ${e.message}")
            }
        }
    }

    fun removeMember(crewId: String, memberId: String) {
        scope.launch {
            try {
                CrewService.removeMember(crewId, memberId)
                updateState { copy(selectedCrewMembers = selectedCrewMembers.filter { it.userId != memberId }) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove member: ${e.message}")
            }
        }
    }

    fun updateCrewName(crewId: String, newName: String, onSuccess: () -> Unit = {}) {
        scope.launch {
            try {
                val updated = CrewService.updateCrewName(crewId, newName)
                updateState {
                    copy(
                        crews = crews.map { if (it.id == crewId) updated else it },
                        selectedCrew = if (selectedCrew?.id == crewId) updated else selectedCrew
                    )
                }
                onSuccess()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update crew name: ${e.message}")
                throw e
            }
        }
    }

    fun setActiveCrewId(id: String?) {
        updateState { copy(activeCrewId = id) }
    }

    fun isCreator(crew: Crew): Boolean = crew.createdBy == AuthManager.currentUserId

    // MARK: - Waypoint Sharing

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

    // MARK: - Crew Waypoint Operations

    private fun loadCrewWaypoints() {
        val userId = AuthManager.currentUserId ?: return
        scope.launch(Dispatchers.IO) {
            try {
                val crewIds = _state.value.crews.map { it.id }

                if (crewIds.isEmpty()) {
                    Log.d(TAG, "No crews found, skipping crew waypoint load")
                    return@launch
                }

                val loaded = RealtimeWaypointService.loadInitialCrewWaypoints(crewIds)
                updateState { copy(crewWaypoints = loaded) }

                RealtimeWaypointService.startListening(
                    crewIds = crewIds,
                    currentUserId = userId,
                    onWaypointReceived = { sharedWaypoint ->
                        upsertCrewWaypoint(sharedWaypoint)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load crew waypoints: ${e.message}")
            }
        }
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

    // MARK: - Auth Lifecycle

    fun handleSignOut() {
        scope.launch {
            RealtimeWaypointService.stopListening()
        }
        updateState {
            copy(
                crewWaypoints = emptyList(),
                crews = emptyList(),
                selectedCrew = null,
                selectedCrewMembers = emptyList(),
                activeCrewId = null,
            )
        }
        hasLoadedCrews = false
    }
}
