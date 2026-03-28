package com.example.saltyoffshore.data.waypoint

import com.example.saltyoffshore.auth.SupabaseClientProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Syncs owned waypoints to Supabase for cross-device support.
 *
 * iOS ref: Features/Waypoints/Services/WaypointSyncService.swift
 */
object WaypointSyncService {

    private val mutex = Mutex()
    private val supabase get() = SupabaseClientProvider.client

    @Serializable
    private data class WaypointDBRow(
        val id: String,
        @SerialName("user_id") val userId: String,
        @SerialName("waypoint_data") val waypointData: Waypoint
    )

    @Serializable
    private data class WaypointUpsert(
        val id: String,
        @SerialName("waypoint_data") val waypointData: Waypoint
    )

    // MARK: - Fetch

    /** Fetch all owned waypoints from Supabase (source of truth for cross-device). */
    suspend fun fetchWaypoints(): List<Waypoint> = mutex.withLock {
        val rows: List<WaypointDBRow> = supabase
            .from("waypoints")
            .select()
            .decodeList()
        rows.map { it.waypointData }
    }

    // MARK: - Upsert

    /** Upsert a single waypoint to Supabase. */
    suspend fun upsertWaypoint(waypoint: Waypoint) = mutex.withLock {
        val payload = WaypointUpsert(id = waypoint.id, waypointData = waypoint)
        supabase.from("waypoints").upsert(payload)
    }

    /** Batch upsert waypoints (for initial sync or GPX import). */
    suspend fun upsertWaypoints(waypoints: List<Waypoint>) {
        if (waypoints.isEmpty()) return
        mutex.withLock {
            val payloads = waypoints.map { WaypointUpsert(id = it.id, waypointData = it) }
            supabase.from("waypoints").upsert(payloads)
        }
    }

    // MARK: - Delete

    /** Delete a single waypoint from Supabase by ID. */
    suspend fun deleteWaypoint(id: String) = mutex.withLock {
        supabase.from("waypoints").delete { filter { eq("id", id) } }
    }

    /**
     * Delete all waypoints for the current user.
     * RLS automatically scopes the delete to the authenticated user's rows.
     */
    suspend fun deleteAllWaypoints() = mutex.withLock {
        // neq with a random UUID matches all rows; RLS scopes to the current user.
        supabase.from("waypoints").delete {
            filter { neq("id", java.util.UUID.randomUUID().toString()) }
        }
    }
}
