package com.example.saltyoffshore.viewmodel

import androidx.compose.runtime.Stable
import com.example.saltyoffshore.data.AppStatus
import com.example.saltyoffshore.data.CurrentValue
import com.example.saltyoffshore.data.Dataset
import com.example.saltyoffshore.data.DatasetRenderConfig
import com.example.saltyoffshore.data.DatasetRenderingSnapshot
import com.example.saltyoffshore.data.DatasetType
import com.example.saltyoffshore.data.DepthFilterState
import com.example.saltyoffshore.data.RegionGroup
import com.example.saltyoffshore.data.RegionListItem
import com.example.saltyoffshore.data.RegionMetadata
import com.example.saltyoffshore.data.TimeEntry
import com.example.saltyoffshore.data.UserPreferences
import com.example.saltyoffshore.data.VisualLayerSource

/**
 * Single immutable UI state for the entire app.
 * Compose sees ONE atomic update per state change instead of 12+ individual mutations.
 */
@Stable
data class MapScreenState(
    // App status
    val appStatus: AppStatus = AppStatus.Idle,

    // Region data
    val regions: List<RegionListItem> = emptyList(),
    val regionGroups: List<RegionGroup> = emptyList(),
    val selectedRegion: RegionMetadata? = null,

    // Dataset data
    val selectedDataset: Dataset? = null,
    val selectedEntry: TimeEntry? = null,
    val primaryConfig: DatasetRenderConfig? = null,
    val renderingSnapshot: DatasetRenderingSnapshot = DatasetRenderingSnapshot.default(),
    val depthFilterState: DepthFilterState = DepthFilterState(),
    val visualSource: VisualLayerSource = VisualLayerSource.None,

    // Crosshair
    val primaryValue: CurrentValue = CurrentValue(),
    val currentZoom: Double = 4.0,
    val currentLatitude: Double = 30.0,

    // User preferences
    val userPreferences: UserPreferences? = null,

    // FTUX state
    val ftuxLoadingRegionId: String? = null,
    val preferredRegionId: String? = null,
    val hasCompletedInitialLoad: Boolean = false,
) {
    val isDataLayerActive: Boolean get() = selectedDataset != null && selectedEntry != null
    val currentDatasetType: DatasetType? get() = selectedDataset?.let { DatasetType.fromRawValue(it.type) }
}
