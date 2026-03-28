package com.example.saltyoffshore.data.waypoint

import android.util.Log
import com.example.saltyoffshore.auth.SupabaseClientProvider
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import java.util.UUID

private const val TAG = "RealtimeWaypointService"

/**
 * Supabase Realtime subscription service for crew waypoint live updates.
 * Port of iOS RealtimeWaypointService (actor -> object + Mutex).
 *
 * Listens for INSERT events on shared_waypoints table and delivers
 * new waypoints via callback. Filters client-side by crewIds and
 * deduplicates by waypoint.id (geographic identity).
 *
 * NOTE: Requires `install(Realtime)` in SupabaseClientProvider.
 */
object RealtimeWaypointService {

    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var channel: io.github.jan.supabase.realtime.RealtimeChannel? = null
    private var listeningJob: Job? = null
    private var subscribedCrewIds: List<String> = emptyList()
    private var currentUserId: String? = null

    /** Set of waypoint.id values we've already seen (geographic dedup). */
    private val seenWaypointIds = mutableSetOf<String>()

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    // MARK: - Public API

    /**
     * Start listening for INSERT events on shared_waypoints.
     *
     * @param crewIds Crew IDs to filter events for (client-side).
     * @param currentUserId The authenticated user's ID (to skip own shares).
     * @param onWaypointReceived Callback invoked on each new SharedWaypoint.
     */
    suspend fun startListening(
        crewIds: List<String>,
        currentUserId: String,
        onWaypointReceived: (SharedWaypoint) -> Unit
    ) = mutex.withLock {
        Log.d(TAG, "Starting realtime subscription for ${crewIds.size} crew(s)")

        // Tear down any existing subscription
        stopListeningInternal()

        if (crewIds.isEmpty()) {
            Log.d(TAG, "No crew IDs provided, skipping subscription")
            return@withLock
        }

        this.subscribedCrewIds = crewIds
        this.currentUserId = currentUserId
        seenWaypointIds.clear()

        val supabase = SupabaseClientProvider.client
        val channelName = "shared-waypoints-${UUID.randomUUID()}"
        val newChannel = supabase.channel(channelName)

        // Listen for INSERT events on shared_waypoints table.
        // Supabase Kotlin SDK does not support server-side IN filters on channels,
        // so we filter client-side after receiving events.
        val insertFlow = newChannel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "shared_waypoints"
        }

        // Subscribe to the channel
        newChannel.subscribe(blockUntilSubscribed = true)
        this.channel = newChannel
        Log.d(TAG, "Channel subscribed: $channelName")

        // Collect inserts in a background coroutine
        listeningJob = scope.launch {
            insertFlow.collect { action ->
                handleInsert(action, onWaypointReceived)
            }
            Log.d(TAG, "Channel stream ended")
        }

        Log.d(TAG, "Realtime subscription active")
    }

    /**
     * Stop listening and clean up channel + coroutine.
     */
    suspend fun stopListening() = mutex.withLock {
        stopListeningInternal()
    }

    /**
     * Load initial crew waypoints from the database.
     * Delegates to WaypointSharingService.loadCrewWaypoints.
     */
    suspend fun loadInitialCrewWaypoints(crewIds: List<String>): List<SharedWaypoint> {
        if (crewIds.isEmpty()) return emptyList()

        return try {
            val waypoints = WaypointSharingService.loadCrewWaypoints(
                supabase = SupabaseClientProvider.client,
                crewIds = crewIds
            )
            // Seed the dedup set with existing waypoint IDs
            mutex.withLock {
                waypoints.forEach { sw ->
                    seenWaypointIds.add(sw.waypoint.id)
                }
            }
            Log.d(TAG, "Loaded ${waypoints.size} initial crew waypoints")
            waypoints
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load initial crew waypoints: ${e.message}")
            emptyList()
        }
    }

    // MARK: - Private

    /**
     * Internal stop — caller must hold mutex.
     */
    private suspend fun stopListeningInternal() {
        Log.d(TAG, "Stopping realtime subscription")

        listeningJob?.cancel()
        listeningJob = null

        channel?.let { ch ->
            try {
                ch.unsubscribe()
            } catch (e: Exception) {
                Log.e(TAG, "Error unsubscribing channel: ${e.message}")
            }
        }
        channel = null

        subscribedCrewIds = emptyList()
        currentUserId = null
        seenWaypointIds.clear()
    }

    /**
     * Process an INSERT action from the realtime channel.
     *
     * Edge cases handled:
     * 1. Filter by subscribed crewIds (client-side)
     * 2. Deduplicate by waypoint.id (geographic identity)
     * 3. Skip own shares (sharedByUserId == currentUserId)
     * 4. sharedByName will be null (realtime events lack JOIN data)
     */
    private fun handleInsert(
        action: PostgresAction.Insert,
        onWaypointReceived: (SharedWaypoint) -> Unit
    ) {
        try {
            val sharedWaypoint = json.decodeFromJsonElement(SharedWaypoint.serializer(), action.record)

            // 1. Filter: only process events for subscribed crews
            if (sharedWaypoint.crewId !in subscribedCrewIds) {
                Log.d(TAG, "Skipping event for unsubscribed crew: ${sharedWaypoint.crewId}")
                return
            }

            // 2. Dedup by waypoint.id (geographic identity, not SharedWaypoint.id)
            val waypointId = sharedWaypoint.waypoint.id
            val alreadySeen = synchronized(seenWaypointIds) {
                !seenWaypointIds.add(waypointId)
            }
            if (alreadySeen) {
                Log.d(TAG, "Skipping duplicate waypoint: $waypointId")
                return
            }

            // 3. Skip own shares
            if (sharedWaypoint.sharedByUserId == currentUserId) {
                Log.d(TAG, "Skipping own waypoint share")
                return
            }

            // 4. Deliver to callback (sharedByName is null from realtime)
            Log.d(TAG, "New shared waypoint received: ${sharedWaypoint.waypoint.name ?: "Unnamed"}")
            onWaypointReceived(sharedWaypoint)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode shared waypoint: ${e.message}")
        }
    }
}
