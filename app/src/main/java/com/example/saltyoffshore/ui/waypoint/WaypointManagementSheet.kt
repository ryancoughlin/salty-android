package com.example.saltyoffshore.ui.waypoint

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.data.waypoint.Waypoint
import com.example.saltyoffshore.data.waypoint.WaypointSection
import com.example.saltyoffshore.data.waypoint.WaypointSortOption
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Waypoint management bottom sheet — ports iOS WaypointManagementView.
 *
 * Full waypoint list with search, sort, swipe-to-delete, and section headers.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WaypointManagementSheet(
    sections: List<WaypointSection>,
    sortOption: WaypointSortOption,
    selectedWaypointId: String?,
    onSortOptionChanged: (WaypointSortOption) -> Unit,
    onWaypointTap: (String) -> Unit,
    onWaypointDelete: (Waypoint) -> Unit,
    onImportGPX: () -> Unit,
    onDismiss: () -> Unit
) {
    var searchText by rememberSaveable { mutableStateOf("") }

    // Filter sections by search text
    val filteredSections = remember(sections, searchText) {
        if (searchText.isBlank()) sections
        else sections.map { section ->
            section.copy(
                waypoints = section.waypoints.filter { wp ->
                    (wp.name ?: "").contains(searchText, ignoreCase = true) ||
                        (wp.notes ?: "").contains(searchText, ignoreCase = true)
                }
            )
        }.filter { it.waypoints.isNotEmpty() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Header ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Waypoints",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            // ── Search bar ──────────────────────────────────────────────────
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = { Text("Search waypoints") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchText.isNotEmpty()) {
                        IconButton(onClick = { searchText = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Sort control ────────────────────────────────────────────────
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                WaypointSortOption.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = sortOption == option,
                        onClick = { onSortOptionChanged(option) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = WaypointSortOption.entries.size
                        )
                    ) {
                        Text(option.displayName)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Content ─────────────────────────────────────────────────────
            if (filteredSections.isEmpty()) {
                WaypointEmptyState(
                    isFiltered = searchText.isNotEmpty(),
                    onClearSearch = { searchText = "" },
                    onImportGPX = onImportGPX
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    filteredSections.forEach { section ->
                        stickyHeader(key = "header-${section.id}") {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.background
                            ) {
                                Text(
                                    text = section.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                        items(
                            items = section.waypoints,
                            key = { it.id }
                        ) { waypoint ->
                            WaypointListRow(
                                waypoint = waypoint,
                                isSelected = waypoint.id == selectedWaypointId,
                                onClick = { onWaypointTap(waypoint.id) },
                                onDelete = { onWaypointDelete(waypoint) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── WaypointListRow ─────────────────────────────────────────────────────────

@Composable
private fun WaypointListRow(
    waypoint: Waypoint,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                showDeleteDialog = true
                false // Don't dismiss yet — wait for confirmation
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color by animateColorAsState(
                targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.surface
                },
                label = "swipe_bg"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onError
                )
            }
        },
        enableDismissFromStartToEnd = false
    ) {
        Surface(
            color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Symbol icon
                val iconResId = context.resources.getIdentifier(
                    waypoint.symbol.imageName.lowercase().replace(" ", "_"),
                    "drawable",
                    context.packageName
                )
                if (iconResId != 0) {
                    Image(
                        painter = painterResource(id = iconResId),
                        contentDescription = waypoint.symbol.rawValue,
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    Box(modifier = Modifier.size(32.dp))
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Text content
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = waypoint.name ?: "Unnamed Waypoint",
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatWaypointSubtitle(waypoint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Chevron
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    // Delete confirmation
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Remove this waypoint?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ── WaypointEmptyState ──────────────────────────────────────────────────────

@Composable
private fun WaypointEmptyState(
    isFiltered: Boolean,
    onClearSearch: () -> Unit,
    onImportGPX: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.LocationOff,
            contentDescription = null,
            modifier = Modifier.size(50.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isFiltered) "No Matches Found" else "No Waypoints Yet",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isFiltered) "Try adjusting your search terms"
            else "Drop pins on the map or import from GPX files",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isFiltered) {
            OutlinedButton(onClick = onClearSearch) {
                Text("Clear Search")
            }
        } else {
            OutlinedButton(onClick = onImportGPX) {
                Text("Import GPX")
            }
        }
    }
}

// ── Date formatting helper ──────────────────────────────────────────────────

private val abbreviatedDateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

private fun formatWaypointSubtitle(waypoint: Waypoint): String {
    return try {
        val instant = Instant.parse(waypoint.createdAt)
        val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
        val today = LocalDate.now()

        when {
            localDate == today -> "Today"
            localDate == today.minusDays(1) -> "Yesterday"
            else -> abbreviatedDateFormatter.format(localDate)
        }
    } catch (_: Exception) {
        waypoint.createdAt.take(10)
    }
}
