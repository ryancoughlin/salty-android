package com.example.saltyoffshore

import android.app.Application
import android.util.Log
import com.example.saltyoffshore.auth.SupabaseClientProvider
import com.example.saltyoffshore.data.network.NetworkMonitor

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

        // Initialize network connectivity monitor
        NetworkMonitor.init(this)

        // Preload native library on startup (avoids lazy load during render path)
        System.loadLibrary("zarr-shader")
    }

    companion object {
        private const val TAG = "SaltyApplication"
    }
}
