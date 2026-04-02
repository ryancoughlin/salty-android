package com.example.saltyoffshore.ui.crew

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.auth.AuthManager
import com.example.saltyoffshore.data.waypoint.Crew
import com.example.saltyoffshore.data.waypoint.CrewMember
import com.example.saltyoffshore.ui.theme.SaltyColors
import com.example.saltyoffshore.ui.theme.SaltyType
import com.example.saltyoffshore.ui.theme.Spacing
import com.example.saltyoffshore.ui.theme.SplineSansMono

@Composable
fun CrewInfoView(
    crew: Crew,
    members: List<CrewMember>,
    isCreator: Boolean,
    onEditName: () -> Unit,
    onShowInviteCode: () -> Unit,
    onRemoveMember: (CrewMember) -> Unit,
    onLeaveCrew: () -> Unit,
    onDeleteCrew: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var memberToRemove by remember { mutableStateOf<CrewMember?>(null) }
    var showLeaveConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.large),
        verticalArrangement = Arrangement.spacedBy(Spacing.large),
    ) {
        // 1. Header
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = SaltyColors.textPrimary,
                    )
                }
                Spacer(Modifier.width(Spacing.small))
                Text("Crew Info", style = SaltyType.heading, color = SaltyColors.textPrimary)
            }
        }

        // 2. Crew Name
        item {
            SunkenCard {
                Row(
                    modifier = Modifier.padding(Spacing.large),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(SaltyColors.accent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            crew.initials,
                            style = SaltyType.heading,
                            color = Color.White,
                        )
                    }
                    Spacer(Modifier.width(Spacing.medium))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(crew.name, style = SaltyType.heading, color = SaltyColors.textPrimary)
                        Text(
                            "${members.size} members",
                            style = SaltyType.caption,
                            color = SaltyColors.textSecondary,
                        )
                    }
                    if (isCreator) {
                        IconButton(onClick = onEditName) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit name",
                                tint = SaltyColors.iconButton,
                            )
                        }
                    }
                }
            }
        }

        // 3. Members
        item {
            SectionHeader("MEMBERS")
        }
        item {
            SunkenCard {
                members.forEachIndexed { index, member ->
                    Row(
                        modifier = Modifier.padding(Spacing.large),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MemberAvatar(member, size = 36.dp)
                        Spacer(Modifier.width(Spacing.medium))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                member.userName ?: "Unknown",
                                style = SaltyType.body,
                                color = SaltyColors.textPrimary,
                            )
                            if (member.userId == crew.createdBy) {
                                Text(
                                    "Creator",
                                    style = SaltyType.caption,
                                    color = SaltyColors.accent,
                                )
                            }
                        }
                        if (isCreator && member.userId != AuthManager.currentUserId) {
                            IconButton(onClick = { memberToRemove = member }) {
                                Icon(
                                    Icons.Default.RemoveCircleOutline,
                                    contentDescription = "Remove member",
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                    if (index < members.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 60.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                        )
                    }
                }
            }
        }

        // 4. Invite Code
        item {
            SectionHeader("INVITE CODE")
        }
        item {
            SunkenCard {
                Row(
                    modifier = Modifier.padding(Spacing.large),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        crew.inviteCode.take(3) + " \u2013 " + crew.inviteCode.drop(3),
                        style = SaltyType.mono(20),
                        color = SaltyColors.textPrimary,
                    )
                    Spacer(Modifier.weight(1f))
                    OutlinedButton(onClick = onShowInviteCode) {
                        Text("Copy & Share")
                    }
                }
            }
        }

        // 5. Actions
        item {
            Spacer(Modifier.height(32.dp))
            if (!isCreator) {
                TextButton(onClick = { showLeaveConfirm = true }) {
                    Text("Leave Crew", color = MaterialTheme.colorScheme.error)
                }
            } else {
                TextButton(onClick = { showDeleteConfirm = true }) {
                    Text("Delete Crew", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    // Dialogs

    memberToRemove?.let { member ->
        AlertDialog(
            onDismissRequest = { memberToRemove = null },
            title = { Text("Remove Member") },
            text = { Text("Remove ${member.userName ?: "this member"} from ${crew.name}?") },
            confirmButton = {
                TextButton(onClick = { onRemoveMember(member); memberToRemove = null }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToRemove = null }) { Text("Cancel") }
            },
        )
    }

    if (showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirm = false },
            title = { Text("Leave Crew") },
            text = { Text("You'll no longer see shared waypoints from ${crew.name}.") },
            confirmButton = {
                TextButton(onClick = { onLeaveCrew(); showLeaveConfirm = false }) {
                    Text("Leave", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirm = false }) { Text("Cancel") }
            },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Crew") },
            text = { Text("This will remove all members and shared waypoints. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { onDeleteCrew(); showDeleteConfirm = false }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

// ── Private composables ─────────────────────────────────────────────────────

@Composable
private fun MemberAvatar(member: CrewMember, size: Dp = 36.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(SaltyColors.accent),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            member.initials,
            style = SaltyType.caption,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun SunkenCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(SaltyColors.sunken),
    ) {
        content()
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = SaltyType.caption,
        color = SaltyColors.textSecondary,
        modifier = Modifier.padding(start = Spacing.small),
    )
}
