package com.example.saltyoffshore.ui.crew

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.data.SavedMap
import com.example.saltyoffshore.data.waypoint.Crew
import com.example.saltyoffshore.data.waypoint.CrewMember
import com.example.saltyoffshore.data.waypoint.SharedWaypoint
import com.example.saltyoffshore.ui.theme.SaltyColors
import com.example.saltyoffshore.ui.theme.SaltyType
import com.example.saltyoffshore.ui.theme.Spacing

// ── Inline navigation ──────────────────────────────────────────────────────

sealed class CrewSheetNav {
    data object List : CrewSheetNav()
    data class Detail(val crew: Crew) : CrewSheetNav()
    data class Info(val crew: Crew) : CrewSheetNav()
}

// ── CrewListSheet ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrewListSheet(
    crews: List<Crew>,
    crewWaypoints: List<SharedWaypoint>,
    savedMaps: List<SavedMap>,
    selectedCrew: Crew?,
    selectedCrewMembers: List<CrewMember>,
    isCreator: Boolean,
    hasDisplayName: Boolean,
    onSelectCrew: (Crew?) -> Unit,
    onCreateCrew: (String, (Crew) -> Unit) -> Unit,
    onJoinCrew: (String, (Crew) -> Unit, (Exception) -> Unit) -> Unit,
    onLeaveCrew: (Crew, () -> Unit) -> Unit,
    onDeleteCrew: (Crew, () -> Unit) -> Unit,
    onRemoveMember: (String, String) -> Unit,
    onUpdateCrewName: (String, String, () -> Unit) -> Unit,
    onSaveName: suspend (String, String) -> Unit,
    onWaypointTap: (SharedWaypoint) -> Unit,
    onLoadMap: (SavedMap) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    var nav by remember { mutableStateOf<CrewSheetNav>(CrewSheetNav.List) }
    var showCreateSheet by remember { mutableStateOf(false) }
    var showJoinSheet by remember { mutableStateOf(false) }
    var showEditNameSheet by remember { mutableStateOf(false) }
    var showInviteCodeSheet by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SaltyColors.overlay,
        dragHandle = { BottomSheetDragHandle() },
    ) {
        AnimatedContent(
            targetState = nav,
            transitionSpec = {
                if (targetState is CrewSheetNav.List) {
                    slideInHorizontally { -it } + fadeIn() togetherWith
                        slideOutHorizontally { it } + fadeOut()
                } else {
                    slideInHorizontally { it } + fadeIn() togetherWith
                        slideOutHorizontally { -it } + fadeOut()
                }
            },
            label = "crew_sheet_nav",
        ) { currentNav ->
            when (currentNav) {
                is CrewSheetNav.List -> {
                    CrewListContent(
                        crews = crews,
                        onCrewTap = { crew -> nav = CrewSheetNav.Detail(crew) },
                        onCreateCrew = { showCreateSheet = true },
                        onJoinCrew = { showJoinSheet = true },
                        onDismiss = onDismiss,
                    )
                }

                is CrewSheetNav.Detail -> {
                    val crew = currentNav.crew
                    LaunchedEffect(crew.id) { onSelectCrew(crew) }
                    CrewDetailView(
                        crew = crew,
                        members = selectedCrewMembers,
                        isCreator = isCreator,
                        crewWaypoints = crewWaypoints,
                        crewMaps = savedMaps,
                        onInfo = { nav = CrewSheetNav.Info(crew) },
                        onBack = { nav = CrewSheetNav.List; onSelectCrew(null) },
                        onWaypointTap = onWaypointTap,
                        onLoadMap = onLoadMap,
                    )
                }

                is CrewSheetNav.Info -> {
                    val crew = currentNav.crew
                    CrewInfoView(
                        crew = crew,
                        members = selectedCrewMembers,
                        isCreator = isCreator,
                        onEditName = { showEditNameSheet = true },
                        onShowInviteCode = { showInviteCodeSheet = true },
                        onRemoveMember = { member ->
                            onRemoveMember(crew.id, member.userId)
                        },
                        onLeaveCrew = { onLeaveCrew(crew) { nav = CrewSheetNav.List } },
                        onDeleteCrew = { onDeleteCrew(crew) { nav = CrewSheetNav.List } },
                        onBack = { nav = CrewSheetNav.Detail(crew) },
                    )
                }
            }
        }
    }

    // ── Sub-sheets ──────────────────────────────────────────────────────────

    if (showCreateSheet) {
        CreateCrewSheet(
            onDismiss = { showCreateSheet = false },
            onCrewCreated = { showCreateSheet = false },
            onSaveName = onSaveName,
            hasDisplayName = hasDisplayName,
        )
    }

    if (showJoinSheet) {
        JoinCrewSheet(
            onDismiss = { showJoinSheet = false },
            onCrewJoined = { crew ->
                showJoinSheet = false
                nav = CrewSheetNav.Detail(crew)
            },
            onSaveName = onSaveName,
            hasDisplayName = hasDisplayName,
        )
    }

    if (showEditNameSheet) {
        val crew = (nav as? CrewSheetNav.Info)?.crew
        crew?.let {
            EditCrewNameSheet(
                currentName = it.name,
                onSave = { newName -> onUpdateCrewName(it.id, newName) {} },
                onDismiss = { showEditNameSheet = false },
            )
        }
    }

    if (showInviteCodeSheet) {
        val crew = (nav as? CrewSheetNav.Info)?.crew
        crew?.let {
            InviteCodeSheet(
                crew = it,
                onDismiss = { showInviteCodeSheet = false },
            )
        }
    }
}

