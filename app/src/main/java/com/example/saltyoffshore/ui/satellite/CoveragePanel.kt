package com.example.saltyoffshore.ui.satellite

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saltyoffshore.data.satellite.CoverageSummary
import com.example.saltyoffshore.data.satellite.DayNight
import com.example.saltyoffshore.data.satellite.PassStatus
import com.example.saltyoffshore.data.satellite.RegionalPass
import com.example.saltyoffshore.data.satellite.SatelliteDatasetType
import com.example.saltyoffshore.data.satellite.SkipReason
import com.example.saltyoffshore.ui.theme.SaltyType
import com.example.saltyoffshore.ui.theme.Spacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// =============================================================================
// MARK: - CoveragePanel
// =============================================================================

@Composable
fun CoveragePanel(
    summary: CoverageSummary?,
    passes: List<RegionalPass>,
    selectedId: String?,
    showNightPasses: Boolean,
    onSelect: (String) -> Unit,
    onToggleNightPasses: () -> Unit
) {
    val listState = rememberLazyListState()

    // Auto-scroll to selected pass
    LaunchedEffect(selectedId) {
        val id = selectedId ?: return@LaunchedEffect
        val index = passes.indexOfFirst { it.id == id }
        if (index >= 0) listState.animateScrollToItem(index)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.95f), RoundedCornerShape(10.dp))
            .padding(horizontal = Spacing.large)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Recent Passes", style = SaltyType.body, color = Color.White)

            Spacer(Modifier.width(4.dp))

            Text("24h", style = SaltyType.mono(12), color = Color.White.copy(alpha = 0.4f))

            Spacer(Modifier.weight(1f))

            // Night passes toggle
            NightToggleButton(active = showNightPasses, onClick = onToggleNightPasses)
        }

        // Divider
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.1f))
        )

        // Pass list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 200.dp)
        ) {
            itemsIndexed(passes, key = { _, pass -> pass.id }) { index, pass ->
                // Yesterday header
                if (shouldShowYesterdayHeader(pass, if (index > 0) passes[index - 1] else null)) {
                    YesterdayHeader()
                }

                PassRow(
                    pass = pass,
                    isSelected = pass.id == selectedId,
                    onTap = { onSelect(pass.id) }
                )
            }
        }

        // Divider
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.1f))
        )

        // Footer
        Text(
            "Not all satellite passes have usable data.",
            style = SaltyType.caption,
            color = Color.White.copy(alpha = 0.4f),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
        )
    }
}

// =============================================================================
// MARK: - Night Toggle Button
// =============================================================================

@Composable
private fun NightToggleButton(active: Boolean, onClick: () -> Unit) {
    val bg = if (active) Color(0xFF3F51B5).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f)

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("\uD83C\uDF19", fontSize = 10.sp) // moon
        Text(
            if (active) "On" else "Off",
            style = SaltyType.mono(12),
            color = Color.White
        )
    }
}

// =============================================================================
// MARK: - Pass Row
// =============================================================================

@Composable
private fun PassRow(
    pass: RegionalPass,
    isSelected: Boolean,
    onTap: () -> Unit
) {
    val isDisabled = pass.status == null ||
        pass.status == PassStatus.SKIPPED ||
        pass.status == PassStatus.UNAVAILABLE

    val isStruck = isDisabled
    val textColor = if (isSelected) Color.Black else Color.White
    val bgColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.03f)
    val strikethrough = if (isStruck) TextDecoration.LineThrough else TextDecoration.None

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(bgColor)
            .then(if (!isDisabled) Modifier.clickable(onClick = onTap) else Modifier)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Status indicator
        StatusIndicator(status = pass.status, isSelected = isSelected)

        // Satellite name
        Text(
            pass.name,
            style = SaltyType.mono(12),
            color = textColor,
            textDecoration = strikethrough,
            modifier = Modifier.width(64.dp)
        )

        // Dataset type icon
        DatasetIcon(
            datasetType = pass.datasetType,
            opacity = if (isStruck) 0.3f else 0.5f,
            tint = textColor,
            modifier = Modifier.width(16.dp)
        )

        // Time
        Text(
            pass.timeLocal,
            style = SaltyType.mono(12),
            color = textColor,
            textDecoration = strikethrough,
            modifier = Modifier.width(72.dp)
        )

        Spacer(Modifier.weight(1f))

        // Trailing content
        TrailingContent(pass = pass, textColor = textColor)
    }
}

