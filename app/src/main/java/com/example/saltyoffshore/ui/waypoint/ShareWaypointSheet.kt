package com.example.saltyoffshore.ui.waypoint

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api
import com.example.saltyoffshore.data.waypoint.Crew
import com.example.saltyoffshore.data.waypoint.Waypoint

/**
 * Bottom sheet for sharing a waypoint to one or more crews.
 * Port of iOS ShareWaypointSheet.swift.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareWaypointSheet(
    waypoint: Waypoint,
    crews: List<Crew>,
    onShare: (waypointId: String, crewIds: List<String>) -> Unit,
    onDismiss: () -> Unit,
    isSharing: Boolean = false
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedCrewIds by remember { mutableStateOf(emptySet<String>()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // -- Waypoint info header --
            WaypointInfoHeader(waypoint)

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // -- Crew selection label --
            Text(
                text = "SELECT CREWS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )

            // -- Crew list or empty state --
            if (crews.isEmpty()) {
                EmptyCrewsView()
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(horizontal = 16.dp)
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    items(crews, key = { it.id }) { crew ->
                        CrewSelectionRow(
                            crew = crew,
                            isSelected = crew.id in selectedCrewIds,
                            onToggle = {
                                selectedCrewIds = if (crew.id in selectedCrewIds) {
                                    selectedCrewIds - crew.id
                                } else {
                                    selectedCrewIds + crew.id
                                }
                            }
                        )
                        if (crew != crews.last()) {
                            HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
                        }
                    }
                }
            }

            // -- Share button (sticky bottom) --
            Surface(
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { onShare(waypoint.id, selectedCrewIds.toList()) },
                    enabled = selectedCrewIds.isNotEmpty() && !isSharing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    if (isSharing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(shareButtonText(selectedCrewIds.size))
                }
            }
        }
    }
}

// MARK: - Waypoint Info Header

@Composable
private fun WaypointInfoHeader(waypoint: Waypoint) {
    val context = LocalContext.current
    val iconResId = context.resources.getIdentifier(
        waypoint.symbol.imageName.lowercase().replace(" ", "_"),
        "drawable",
        context.packageName
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(16.dp)
    ) {
        // Symbol icon in circle
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            modifier = Modifier.size(44.dp)
        ) {
            if (iconResId != 0) {
                Image(
                    painter = painterResource(id = iconResId),
                    contentDescription = waypoint.symbol.rawValue,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(24.dp)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column {
            Text(
                text = waypoint.name ?: "Unnamed",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = formatCoordinates(waypoint.latitude, waypoint.longitude),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// MARK: - Crew Selection Row

@Composable
private fun CrewSelectionRow(
    crew: Crew,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        // Crew icon
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                Icons.Default.People,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(8.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Text(
            text = crew.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )

        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() }
        )
    }
}

// MARK: - Empty Crews View

@Composable
private fun EmptyCrewsView() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp)
    ) {
        Icon(
            Icons.Default.GroupAdd,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "No Crews Yet",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Join or create a crew to share waypoints",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// MARK: - Helpers

private fun shareButtonText(count: Int): String {
    return if (count == 0) {
        "Select Crews"
    } else {
        val label = if (count == 1) "Crew" else "Crews"
        "Share to $count $label"
    }
}

private fun formatCoordinates(lat: Double, lon: Double): String {
    val latDir = if (lat >= 0) "N" else "S"
    val lonDir = if (lon >= 0) "E" else "W"
    return "%.4f%s %.4f%s".format(
        kotlin.math.abs(lat), latDir,
        kotlin.math.abs(lon), lonDir
    )
}
