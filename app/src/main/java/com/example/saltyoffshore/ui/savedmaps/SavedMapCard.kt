package com.example.saltyoffshore.ui.savedmaps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.saltyoffshore.data.SavedMap
import com.example.saltyoffshore.ui.theme.SaltyColors
import com.example.saltyoffshore.ui.theme.SaltyType
import com.example.saltyoffshore.ui.theme.Spacing
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SavedMapCard(
    map: SavedMap,
    isOwner: Boolean,
    onClick: () -> Unit,
    onShareLink: (() -> Unit)? = null,
    onShareToCrew: (() -> Unit)? = null,
    onUnshare: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(200.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = SaltyColors.raised),
    ) {
        Box(Modifier.fillMaxSize()) {
            // 1. Thumbnail
            if (map.thumbnailUrl != null) {
                AsyncImage(
                    model = map.thumbnailUrl,
                    contentDescription = map.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(SaltyColors.sunken),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Map,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = SaltyColors.textSecondary,
                    )
                }
            }

            // 2. Gradient scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.2f to Color.Transparent,
                            0.85f to Color.Black.copy(alpha = 0.8f),
                        )
                    ),
            )

            // 3. Content overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(Spacing.large),
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (map.isCrewMap) {
                        MapChip(
                            text = "Crew",
                            icon = Icons.Default.People,
                            backgroundColor = Color.White.copy(alpha = 0.3f),
                        )
                    }
                    if (map.datasetName != null) {
                        MapChip(
                            text = map.datasetName,
                            backgroundColor = SaltyColors.accent.copy(alpha = 0.6f),
                        )
                    }
                    if (map.regionName != null) {
                        MapChip(
                            text = map.regionName,
                            backgroundColor = Color.White.copy(alpha = 0.2f),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    map.name,
                    style = SaltyType.body,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    formatCreatedAt(map.createdAt),
                    style = SaltyType.caption,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                )
            }

            // 4. Crew attribution (top-right)
            if (map.isCrewMap && map.sharedByName != null) {
                Text(
                    "Shared by ${map.sharedByName}",
                    style = SaltyType.captionSmall,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(Spacing.medium),
                )
            }
        }
    }
}

// ── Chip ─────────────────────────────────────────────────────────────────────

@Composable
private fun MapChip(
    text: String,
    icon: ImageVector? = null,
    backgroundColor: Color = Color.White.copy(alpha = 0.2f),
    textColor: Color = Color.White,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(10.dp), tint = textColor)
        }
        Text(text, style = SaltyType.captionSmall, color = textColor, maxLines = 1)
    }
}

// ── Date Formatting ──────────────────────────────────────────────────────────

private val dateFormatter = DateTimeFormatter
    .ofPattern("EEE, MMM d '\u00B7' h:mm a", Locale.US)

private fun formatCreatedAt(iso8601: String): String {
    return try {
        val instant = Instant.parse(iso8601)
        val local = instant.atZone(ZoneId.systemDefault())
        dateFormatter.format(local)
    } catch (_: Exception) {
        iso8601
    }
}
