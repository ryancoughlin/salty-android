package com.example.saltyoffshore.viewmodel

import android.content.Context
import android.util.Log
import com.example.saltyoffshore.auth.AuthManager
import com.example.saltyoffshore.auth.SupabaseClientProvider
import com.example.saltyoffshore.data.DepthUnits
import com.example.saltyoffshore.data.DistanceUnits
import com.example.saltyoffshore.data.GpsFormat
import com.example.saltyoffshore.data.MapTheme
import com.example.saltyoffshore.data.SpeedUnits
import com.example.saltyoffshore.data.TemperatureUnits
import com.example.saltyoffshore.data.UserPreferences
import com.example.saltyoffshore.preferences.AppPreferencesDataStore
import com.example.saltyoffshore.repository.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "UserPreferencesStore"

data class UserPreferencesState(
    val userPreferences: UserPreferences? = null,
    val isSavingProfile: Boolean = false,
) {
    val hasDisplayName: Boolean
        get() = userPreferences?.hasName == true

    val currentDistanceUnits: DistanceUnits
        get() = userPreferences?.distanceUnits?.let { DistanceUnits.fromRawValue(it) }
            ?: DistanceUnits.NAUTICAL_MILES
}

class UserPreferencesStore(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private val preferencesRepository = UserPreferencesRepository(SupabaseClientProvider.client)

    private val _state = MutableStateFlow(UserPreferencesState())
    val state: StateFlow<UserPreferencesState> = _state.asStateFlow()

    private fun updateState(transform: UserPreferencesState.() -> UserPreferencesState) {
        _state.update { it.transform() }
    }

    fun loadUserPreferences() {
        val userId = AuthManager.currentUserId ?: return
        scope.launch(Dispatchers.IO) {
            val prefs = preferencesRepository.fetchPreferences(userId)
            updateState { copy(userPreferences = prefs) }
            Log.d(TAG, "Loaded user preferences: ${prefs != null}")
        }
    }

    fun updateDepthUnits(units: DepthUnits) {
        val userId = AuthManager.currentUserId ?: return
        scope.launch(Dispatchers.IO) {
            AppPreferencesDataStore.setDepthUnits(context, units.rawValue)
            if (preferencesRepository.updateField(userId, "depth_units", units.rawValue)) {
                updateState { copy(userPreferences = userPreferences?.copy(depthUnits = units.rawValue)) }
                Log.d(TAG, "Updated depth units to ${units.displayName}")
            }
        }
    }

    fun updateDistanceUnits(units: DistanceUnits) {
        val userId = AuthManager.currentUserId ?: return
        scope.launch(Dispatchers.IO) {
            AppPreferencesDataStore.setDistanceUnits(context, units.rawValue)
            if (preferencesRepository.updateField(userId, "distance_units", units.rawValue)) {
                updateState { copy(userPreferences = userPreferences?.copy(distanceUnits = units.rawValue)) }
                Log.d(TAG, "Updated distance units to ${units.displayName}")
            }
        }
    }

    fun updateSpeedUnits(units: SpeedUnits) {
        val userId = AuthManager.currentUserId ?: return
        scope.launch(Dispatchers.IO) {
            AppPreferencesDataStore.setSpeedUnits(context, units.rawValue)
            if (preferencesRepository.updateField(userId, "speed_units", units.rawValue)) {
                updateState { copy(userPreferences = userPreferences?.copy(speedUnits = units.rawValue)) }
                Log.d(TAG, "Updated speed units to ${units.displayName}")
            }
        }
    }

    fun updateTemperatureUnits(units: TemperatureUnits) {
        val userId = AuthManager.currentUserId ?: return
        scope.launch(Dispatchers.IO) {
            AppPreferencesDataStore.setTemperatureUnits(context, units.rawValue)
            if (preferencesRepository.updateField(userId, "temperature_units", units.rawValue)) {
                updateState { copy(userPreferences = userPreferences?.copy(temperatureUnits = units.rawValue)) }
                Log.d(TAG, "Updated temperature units to ${units.displayName}")
            }
        }
    }

    fun updateGpsFormat(format: GpsFormat) {
        val userId = AuthManager.currentUserId ?: return
        scope.launch(Dispatchers.IO) {
            AppPreferencesDataStore.setGpsFormat(context, format.rawValue)
            if (preferencesRepository.updateField(userId, "gps_format", format.rawValue)) {
                updateState { copy(userPreferences = userPreferences?.copy(gpsFormat = format.rawValue)) }
                Log.d(TAG, "Updated GPS format to ${format.displayName}")
            }
        }
    }

    fun updateMapTheme(theme: MapTheme) {
        val userId = AuthManager.currentUserId ?: return
        scope.launch(Dispatchers.IO) {
            AppPreferencesDataStore.setMapTheme(context, theme.rawValue)
            if (preferencesRepository.updateField(userId, "map_theme", theme.rawValue)) {
                updateState { copy(userPreferences = userPreferences?.copy(mapTheme = theme.rawValue)) }
                Log.d(TAG, "Updated map theme to ${theme.displayName}")
            }
        }
    }

    fun updateProfile(firstName: String?, lastName: String?, location: String?) {
        val userId = AuthManager.currentUserId ?: return
        updateState { copy(isSavingProfile = true) }
        scope.launch(Dispatchers.IO) {
            val currentPrefs = _state.value.userPreferences ?: UserPreferences.empty(userId)
            val updatedPrefs = currentPrefs.copy(
                firstName = firstName,
                lastName = lastName,
                location = location
            )
            val result = preferencesRepository.updatePreferences(updatedPrefs)
            updateState {
                copy(
                    userPreferences = result ?: userPreferences,
                    isSavingProfile = false
                )
            }
            Log.d(TAG, "Updated profile: ${result != null}")
        }
    }

    suspend fun saveName(firstName: String, lastName: String) {
        val userId = AuthManager.currentUserId ?: return
        try {
            preferencesRepository.updateField(userId, "first_name", firstName)
            preferencesRepository.updateField(userId, "last_name", lastName)
            val updated = preferencesRepository.fetchPreferences(userId)
            if (updated != null) updateState { copy(userPreferences = updated) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save name: ${e.message}")
        }
    }

    fun updatePreferredRegion(regionId: String) {
        val userId = AuthManager.currentUserId
        scope.launch(Dispatchers.IO) {
            AppPreferencesDataStore.setPreferredRegionId(context, regionId)
            if (userId != null) {
                preferencesRepository.updateField(userId, "preferred_region_id", regionId)
            }
            updateState {
                copy(userPreferences = userPreferences?.copy(preferredRegionId = regionId))
            }
            Log.d(TAG, "Updated preferred region to $regionId")
        }
    }

    fun clearPreferences() {
        updateState { copy(userPreferences = null, isSavingProfile = false) }
    }
}
