package com.example.saltyoffshore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saltyoffshore.data.Dataset
import com.example.saltyoffshore.data.DatasetGroup
import com.example.saltyoffshore.data.DatasetType
import com.example.saltyoffshore.data.group

/**
 * Modal bottom sheet for selecting datasets.
 * Matches iOS DatasetSelectorView layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatasetSelectorSheet(
    datasets: List<Dataset>,
    selectedDataset: Dataset?,
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
        containerColor = Color(0xFF1C1C1E),
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
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Change Dataset",
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            // Dataset list grouped by category
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        DatasetListItem(
                            dataset = dataset,
                            isSelected = selectedDataset?.id == dataset.id,
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
        style = TextStyle(
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.5f),
            letterSpacing = 1.sp
        ),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

@Composable
private fun DatasetListItem(
    dataset: Dataset,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val backgroundColor = if (isSelected) Color.White else Color.Transparent
    val textColor = if (isSelected) Color.Black else Color.White

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Dataset type indicator
        val datasetType = DatasetType.fromRawValue(dataset.type)
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(datasetType?.defaultColorscale?.colors?.firstOrNull()?.let { Color(it) } ?: Color(0xFF808080)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = datasetType?.shortName ?: "?",
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Dataset info
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = dataset.name,
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor
                    )
                )
                if (dataset.beta == true) {
                    Spacer(modifier = Modifier.width(6.dp))
                    BetaBadge()
                }
            }

            // Metadata chips
            Spacer(modifier = Modifier.height(4.dp))
            DatasetMetadataRow(dataset = dataset, textColor = textColor)
        }
    }
}

@Composable
private fun BetaBadge() {
    Text(
        text = "BETA",
        style = TextStyle(
            fontSize = 8.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        ),
        modifier = Modifier
            .background(Color(0xFFFF9500), RoundedCornerShape(2.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp)
    )
}

@Composable
private fun DatasetMetadataRow(
    dataset: Dataset,
    textColor: Color
) {
    val chipColor = textColor.copy(alpha = 0.6f)
    val metadata = dataset.metadata

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Sensor chip
        metadata?.sensor?.let { sensor ->
            MetadataChip(text = sensor, color = chipColor)
        }

        // Frequency chip
        metadata?.frequency?.let { frequency ->
            MetadataChip(text = frequency, color = chipColor)
        }
    }
}

@Composable
private fun MetadataChip(
    text: String,
    color: Color
) {
    Text(
        text = text,
        style = TextStyle(
            fontSize = 11.sp,
            color = color
        )
    )
}
