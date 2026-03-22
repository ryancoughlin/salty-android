package com.example.saltyoffshore.ui.screen

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.saltyoffshore.data.RegionGroup

// Salty design tokens (matching AccountHubSheet)
private val SaltyBase = Color(0xFF0A0A0F)
private val SaltySunken = Color(0xFF141419)
private val SaltyTextPrimary = Color(0xFFFFFFFF)
private val SaltyTextSecondary = Color(0xFF8A8A9A)
private val SaltyAccent = Color(0xFF00D4AA)

/**
 * Region selection bottom sheet matching iOS RegionSelectionView.swift.
 * Shows regions grouped by area with checkmark for selected.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegionSelectionSheet(
    sheetState: SheetState,
    regionGroups: List<RegionGroup>,
    selectedRegionId: String?,
    onRegionSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SaltyBase
    ) {
        Column {
            // Title
            Text(
                text = "Preferred Region",
                color = SaltyTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (regionGroups.isEmpty()) {
                // Loading state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = SaltyAccent)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Loading regions...", color = SaltyTextSecondary, fontSize = 14.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    regionGroups.forEach { group ->
                        // Group header
                        item(key = "header_${group.group}") {
                            Text(
                                text = group.group.uppercase(),
                                color = SaltyTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, start = 4.dp)
                            )
                        }

                        // Region rows in a card
                        items(
                            items = group.regions,
                            key = { it.id }
                        ) { region ->
                            val isFirst = region == group.regions.first()
                            val isLast = region == group.regions.last()
                            val isSelected = region.id == selectedRegionId

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (isFirst) Modifier.clip(
                                            RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                                        ) else if (isLast) Modifier.clip(
                                            RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                                        ) else Modifier
                                    )
                                    .background(SaltySunken)
                                    .clickable {
                                        onRegionSelected(region.id)
                                        onDismiss()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Thumbnail
                                    AsyncImage(
                                        model = region.thumbnail,
                                        contentDescription = region.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    // Region name
                                    Text(
                                        text = region.name,
                                        color = SaltyTextPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )

                                    // Checkmark
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = SaltyAccent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                // Divider (not on last item)
                                if (!isLast) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 64.dp),
                                        color = Color.White.copy(alpha = 0.06f)
                                    )
                                }
                            }
                        }
                    }

                    // Bottom spacing
                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
        }
    }
}
