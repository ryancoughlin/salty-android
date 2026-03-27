package com.example.saltyoffshore.ui.satellite

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.data.satellite.OrbitDirection
import com.example.saltyoffshore.data.satellite.SatelliteDatasetType
import com.example.saltyoffshore.data.satellite.SatelliteTrack
import com.example.saltyoffshore.ui.theme.SaltyType

// =============================================================================
// MARK: - TrackerPanel
// =============================================================================

@Composable
fun TrackerPanel(
    tracks: List<SatelliteTrack>,
    selectedId: String?,
    onSelect: (String) -> Unit
) {
    if (tracks.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { tracks.size })
    var hasInteracted by remember { mutableStateOf(false) }

    // Swipe -> onSelect (only after user interaction)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            if (hasInteracted && page < tracks.size) {
                onSelect(tracks[page].id)
            }
        }
    }

    // Track user drags
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.isScrollInProgress }.collect { scrolling ->
            if (scrolling) hasInteracted = true
        }
    }

    // External selectedId -> animate pager
    LaunchedEffect(selectedId) {
        val id = selectedId ?: return@LaunchedEffect
        val index = tracks.indexOfFirst { it.id == id }
        if (index >= 0 && pagerState.currentPage != index) {
            pagerState.animateScrollToPage(index)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.85f)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(horizontal = 12.dp)
                .padding(top = 12.dp)
        ) { page ->
            TrackerCard(
                track = tracks[page],
                isSelected = tracks[page].id == selectedId,
                index = page + 1,
                total = tracks.size
            )
        }

        PageDots(
            count = tracks.size,
            current = pagerState.currentPage,
            modifier = Modifier.padding(vertical = 16.dp)
        )
    }
}

// =============================================================================
// MARK: - TrackerCard
// =============================================================================

@Composable
private fun TrackerCard(
    track: SatelliteTrack,
    isSelected: Boolean,
    index: Int,
    total: Int
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else Color.Black,
        animationSpec = tween(durationMillis = 1000),
        label = "cardBg"
    )
    val fgColor = if (isSelected) Color.Black else Color.White

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .padding(horizontal = 4.dp)
            .shadow(8.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .then(
                if (!isSelected) Modifier.border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                else Modifier
            )
            .padding(20.dp)
    ) {
        // Header row
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.name,
                    style = SaltyType.heading,
                    color = fgColor
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = track.instrument.uppercase(),
                        style = SaltyType.mono(10),
                        color = fgColor.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "\u00B7",
                        style = SaltyType.mono(10),
                        color = fgColor.copy(alpha = 0.5f)
                    )
                    Icon(
                        imageVector = track.datasetType.icon,
                        contentDescription = null,
                        modifier = Modifier.size(10.dp),
                        tint = fgColor.copy(alpha = 0.5f)
                    )
                    Text(
                        text = track.datasetType.label.uppercase(),
                        style = SaltyType.mono(10),
                        color = fgColor.copy(alpha = 0.5f)
                    )
                }
            }

            Text(
                text = "$index/$total",
                style = SaltyType.mono(10),
                color = fgColor.copy(alpha = 0.4f)
            )
        }

        Spacer(Modifier.weight(1f))

        // Stats row
        Row(verticalAlignment = Alignment.Bottom) {
            StatBlock(label = "TIME", value = track.current.timeLocal, fgColor = fgColor, valueOpacity = 1f)
            Spacer(Modifier.width(20.dp))
            StatBlock(label = "AGE", value = track.current.ageShort, fgColor = fgColor)
            Spacer(Modifier.width(20.dp))
            StatBlock(
                label = "DIR",
                value = "${track.directionSymbol} ${
                    when (track.direction) {
                        OrbitDirection.ASCENDING -> "ASC"
                        OrbitDirection.DESCENDING -> "DESC"
                        OrbitDirection.UNKNOWN -> "\u2014"
                    }
                }",
                fgColor = fgColor
            )

            Spacer(Modifier.weight(1f))

            track.current.dayNight?.let { dayNight ->
                DayNightBadge(dayNight = dayNight, size = BadgeSize.REGULAR)
            }
        }
    }
}

// =============================================================================
// MARK: - StatBlock
// =============================================================================

@Composable
private fun StatBlock(
    label: String,
    value: String,
    fgColor: Color,
    valueOpacity: Float = 0.85f
) {
    Column {
        Text(
            text = label,
            style = SaltyType.mono(10),
            color = fgColor.copy(alpha = 0.5f)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = SaltyType.mono(12),
            color = fgColor.copy(alpha = valueOpacity)
        )
    }
}

// =============================================================================
// MARK: - PageDots
// =============================================================================

@Composable
private fun PageDots(
    count: Int,
    current: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { i ->
            val width by animateDpAsState(
                targetValue = if (i == current) 26.dp else 8.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "dotWidth"
            )
            Box(
                modifier = Modifier
                    .width(width)
                    .height(5.dp)
                    .clip(RoundedCornerShape(2.5.dp))
                    .background(Color.White.copy(alpha = if (i == current) 1f else 0.3f))
            )
        }
    }
}

// =============================================================================
// MARK: - Dataset Type Icon
// =============================================================================

private val SatelliteDatasetType.icon: ImageVector
    get() = when (this) {
        SatelliteDatasetType.CHLOROPHYLL -> Icons.Filled.Eco
        SatelliteDatasetType.SST -> Icons.Filled.Thermostat
        SatelliteDatasetType.ALTIMETRY -> Icons.Filled.Waves
    }
