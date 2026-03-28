package com.example.saltyoffshore.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A persisted map configuration with metadata and thumbnail.
 * Port of iOS SavedMapTypes.swift -> SavedMap struct.
 *
 * Personal maps have crewId = null. Crew maps have crewId set — visible to all crew members.
 */
@Serializable
data class SavedMap(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("crew_id") val crewId: String? = null,
    val name: String,
    @SerialName("map_config") val mapConfig: MapConfiguration,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerialName("region_name") val regionName: String? = null,
    @SerialName("dataset_name") val datasetName: String? = null,
    @SerialName("shared_by_name") val sharedByName: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
) {
    val isCrewMap: Boolean get() = crewId != null

    fun isOwnedBy(userId: String?): Boolean = this.userId == userId
}

/**
 * Database insert payload — only the fields the client sends.
 * id, created_at, updated_at are server-generated.
 * Port of iOS SavedMapTypes.swift -> SavedMapInsert struct.
 */
@Serializable
data class SavedMapInsert(
    val name: String,
    @SerialName("map_config") val mapConfig: MapConfiguration,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerialName("region_name") val regionName: String? = null,
    @SerialName("dataset_name") val datasetName: String? = null
)

/**
 * Rename payload.
 * Port of iOS SavedMapTypes.swift -> SavedMapNameUpdate struct.
 */
@Serializable
data class SavedMapNameUpdate(
    val name: String,
    @SerialName("updated_at") val updatedAt: String
)

/**
 * UPDATE payload for sharing a map with a crew.
 * Sets crew_id + shared_by_name on an existing row.
 * Port of iOS SavedMapTypes.swift -> SavedMapShareUpdate struct.
 */
@Serializable
data class SavedMapShareUpdate(
    @SerialName("crew_id") val crewId: String? = null,
    @SerialName("shared_by_name") val sharedByName: String? = null,
    @SerialName("updated_at") val updatedAt: String
)

/**
 * Errors that can occur during saved map operations.
 * Port of iOS SavedMapTypes.swift -> SavedMapError enum.
 */
sealed class SavedMapError : Exception() {
    data object NotAuthenticated : SavedMapError() {
        override val message = "Please sign in to save maps"
    }
    data object EmptyName : SavedMapError() {
        override val message = "Map name cannot be empty"
    }
    data object NameTooLong : SavedMapError() {
        override val message = "Map name is too long (max 100 characters)"
    }
    data class ThumbnailUploadFailed(val detail: String) : SavedMapError() {
        override val message = "Thumbnail upload failed: $detail"
    }
    data class DatabaseError(val detail: String) : SavedMapError() {
        override val message = "Database error: $detail"
    }
}