// =============================================================================
// MARK: - Status Indicator
// =============================================================================

@Composable
private fun StatusIndicator(status: PassStatus?, isSelected: Boolean) {
    Box(modifier = Modifier.size(10.dp), contentAlignment = Alignment.Center) {
        when (status) {
            PassStatus.SUCCESS -> {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color.Green)
                )
            }
            PassStatus.RUNNING -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(10.dp),
                    strokeWidth = 1.5.dp,
                    color = if (isSelected) Color.Black else Color.White
                )
            }
            PassStatus.SKIPPED, PassStatus.UNAVAILABLE, null -> {
                Icon(
                    Icons.Filled.Block,
                    contentDescription = null,
                    modifier = Modifier.size(10.dp),
                    tint = Color.White.copy(alpha = 0.3f)
                )
            }
        }
    }
}

// =============================================================================
// MARK: - Dataset Icon
// =============================================================================

@Composable
private fun DatasetIcon(
    datasetType: SatelliteDatasetType,
    opacity: Float,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val icon = when (datasetType) {
        SatelliteDatasetType.CHLOROPHYLL -> Icons.Filled.Eco
        SatelliteDatasetType.SST -> Icons.Filled.Thermostat
        SatelliteDatasetType.ALTIMETRY -> Icons.Filled.Waves
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Icon(
            icon,
            contentDescription = datasetType.label,
            modifier = Modifier.size(10.dp),
            tint = tint.copy(alpha = opacity)
        )
    }
}

// =============================================================================
// MARK: - Trailing Content
// =============================================================================

@Composable
private fun TrailingContent(pass: RegionalPass, textColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        when (pass.status) {
            PassStatus.SUCCESS -> {
                pass.coverageDisplay?.let {
                    Text(it, style = SaltyType.mono(12), color = textColor.copy(alpha = 0.7f))
                }
            }
            PassStatus.RUNNING -> {
                Text("Processing...", style = SaltyType.mono(12), color = textColor.copy(alpha = 0.5f))
            }
            PassStatus.SKIPPED -> {
                val reason = pass.skipReason
                if (reason != null && reason != SkipReason.NIGHTTIME) {
                    Text(
                        reason.label,
                        style = SaltyType.mono(12),
                        color = textColor.copy(alpha = 0.5f),
                        textDecoration = TextDecoration.LineThrough
                    )
                }
            }
            PassStatus.UNAVAILABLE -> {
                Text(
                    "Not available",
                    style = SaltyType.mono(12),
                    color = textColor.copy(alpha = 0.5f),
                    textDecoration = TextDecoration.LineThrough
                )
            }
            null -> {}
        }

        pass.dayNight?.let { dayNight ->
            DayNightBadge(dayNight = dayNight, size = BadgeSize.COMPACT)
        }
    }
}

// =============================================================================
// MARK: - Yesterday Header
// =============================================================================

@Composable
private fun YesterdayHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f))
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Text("Yesterday", style = SaltyType.body, color = Color.White)
    }
}

// =============================================================================
// MARK: - Helpers
// =============================================================================

private val isoParser: DateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME

private fun shouldShowYesterdayHeader(pass: RegionalPass, previousPass: RegionalPass?): Boolean {
    val passDate = parseLocalDate(pass.capturedAt) ?: return false
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)
    val isPassYesterday = passDate == yesterday

    if (previousPass == null) return isPassYesterday

    val prevDate = parseLocalDate(previousPass.capturedAt) ?: return false
    return prevDate == today && isPassYesterday
}

private fun parseLocalDate(iso: String): LocalDate? =
    runCatching {
        Instant.from(isoParser.parse(iso))
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }.getOrNull()
