package com.example.saltyoffshore.ui.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Tools menu sheet matching iOS MapToolBar.
 *
 * Sections:
 * 1. Tools grid (3-col): quick action buttons
 * 2. My Stuff: full-width rows for managing content
 * 3. More: additional tools
 *
 * iOS ref: Map/Controls/MapToolBar.swift
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapToolBar(
    onAddWaypoint: () -> Unit,
    onSatellites: () -> Unit,
    onMyLocation: () -> Unit,
    onShare: () -> Unit,
    onWaypoints: () -> Unit,
    onCrews: () -> Unit,
    onSavedMaps: () -> Unit,
    onSaveMap: () -> Unit,
    onCreateCrew: () -> Unit,
    onJoinCrew: () -> Unit,
    onDatasetGuide: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Tools",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                actions = {
                    TextButton(onClick = onDismiss) {
                        Text("Done")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .padding(innerPadding)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                MapToolButton(
                    icon = Icons.Default.Place,
                    label = "Add Waypoint",
                    onClick = onAddWaypoint
                )
            }
            item {
                MapToolButton(
                    icon = Icons.Default.SatelliteAlt,
                    label = "Satellites",
                    onClick = onSatellites
                )
            }
            item {
                MapToolButton(
                    icon = Icons.Default.MyLocation,
                    label = "My Location",
                    onClick = onMyLocation
                )
            }
            item {
                MapToolButton(
                    icon = Icons.Default.Share,
                    label = "Share",
                    onClick = onShare
                )
            }
            item {
                MapToolButton(
                    icon = Icons.Default.BookmarkAdd,
                    label = "Save Map",
                    onClick = onSaveMap
                )
            }

            // -- My Stuff Section --
            item(span = { GridItemSpan(3) }) {
                Spacer(Modifier.height(8.dp))
            }
            item(span = { GridItemSpan(3) }) {
                SectionHeader("My Stuff")
            }
            item(span = { GridItemSpan(3) }) {
                SecondaryToolButton(
                    icon = Icons.Default.Place,
                    title = "Waypoints",
                    subtitle = "View and organize your spots",
                    onClick = onWaypoints
                )
            }
            item(span = { GridItemSpan(3) }) {
                SecondaryToolButton(
                    icon = Icons.Default.Group,
                    title = "Crews",
                    subtitle = "Share waypoints in real-time",
                    onClick = onCrews
                )
            }
            item(span = { GridItemSpan(3) }) {
                SecondaryToolButton(
                    icon = Icons.Default.Map,
                    title = "Saved Maps",
                    subtitle = "Browse your saved views",
                    onClick = onSavedMaps
                )
            }
            // -- More Section --
            item(span = { GridItemSpan(3) }) {
                Spacer(Modifier.height(8.dp))
            }
            item(span = { GridItemSpan(3) }) {
                SectionHeader("More")
            }
            item(span = { GridItemSpan(3) }) {
                SecondaryToolButton(
                    icon = Icons.Default.Info,
                    title = "Dataset Guide",
                    subtitle = "Learn about ocean data layers",
                    onClick = onDatasetGuide
                )
            }
            item(span = { GridItemSpan(3) }) {
                SecondaryToolButton(
                    icon = Icons.Default.GroupAdd,
                    title = "Create Crew",
                    subtitle = "Start a new crew",
                    onClick = onCreateCrew
                )
            }
            item(span = { GridItemSpan(3) }) {
                SecondaryToolButton(
                    icon = Icons.Default.PersonAdd,
                    title = "Join Crew",
                    subtitle = "Enter an invite code",
                    onClick = onJoinCrew
                )
            }

            // Bottom spacing
            item(span = { GridItemSpan(3) }) {
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}
