package com.example.saltyoffshore.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// MARK: - Satellite Mode

enum class SatelliteMode(val label: String) {
    TRACKER("Tracker"),    // Global: "Where are satellites now?"
    COVERAGE("My Region")  // Regional: "What data is over my water?"
}

// MARK: - Satellite Tracking Mode

/**
 * Standalone mode state for satellite tracking.
 *
 * Manages both mode lifecycle (active/inactive) and UI state (selections, filters).
 * Data lives in SatelliteStore — this object coordinates mode and selection state.
 *
 * iOS ref: SatelliteTrackingMode (@Observable class)
 */
class SatelliteTrackingMode {

    // MARK: - Mode State

    var isActive by mutableStateOf(false)
        private set

    var mode by mutableStateOf(SatelliteMode.TRACKER)
        private set

    /** Region ID for coverage mode */
    var regionId by mutableStateOf<String?>(null)

    // MARK: - Selection State

    /** Selected track ID (tracker mode) */
    var selectedTrackId by mutableStateOf<String?>(null)

    /** Selected pass ID (coverage mode) */
    var selectedPassId by mutableStateOf<String?>(null)

    /** Night pass filter toggle */
    var showNightPasses by mutableStateOf(false)

    // MARK: - Dependencies

    /** Closure to dismiss the active sheet. Wired post-init to avoid circular dependency. */
    var dismissSheet: () -> Unit = {}

    /** Provider for current region ID — avoids coupling to AppViewModel */
    var selectedRegionIdProvider: () -> String? = { null }

    // MARK: - Actions

    fun enter() {
        isActive = true
        regionId = selectedRegionIdProvider()
        dismissSheet()
    }

    fun exit() {
        isActive = false
        mode = SatelliteMode.TRACKER
        selectedTrackId = null
        selectedPassId = null
    }

    fun setMode(newMode: SatelliteMode) {
        if (newMode == mode || !isActive) return
        mode = newMode
    }

    fun selectTrack(id: String?) { selectedTrackId = id }
    fun selectPass(id: String?) { selectedPassId = id }
}
