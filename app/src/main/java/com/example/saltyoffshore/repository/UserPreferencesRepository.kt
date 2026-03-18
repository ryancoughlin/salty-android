package com.example.saltyoffshore.repository

import android.util.Log
import com.example.saltyoffshore.data.UserPreferences
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import java.time.Instant

/**
 * Repository for managing user preferences in Supabase.
 */
class UserPreferencesRepository(
    private val supabase: SupabaseClient
) {
    private val tableName = "user_preferences"

    /**
     * Fetch preferences for a user.
     * Returns null if no preferences exist (PGRST116 error).
     */
    suspend fun fetchPreferences(userId: String): UserPreferences? {
        return try {
            supabase.from(tableName)
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingleOrNull<UserPreferences>()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch preferences: ${e.message}")
            null
        }
    }

    /**
     * Create initial preferences for a new user.
     */
    suspend fun createInitialPreferences(
        userId: String,
        firstName: String? = null,
        lastName: String? = null
    ): UserPreferences? {
        val now = Instant.now().toString()
        val initialPreferences = UserPreferences(
            id = userId,
            firstName = firstName,
            lastName = lastName,
            createdAt = now,
            updatedAt = now
        )

        return try {
            supabase.from(tableName)
                .insert(initialPreferences)
                .decodeSingleOrNull<UserPreferences>()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create preferences: ${e.message}")
            null
        }
    }

    /**
     * Update existing preferences.
     */
    suspend fun updatePreferences(prefs: UserPreferences): UserPreferences? {
        val now = Instant.now().toString()
        val updatedPrefs = prefs.copy(updatedAt = now)

        return try {
            supabase.from(tableName)
                .update(updatedPrefs) {
                    filter {
                        eq("id", prefs.id)
                    }
                }
                .decodeSingleOrNull<UserPreferences>()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update preferences: ${e.message}")
            null
        }
    }

    /**
     * Update a single preference field.
     */
    suspend fun updateField(userId: String, field: String, value: String?): Boolean {
        val now = Instant.now().toString()
        return try {
            supabase.from(tableName)
                .update(mapOf(field to value, "updated_at" to now)) {
                    filter {
                        eq("id", userId)
                    }
                }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update field $field: ${e.message}")
            false
        }
    }

    companion object {
        private const val TAG = "UserPreferencesRepo"
    }
}
