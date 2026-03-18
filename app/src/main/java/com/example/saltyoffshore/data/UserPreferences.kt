package com.example.saltyoffshore.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * User profile preferences stored in Supabase.
 * Property names use snake_case to match Supabase column names exactly.
 */
@Serializable
data class UserPreferences(
    val id: String,
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    val location: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,

    // Display preferences
    @SerialName("depth_units") val depthUnits: String? = null,
    @SerialName("distance_units") val distanceUnits: String? = null,
    @SerialName("speed_units") val speedUnits: String? = null,
    @SerialName("temperature_units") val temperatureUnits: String? = null,
    @SerialName("coordinate_system") val coordinateSystem: String? = null,
    @SerialName("map_theme") val mapTheme: String? = null,
    @SerialName("gps_format") val gpsFormat: String? = null,

    // Region preferences
    @SerialName("selected_region_id") val selectedRegionId: String? = null,
    @SerialName("preferred_region_id") val preferredRegionId: String? = null,
    @SerialName("selected_loran_region") val selectedLoranRegion: String? = null,
    @SerialName("loran_chain") val loranChain: String? = null
) {
    /**
     * Computed display name from first/last name.
     * Returns null if both names are empty/null.
     */
    val displayName: String?
        get() {
            val components = listOfNotNull(firstName, lastName)
                .filter { it.isNotEmpty() }
            return if (components.isEmpty()) null else components.joinToString(" ")
        }

    /**
     * Display name with fallback to "Crew Member".
     * Never returns null - always returns a usable string.
     */
    val displayNameOrFallback: String
        get() = displayName ?: "Crew Member"

    /**
     * Check if user has set their name.
     */
    val hasName: Boolean
        get() = displayName != null

    companion object {
        fun empty(id: String): UserPreferences = UserPreferences(id = id)
    }
}
