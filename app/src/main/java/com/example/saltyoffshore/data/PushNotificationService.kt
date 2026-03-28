package com.example.saltyoffshore.data

import android.util.Log
import com.example.saltyoffshore.auth.SupabaseClientProvider
import com.google.firebase.messaging.FirebaseMessaging
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.tasks.await

/**
 * Manages FCM token lifecycle and Supabase device token registration.
 *
 * iOS ref: Services/PushNotificationService.swift
 *
 * Flow:
 *  1. App launches → call registerFCMToken() after sign-in
 *  2. FCM rotates token → FirebaseMessagingService subclass calls saveFCMToken()
 *  3. User signs out → call deleteFCMToken()
 */
object PushNotificationService {

    private const val TAG = "PushNotificationService"
    private val supabase get() = SupabaseClientProvider.client

    // MARK: - Token Management

    /**
     * Fetch the current FCM token and upsert it to Supabase device_tokens.
     * Call this after a successful sign-in.
     */
    suspend fun registerFCMToken() {
        val userId = supabase.auth.currentSessionOrNull()?.user?.id ?: run {
            Log.w(TAG, "No user session — skipping FCM registration")
            return
        }
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            saveFCMToken(token, userId.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch FCM token: ${e.message}")
        }
    }

    /**
     * Save an FCM token to Supabase. Called by the FirebaseMessagingService
     * when a new token is generated.
     */
    suspend fun saveFCMToken(token: String) {
        val userId = supabase.auth.currentSessionOrNull()?.user?.id ?: run {
            Log.w(TAG, "No user session — cannot save FCM token")
            return
        }
        saveFCMToken(token, userId.toString())
    }

    private suspend fun saveFCMToken(token: String, userId: String) {
        try {
            supabase.from("device_tokens").upsert(
                mapOf(
                    "user_id" to userId,
                    "token" to token,
                    "platform" to "android",
                    "updated_at" to java.time.Instant.now().toString()
                )
            )
            Log.i(TAG, "FCM token saved successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save FCM token: ${e.message}")
        }
    }

    /**
     * Delete the current FCM token from Supabase.
     * Call this on sign-out.
     */
    suspend fun deleteFCMToken() {
        val userId = supabase.auth.currentSessionOrNull()?.user?.id ?: return
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            supabase.from("device_tokens").delete {
                filter {
                    eq("user_id", userId.toString())
                    eq("token", token)
                }
            }
            Log.i(TAG, "FCM token deleted")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete FCM token: ${e.message}")
        }
    }
}
