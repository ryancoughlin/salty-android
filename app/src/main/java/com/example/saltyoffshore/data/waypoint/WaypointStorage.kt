package com.example.saltyoffshore.data.waypoint

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

private const val TAG = "WaypointStorage"
private const val FILENAME = "waypoints.json"
private const val BACKUP_FILENAME = "waypoints.backup.json"

object WaypointStorage {

    private val mutex = Mutex()

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun save(context: Context, waypoints: List<Waypoint>) {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                val validated = validate(waypoints)
                val data = json.encodeToString(
                    kotlinx.serialization.builtins.ListSerializer(Waypoint.serializer()),
                    validated
                )

                val file = File(context.filesDir, FILENAME)
                val tempFile = File(context.filesDir, "$FILENAME.tmp")

                // Atomic write: write to temp, then rename
                tempFile.writeText(data)
                tempFile.renameTo(file)

                Log.d(TAG, "Saved ${validated.size} waypoints")
            }
        }
    }

    suspend fun load(context: Context): List<Waypoint> {
        return mutex.withLock {
            withContext(Dispatchers.IO) {
                val file = File(context.filesDir, FILENAME)
                if (!file.exists()) return@withContext emptyList()

                try {
                    val data = file.readText()
                    val waypoints = json.decodeFromString(
                        kotlinx.serialization.builtins.ListSerializer(Waypoint.serializer()),
                        data
                    )
                    val validated = validate(waypoints)
                    Log.d(TAG, "Loaded ${validated.size} waypoints")
                    validated
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load waypoints", e)
                    emptyList()
                }
            }
        }
    }

    suspend fun deleteAll(context: Context) {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                val file = File(context.filesDir, FILENAME)
                val backup = File(context.filesDir, BACKUP_FILENAME)
                val temp = File(context.filesDir, "$FILENAME.tmp")

                if (file.exists()) file.delete()
                if (backup.exists()) backup.delete()
                if (temp.exists()) temp.delete()

                Log.d(TAG, "Deleted all waypoint files")
            }
        }
    }

    // MARK: - Validation

    private fun validate(waypoints: List<Waypoint>): List<Waypoint> {
        // Filter invalid coordinates
        val valid = waypoints.filter {
            it.latitude in -90.0..90.0 && it.longitude in -180.0..180.0
        }

        // Deduplicate by ID (keep first occurrence)
        val seen = mutableSetOf<String>()
        val unique = valid.filter { wp ->
            val isNew = seen.add(wp.id)
            if (!isNew) Log.w(TAG, "Duplicate waypoint ID: ${wp.id}")
            isNew
        }

        if (unique.size < waypoints.size) {
            Log.w(TAG, "Removed ${waypoints.size - unique.size} invalid/duplicate waypoints")
        }

        return unique
    }
}
