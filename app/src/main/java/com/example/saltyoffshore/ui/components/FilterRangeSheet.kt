package com.example.saltyoffshore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SheetState
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saltyoffshore.data.Colorscale
import com.example.saltyoffshore.data.DatasetType

/**
 * Modal bottom sheet for filtering data range.
 * Matches iOS DatasetFilterSheet with FilterGradientBar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterRangeSheet(
    datasetType: DatasetType,
    colorscale: Colorscale,
    currentMin: Double,
    currentMax: Double,
    dataMin: Double,
    dataMax: Double,
    unit: String,
    sheetState: SheetState,
    onRangeChanged: (min: Double, max: Double) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    // Track if range has been modified
    val isModified = currentMin != dataMin || currentMax != dataMax

    // Local slider state (as floats for RangeSlider)
    var sliderRange by remember(currentMin, currentMax) {
        mutableStateOf(currentMin.toFloat()..currentMax.toFloat())
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1C1C1E),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Range & Color",
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                )

                Row {
                    // Reset button (only visible when modified)
                    if (isModified) {
                        TextButton(onClick = {
                            sliderRange = dataMin.toFloat()..dataMax.toFloat()
                            onReset()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset",
                                tint = Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "Reset",
                                color = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Min/Max value display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ValueLabel(
                    value = sliderRange.start.toDouble(),
                    unit = unit,
                    decimalPlaces = datasetType.numberDecimalPlaces
                )
                ValueLabel(
                    value = sliderRange.endInclusive.toDouble(),
                    unit = unit,
                    decimalPlaces = datasetType.numberDecimalPlaces
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Gradient background bar
            val gradientColors = colorscale.stops.map { Color(it.color) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = if (gradientColors.isNotEmpty()) gradientColors else listOf(Color.Gray, Color.White)
                        )
                    )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Range slider
            RangeSlider(
                value = sliderRange,
                onValueChange = { range ->
                    sliderRange = range
                },
                onValueChangeFinished = {
                    onRangeChanged(
                        sliderRange.start.toDouble(),
                        sliderRange.endInclusive.toDouble()
                    )
                },
                valueRange = dataMin.toFloat()..dataMax.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White.copy(alpha = 0.3f),
                    inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Info text
            Text(
                text = "Drag handles to filter data range",
                style = TextStyle(
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f)
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ValueLabel(
    value: Double,
    unit: String,
    decimalPlaces: Int
) {
    val formattedValue = String.format("%.${decimalPlaces}f", value)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = formattedValue,
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        )
        Text(
            text = " $unit",
            style = TextStyle(
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        )
    }
}
