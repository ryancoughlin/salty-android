package com.example.saltyoffshore.ui.screen

import androidx.compose.foundation.Image
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.example.saltyoffshore.R
import com.example.saltyoffshore.data.RegionGroup
import com.example.saltyoffshore.data.RegionListItem

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
                .background(MaterialTheme.colorScheme.background)
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
        Image(
            painter = painterResource(R.drawable.salty_mark),
            contentDescription = "Salty Offshore",
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Welcome to Salty Offshore",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Loading available regions...",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
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
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
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
                                modifier = Modifier.padding(start = 128.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
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
        Image(
            painter = painterResource(R.drawable.salty_mark),
            contentDescription = "Salty Offshore",
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Welcome to Salty Offshore",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Choose your home waters for offline access and personalized data",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "You can change regions or explore others anytime",
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodySmall,
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
        // Thumbnail (100x72, matching iOS)
        AsyncImage(
            model = region.thumbnail,
            contentDescription = region.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(100.dp)
                .height(72.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Region name
        Text(
            text = region.name,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )

        // Loading indicator or chevron
        if (isLoading) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = "\u203A",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
