package com.example.saltyoffshore.data

import android.content.Context
import android.util.Log
import com.example.saltyoffshore.auth.SupabaseClientProvider
import com.example.saltyoffshore.preferences.AppPreferencesDataStore
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

private const val TAG = "AnnouncementService"

/**
 * Singleton service for checking and managing announcements.
 * Matches iOS AnnouncementService — fetches from Supabase "announcement" table,
 * compares version against stored lastAnnouncementVersion.
 *
 * iOS ref: Features/Announcement/Services/AnnouncementService.swift
 */
object AnnouncementService {

    /**
     * Fetch the singleton announcement from Supabase and determine display state.
     * Returns Hidden if no announcement, not active, or already seen.
     */
    suspend fun checkForAnnouncements(context: Context): AnnouncementDisplayState =
        withContext(Dispatchers.IO) {
            try {
                val announcements = SupabaseClientProvider.client
                    .from("announcement")
                    .select {
                        filter { eq("id", "singleton") }
                    }
                    .decodeList<Announcement>()
                val announcement = announcements.firstOrNull()

                if (announcement == null) {
                    Log.d(TAG, "Announcement check: none")
                    return@withContext AnnouncementDisplayState.Hidden
                }

                if (!announcement.isActive) {
                    Log.d(TAG, "Announcement check: inactive")
                    return@withContext AnnouncementDisplayState.Hidden
                }

                val lastSeenVersion = AppPreferencesDataStore
                    .getLastAnnouncementVersion(context)
                    .first()

                if (announcement.version > lastSeenVersion) {
                    Log.d(TAG, "Announcement check: visible — ${announcement.title}")
                    AnnouncementDisplayState.Visible(announcement)
                } else {
                    Log.d(TAG, "Announcement check: already seen v${announcement.version}")
                    AnnouncementDisplayState.Hidden
                }
            } catch (e: Exception) {
                // Silent failure — announcements are non-critical
                Log.e(TAG, "Failed to check announcements", e)
                AnnouncementDisplayState.Hidden
            }
        }

    /**
     * Persist that the user has seen this announcement version.
     */
    suspend fun markAsSeen(context: Context, version: Int) {
        withContext(Dispatchers.IO) {
            AppPreferencesDataStore.setLastAnnouncementVersion(context, version)
        }
    }
}
