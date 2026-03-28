package com.example.saltyoffshore.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Singleton DataStore instance for app preferences.
 * Mirrors iOS AppPreferences, RegionPreferences, CachePreferences, TechnicalPreferences.
 */
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

object AppPreferencesDataStore {

    // =============================================================================
    // MARK: - Region Preferences (iOS: RegionPreferences.swift)
    // =============================================================================

    private val SELECTED_REGION_ID = stringPreferencesKey("selected_region_id")
    private val PREFERRED_REGION_ID = stringPreferencesKey("preferred_region_id")
    private val REGION_BOUNDS = stringPreferencesKey("preferred_region_bounds") // JSON encoded [[Double]]
    private val SELECTED_LORAN_REGION = stringPreferencesKey("selected_loran_region")
    private val LORAN_CHAIN = stringPreferencesKey("preferred_loran_chain")
    private val REGION_LAST_UPDATED = stringPreferencesKey("region_last_updated") // JSON encoded [String: String]
    private val SELECTED_TOURNAMENT_ID = stringPreferencesKey("selected_tournament_id")

    // Selected Region

    fun getSelectedRegionId(context: Context): Flow<String?> =
        context.dataStore.data.map { it[SELECTED_REGION_ID] }

    suspend fun setSelectedRegionId(context: Context, id: String?) {
        context.dataStore.edit { prefs ->
            if (id != null) prefs[SELECTED_REGION_ID] = id
            else prefs.remove(SELECTED_REGION_ID)
        }
    }

    // Preferred Region

    fun getPreferredRegionId(context: Context): Flow<String?> =
        context.dataStore.data.map { it[PREFERRED_REGION_ID] }

    suspend fun setPreferredRegionId(context: Context, id: String?) {
        context.dataStore.edit { prefs ->
            if (id != null) prefs[PREFERRED_REGION_ID] = id
            else prefs.remove(PREFERRED_REGION_ID)
        }
    }

    // Region Bounds (JSON encoded)

    fun getRegionBounds(context: Context): Flow<String?> =
        context.dataStore.data.map { it[REGION_BOUNDS] }

    suspend fun setRegionBounds(context: Context, boundsJson: String?) {
        context.dataStore.edit { prefs ->
            if (boundsJson != null) prefs[REGION_BOUNDS] = boundsJson
            else prefs.remove(REGION_BOUNDS)
        }
    }

    // Selected LORAN Region

    fun getSelectedLoranRegion(context: Context): Flow<String?> =
        context.dataStore.data.map { it[SELECTED_LORAN_REGION] }

    suspend fun setSelectedLoranRegion(context: Context, regionId: String?) {
        context.dataStore.edit { prefs ->
            if (regionId != null) prefs[SELECTED_LORAN_REGION] = regionId
            else prefs.remove(SELECTED_LORAN_REGION)
        }
    }

    // LORAN Chain (default: "9960")

    fun getLoranChain(context: Context): Flow<String> =
        context.dataStore.data.map { it[LORAN_CHAIN] ?: "9960" }

    suspend fun setLoranChain(context: Context, chainId: String) {
        context.dataStore.edit { it[LORAN_CHAIN] = chainId }
    }

    // Region Last Updated (JSON encoded map)

    fun getRegionLastUpdated(context: Context): Flow<String?> =
        context.dataStore.data.map { it[REGION_LAST_UPDATED] }

    suspend fun setRegionLastUpdated(context: Context, timestampsJson: String?) {
        context.dataStore.edit { prefs ->
            if (timestampsJson != null) prefs[REGION_LAST_UPDATED] = timestampsJson
            else prefs.remove(REGION_LAST_UPDATED)
        }
    }

    // Selected Tournament ID

    fun getSelectedTournamentId(context: Context): Flow<String?> =
        context.dataStore.data.map { it[SELECTED_TOURNAMENT_ID] }

    suspend fun setSelectedTournamentId(context: Context, tournamentId: String?) {
        context.dataStore.edit { prefs ->
            if (tournamentId != null) prefs[SELECTED_TOURNAMENT_ID] = tournamentId
            else prefs.remove(SELECTED_TOURNAMENT_ID)
        }
    }

    // =============================================================================
    // MARK: - Display Preferences (iOS: AppPreferences.swift)
    // =============================================================================

    private val MAP_THEME = stringPreferencesKey("map_theme")
    private val DEPTH_UNITS = stringPreferencesKey("depth_units")
    private val DISTANCE_UNITS = stringPreferencesKey("distance_units")
    private val SPEED_UNITS = stringPreferencesKey("speed_units")
    private val TEMPERATURE_UNITS = stringPreferencesKey("temperature_units")
    private val COORDINATE_SYSTEM = stringPreferencesKey("coordinate_system")
    private val GPS_FORMAT = stringPreferencesKey("gps_format")

