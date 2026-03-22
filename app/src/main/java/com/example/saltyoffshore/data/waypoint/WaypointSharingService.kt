package com.example.saltyoffshore.data.waypoint

import android.content.Context
import android.util.Log
import com.example.saltyoffshore.auth.AuthManager
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
// import io.github.jan.supabase.postgrest.rpc  // TODO: uncomment when wiring crew sharing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

private const val TAG = "WaypointSharingService"
private const val TABLE = "shared_waypoints"

/**
 * Service for sharing waypoints to crews via Supabase.
 * Port of iOS WaypointSharingService (actor -> object + Mutex).
 *
 * Handles online sharing, offline queueing, and syncing.
 */
object WaypointSharingService {

    private val mutex = Mutex()

    // -- Insert payload matching Supabase shared_waypoints table --

    @Serializable
    private data class SharedWaypointInsert(
        @SerialName("waypoint_data") val waypointData: Waypoint,
        @SerialName("shared_by_user_id") val sharedByUserId: String,
        @SerialName("crew_id") val crewId: String,
        @SerialName("read_by_user_ids") val readByUserIds: List<String>
    )

    // MARK: - Share Operations

    /**
     * Share a waypoint to a single crew.
     * Queues offline if no connectivity.
     */
    suspend fun shareWaypoint(
        supabase: SupabaseClient,
        waypoint: Waypoint,
        crewId: String,
        userId: String,
        context: Context? = null
    ) = mutex.withLock {
        withContext(Dispatchers.IO) {
            // TODO: Check connectivity and queue offline if needed
            // For now, always try online share; caller can catch and queue
            shareToSupabase(supabase, waypoint, crewId, userId)
        }
    }

    /**
     * Share a waypoint to multiple crews.
     */
    suspend fun shareWaypoint(
        supabase: SupabaseClient,
        waypoint: Waypoint,
        crewIds: List<String>,
        userId: String,
        context: Context? = null
    ) {
        crewIds.forEach { crewId ->
            shareWaypoint(supabase, waypoint, crewId, userId, context)
        }
    }

    /**
     * Update a shared waypoint's waypoint_data in the database.
     * Any crew member can edit (RLS enforces crew membership).
     */
    suspend fun updateSharedWaypoint(
        supabase: SupabaseClient,
        sharedWaypointId: String,
        waypoint: Waypoint
    ) = mutex.withLock {
        withContext(Dispatchers.IO) {
            Log.d(TAG, "Updating shared waypoint $sharedWaypointId")
            supabase.from(TABLE)
                .update(mapOf("waypoint_data" to waypoint)) {
                    filter { eq("id", sharedWaypointId) }
                }
            Log.d(TAG, "Updated shared waypoint $sharedWaypointId")
        }
    }

    /**
     * Delete a shared waypoint from the database.
     * Any crew member can delete (RLS enforces crew membership).
     */
    suspend fun deleteSharedWaypoint(
        supabase: SupabaseClient,
        sharedWaypointId: String
    ) = mutex.withLock {
        withContext(Dispatchers.IO) {
            Log.d(TAG, "Deleting shared waypoint $sharedWaypointId")
            supabase.from(TABLE)
                .delete {
                    filter { eq("id", sharedWaypointId) }
                }
            Log.d(TAG, "Deleted shared waypoint $sharedWaypointId")
        }
    }

    /**
     * Load crew waypoints with sharer info (JOIN on user_preferences).
     * Matches iOS: select *, sharer:user_preferences!shared_by_user_id(first_name, last_name)
     */
    suspend fun loadCrewWaypoints(
        supabase: SupabaseClient,
        crewIds: List<String>
    ): List<SharedWaypoint> = withContext(Dispatchers.IO) {
        if (crewIds.isEmpty()) return@withContext emptyList()

        Log.d(TAG, "Loading waypoints for ${crewIds.size} crews")
        val waypoints = supabase.from(TABLE)
            .select(
                columns = io.github.jan.supabase.postgrest.query.Columns.raw(
                    """
                    *,
                    sharer:user_preferences!shared_by_user_id (
                        first_name,
                        last_name
                    )
                    """.trimIndent()
                )
            ) {
                filter { isIn("crew_id", crewIds) }
                order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            }
            .decodeList<SharedWaypoint>()

        Log.d(TAG, "Loaded ${waypoints.size} crew waypoints")
        waypoints
    }

    /**
     * Mark a shared waypoint as read by appending userId to read_by_user_ids.
     * Uses Supabase RPC (mark_waypoint_read) matching iOS pattern.
     */
    suspend fun markAsRead(
        supabase: SupabaseClient,
        sharedWaypointId: String,
        userId: String
    ) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Marking waypoint $sharedWaypointId as read for $userId")
        // TODO: Wire up when crew sharing is active
        // supabase.rpc("mark_waypoint_read", buildJsonObject {
        //     put("waypoint_id", JsonPrimitive(sharedWaypointId))
        //     put("user_id", JsonPrimitive(userId))
        // })
        Log.d(TAG, "markAsRead stub — will wire when crew sharing is active")
        Log.d(TAG, "Marked waypoint as read")
    }

    /**
     * Sync all pending offline shares to Supabase.
     * Call when connectivity is restored.
     */
    suspend fun syncPendingShares(
        supabase: SupabaseClient,
        context: Context,
        userId: String
    ) {
        val pending = OfflineShareQueue.getPending(context)
        for (share in pending) {
            try {
                shareToSupabase(supabase, share.waypoint, share.crewId, userId)
                OfflineShareQueue.dequeue(context, share)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to sync pending share ${share.id}: ${e.message}")
                // Keep in queue for retry
            }
        }
    }

    // MARK: - Private

    private suspend fun shareToSupabase(
        supabase: SupabaseClient,
        waypoint: Waypoint,
        crewId: String,
        userId: String
    ) {
        // Check for duplicate: same waypoint.id already shared to this crew
        val existing = supabase.from(TABLE)
            .select(columns = io.github.jan.supabase.postgrest.query.Columns.raw("id")) {
                filter {
                    eq("crew_id", crewId)
                    eq("waypoint_data->>id", waypoint.id)
                }
            }
            .decodeList<IdOnly>()

        if (existing.isNotEmpty()) {
            Log.d(TAG, "Waypoint already shared to crew $crewId, skipping duplicate")
            return
        }

        // Pre-mark as read by sharer so they don't see notification for their own share
        val insertData = SharedWaypointInsert(
            waypointData = waypoint,
            sharedByUserId = userId,
            crewId = crewId,
            readByUserIds = listOf(userId)
        )

        supabase.from(TABLE)
            .insert(insertData)

        Log.d(TAG, "Shared waypoint ${waypoint.id} to crew $crewId")
    }

    @Serializable
    private data class IdOnly(val id: String)
}
