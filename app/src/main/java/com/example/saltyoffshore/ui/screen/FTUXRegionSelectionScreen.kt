package com.example.saltyoffshore.ui.screen

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.saltyoffshore.data.RegionGroup
import com.example.saltyoffshore.data.RegionListItem

// Design tokens
private val SaltyBase = Color(0xFF0A0A0F)
private val SaltySunken = Color(0xFF141419)
private val SaltyAccent = Color(0xFF00D4AA)
private val SaltyTextPrimary = Color(0xFFFFFFFF)
private val SaltyTextSecondary = Color(0xFF8A8A9A)
private val SaltyTextTertiary = Color(0xFF5A5A6A)

/**
 * FTUX Region Selection matching iOS FTUXRegionSelectionView.swift.
 *
 * Shown as a blocking full-screen dialog when preferredRegionId is null.
 * Cannot be dismissed by gesture (matching interactiveDismissDisabled).
 *
 * Two states:
 * 1. Loading: logo + spinner (when regionGroups is empty)
 * 2. Region list: header + grouped list with thumbnails
 */
@Composable
fun FTUXRegionSelectionScreen(
    regionGroups: List<RegionGroup>,
    loadingRegionId: String?,
    onRegionSelected: (String) -> Unit
) {
    Dialog(
        onDismissRequest = { /* Cannot dismiss */ },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SaltyBase)
        ) {
            if (regionGroups.isEmpty()) {
                LoadingView()
            } else {
                RegionListView(
                    regionGroups = regionGroups,
                    loadingRegionId = loadingRegionId,
                    onRegionSelected = onRegionSelected
                )
            }
        }
    }
}

@Composable
private fun LoadingView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo placeholder (80x80)
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(SaltyAccent),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "S",
                color = SaltyBase,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Welcome to Salty Offshore",
            color = SaltyTextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Loading available regions...",
            color = SaltyTextSecondary,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        CircularProgressIndicator(
            color = SaltyAccent,
            modifier = Modifier.size(48.dp),
            strokeWidth = 3.dp
        )
    }
}

@Composable
private fun RegionListView(
    regionGroups: List<RegionGroup>,
    loadingRegionId: String?,
    onRegionSelected: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        // Parallax header
        item {
            FTUXHeader()
        }

        // Region groups
        regionGroups.forEach { group ->
            // Group header
            item {
                Text(
                    text = group.group.uppercase(),
                    color = SaltyTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 8.dp)
                )
            }

            // Region rows in sunken card
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SaltySunken)
                ) {
                    val activeRegions = group.regions.filter {
                        it.status == com.example.saltyoffshore.data.RegionStatus.ACTIVE
                    }
                    activeRegions.forEachIndexed { index, region ->
                        RegionRow(
                            region = region,
                            isLoading = loadingRegionId == region.id,
                            isDisabled = loadingRegionId != null,
                            onClick = { onRegionSelected(region.id) }
                        )
                        if (index < activeRegions.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 108.dp),
                                color = Color.White.copy(alpha = 0.06f)
                            )
                        }
                    }
                }
            }
        }

        // Bottom padding
        item {
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun FTUXHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .padding(top = 64.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo placeholder (72x72)
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(SaltyAccent),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "S",
                color = SaltyBase,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Welcome to Salty Offshore",
            color = SaltyTextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Choose your home waters for offline access and personalized data",
            color = SaltyTextSecondary,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "You can change regions or explore others anytime",
            color = SaltyTextTertiary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RegionRow(
    region: RegionListItem,
    isLoading: Boolean,
    isDisabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isDisabled) { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail placeholder (80x60)
        // TODO: Add Coil dependency for AsyncImage loading of region.thumbnail
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(60.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Gray.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = region.name.take(2).uppercase(),
                color = SaltyTextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Region name
        Text(
            text = region.name,
            color = SaltyTextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )

        // Loading indicator or chevron
        if (isLoading) {
            CircularProgressIndicator(
                color = SaltyAccent,
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = "\u203A",
                color = SaltyTextSecondary,
                fontSize = 20.sp
            )
        }
    }
}