    // Map Theme (default: "light")

    fun getMapTheme(context: Context): Flow<String> =
        context.dataStore.data.map { it[MAP_THEME] ?: "light" }

    suspend fun setMapTheme(context: Context, theme: String) {
        context.dataStore.edit { it[MAP_THEME] = theme }
    }

    // Depth Units (default: "feet")

    fun getDepthUnits(context: Context): Flow<String> =
        context.dataStore.data.map { it[DEPTH_UNITS] ?: "feet" }

    suspend fun setDepthUnits(context: Context, units: String) {
        context.dataStore.edit { it[DEPTH_UNITS] = units }
    }

    // Distance Units (default: "miles")

    fun getDistanceUnits(context: Context): Flow<String> =
        context.dataStore.data.map { it[DISTANCE_UNITS] ?: "miles" }

    suspend fun setDistanceUnits(context: Context, units: String) {
        context.dataStore.edit { it[DISTANCE_UNITS] = units }
    }

    // Speed Units (default: "knots")

    fun getSpeedUnits(context: Context): Flow<String> =
        context.dataStore.data.map { it[SPEED_UNITS] ?: "knots" }

    suspend fun setSpeedUnits(context: Context, units: String) {
        context.dataStore.edit { it[SPEED_UNITS] = units }
    }

    // Temperature Units (default: "fahrenheit")

    fun getTemperatureUnits(context: Context): Flow<String> =
        context.dataStore.data.map { it[TEMPERATURE_UNITS] ?: "fahrenheit" }

    suspend fun setTemperatureUnits(context: Context, units: String) {
        context.dataStore.edit { it[TEMPERATURE_UNITS] = units }
    }

    // Coordinate System (default: "GPS (DMM)")

    fun getCoordinateSystem(context: Context): Flow<String> =
        context.dataStore.data.map { it[COORDINATE_SYSTEM] ?: "GPS (DMM)" }

    suspend fun setCoordinateSystem(context: Context, system: String) {
        context.dataStore.edit { it[COORDINATE_SYSTEM] = system }
    }

    // GPS Format (default: "dmm")

    fun getGpsFormat(context: Context): Flow<String> =
        context.dataStore.data.map { it[GPS_FORMAT] ?: "dmm" }

    suspend fun setGpsFormat(context: Context, format: String) {
        context.dataStore.edit { it[GPS_FORMAT] = format }
    }

    // =============================================================================
    // MARK: - Cache Preferences (iOS: CachePreferences.swift)
    // =============================================================================

    private val NETWORK_DOWNLOAD_PREFERENCE = stringPreferencesKey("network_download_preference")
    private val LAST_OFFLINE_DOWNLOAD_DATE = longPreferencesKey("last_offline_download_date")
    private val OFFLINE_DOWNLOAD_DAY_RANGE = intPreferencesKey("offline_download_day_range")
    private val LATEST_AVAILABLE_DATA_TIMESTAMP = longPreferencesKey("latest_available_data_timestamp")

    // Network Download Preference (default: "wifi_only")

    fun getNetworkDownloadPreference(context: Context): Flow<String> =
        context.dataStore.data.map { it[NETWORK_DOWNLOAD_PREFERENCE] ?: "wifi_only" }

    suspend fun setNetworkDownloadPreference(context: Context, preference: String) {
        context.dataStore.edit { it[NETWORK_DOWNLOAD_PREFERENCE] = preference }
    }

    // Last Offline Download Date (epoch millis, null if never)

    fun getLastOfflineDownloadDate(context: Context): Flow<Long?> =
        context.dataStore.data.map { it[LAST_OFFLINE_DOWNLOAD_DATE] }

    suspend fun setLastOfflineDownloadDate(context: Context, epochMillis: Long?) {
        context.dataStore.edit { prefs ->
            if (epochMillis != null) prefs[LAST_OFFLINE_DOWNLOAD_DATE] = epochMillis
            else prefs.remove(LAST_OFFLINE_DOWNLOAD_DATE)
        }
    }

    // Offline Download Day Range (default: 3)

    fun getOfflineDownloadDayRange(context: Context): Flow<Int> =
        context.dataStore.data.map { prefs ->
            val value = prefs[OFFLINE_DOWNLOAD_DAY_RANGE] ?: 0
            if (value > 0) value else 3
        }

    suspend fun setOfflineDownloadDayRange(context: Context, days: Int) {
        context.dataStore.edit { it[OFFLINE_DOWNLOAD_DAY_RANGE] = days }
    }

    // Latest Available Data Timestamp (epoch millis, null if unknown)

    fun getLatestAvailableDataTimestamp(context: Context): Flow<Long?> =
        context.dataStore.data.map { it[LATEST_AVAILABLE_DATA_TIMESTAMP] }

