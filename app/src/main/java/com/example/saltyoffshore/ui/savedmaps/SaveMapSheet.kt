package com.example.saltyoffshore.ui.savedmaps

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.data.waypoint.Crew
import com.example.saltyoffshore.ui.crew.StandardDragHandle
import com.example.saltyoffshore.ui.theme.SaltyColors
import com.example.saltyoffshore.ui.theme.SaltyType
import com.example.saltyoffshore.ui.theme.Spacing

/**
 * Bottom sheet for saving the current map configuration.
 * Port of iOS SaveMapSheet.swift.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveMapSheet(
    crews: List<Crew>,
    regionName: String?,
    datasetName: String?,
    isSaving: Boolean,
    onSave: (name: String, crewId: String?) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    var mapName by remember { mutableStateOf("") }
    var shareWithCrew by remember { mutableStateOf(false) }
    var selectedCrewId by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    // Clear crew selection when toggle is off
    LaunchedEffect(shareWithCrew) {
        if (!shareWithCrew) {
            selectedCrewId = null
        } else if (crews.size == 1) {
            selectedCrewId = crews.first().id
        }
    }

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
                .padding(horizontal = Spacing.extraLarge)
                .padding(top = Spacing.medium, bottom = Spacing.large),
        ) {
            // Header
            Text(
                text = "Save Map",
                style = SaltyType.heading,
                color = SaltyColors.textPrimary,
            )

            Spacer(Modifier.height(Spacing.large))

            // Name field
            OutlinedTextField(
                value = mapName,
                onValueChange = { mapName = it },
                label = { Text("Map Name") },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )

            Spacer(Modifier.height(Spacing.large))

            // Crew sharing toggle + picker
            if (crews.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        Icons.Default.People,
                        contentDescription = null,
                        tint = SaltyColors.textSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(Spacing.small))
                    Text(
                        text = "Share with Crew",
                        style = SaltyType.body,
                        color = SaltyColors.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = shareWithCrew,
                        onCheckedChange = { shareWithCrew = it },
                    )
                }

                if (shareWithCrew) {
                    Spacer(Modifier.height(Spacing.medium))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SaltyColors.raised),
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
            }

            // Config summary
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SaltyColors.raised)
                    .padding(Spacing.medium),
            ) {
                ConfigRow(title = "Region", value = regionName ?: "Unknown")
                Spacer(Modifier.height(Spacing.medium))
                ConfigRow(title = "Dataset", value = datasetName ?: "Unknown")
            }

            Spacer(Modifier.height(Spacing.large))

            // Save button
            val buttonText = if (shareWithCrew && selectedCrewId != null) {
                val crewName = crews.find { it.id == selectedCrewId }?.name ?: "Crew"
                "Save & Share to $crewName"
            } else {
                "Save Map"
            }

            val isDisabled = mapName.isBlank() || isSaving ||
                (shareWithCrew && selectedCrewId == null)

            Button(
                onClick = {
                    val crewId = if (shareWithCrew) selectedCrewId else null
                    onSave(mapName.trim(), crewId)
                    onDismiss()
                },
                enabled = !isDisabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(Spacing.small))
                }
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
            .padding(horizontal = Spacing.medium, vertical = Spacing.small + 4.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(SaltyColors.accent.copy(alpha = 0.2f)),
        ) {
            Icon(
                Icons.Default.People,
                contentDescription = null,
                tint = SaltyColors.accent,
                modifier = Modifier.size(14.dp),
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

// MARK: - Config Row

@Composable
private fun ConfigRow(title: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = SaltyType.bodySmall,
            color = SaltyColors.textSecondary,
        )
        Spacer(Modifier.width(Spacing.medium))
        Text(
            text = value,
            style = SaltyType.bodySmall,
            color = SaltyColors.textPrimary,
        )
    }
}
