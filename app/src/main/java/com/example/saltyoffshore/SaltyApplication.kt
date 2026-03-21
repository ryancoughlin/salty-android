package com.example.saltyoffshore

import android.app.Application
import android.util.Log
import com.example.saltyoffshore.auth.SupabaseClientProvider

/**
 * Application subclass for Salty Offshore.
 * Matches iOS AppDelegate / SaltyOffshoreApp.init() pattern.
 *
 * Responsibilities:
 * - Initialize Supabase client eagerly (session restore needs it early)
 * - Future: Firebase, RevenueCat, push notifications
 */
class SaltyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "SaltyApplication.onCreate()")

        // Eagerly initialize Supabase client so session restore happens before UI
        SupabaseClientProvider.client
    }

    companion object {
        private const val TAG = "SaltyApplication"
    }
}
