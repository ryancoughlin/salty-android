package com.example.saltyoffshore.ui.crew

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.data.waypoint.Crew
import com.example.saltyoffshore.data.waypoint.Waypoint
import com.example.saltyoffshore.ui.theme.SaltyColors
import com.example.saltyoffshore.ui.theme.SaltyType
import com.example.saltyoffshore.ui.theme.Spacing

/**
 * Bottom sheet for sharing a waypoint to one or more crews.
 * Port of iOS ShareWaypointSheet.swift.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareWaypointSheet(
    waypoint: Waypoint,
    crews: List<Crew>,
    onShare: (List<String>) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(),
) {
    var selectedCrewIds by remember { mutableStateOf(emptySet<String>()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SaltyColors.overlay,
        dragHandle = { StandardDragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.large),
        ) {
            // Header
            Text(
                text = "Share Waypoint",
                style = SaltyType.heading,
                color = SaltyColors.textPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.large),
                textAlign = TextAlign.Center,
            )

            // Waypoint info row
            WaypointInfoRow(waypoint)

            HorizontalDivider(
                color = SaltyColors.borderSubtle,
                modifier = Modifier.padding(vertical = Spacing.large),
            )

            // Crew selection
            if (crews.isEmpty()) {
                EmptyCrewsState()
            } else {
                Text(
                    text = "SELECT CREWS",
                    style = SaltyType.caption,
                    color = SaltyColors.textSecondary,
                    modifier = Modifier.padding(bottom = Spacing.medium),
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SaltyColors.sunken),
                ) {
                    crews.forEachIndexed { index, crew ->
                        CrewCheckboxRow(
                            crew = crew,
                            isSelected = crew.id in selectedCrewIds,
                            onToggle = {
                                selectedCrewIds = if (crew.id in selectedCrewIds) {
                                    selectedCrewIds - crew.id
                                } else {
                                    selectedCrewIds + crew.id
                                }
                            },
                        )
                        if (index < crews.lastIndex) {
                            HorizontalDivider(
                                color = SaltyColors.borderSubtle,
                                modifier = Modifier.padding(start = 48.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(Spacing.large))

            // Share button
            Button(
                onClick = {
                    onShare(selectedCrewIds.toList())
                    onDismiss()
                },
                enabled = selectedCrewIds.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.large),
            ) {
                Text(shareButtonText(selectedCrewIds.size))
            }
        }
    }
}

// MARK: - Waypoint Info Row

@Composable
private fun WaypointInfoRow(waypoint: Waypoint) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(SaltyColors.accent.copy(alpha = 0.1f)),
        ) {
            Icon(
                Icons.Default.People,
                contentDescription = null,
                tint = SaltyColors.accent,
                modifier = Modifier.size(24.dp),
            )
        }

        Spacer(Modifier.width(Spacing.medium))

        Column {
            Text(
                text = waypoint.name ?: "Unnamed",
                style = SaltyType.body,
                color = SaltyColors.textPrimary,
            )
            Text(
                text = formatCoordinates(waypoint.latitude, waypoint.longitude),
                style = SaltyType.mono(12),
                color = SaltyColors.textSecondary,
            )
        }
    }
}

// MARK: - Crew Checkbox Row

@Composable
private fun CrewCheckboxRow(
    crew: Crew,
    isSelected: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = Spacing.medium, vertical = Spacing.medium),
    ) {
        Icon(
            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isSelected) SaltyColors.accent else SaltyColors.textSecondary,
            modifier = Modifier.size(24.dp),
        )

        Spacer(Modifier.width(Spacing.medium))

        // Crew initials circle
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(SaltyColors.accent.copy(alpha = 0.15f)),
        ) {
            Text(
                text = crew.initials,
                style = SaltyType.caption,
                color = SaltyColors.accent,
            )
        }

        Spacer(Modifier.width(Spacing.medium))

        Text(
            text = crew.name,
            style = SaltyType.body,
            color = SaltyColors.textPrimary,
        )
    }
}

// MARK: - Empty Crews State

@Composable
private fun EmptyCrewsState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SaltyColors.sunken)
            .padding(Spacing.extraLarge),
    ) {
        Icon(
            Icons.Default.People,
            contentDescription = null,
            tint = SaltyColors.textSecondary,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(Spacing.medium))
        Text(
            text = "No Crews Yet",
            style = SaltyType.body,
            color = SaltyColors.textPrimary,
        )
        Spacer(Modifier.height(Spacing.small))
        Text(
            text = "Create or join a crew to share waypoints",
            style = SaltyType.bodySmall,
            color = SaltyColors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

// MARK: - Shared Components

@Composable
internal fun StandardDragHandle() {
    Box(
        modifier = Modifier
            .padding(vertical = 12.dp)
            .width(36.dp)
            .height(4.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)),
    )
}

// MARK: - Helpers

private fun shareButtonText(count: Int): String {
    if (count == 0) return "Select Crews"
    val label = if (count == 1) "Crew" else "Crews"
    return "Share to $count $label"
}

private fun formatCoordinates(lat: Double, lon: Double): String {
    val latDir = if (lat >= 0) "N" else "S"
    val lonDir = if (lon >= 0) "E" else "W"
    return "%.4f%s %.4f%s".format(
        kotlin.math.abs(lat), latDir,
        kotlin.math.abs(lon), lonDir,
    )
}
