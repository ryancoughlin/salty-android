package com.example.saltyoffshore.data

import android.util.Log
import com.example.saltyoffshore.auth.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import java.time.Instant

/**
 * Manages FCM token lifecycle and Supabase device_tokens registration.
 *
 * iOS ref: Services/PushNotificationService.swift
 *
 * NOTE: Firebase Cloud Messaging dependency is not yet added to this project.
 * To complete this service:
 *   1. Add google-services.json to app/
 *   2. Add to build.gradle.kts:
 *        implementation(platform("com.google.firebase:firebase-bom:33.x.x"))
 *        implementation("com.google.firebase:firebase-messaging-ktx")
 *        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.x.x")
 *   3. Add Google Services plugin to build.gradle.kts plugins block
 *   4. Replace getFCMToken() stub below with:
 *        FirebaseMessaging.getInstance().token.await()
 *
 * Flow (once Firebase is wired):
 *  1. App launches → call registerFCMToken() after sign-in
 *  2. FCM rotates token → SaltyFirebaseMessagingService calls saveFCMToken()
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
        val token = getFCMToken() ?: return
        saveFCMToken(token, userId.toString())
    }

    /**
     * Save an FCM token to Supabase.
     * Called by the FirebaseMessagingService when a new token is generated.
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
                    "updated_at" to Instant.now().toString()
                )
            )
            Log.i(TAG, "FCM token saved successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save FCM token: ${e.message}")
        }
    }

    /**
     * Delete the current FCM token from Supabase. Call this on sign-out.
     */
    suspend fun deleteFCMToken() {
        val userId = supabase.auth.currentSessionOrNull()?.user?.id ?: return
        val token = getFCMToken() ?: return
        try {
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

    /**
     * Returns the current FCM registration token.
     * Stub — replace with FirebaseMessaging.getInstance().token.await() once Firebase is added.
     */
    private suspend fun getFCMToken(): String? {
        // TODO: return FirebaseMessaging.getInstance().token.await() once Firebase dep is added
        Log.w(TAG, "getFCMToken() is a stub — Firebase not yet configured")
        return null
    }
}
