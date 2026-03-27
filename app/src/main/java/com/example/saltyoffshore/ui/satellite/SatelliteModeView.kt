package com.example.saltyoffshore.ui.satellite

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saltyoffshore.data.satellite.DayNight
import com.example.saltyoffshore.ui.theme.Spacing
import com.example.saltyoffshore.viewmodel.SatelliteMode
import com.example.saltyoffshore.viewmodel.SatelliteStore
import com.example.saltyoffshore.viewmodel.SatelliteTrackingMode

// MARK: - SatelliteModeView

/**
 * Main satellite overlay — full-screen container on top of the map.
 *
 * Switches between Tracker mode (global satellite tracks) and
 * Coverage mode (regional passes + predictions).
 *
 * iOS ref: SatelliteModeView.swift
 */
@Composable
fun SatelliteModeView(
    trackingMode: SatelliteTrackingMode,
    store: SatelliteStore,
    onDismiss: () -> Unit
) {
    var showPredictionSheet by remember { mutableStateOf(false) }

    // Derived state: filter night passes when toggle is off
    val visiblePasses = if (trackingMode.showNightPasses) store.passes
    else store.passes.filter { it.dayNight != DayNight.NIGHT }

    // MARK: - Data Loading

    LaunchedEffect(trackingMode.mode) {
        when (trackingMode.mode) {
            SatelliteMode.TRACKER -> {
                store.loadTracks()
                if (trackingMode.selectedTrackId == null) {
                    store.tracks.firstOrNull()?.let { trackingMode.selectTrack(it.id) }
                }
            }
            SatelliteMode.COVERAGE -> {
                val regionId = trackingMode.regionId ?: return@LaunchedEffect
                store.loadCoverage(regionId)
                if (trackingMode.selectedPassId == null) {
                    visiblePasses.firstOrNull()?.let { trackingMode.selectPass(it.id) }
                }
            }
        }
    }

    // Reload coverage when region changes
    LaunchedEffect(trackingMode.regionId) {
        if (trackingMode.mode != SatelliteMode.COVERAGE) return@LaunchedEffect
        val regionId = trackingMode.regionId ?: return@LaunchedEffect
        store.loadCoverage(regionId)
        if (trackingMode.selectedPassId == null) {
            visiblePasses.firstOrNull()?.let { trackingMode.selectPass(it.id) }
        }
    }

    // Cleanup on dismiss
    DisposableEffect(Unit) {
        onDispose { store.clear() }
    }

    // MARK: - Layout

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar
        TopBar(
            trackingMode = trackingMode,
            isLoading = store.isLoading,
            onDismiss = onDismiss
        )

        // Push bottom panel down
        Spacer(modifier = Modifier.weight(1f))

        // Bottom panel (mode-specific)
        when (trackingMode.mode) {
            SatelliteMode.TRACKER -> {
                if (store.tracks.isNotEmpty()) {
                    TrackerPanel(
                        tracks = store.tracks,
                        selectedId = trackingMode.selectedTrackId,
                        onSelect = { trackingMode.selectTrack(it) }
                    )
                }
            }
            SatelliteMode.COVERAGE -> {
                Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Spacing.medium)) {
                    // Next pass prediction row
                    store.predictions?.let { coverage ->
                        NextPassRow(
                            coverage = coverage,
                            onTap = { showPredictionSheet = true }
                        )
                    }

                    // Recent passes
                    if (store.passes.isNotEmpty()) {
                        CoveragePanel(
                            summary = store.summary,
                            passes = visiblePasses,
                            selectedId = trackingMode.selectedPassId,
                            showNightPasses = trackingMode.showNightPasses,
                            onSelect = { trackingMode.selectPass(it) },
                            onToggleNightPasses = { trackingMode.showNightPasses = !trackingMode.showNightPasses }
                        )
                    } else if (store.error != null) {
                        ErrorCard(message = store.error!!)
                    }
                }
            }
        }
    }

    // Prediction sheet
    if (showPredictionSheet) {
        store.predictions?.let { coverage ->
            PassPredictionSheet(
                coverage = coverage,
                onDismiss = { showPredictionSheet = false }
            )
        }
    }
}

// MARK: - Top Bar

@Composable
private fun TopBar(
    trackingMode: SatelliteTrackingMode,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.medium)
            .padding(top = Spacing.medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Close button (48x48 tappable area)
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Close",
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        ModeToggle(trackingMode = trackingMode)

        Spacer(modifier = Modifier.weight(1f))

        // Loading indicator (matches 48dp close button)
        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

// MARK: - Mode Toggle

@Composable
private fun ModeToggle(trackingMode: SatelliteTrackingMode) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.85f))
            .padding(3.dp)
    ) {
        SatelliteMode.entries.forEach { mode ->
            val isSelected = trackingMode.mode == mode
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else Color.Transparent,
                animationSpec = tween(200),
                label = "modeBg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) Color.Black else Color.White.copy(alpha = 0.6f),
                animationSpec = tween(200),
                label = "modeText"
            )

            Text(
                text = mode.label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = textColor,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(bgColor)
                    .clickable { trackingMode.switchMode(mode) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

// MARK: - Error Card

@Composable
private fun ErrorCard(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.large)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.95f))
            .padding(Spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Spacing.medium)
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = Color.Yellow,
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = message,
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}
