package com.example.saltyoffshore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saltyoffshore.data.DatasetType
import com.example.saltyoffshore.viewmodel.AppViewModel

/**
 * Matches iOS PrimaryDatasetPage layout exactly:
 * - Row 1: Dataset name (16pt Medium) + Change/Collapse buttons
 * - Row 2: GradientScaleBar
 * - Row 3: TimelineControl
 * - Glass background (white with blur effect simulated)
 * - 12pt between major elements, 8pt between rows
 */
@Composable
fun SaltyDatasetControl(
    viewModel: AppViewModel,
    onExpand: () -> Unit = {},
    onChange: () -> Unit = {},
    onFilter: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val dataset = viewModel.selectedDataset
    val entry = viewModel.selectedEntry

    if (dataset == null) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1: Dataset name + buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dataset.name,
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TextButton(onClick = onFilter) {
                    Icon(Icons.Default.Tune, contentDescription = "Filter", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Filter", color = Color.White, fontSize = 12.sp)
                }
                TextButton(onClick = onChange) {
                    Icon(Icons.Default.SwapVert, contentDescription = "Change", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Change", color = Color.White, fontSize = 12.sp)
                }
                IconButton(onClick = onExpand) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Collapse", tint = Color.White)
                }
            }
        }

        // Row 2: Gradient scale bar
        entry?.let { currentEntry ->
            val datasetType = DatasetType.fromRawValue(dataset.type)
            val rangeKey = datasetType?.rangeKey ?: dataset.type
            val range = currentEntry.ranges?.get(rangeKey)
            val colorscale = datasetType?.defaultColorscale

            if (colorscale != null) {
                GradientScaleBar(
                    min = range?.min ?: viewModel.renderingSnapshot.dataMin,
                    max = range?.max ?: viewModel.renderingSnapshot.dataMax,
                    unit = range?.unit ?: "°F",
                    colorscale = colorscale
                )
            }
        }

        // Row 3: Timeline
        TimelineControl(
            entries = dataset.entries,
            selectedEntry = entry,
            onEntrySelected = { viewModel.selectEntry(it) }
        )
    }
}
