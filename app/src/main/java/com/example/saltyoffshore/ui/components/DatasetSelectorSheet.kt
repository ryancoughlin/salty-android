package com.example.saltyoffshore.ui.components

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.saltyoffshore.data.Dataset
import com.example.saltyoffshore.data.DatasetGroup
import com.example.saltyoffshore.data.DatasetType
import com.example.saltyoffshore.data.TimeEntry
import com.example.saltyoffshore.data.group
import com.example.saltyoffshore.ui.theme.SaltyColors
import com.example.saltyoffshore.ui.theme.SaltyLayout
import com.example.saltyoffshore.ui.theme.SaltyType
import com.example.saltyoffshore.ui.theme.Spacing

/**
 * Modal bottom sheet for selecting datasets.
 * Matches iOS DatasetSelectorView layout with preview images and info chips.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatasetSelectorSheet(
    datasets: List<Dataset>,
    selectedDataset: Dataset?,
    selectedEntry: TimeEntry?,
    isPremium: Boolean,
    sheetState: SheetState,
    onDatasetSelected: (Dataset) -> Unit,
    onDismiss: () -> Unit
) {
    // Group datasets by category (matching iOS)
    val groupedDatasets = datasets
        .filter { it.type != "water_type" }
        .groupBy { dataset ->
            DatasetType.fromRawValue(dataset.type)?.group ?: DatasetGroup.OTHER
        }
        .toSortedMap(compareBy { it.displayOrder })

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SaltyColors.overlay,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.large, vertical = Spacing.medium),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Change Dataset",
                    style = SaltyType.heading.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = SaltyColors.textPrimary
                    )
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = SaltyColors.iconButton
                    )
                }
            }

            // Dataset list grouped by category
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.large)
            ) {
                groupedDatasets.forEach { (group, datasetsInGroup) ->
                    // Category header
                    item(key = "header-${group.name}") {
                        CategoryHeader(title = group.title)
                    }

                    // Datasets in this category
                    items(
                        items = datasetsInGroup,
                        key = { it.id }
                    ) { dataset ->
                        val isSelected = selectedDataset?.id == dataset.id
                        DatasetListItem(
                            dataset = dataset,
                            isSelected = isSelected,
                            selectedEntry = if (isSelected) selectedEntry else null,
                            isPremium = isPremium,
                            onSelect = {
                                onDatasetSelected(dataset)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryHeader(title: String) {
    Text(
        text = title,
        style = SaltyType.caption.copy(
            fontWeight = FontWeight.SemiBold,
            color = SaltyColors.textSecondary,
            letterSpacing = 1.sp
        ),
        modifier = Modifier.padding(horizontal = Spacing.large, vertical = Spacing.small)
    )
}

/**
 * Dataset list item — matches iOS DatasetListItem layout:
 * [Preview Image 120x80] [8dp] [Name + BETA badge / Info chips]
 */
@Composable
private fun DatasetListItem(
    dataset: Dataset,
    isSelected: Boolean,
    selectedEntry: TimeEntry?,
    isPremium: Boolean,
    onSelect: () -> Unit
) {
    val isLocked = !isSelected && !isPremium
    val backgroundColor = if (isSelected) SaltyColors.textPrimary else Color.Transparent
    val textColor = if (isSelected) SaltyColors.base else SaltyColors.textPrimary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isLocked) 0.85f else 1f)
            .clickable(enabled = !isLocked, onClick = onSelect)
            .background(backgroundColor)
            .padding(horizontal = Spacing.large, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Preview image with lock overlay
        if (dataset.previewUrl != null) {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(80.dp)
                    .clip(RoundedCornerShape(6.dp))
            ) {
                AsyncImage(
                    model = dataset.previewUrl,
                    contentDescription = dataset.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Lock overlay for non-premium, non-selected datasets
                if (isLocked) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.8f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Locked",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
        }

        // Dataset info
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = dataset.name,
                    style = SaltyType.body.copy(color = textColor)
                )
                if (dataset.beta == true) {
                    Spacer(modifier = Modifier.width(6.dp))
                    BetaBadge()
                }
            }

            Spacer(modifier = Modifier.height(Spacing.small))

            // Info chips (sensor, cloud, frequency, HD, free preview)
            DatasetInfoChips(
                dataset = dataset,
                selectedEntry = selectedEntry
            )
        }
    }
}

@Composable
private fun BetaBadge() {
    Text(
        text = "BETA",
        style = SaltyType.captionSmall.copy(
            fontSize = 8.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        ),
        modifier = Modifier
            .background(Color(0xFFFF9500).copy(alpha = 0.8f), RoundedCornerShape(2.dp))
            .padding(horizontal = Spacing.small, vertical = 2.dp)
    )
}
