package com.example.saltyoffshore.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.saltyoffshore.data.GlobalLayerType
import com.example.saltyoffshore.data.GlobalLayerVisibility
import com.example.saltyoffshore.data.LayerState
import com.example.saltyoffshore.data.LoranRegionConfig
import com.example.saltyoffshore.data.OverlayCategory
import com.example.saltyoffshore.data.Tournament
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "GlobalLayerManager"

private val Context.layerPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "layer_preferences")

/**
 * Global layer manager - handles all global/overlay layers with persistence.
 * Matches iOS GlobalLayerManager.
 */
class GlobalLayerManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    // Layer states (excluding hidden layers from UI)
    private val _layers = mutableStateListOf<LayerState>()
    val layers: List<LayerState> get() = _layers

    // Tournament state
    private var _allTournaments = mutableStateListOf<Tournament>()
    val allTournaments: List<Tournament> get() = _allTournaments

    var selectedTournament by mutableStateOf<Tournament?>(null)
        private set

    // LORAN region config
    var selectedLoranConfig by mutableStateOf<LoranRegionConfig?>(LoranRegionConfig.default)
        private set

    private val dataStore = context.layerPreferencesDataStore

    init {
        // Initialize layer states from defaults
        GlobalLayerType.uiVisibleTypes.forEach { type ->
            _layers.add(LayerState(type))
        }
        Log.d(TAG, "Initialized ${_layers.size} layer states")
        _layers.filter { it.isEnabled }.forEach {
            Log.d(TAG, "  Enabled by default: ${it.type.name}")
        }

        // Load persisted preferences
        scope.launch(Dispatchers.IO) {
            loadPersistedState()
        }
    }

    private suspend fun loadPersistedState() {
        val prefs = dataStore.data.first()

        withContext(Dispatchers.Main) {
            _layers.forEachIndexed { index, state ->
                val opacityKey = doublePreferencesKey("layer.${state.type.name}.opacity")
                val enabledKey = booleanPreferencesKey("layer.${state.type.name}.enabled")

                val opacity = prefs[opacityKey] ?: state.type.defaultOpacity
                val enabled = prefs[enabledKey] ?: state.type.defaultEnabled

                _layers[index] = state.copy(opacity = opacity, isEnabled = enabled)
            }

            // Load LORAN region
            val loranKey = stringPreferencesKey("layer.loran.regionId")
            prefs[loranKey]?.let { regionId ->
                selectedLoranConfig = LoranRegionConfig.getRegion(regionId) ?: LoranRegionConfig.default
            }

            // Load selected tournament
            val tournamentKey = stringPreferencesKey("layer.tournament.selectedId")
            prefs[tournamentKey]?.let { tournamentId ->
                selectedTournament = _allTournaments.find { it.id == tournamentId }
            }
        }
    }

    // MARK: - State Access

    fun isEnabled(type: GlobalLayerType): Boolean {
        return _layers.find { it.type == type }?.isEnabled ?: false
    }

    fun opacity(type: GlobalLayerType): Double {
        return _layers.find { it.type == type }?.opacity ?: type.defaultOpacity
    }

    fun layerState(type: GlobalLayerType): LayerState? {
        return _layers.find { it.type == type }
    }

    // MARK: - State Updates

    fun toggleEnabled(type: GlobalLayerType) {
        val index = _layers.indexOfFirst { it.type == type }
        if (index >= 0) {
            val current = _layers[index]
            setEnabled(type, !current.isEnabled)
        }
    }

    fun setEnabled(type: GlobalLayerType, enabled: Boolean) {
        val index = _layers.indexOfFirst { it.type == type }
        if (index >= 0) {
            _layers[index] = _layers[index].copy(isEnabled = enabled)

            scope.launch(Dispatchers.IO) {
                dataStore.edit { prefs ->
                    prefs[booleanPreferencesKey("layer.${type.name}.enabled")] = enabled
                }
            }
        }
    }

    fun setOpacity(type: GlobalLayerType, opacity: Double) {
        val index = _layers.indexOfFirst { it.type == type }
        if (index >= 0) {
            _layers[index] = _layers[index].copy(opacity = opacity)

            scope.launch(Dispatchers.IO) {
                dataStore.edit { prefs ->
                    prefs[doublePreferencesKey("layer.${type.name}.opacity")] = opacity
                }
            }
        }
    }

    // MARK: - Convenience

    /** Get render-ready visibility snapshot for map layers */
    val visibility: GlobalLayerVisibility
        get() {
            val enabled = _layers.filter { it.isEnabled }.map { it.type }.toMutableSet()
            // Always include shaded relief (read-only, always rendered)
            enabled.add(GlobalLayerType.SHADED_RELIEF)

            val opacities = _layers.associate { it.type to it.opacity }.toMutableMap()
            // Include hidden layers with their default opacities
            GlobalLayerType.entries.forEach { type ->
                if (!opacities.containsKey(type)) {
                    opacities[type] = type.defaultOpacity
                }
            }

            return GlobalLayerVisibility(enabled, opacities)
        }

    /** Get layers grouped by category for UI */
    val layersByCategory: List<Pair<OverlayCategory, List<LayerState>>>
        get() {
            val filteredLayers = _layers.filter { it.type != GlobalLayerType.WIND }
            val grouped = filteredLayers.groupBy { it.type.category }

            return OverlayCategory.entries.mapNotNull { category ->
                val items = grouped[category]
                if (items.isNullOrEmpty()) null else category to items
            }
        }

    // MARK: - LORAN Region

    fun setLoranRegion(config: LoranRegionConfig) {
        selectedLoranConfig = config
        scope.launch(Dispatchers.IO) {
            dataStore.edit { prefs ->
                prefs[stringPreferencesKey("layer.loran.regionId")] = config.id
            }
        }
    }

    // MARK: - Tournaments

    fun setTournaments(tournaments: List<Tournament>) {
        _allTournaments.clear()
        _allTournaments.addAll(tournaments)

        // Restore selection if previously selected
        scope.launch(Dispatchers.IO) {
            val prefs = dataStore.data.first()
            val tournamentKey = stringPreferencesKey("layer.tournament.selectedId")
            prefs[tournamentKey]?.let { tournamentId ->
                withContext(Dispatchers.Main) {
                    selectedTournament = tournaments.find { it.id == tournamentId }
                }
            }
        }
    }

    fun selectTournament(tournament: Tournament) {
        selectedTournament = tournament
        scope.launch(Dispatchers.IO) {
            dataStore.edit { prefs ->
                prefs[stringPreferencesKey("layer.tournament.selectedId")] = tournament.id
            }
        }
    }

    fun deselectTournament() {
        selectedTournament = null
        scope.launch(Dispatchers.IO) {
            dataStore.edit { prefs ->
                prefs.remove(stringPreferencesKey("layer.tournament.selectedId"))
            }
        }
    }
}
