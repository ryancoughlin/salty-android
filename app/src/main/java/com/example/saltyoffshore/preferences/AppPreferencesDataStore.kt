package com.example.saltyoffshore.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Singleton DataStore instance for app preferences.
 * Mirrors iOS RegionPreferences for selectedRegionId and preferredRegionId.
 */
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

object AppPreferencesDataStore {

    private val SELECTED_REGION_ID = stringPreferencesKey("selected_region_id")
    private val PREFERRED_REGION_ID = stringPreferencesKey("preferred_region_id")

    // MARK: - Selected Region

    fun getSelectedRegionId(context: Context): Flow<String?> =
        context.dataStore.data.map { preferences ->
            preferences[SELECTED_REGION_ID]
        }

    suspend fun saveSelectedRegionId(context: Context, id: String?) {
        context.dataStore.edit { preferences ->
            if (id != null) {
                preferences[SELECTED_REGION_ID] = id
            } else {
                preferences.remove(SELECTED_REGION_ID)
            }
        }
    }

    // MARK: - Preferred Region

    fun getPreferredRegionId(context: Context): Flow<String?> =
        context.dataStore.data.map { preferences ->
            preferences[PREFERRED_REGION_ID]
        }

    suspend fun savePreferredRegionId(context: Context, id: String?) {
        context.dataStore.edit { preferences ->
            if (id != null) {
                preferences[PREFERRED_REGION_ID] = id
            } else {
                preferences.remove(PREFERRED_REGION_ID)
            }
        }
    }
}