// ── List content ───────────────────────────────────────────────────────────

@Composable
private fun CrewListContent(
    crews: List<Crew>,
    onCrewTap: (Crew) -> Unit,
    onCreateCrew: () -> Unit,
    onJoinCrew: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.large),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Crews", style = SaltyType.heading, color = SaltyColors.textPrimary)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onDismiss) {
                Text("Done", style = SaltyType.bodySmall, color = SaltyColors.accent)
            }
        }

        if (crews.isEmpty()) {
            CrewEmptyState()
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = Spacing.large,
                    vertical = Spacing.medium,
                ),
            ) {
                items(crews, key = { it.id }) { crew ->
                    CrewRow(crew = crew, onClick = { onCrewTap(crew) })
                }
            }
        }

        // Bottom buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.large),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = onCreateCrew,
                modifier = Modifier.weight(1f),
            ) {
                Text("Create Crew")
            }
            OutlinedButton(
                onClick = onJoinCrew,
                modifier = Modifier.weight(1f),
            ) {
                Text("Join Crew")
            }
        }
    }
}

// ── Crew row ───────────────────────────────────────────────────────────────

@Composable
private fun CrewRow(crew: Crew, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(SaltyColors.sunken)
            .clickable(onClick = onClick)
            .padding(Spacing.large),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(SaltyColors.accent),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                crew.initials,
                style = SaltyType.bodySmall,
                color = Color.White,
            )
        }

        Spacer(Modifier.width(Spacing.medium))

        // Name
        Column(modifier = Modifier.weight(1f)) {
            Text(crew.name, style = SaltyType.body, color = SaltyColors.textPrimary)
        }

        // Chevron
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = SaltyColors.textSecondary,
        )
    }
}

// ── Empty state ────────────────────────────────────────────────────────────

@Composable
private fun CrewEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Default.Group,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = SaltyColors.textSecondary,
        )
        Spacer(Modifier.height(Spacing.medium))
        Text(
            "No Crews Yet",
            style = SaltyType.heading,
            color = SaltyColors.textPrimary,
        )
        Spacer(Modifier.height(Spacing.small))
        Text(
            "Create or join a crew to share waypoints",
            style = SaltyType.caption,
            color = SaltyColors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

// ── Drag handle ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomSheetDragHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 4.dp)
                .clip(MaterialTheme.shapes.small)
                .background(SaltyColors.textSecondary.copy(alpha = 0.4f)),
        )
    }
}
