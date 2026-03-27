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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MyLocation
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
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // -- Tools Section --
            item(span = { GridItemSpan(3) }) {
                SectionHeader("Tools")
            }

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
                    icon = Icons.Default.Bookmark,
                    title = "Saved Maps",
                    subtitle = "Your saved map configurations",
                    onClick = { /* TODO: Saved maps */ }
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
