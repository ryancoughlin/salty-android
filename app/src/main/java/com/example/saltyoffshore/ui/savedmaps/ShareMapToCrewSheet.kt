package com.example.saltyoffshore.ui.savedmaps

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
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import com.example.saltyoffshore.data.SavedMap
import com.example.saltyoffshore.data.waypoint.Crew
import com.example.saltyoffshore.ui.crew.StandardDragHandle
import com.example.saltyoffshore.ui.theme.SaltyColors
import com.example.saltyoffshore.ui.theme.SaltyType
import com.example.saltyoffshore.ui.theme.Spacing

/**
 * Bottom sheet for sharing a saved map to a single crew.
 * Port of iOS ShareMapToCrewSheet.swift.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareMapToCrewSheet(
    map: SavedMap,
    crews: List<Crew>,
    onShare: (crewId: String) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(),
) {
    var selectedCrewId by remember { mutableStateOf<String?>(null) }

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
                text = "Share to Crew",
                style = SaltyType.heading,
                color = SaltyColors.textPrimary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(Spacing.extraLarge))

            // Map info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(SaltyColors.accent.copy(alpha = 0.1f)),
                ) {
                    Icon(
                        Icons.Default.Map,
                        contentDescription = null,
                        tint = SaltyColors.accent,
                        modifier = Modifier.size(28.dp),
                    )
                }

                Spacer(Modifier.height(Spacing.medium))

                Text(
                    text = map.name,
                    style = SaltyType.body,
                    color = SaltyColors.textPrimary,
                )
                if (map.regionName != null) {
                    Text(
                        text = map.regionName,
                        style = SaltyType.bodySmall,
                        color = SaltyColors.textSecondary,
                    )
                }
            }

            Spacer(Modifier.height(Spacing.extraLarge))

            // Crew selection
            Text(
                text = "SELECT CREW",
                style = SaltyType.caption,
                color = SaltyColors.textSecondary,
                modifier = Modifier.padding(bottom = Spacing.medium),
            )

            if (crews.isEmpty()) {
                EmptyCrewsState()
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SaltyColors.sunken),
                ) {
                    crews.forEachIndexed { index, crew ->
                        CrewRadioRow(
                            crew = crew,
                            isSelected = crew.id == selectedCrewId,
                            onSelect = {
                                selectedCrewId = if (selectedCrewId == crew.id) null else crew.id
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
            val selectedCrew = crews.find { it.id == selectedCrewId }
            val buttonText = selectedCrew?.let { "Share to ${it.name}" } ?: "Select a Crew"

            Button(
                onClick = {
                    selectedCrewId?.let { crewId ->
                        onShare(crewId)
                        onDismiss()
                    }
                },
                enabled = selectedCrewId != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.large),
            ) {
                Text(buttonText)
            }
        }
    }
}

// MARK: - Crew Radio Row

@Composable
private fun CrewRadioRow(
    crew: Crew,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(Spacing.medium),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(SaltyColors.accent.copy(alpha = 0.2f)),
        ) {
            Icon(
                Icons.Default.People,
                contentDescription = null,
                tint = SaltyColors.accent,
            )
        }

        Spacer(Modifier.width(Spacing.medium))

        Text(
            text = crew.name,
            style = SaltyType.body,
            color = SaltyColors.textPrimary,
            modifier = Modifier.weight(1f),
        )

        Icon(
            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isSelected) SaltyColors.accent else SaltyColors.textSecondary,
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
            text = "Create or join a crew to share maps",
            style = SaltyType.bodySmall,
            color = SaltyColors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}
