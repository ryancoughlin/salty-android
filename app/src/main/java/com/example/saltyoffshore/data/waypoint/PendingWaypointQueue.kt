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
import kotlinx.serialization.json.Json

private const val TAG = "PendingWaypointQueue"

/**
 * Thread-safe queue for tracking pending waypoint sync operations (upserts and deletes) when offline.
 * Persists to DataStore to survive app restarts.
 * Port of iOS PendingWaypointQueue (actor -> object + Mutex).
 */
object PendingWaypointQueue {

    private val mutex = Mutex()
    private val PENDING_UPSERTS_KEY = stringPreferencesKey("pending_waypoint_upserts")
    private val PENDING_DELETES_KEY = stringPreferencesKey("pending_waypoint_deletes")

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** Mark a waypoint ID for upsert on next sync. */
    suspend fun markForUpsert(context: Context, id: String) = mutex.withLock {
        withContext(Dispatchers.IO) {
            val upserts = loadSet(context, PENDING_UPSERTS_KEY).toMutableSet()
            upserts.add(id)
            saveSet(context, PENDING_UPSERTS_KEY, upserts)
            Log.d(TAG, "Marked waypoint $id for upsert")
        }
    }

    /** Mark a waypoint ID for delete on next sync. Also removes from upserts. */
    suspend fun markForDelete(context: Context, id: String) = mutex.withLock {
        withContext(Dispatchers.IO) {
            val upserts = loadSet(context, PENDING_UPSERTS_KEY).toMutableSet()
            upserts.remove(id)
            saveSet(context, PENDING_UPSERTS_KEY, upserts)

            val deletes = loadSet(context, PENDING_DELETES_KEY).toMutableSet()
            deletes.add(id)
            saveSet(context, PENDING_DELETES_KEY, deletes)
            Log.d(TAG, "Marked waypoint $id for delete")
        }
    }

    /** Remove from upserts after successful sync. */
    suspend fun completedUpsert(context: Context, id: String) = mutex.withLock {
        withContext(Dispatchers.IO) {
            val upserts = loadSet(context, PENDING_UPSERTS_KEY).toMutableSet()
            upserts.remove(id)
            saveSet(context, PENDING_UPSERTS_KEY, upserts)
            Log.d(TAG, "Completed upsert for waypoint $id")
        }
    }

    /** Remove from deletes after successful sync. */
    suspend fun completedDelete(context: Context, id: String) = mutex.withLock {
        withContext(Dispatchers.IO) {
            val deletes = loadSet(context, PENDING_DELETES_KEY).toMutableSet()
            deletes.remove(id)
            saveSet(context, PENDING_DELETES_KEY, deletes)
            Log.d(TAG, "Completed delete for waypoint $id")
        }
    }

    /** Returns current set of waypoint IDs pending upsert. */
    suspend fun pendingUpsertIds(context: Context): Set<String> = mutex.withLock {
        withContext(Dispatchers.IO) {
            loadSet(context, PENDING_UPSERTS_KEY)
        }
    }

    /** Returns current set of waypoint IDs pending delete. */
    suspend fun pendingDeleteIds(context: Context): Set<String> = mutex.withLock {
        withContext(Dispatchers.IO) {
            loadSet(context, PENDING_DELETES_KEY)
        }
    }

    /** Returns true if there are any pending upserts or deletes. */
    suspend fun hasPending(context: Context): Boolean = mutex.withLock {
        withContext(Dispatchers.IO) {
            loadSet(context, PENDING_UPSERTS_KEY).isNotEmpty() ||
                loadSet(context, PENDING_DELETES_KEY).isNotEmpty()
        }
    }

    /** Clear all pending upserts and deletes. */
    suspend fun clear(context: Context) = mutex.withLock {
        withContext(Dispatchers.IO) {
            saveSet(context, PENDING_UPSERTS_KEY, emptySet())
            saveSet(context, PENDING_DELETES_KEY, emptySet())
            Log.d(TAG, "Cleared all pending waypoint operations")
        }
    }

    // MARK: - Persistence (DataStore)

    private suspend fun loadSet(
        context: Context,
        key: androidx.datastore.preferences.core.Preferences.Key<String>
    ): Set<String> {
        val prefs = context.dataStore.data.first()
        val raw = prefs[key] ?: return emptySet()
        return try {
            json.decodeFromString<Set<String>>(raw)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode pending set for ${key.name}", e)
            emptySet()
        }
    }

    private suspend fun saveSet(
        context: Context,
        key: androidx.datastore.preferences.core.Preferences.Key<String>,
        set: Set<String>
    ) {
        val raw = json.encodeToString(set.toList())
        context.dataStore.edit { prefs ->
            prefs[key] = raw
        }
    }
}
