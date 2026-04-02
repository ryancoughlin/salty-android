package com.example.saltyoffshore.ui.crew

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saltyoffshore.data.SavedMap
import com.example.saltyoffshore.data.waypoint.Crew
import com.example.saltyoffshore.data.waypoint.CrewMember
import com.example.saltyoffshore.data.waypoint.SharedWaypoint
import com.example.saltyoffshore.ui.savedmaps.SavedMapCard
import com.example.saltyoffshore.ui.theme.SaltyColors
import com.example.saltyoffshore.ui.theme.SaltyType
import com.example.saltyoffshore.ui.theme.Spacing

@Composable
fun CrewDetailView(
    crew: Crew,
    members: List<CrewMember>,
    isCreator: Boolean,
    crewWaypoints: List<SharedWaypoint>,
    crewMaps: List<SavedMap>,
    onInfo: () -> Unit,
    onBack: () -> Unit,
    onWaypointTap: (SharedWaypoint) -> Unit,
    onLoadMap: (SavedMap) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasContent = crewMaps.isNotEmpty() || crewWaypoints.isNotEmpty()

    LazyColumn(modifier = modifier.fillMaxWidth()) {
        // 1. Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(Spacing.large),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(Modifier.weight(1f))
                LazyRow {
                    itemsIndexed(members.take(4)) { index, member ->
                        Box(modifier = Modifier.offset(x = (-8).dp * index)) {
                            MemberAvatar(member)
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onInfo) {
                    Icon(Icons.Default.Info, contentDescription = "Info")
                }
            }
        }

        // 2. Title Section
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.large),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    crew.name,
                    style = SaltyType.headingLarge,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "${crewMaps.size} maps \u00B7 ${crewWaypoints.size} spots",
                    style = SaltyType.caption,
                    color = SaltyColors.textSecondary,
                )
            }
        }

        // 3. Shared Maps Section
        if (crewMaps.isNotEmpty()) {
            item { Spacer(Modifier.height(Spacing.extraLarge)) }
            item { SectionHeader("SHARED MAPS") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Spacing.large),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                ) {
                    items(crewMaps, key = { it.id }) { map ->
                        SavedMapCard(
                            map = map,
                            isOwner = false,
                            onClick = { onLoadMap(map) },
                            modifier = Modifier.width(260.dp).height(140.dp),
                        )
                    }
                }
            }
        }

        // 4. Shared Waypoints Section
        if (crewWaypoints.isNotEmpty()) {
            item { Spacer(Modifier.height(Spacing.extraLarge)) }
            item { SectionHeader("SHARED WAYPOINTS") }
            item {
                SunkenCard {
                    crewWaypoints.forEachIndexed { index, waypoint ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onWaypointTap(waypoint) }
                                .padding(Spacing.large),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(SaltyColors.sunken),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.Place,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = SaltyColors.accent,
                                )
                            }
                            Spacer(Modifier.width(Spacing.medium))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    waypoint.waypoint.name ?: "Unnamed",
                                    style = SaltyType.body,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    waypoint.sharedByName ?: "Unknown",
                                    style = SaltyType.caption,
                                    color = SaltyColors.textSecondary,
                                )
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = SaltyColors.textSecondary,
                            )
                        }
                        if (index < crewWaypoints.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 60.dp),
                                color = SaltyColors.textPrimary.copy(alpha = 0.06f),
                            )
                        }
                    }
                }
            }
        }

        // 5. Empty State
        if (!hasContent) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        Icons.Default.GroupAdd,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = SaltyColors.textSecondary,
                    )
                    Spacer(Modifier.height(Spacing.large))
                    Text("No shared content yet", style = SaltyType.heading)
                    Spacer(Modifier.height(Spacing.small))
                    Text(
                        "Share waypoints or maps with your crew to see them here",
                        style = SaltyType.bodySmall,
                        color = SaltyColors.textSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

// ── Private Components ──────────────────────────────────────────────────────

@Composable
private fun MemberAvatar(member: CrewMember, size: Dp = 32.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(SaltyColors.accent)
            .border(2.dp, SaltyColors.overlay, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            member.initials,
            style = SaltyType.captionSmall,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        modifier = Modifier.padding(horizontal = Spacing.large, vertical = Spacing.small),
        style = SaltyType.caption,
        color = SaltyColors.textSecondary,
        letterSpacing = 1.sp,
    )
}

@Composable
private fun SunkenCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.large)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(SaltyColors.sunken),
        content = content,
    )
}
