package com.example.saltyoffshore.ui.components.entrygallery

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saltyoffshore.data.Dataset
import com.example.saltyoffshore.data.EntryGalleryItem
import com.example.saltyoffshore.data.TimeEntry
import com.example.saltyoffshore.data.toGalleryItems
import com.example.saltyoffshore.ui.components.PreviewImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// MARK: - Entry Gallery View

/**
 * Gallery view showing all entries with preview images and filtering.
 * iOS ref: Features/Timeline/Views/EntryGalleryView.swift
 */
@Composable
fun EntryGalleryView(
    dataset: Dataset,
    selectedDepth: Int,
    currentEntryId: String?,
    onEntrySelected: (TimeEntry) -> Unit,
    onDismiss: () -> Unit,
) {
    val supportsCoverageFiltering = dataset.metadata?.cloudFree == false

    // Coverage filter state
    var minClearPercentage by remember { mutableFloatStateOf(0f) }
    var filteredCount by remember { mutableIntStateOf(0) }
    var totalCount by remember { mutableIntStateOf(0) }

    // Grouped data
    var isLoading by remember { mutableStateOf(true) }
    var groupedItems by remember { mutableStateOf<List<GallerySection>>(emptyList()) }
    var filteredGroups by remember { mutableStateOf<List<GallerySection>>(emptyList()) }

    // Load and group entries off main thread
    LaunchedEffect(dataset.entries, selectedDepth) {
        isLoading = true
        val result = withContext(Dispatchers.Default) {
            val entries = dataset.entries
                ?.filter { it.depth == selectedDepth }
                ?.sortedByDescending { it.timestamp }
                ?: emptyList()

            val galleryItems = entries.toGalleryItems()
            groupByDate(galleryItems)
        }
        groupedItems = result
        isLoading = false
    }

    // Apply coverage filter
    LaunchedEffect(groupedItems, minClearPercentage, supportsCoverageFiltering) {
        val threshold = minClearPercentage.toDouble()
        val result = withContext(Dispatchers.Default) {
            if (supportsCoverageFiltering && threshold > 0) {
                groupedItems.mapNotNull { section ->
                    val filtered = section.items.filter { item ->
                        item.entry.dataCoveragePercentage?.let { it >= threshold } ?: true
                    }
                    if (filtered.isEmpty()) null else section.copy(items = filtered)
                }
            } else {
                groupedItems
            }
        }
        filteredGroups = result
        totalCount = groupedItems.sumOf { it.items.size }
        filteredCount = result.sumOf { it.items.size }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Sheet header
            SheetHeader(title = "Entry Gallery", onClose = onDismiss)

            // Content
            when {
                isLoading -> LoadingView()
                filteredGroups.isEmpty() -> EmptyStateView(
                    hasActiveFilter = minClearPercentage > 0,
                    onResetFilter = { minClearPercentage = 0f }
                )
                else -> EntryGrid(
                    groups = filteredGroups,
                    dataset = dataset,
                    currentEntryId = currentEntryId,
                    supportsCoverageFiltering = supportsCoverageFiltering,
                    onEntrySelected = onEntrySelected,
                )
            }
        }

        // Floating coverage control
        if (supportsCoverageFiltering) {
            FloatingCoverageControl(
                minClearPercentage = minClearPercentage,
                onMinClearPercentageChanged = { minClearPercentage = it },
                filteredCount = filteredCount,
                totalCount = totalCount,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            )
        }
    }
}

// MARK: - Sheet Header

@Composable
private fun SheetHeader(title: String, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onClose) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Close",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// MARK: - Entry Grid

@Composable
private fun EntryGrid(
    groups: List<GallerySection>,
    dataset: Dataset,
    currentEntryId: String?,
    supportsCoverageFiltering: Boolean,
    onEntrySelected: (TimeEntry) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = 0.dp,
            bottom = if (supportsCoverageFiltering) 100.dp else 16.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        for (section in groups) {
            // Sticky-ish date header (full span)
            item(
                key = "header_${section.date}",
                span = { GridItemSpan(2) }
            ) {
                SectionHeader(date = section.date, count = section.items.size)
            }

            // Entry cards
            items(
                items = section.items,
                key = { it.id }
            ) { item ->
                EntryGalleryCard(
                    item = item,
                    dataset = dataset,
                    isSelected = item.entry.id == currentEntryId,
                    onTap = { onEntrySelected(item.entry) }
                )
            }
        }
    }
}

// MARK: - Section Header

@Composable
private fun SectionHeader(date: LocalDate, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 0.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatDateHeader(date),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = "($count)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// MARK: - Entry Gallery Card

@Composable
private fun EntryGalleryCard(
    item: EntryGalleryItem,
    dataset: Dataset,
    isSelected: Boolean,
    onTap: () -> Unit,
) {
    val effectiveCloudFree = dataset.metadata?.cloudFree ?: true

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
            .then(
                if (isSelected) Modifier.border(3.dp, Color.White, RoundedCornerShape(12.dp))
                else Modifier
            )
            .scale(if (isSelected) 1.02f else 1f)
            .animateContentSize(animationSpec = spring(dampingRatio = 0.8f)),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 1.dp
    ) {
        Column {
            // Preview image with badges
            Box {
                PreviewImage(
                    url = item.entry.previewUrl,
                    aspectRatio = 4f / 3f,
                )

                // Badges in top-right
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    // Coverage percentage badge (only for non-cloud-free datasets)
                    val coverage = item.entry.dataCoveragePercentage
                    if (coverage != null && !effectiveCloudFree) {
                        CoveragePercentageBadge(coverage = coverage)
                    }

                    // Sensor + temporal coverage badge
                    val metadata = item.entry.sourceMetadata
                    if (metadata?.sensor != null || metadata?.temporalCoverage != null) {
                        SensorBadge(
                            sensor = metadata?.sensor,
                            temporalCoverage = metadata?.temporalCoverage
                        )
                    }
                }
            }

            // Time label
            Text(
                text = item.timeOnly,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }
    }
}

// MARK: - Coverage Badge

@Composable
private fun CoveragePercentageBadge(coverage: Double) {
    Text(
        text = "${coverage.toInt()}%",
        color = Color.White,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

// MARK: - Loading View

@Composable
private fun LoadingView() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Loading entries...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// MARK: - Empty State

@Composable
private fun EmptyStateView(hasActiveFilter: Boolean, onResetFilter: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.Photo,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "No Entries Found",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))

            if (hasActiveFilter) {
                Text(
                    text = "Try lowering the clear data threshold",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = onResetFilter) {
                    Text("Show All Entries")
                }
            } else {
                Text(
                    text = "No satellite imagery available yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// MARK: - Data Grouping

data class GallerySection(
    val date: LocalDate,
    val items: List<EntryGalleryItem>
)

private fun groupByDate(items: List<EntryGalleryItem>): List<GallerySection> {
    val zoneId = ZoneId.systemDefault()
    return items
        .groupBy { item ->
            try {
                Instant.parse(item.entry.timestamp).atZone(zoneId).toLocalDate()
            } catch (_: Exception) {
                LocalDate.now()
            }
        }
        .map { (date, items) ->
            GallerySection(date = date, items = items.sortedByDescending { it.entry.timestamp })
        }
        .sortedByDescending { it.date }
}

private fun formatDateHeader(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(DateTimeFormatter.ofPattern("E, MMM d"))
    }
}
