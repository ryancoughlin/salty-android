package com.example.saltyoffshore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saltyoffshore.data.Dataset
import com.example.saltyoffshore.data.DatasetType
import com.example.saltyoffshore.data.TimeEntry

/**
 * Info chips row for a dataset — matches iOS DatasetInfoChips.swift.
 * Shows sensor, cloud coverage, frequency, processing delay, HD, and free preview chips.
 */
@Composable
fun DatasetInfoChips(
    dataset: Dataset,
    selectedEntry: TimeEntry?
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Sensor chips — one per sensor
        val sensors = if (dataset.isCombinedDataset) {
            dataset.uniqueSensors
        } else {
            listOfNotNull(dataset.metadata?.sensor?.takeIf { it.isNotEmpty() })
        }
        sensors.forEach { sensor ->
            DatasetInfoChip(text = sensor, icon = Icons.Filled.Sensors)
        }

        // Cloud coverage chip
        val cloudFree = selectedEntry?.sourceMetadata?.cloudFree
        val cloudText = if (cloudFree == true) "Cloud-free" else "Clear Sky"
        val cloudIcon = if (cloudFree == true) Icons.Filled.Cloud else Icons.Filled.WbSunny
        DatasetInfoChip(text = cloudText, icon = cloudIcon)

        // Frequency chip — non-composite only
        if (!dataset.isCombinedDataset) {
            dataset.metadata?.frequency?.let { frequency ->
                DatasetInfoChip(text = frequency, icon = Icons.Filled.Schedule)
            }
        }

        // Processing delay chip — if present
        selectedEntry?.sourceMetadata?.processingDelay?.takeIf { it.isNotEmpty() }?.let { delay ->
            DatasetInfoChip(text = delay, icon = Icons.Filled.HourglassEmpty)
        }

        // HD chip — no icon
        if (selectedEntry?.sourceMetadata?.highResolution == true) {
            DatasetInfoChip(text = "HD")
        }

        // Free Preview chip — FSLE, MLD, dissolved_oxygen, or hasMultipleDepths
        val datasetType = DatasetType.fromRawValue(dataset.type)
        val isFreePreview = datasetType == DatasetType.FSLE ||
            datasetType == DatasetType.MLD ||
            datasetType == DatasetType.DISSOLVED_OXYGEN ||
            dataset.hasMultipleDepths
        if (isFreePreview) {
            FreePreviewChip()
        }
    }
}

/**
 * Single info chip with optional icon — matches iOS DatasetInfoChip.
 * Glass-style background with secondary text.
 */
@Composable
private fun DatasetInfoChip(
    text: String,
    icon: ImageVector? = null
) {
    Row(
        modifier = Modifier
            .height(20.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                shape = RoundedCornerShape(3.dp)
            )
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(10.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Free preview chip — black background, white text, sparkles icon.
 * Matches iOS FreePreviewChip.
 */
@Composable
private fun FreePreviewChip() {
    Row(
        modifier = Modifier
            .height(20.dp)
            .background(
                color = MaterialTheme.colorScheme.inverseSurface,
                shape = RoundedCornerShape(3.dp)
            )
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.AutoAwesome,
            contentDescription = null,
            modifier = Modifier.size(9.dp),
            tint = MaterialTheme.colorScheme.inverseOnSurface
        )
        Text(
            text = "Free Preview",
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.inverseOnSurface
        )
    }
}
