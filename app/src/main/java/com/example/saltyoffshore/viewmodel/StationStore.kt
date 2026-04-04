package com.example.saltyoffshore.viewmodel

import android.content.Context
import android.util.Log
import com.example.saltyoffshore.data.Station
import com.example.saltyoffshore.data.StationListService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "StationStore"

data class StationState(
    val stations: List<Station> = emptyList(),
    val selectedStationId: String? = null,
)

class StationStore(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(StationState())
    val state: StateFlow<StationState> = _state.asStateFlow()

    private fun updateState(transform: StationState.() -> StationState) {
        _state.update { it.transform() }
    }

    private var hasLoadedStations = false

    fun loadStationsIfNeeded() {
        if (hasLoadedStations) return
        hasLoadedStations = true
        scope.launch(Dispatchers.IO) {
            try {
                val loaded = StationListService.fetchStations(context)
                updateState { copy(stations = loaded) }
                Log.d(TAG, "Loaded ${loaded.size} stations")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load stations", e)
            }
        }
    }

    fun openStationDetail(stationId: String) {
        updateState { copy(selectedStationId = stationId) }
    }

    fun dismissStationDetail() {
        updateState { copy(selectedStationId = null) }
    }
}
