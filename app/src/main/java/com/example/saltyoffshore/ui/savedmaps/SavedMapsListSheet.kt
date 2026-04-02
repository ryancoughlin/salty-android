package com.example.saltyoffshore.ui.savedmaps

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.data.SavedMap
import com.example.saltyoffshore.data.waypoint.Crew
import com.example.saltyoffshore.ui.crew.StandardDragHandle
import com.example.saltyoffshore.ui.theme.SaltyColors
import com.example.saltyoffshore.ui.theme.SaltyType
import com.example.saltyoffshore.ui.theme.Spacing

/**
 * Bottom sheet listing all saved maps, sectioned by personal and crew.
 * Port of iOS SavedMapListView.swift.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedMapsListSheet(
    savedMaps: List<SavedMap>,
    crews: List<Crew>,
    currentUserId: String?,
    isLoading: Boolean,
    onLoadMap: (SavedMap) -> Unit,
    onDeleteMap: (String) -> Unit,
    onShareToCrew: (String, String, String?) -> Unit,
    onUnshare: (String) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    var mapToDelete by remember { mutableStateOf<SavedMap?>(null) }
    var mapToUnshare by remember { mutableStateOf<SavedMap?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SaltyColors.overlay,
        dragHandle = { StandardDragHandle() },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top bar: title + done button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.large),
            ) {
                Text(
                    text = "My Maps",
                    style = SaltyType.heading,
                    color = SaltyColors.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onDismiss) {
                    Text("Done", style = SaltyType.bodySmall)
                }
            }

            Spacer(Modifier.height(Spacing.medium))

            // Content
            when {
                savedMaps.isEmpty() && !isLoading -> {
                    SavedMapsEmptyState()
                }
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = SaltyColors.accent)
                    }
                }
                else -> {
                    val personalMaps = savedMaps.filter { it.crewId == null }
                    val crewGroups = buildCrewMapGroups(savedMaps, crews)

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = Spacing.large),
                    ) {
                        // Personal maps section
                        if (personalMaps.isNotEmpty()) {
                            item { SectionHeader("My Maps", Icons.Default.Map, personalMaps.size) }
                            items(personalMaps, key = { it.id }) { map ->
                                SavedMapCard(
                                    map = map,
                                    isOwner = map.isOwnedBy(currentUserId),
                                    onClick = { onLoadMap(map) },
                                    onDelete = if (map.isOwnedBy(currentUserId)) {
                                        { mapToDelete = map }
                                    } else null,
                                )
                                Spacer(Modifier.height(Spacing.medium))
                            }
                        }

                        // Crew sections
                        crewGroups.forEach { (crew, maps) ->
                            item { SectionHeader(crew.name, Icons.Default.People, maps.size) }
                            items(maps, key = { it.id }) { map ->
                                SavedMapCard(
                                    map = map,
                                    isOwner = map.isOwnedBy(currentUserId),
                                    onClick = { onLoadMap(map) },
                                    onUnshare = if (map.isOwnedBy(currentUserId)) {
                                        { mapToUnshare = map }
                                    } else null,
                                    onDelete = if (map.isOwnedBy(currentUserId)) {
                                        { mapToDelete = map }
                                    } else null,
                                )
                                Spacer(Modifier.height(Spacing.medium))
                            }
                        }

                        item { Spacer(Modifier.height(Spacing.extraLarge)) }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (mapToDelete != null) {
        AlertDialog(
            onDismissRequest = { mapToDelete = null },
            title = { Text("Delete Map?", style = SaltyType.heading) },
            text = {
                Text(
                    "\"${mapToDelete?.name}\" will be permanently deleted.",
                    style = SaltyType.body,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    mapToDelete?.let { onDeleteMap(it.id) }
                    mapToDelete = null
                }) {
                    Text("Delete", color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { mapToDelete = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    // Unshare confirmation dialog
    if (mapToUnshare != null) {
        AlertDialog(
            onDismissRequest = { mapToUnshare = null },
            title = { Text("Remove from Crew?", style = SaltyType.heading) },
            text = {
                Text(
                    "\"${mapToUnshare?.name}\" will be removed from the crew and return to your personal maps.",
                    style = SaltyType.body,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    mapToUnshare?.let { onUnshare(it.id) }
                    mapToUnshare = null
                }) {
                    Text("Remove", color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { mapToUnshare = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

// MARK: - Section Header

@Composable
private fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = Spacing.medium),
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.height(12.dp).width(12.dp),
            tint = SaltyColors.textSecondary,
        )
        Spacer(Modifier.width(Spacing.small))
        Text(
            text = title.uppercase(),
            style = SaltyType.caption,
            color = SaltyColors.textSecondary,
        )
        Spacer(Modifier.width(Spacing.small))
        Text(
            text = "($count)",
            style = SaltyType.caption,
            color = SaltyColors.textSecondary,
        )
    }
}

// MARK: - Helpers

private fun buildCrewMapGroups(
    savedMaps: List<SavedMap>,
    crews: List<Crew>,
): List<Pair<Crew, List<SavedMap>>> {
    val crewMaps = savedMaps.filter { it.crewId != null }
    val crewById = crews.associateBy { it.id }
    return crewMaps
        .groupBy { it.crewId }
        .mapNotNull { (crewId, maps) ->
            val crew = crewById[crewId] ?: return@mapNotNull null
            crew to maps
        }
}
