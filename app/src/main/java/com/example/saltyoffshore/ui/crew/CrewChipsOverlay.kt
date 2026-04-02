package com.example.saltyoffshore.ui.crew

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Badge
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.data.waypoint.Crew
import com.example.saltyoffshore.ui.theme.SaltyColors
import com.example.saltyoffshore.ui.theme.SaltyType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrewChipsOverlay(
    crews: List<Crew>,
    activeCrewId: String?,
    unreadCounts: Map<String, Int>,
    onSelectCrew: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (crews.isEmpty()) return

    var expanded by remember { mutableStateOf(false) }

    Box(modifier) {
        FilterChip(
            selected = activeCrewId != null,
            onClick = { expanded = true },
            label = {
                val label = if (activeCrewId != null) {
                    crews.firstOrNull { it.id == activeCrewId }?.name ?: "All Waypoints"
                } else {
                    "All Waypoints"
                }
                Text(label, style = SaltyType.caption)
            },
            leadingIcon = {
                Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(16.dp))
            },
            trailingIcon = {
                val totalUnread = unreadCounts.values.sum()
                if (totalUnread > 0 && activeCrewId == null) {
                    Badge { Text(totalUnread.toString()) }
                }
            },
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("All Waypoints") },
                onClick = { onSelectCrew(null); expanded = false },
                trailingIcon = {
                    if (activeCrewId == null) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = SaltyColors.accent)
                    }
                },
            )
            HorizontalDivider()
            crews.forEach { crew ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(crew.name)
                            val count = unreadCounts[crew.id] ?: 0
                            if (count > 0) {
                                Spacer(Modifier.width(8.dp))
                                Badge { Text(count.toString()) }
                            }
                        }
                    },
                    onClick = { onSelectCrew(crew.id); expanded = false },
                    trailingIcon = {
                        if (activeCrewId == crew.id) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = SaltyColors.accent)
                        }
                    },
                )
            }
        }
    }
}
