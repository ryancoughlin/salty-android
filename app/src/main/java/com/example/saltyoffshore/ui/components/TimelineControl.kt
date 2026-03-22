package com.example.saltyoffshore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.data.TimeEntry
import com.example.saltyoffshore.ui.theme.SaltyType
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private const val TRACK_HEIGHT = 32
private const val MAX_ENTRIES = 48

/**
 * Matches iOS TimelineControl.swift exactly:
 * - Track height: 32dp
 * - Max 48 entries
 * - Most recent (index 0) on RIGHT
 * - Tick marks: 1.5dp width, 80% height selected, 60% otherwise
 * - Selection capsule: 6dp width, full height, white with shadow
 * - Time display: 85dp width, monospaced
 *
 * Uses Material 3 color tokens for theme-aware rendering.
 */
@Composable
fun TimelineControl(
    entries: List<TimeEntry>,
    selectedEntry: TimeEntry?,
    showTimeDisplay: Boolean = true,
    onEntrySelected: (TimeEntry) -> Unit
) {
    if (entries.isEmpty()) return

    val displayedEntries = entries.take(MAX_ENTRIES)
    var isDragging by remember { mutableStateOf(false) }
    var dragIndex by remember { mutableIntStateOf(0) }

    val selectedIndex = displayedEntries
        .indexOfFirst { it.id == selectedEntry?.id }
        .coerceAtLeast(0)

    val displayIndex = if (isDragging) dragIndex else selectedIndex

    // Theme-aware colors
    val trackBackground = MaterialTheme.colorScheme.surfaceContainerLow
    val tickColor = MaterialTheme.colorScheme.onSurface
    val tickInactiveAlpha = 0.5f
    val capsuleColor = MaterialTheme.colorScheme.primary
    val timeTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(TRACK_HEIGHT.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Track
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .height(TRACK_HEIGHT.dp)
                .clip(RoundedCornerShape((TRACK_HEIGHT / 6).dp))
                .background(trackBackground)
                .pointerInput(displayedEntries) {
                    detectTapGestures { offset ->
                        val index = nearestIndex(offset.x, size.width.toFloat(), displayedEntries.size)
                        if (index in displayedEntries.indices) {
                            onEntrySelected(displayedEntries[index])
                        }
                    }
                }
                .pointerInput(displayedEntries) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = { isDragging = false },
                        onDragCancel = { isDragging = false },
                        onDrag = { change, _ ->
                            val index = nearestIndex(change.position.x, size.width.toFloat(), displayedEntries.size)
                            if (index in displayedEntries.indices) {
                                dragIndex = index
                                onEntrySelected(displayedEntries[index])
                            }
                        }
                    )
                }
        ) {
            val density = LocalDensity.current
            val widthPx = constraints.maxWidth.toFloat()
            val count = displayedEntries.size

            // Tick marks
            displayedEntries.indices.forEach { index ->
                val isSelected = index == displayIndex
                val tickHeight = if (isSelected) TRACK_HEIGHT * 0.8f else TRACK_HEIGHT * 0.6f
                val xPx = xPosition(index, widthPx, count)
                val xDp = with(density) { xPx.toDp() }

                Box(
                    modifier = Modifier
                        .offset(x = xDp - 0.75.dp)
                        .width(1.5.dp)
                        .height(tickHeight.dp)
                        .align(Alignment.CenterStart)
                        .background(
                            if (isSelected) tickColor
                            else tickColor.copy(alpha = tickInactiveAlpha)
                        )
                )
            }

            // Selection capsule
            val handleXPx = xPosition(displayIndex, widthPx, count)
            val handleXDp = with(density) { handleXPx.toDp() }

            Box(
                modifier = Modifier
                    .offset(x = handleXDp - 3.dp)
                    .width(6.dp)
                    .fillMaxHeight()
                    .shadow(
                        elevation = 2.dp,
                        shape = RoundedCornerShape(3.dp),
                        ambientColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    .background(capsuleColor, RoundedCornerShape(3.dp))
            )
        }

        // Time display
        if (showTimeDisplay && selectedEntry != null) {
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.width(85.dp)
            ) {
                Text(
                    text = formatTime(selectedEntry.timestamp),
                    style = SaltyType.mono(12),
                    color = timeTextColor
                )
                Text(
                    text = formatShortDate(selectedEntry.timestamp),
                    style = SaltyType.mono(12),
                    color = timeTextColor
                )
            }
        }
    }
}

// Logic: Most recent (index 0) is on the RIGHT
private fun xPosition(index: Int, width: Float, count: Int): Float {
    if (count <= 1) return width / 2
    val spacing = width / (count - 1)
    val reversedIndex = count - 1 - index
    return reversedIndex * spacing
}

private fun nearestIndex(x: Float, width: Float, count: Int): Int {
    if (count <= 1) return 0
    val spacing = width / (count - 1)
    val reversedIndex = (x / spacing).roundToInt().coerceIn(0, count - 1)
    return count - 1 - reversedIndex
}

// Date formatting matching iOS StandardDateFormatter
private fun formatTime(timestamp: String): String {
    return try {
        val dt = ZonedDateTime.parse(timestamp)
        dt.format(DateTimeFormatter.ofPattern("h:mm a"))
    } catch (e: Exception) {
        timestamp.substring(11, 16)
    }
}

private fun formatShortDate(timestamp: String): String {
    return try {
        val dt = ZonedDateTime.parse(timestamp)
        dt.format(DateTimeFormatter.ofPattern("E, MMM d"))
    } catch (e: Exception) {
        timestamp.substring(0, 10)
    }
}