    suspend fun setLatestAvailableDataTimestamp(context: Context, epochMillis: Long?) {
        context.dataStore.edit { prefs ->
            if (epochMillis != null) prefs[LATEST_AVAILABLE_DATA_TIMESTAMP] = epochMillis
            else prefs.remove(LATEST_AVAILABLE_DATA_TIMESTAMP)
        }
    }

    // =============================================================================
    // MARK: - Technical Preferences (iOS: TechnicalPreferences.swift)
    // =============================================================================

    private val LAST_ANNOUNCEMENT_VERSION = intPreferencesKey("last_seen_announcement_version")
    private val HAS_SEEN_OVERLAY_MIGRATION_BANNER = booleanPreferencesKey("has_seen_overlay_migration_banner")
    private val HAS_SEEN_MY_STUFF_MIGRATION = booleanPreferencesKey("has_seen_my_stuff_migration")
    private val ENABLE_CREW_MAP_OVERLAY = booleanPreferencesKey("enable_crew_map_overlay")
    private val ENABLE_DYNAMIC_RANGE = booleanPreferencesKey("enable_dynamic_range")
    private val ENABLE_RUN_TRACKER = booleanPreferencesKey("enable_run_tracker")
    private val ENABLE_AUTO_WAYPOINT_FILTER = booleanPreferencesKey("enable_auto_waypoint_filter")
    private val ENABLE_WEATHER_LAYERS = booleanPreferencesKey("enable_weather_layers")

    // Last Announcement Version (default: 0)

    fun getLastAnnouncementVersion(context: Context): Flow<Int> =
        context.dataStore.data.map { it[LAST_ANNOUNCEMENT_VERSION] ?: 0 }

    suspend fun setLastAnnouncementVersion(context: Context, version: Int) {
        context.dataStore.edit { it[LAST_ANNOUNCEMENT_VERSION] = version }
    }

    // Has Seen Overlay Migration Banner (default: false)

    fun getHasSeenOverlayMigrationBanner(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[HAS_SEEN_OVERLAY_MIGRATION_BANNER] ?: false }

    suspend fun setHasSeenOverlayMigrationBanner(context: Context, seen: Boolean) {
        context.dataStore.edit { it[HAS_SEEN_OVERLAY_MIGRATION_BANNER] = seen }
    }

    // Has Seen My Stuff Migration (default: false)

    fun getHasSeenMyStuffMigration(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[HAS_SEEN_MY_STUFF_MIGRATION] ?: false }

    suspend fun setHasSeenMyStuffMigration(context: Context, seen: Boolean) {
        context.dataStore.edit { it[HAS_SEEN_MY_STUFF_MIGRATION] = seen }
    }

    // Enable Crew Map Overlay (default: false)

    fun getEnableCrewMapOverlay(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[ENABLE_CREW_MAP_OVERLAY] ?: false }

    suspend fun setEnableCrewMapOverlay(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[ENABLE_CREW_MAP_OVERLAY] = enabled }
    }

    // Enable Dynamic Range (default: false)

    fun getEnableDynamicRange(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[ENABLE_DYNAMIC_RANGE] ?: false }

    suspend fun setEnableDynamicRange(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[ENABLE_DYNAMIC_RANGE] = enabled }
    }

    // Enable Run Tracker (default: false)

    fun getEnableRunTracker(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[ENABLE_RUN_TRACKER] ?: false }

    suspend fun setEnableRunTracker(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[ENABLE_RUN_TRACKER] = enabled }
    }

    // Enable Auto Waypoint Filter (default: false)

    fun getEnableAutoWaypointFilter(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[ENABLE_AUTO_WAYPOINT_FILTER] ?: false }

    suspend fun setEnableAutoWaypointFilter(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[ENABLE_AUTO_WAYPOINT_FILTER] = enabled }
    }

    // Enable Weather Layers (default: false)

    fun getEnableWeatherLayers(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[ENABLE_WEATHER_LAYERS] ?: false }

    suspend fun setEnableWeatherLayers(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[ENABLE_WEATHER_LAYERS] = enabled }
    }

    // =============================================================================
    // MARK: - Notification Preferences (iOS: PushNotificationService.swift)
    // =============================================================================

    private val HAS_SEEN_NOTIFICATION_PRIMER = booleanPreferencesKey("has_seen_notification_primer")

    // Has Seen Notification Primer (default: false)

    fun getHasSeenNotificationPrimer(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[HAS_SEEN_NOTIFICATION_PRIMER] ?: false }

    suspend fun setHasSeenNotificationPrimer(context: Context, seen: Boolean) {
        context.dataStore.edit { it[HAS_SEEN_NOTIFICATION_PRIMER] = seen }
    }
}
