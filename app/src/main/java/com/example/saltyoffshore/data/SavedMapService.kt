package com.example.saltyoffshore.data

import android.util.Log
import com.example.saltyoffshore.auth.AuthManager
import com.example.saltyoffshore.auth.SupabaseClientProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant

/**
 * CRUD service for the Supabase `saved_maps` table.
 *
 * iOS ref: Features/SavedMaps/Services/SavedMapService.swift
 *
 * RLS handles visibility:
 *   - Personal maps: user_id = current user
 *   - Crew maps: crew_id in user's crews (handled by Supabase policies)
 */
object SavedMapService {

    private const val TAG = "SavedMapService"
    private val mutex = Mutex()
    private val supabase get() = SupabaseClientProvider.client

    sealed class SavedMapError : Exception() {
        data object NotAuthenticated : SavedMapError()
        data object EmptyName : SavedMapError()
        data object NameTooLong : SavedMapError()
        data class DatabaseError(val reason: String) : SavedMapError()
    }

    // MARK: - Fetch

    /** Fetch all maps visible to the current user (personal + crew, newest first). */
    suspend fun fetchAllVisibleMaps(): List<SavedMap> = mutex.withLock {
        AuthManager.currentUserId ?: throw SavedMapError.NotAuthenticated
        try {
            supabase.from("saved_maps")
                .select()
                .decodeList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch saved maps: ${e.message}")
            throw SavedMapError.DatabaseError(e.message ?: "Unknown error")
        }
    }

    // MARK: - Create

    /**
     * Insert a new saved map row immediately. Thumbnail upload is a separate
     * background operation handled by the caller.
     */
    suspend fun createSavedMap(
        name: String,
        mapConfig: MapConfiguration,
        regionName: String? = null,
        datasetName: String? = null
    ): SavedMap = mutex.withLock {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) throw SavedMapError.EmptyName
        if (trimmed.length > 100) throw SavedMapError.NameTooLong
        AuthManager.currentUserId ?: throw SavedMapError.NotAuthenticated

        val insert = SavedMapInsert(
            name = trimmed,
            mapConfig = mapConfig,
            thumbnailUrl = null,
            regionName = regionName,
            datasetName = datasetName
        )
        try {
            val result: SavedMap = supabase.from("saved_maps")
                .insert(insert) { select() }
                .decodeSingle()
            Log.i(TAG, "Created saved map: ${result.id}")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create saved map: ${e.message}")
            throw SavedMapError.DatabaseError(e.message ?: "Unknown error")
        }
    }

    // MARK: - Update

    /** Rename an existing saved map. */
    suspend fun updateMapName(id: String, newName: String): SavedMap = mutex.withLock {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) throw SavedMapError.EmptyName
        if (trimmed.length > 100) throw SavedMapError.NameTooLong

        val update = SavedMapNameUpdate(name = trimmed, updatedAt = Instant.now().toString())
        try {
            supabase.from("saved_maps")
                .update(update) {
                    select()
                    filter { eq("id", id) }
                }
                .decodeSingle()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to rename saved map: ${e.message}")
            throw SavedMapError.DatabaseError(e.message ?: "Unknown error")
        }
    }

    // MARK: - Crew Sharing

    /** Share an existing personal map with a crew. */
    suspend fun shareMapWithCrew(mapId: String, crewId: String, sharedByName: String?): SavedMap = mutex.withLock {
        val update = SavedMapShareUpdate(
            crewId = crewId,
            sharedByName = sharedByName,
            updatedAt = Instant.now().toString()
        )
        try {
            val result: SavedMap = supabase.from("saved_maps")
                .update(update) {
                    select()
                    filter { eq("id", mapId) }
                }
                .decodeSingle()
            Log.i(TAG, "Shared map $mapId with crew $crewId")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share map with crew: ${e.message}")
            throw SavedMapError.DatabaseError(e.message ?: "Unknown error")
        }
    }

    /** Remove a map from crew sharing (revert to personal). */
    suspend fun unshareMap(mapId: String): SavedMap = mutex.withLock {
        val update = SavedMapShareUpdate(crewId = null, sharedByName = null, updatedAt = Instant.now().toString())
        try {
            supabase.from("saved_maps")
                .update(update) {
                    select()
                    filter { eq("id", mapId) }
                }
                .decodeSingle()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unshare map: ${e.message}")
            throw SavedMapError.DatabaseError(e.message ?: "Unknown error")
        }
    }

    // MARK: - Delete

    /** Delete a saved map by ID. */
    suspend fun deleteSavedMap(id: String) = mutex.withLock {
        try {
            supabase.from("saved_maps").delete { filter { eq("id", id) } }
            Log.i(TAG, "Deleted saved map: $id")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete saved map: ${e.message}")
            throw SavedMapError.DatabaseError(e.message ?: "Unknown error")
        }
    }

    /** Patch the thumbnail URL after a background upload completes. */
    suspend fun updateThumbnailUrl(id: String, url: String) = mutex.withLock {
        try {
            supabase.from("saved_maps").update(mapOf("thumbnail_url" to url)) {
                filter { eq("id", id) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update thumbnail URL: ${e.message}")
        }
    }
}
