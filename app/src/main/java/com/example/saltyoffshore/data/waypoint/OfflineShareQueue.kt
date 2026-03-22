package com.example.saltyoffshore.data.waypoint

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.saltyoffshore.preferences.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID

private const val TAG = "OfflineShareQueue"

/**
 * Thread-safe queue for pending waypoint shares when offline.
 * Persists to DataStore to survive app restarts.
 * Port of iOS OfflineShareQueue (actor -> object + Mutex).
 */
object OfflineShareQueue {

    @Serializable
    data class PendingShare(
        val id: String,
        val waypoint: Waypoint,
        val crewId: String,
        val timestamp: String
    )

    private val mutex = Mutex()
    private val PENDING_SHARES_KEY = stringPreferencesKey("pending_waypoint_shares")

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** Add a waypoint share to the pending queue. */
    suspend fun enqueue(context: Context, waypoint: Waypoint, crewId: String) = mutex.withLock {
        withContext(Dispatchers.IO) {
            val pending = loadFromDisk(context).toMutableList()
            val share = PendingShare(
                id = UUID.randomUUID().toString(),
                waypoint = waypoint,
                crewId = crewId,
                timestamp = Instant.now().toString()
            )
            pending.add(share)
            saveToDisk(context, pending)
            Log.d(TAG, "Enqueued share for waypoint ${waypoint.id} to crew $crewId")
        }
    }

    /** Get all pending shares. */
    suspend fun getPending(context: Context): List<PendingShare> = mutex.withLock {
        withContext(Dispatchers.IO) {
            loadFromDisk(context)
        }
    }

    /** Remove a share from the queue after successful sync. */
    suspend fun dequeue(context: Context, share: PendingShare) = mutex.withLock {
        withContext(Dispatchers.IO) {
            val pending = loadFromDisk(context).filter { it.id != share.id }
            saveToDisk(context, pending)
            Log.d(TAG, "Dequeued share ${share.id}")
        }
    }

    /** Count of pending shares. */
    suspend fun count(context: Context): Int = mutex.withLock {
        withContext(Dispatchers.IO) {
            loadFromDisk(context).size
        }
    }

    /** Clear all pending shares. */
    suspend fun clear(context: Context) = mutex.withLock {
        withContext(Dispatchers.IO) {
            saveToDisk(context, emptyList())
            Log.d(TAG, "Cleared all pending shares")
        }
    }

    // MARK: - Persistence (DataStore)

    private suspend fun loadFromDisk(context: Context): List<PendingShare> {
        val prefs = context.dataStore.data.first()
        val raw = prefs[PENDING_SHARES_KEY] ?: return emptyList()
        return try {
            json.decodeFromString<List<PendingShare>>(raw)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode pending shares", e)
            emptyList()
        }
    }

    private suspend fun saveToDisk(context: Context, shares: List<PendingShare>) {
        val raw = json.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(PendingShare.serializer()),
            shares
        )
        context.dataStore.edit { prefs ->
            prefs[PENDING_SHARES_KEY] = raw
        }
    }
}
