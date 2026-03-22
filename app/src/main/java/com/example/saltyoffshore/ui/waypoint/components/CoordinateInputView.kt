package com.example.saltyoffshore.ui.waypoint.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.data.coordinate.CoordinateAxisConfig
import com.example.saltyoffshore.data.coordinate.CoordinateAxisValues
import com.example.saltyoffshore.data.coordinate.CoordinateFormatter
import com.example.saltyoffshore.data.coordinate.GPSFormat
import com.example.saltyoffshore.data.coordinate.latitudeConfig
import com.example.saltyoffshore.data.coordinate.longitudeConfig
import com.example.saltyoffshore.data.waypoint.WaypointFormState

/**
 * GPS coordinate input view that adapts to the user's preferred format (DMM, DMS, DD).
 * Config-driven layout -- one CoordinateRow renders any format.
 * Ports iOS CoordinateInputView + CoordinateRowView.
 */
@Composable
fun CoordinateInputView(
    formState: WaypointFormState,
    gpsFormat: GPSFormat,
    onFormStateChange: (WaypointFormState) -> Unit
) {
    val isLatitudeValid = CoordinateFormatter.parse(
        formState.latitudeValues, gpsFormat, isLatitude = true
    ) != null
    val isLongitudeValid = CoordinateFormatter.parse(
        formState.longitudeValues, gpsFormat, isLatitude = false
    ) != null

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CoordinateRow(
            config = gpsFormat.latitudeConfig(),
            values = formState.latitudeValues,
            isValid = isLatitudeValid,
            onValuesChange = { onFormStateChange(formState.copy(latitudeValues = it)) }
        )

        CoordinateRow(
            config = gpsFormat.longitudeConfig(),
            values = formState.longitudeValues,
            isValid = isLongitudeValid,
            onValuesChange = { onFormStateChange(formState.copy(longitudeValues = it)) }
        )
    }
}

/**
 * Single coordinate axis row (latitude or longitude).
 * Renders segment text fields from config + direction segmented button.
 * Ports iOS CoordinateRowView.
 */
@Composable
private fun CoordinateRow(
    config: CoordinateAxisConfig,
    values: CoordinateAxisValues,
    isValid: Boolean,
    onValuesChange: (CoordinateAxisValues) -> Unit
) {
    val hasInput = values.segments.any { it.isNotEmpty() } && values.direction.isNotEmpty()

    Column {
        // Label + validation icon
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = config.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (hasInput && !isValid) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.Warning,
                    contentDescription = "Invalid coordinate",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.height(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Segment input fields
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            config.segments.forEachIndexed { index, segmentConfig ->
                val segmentValue = values.segments.getOrElse(index) { "" }
                OutlinedTextField(
                    value = segmentValue,
                    onValueChange = { newValue ->
                        val newSegments = values.segments.toMutableList()
                        // Ensure list is large enough
                        while (newSegments.size <= index) newSegments.add("")
                        newSegments[index] = newValue
                        onValuesChange(values.copy(segments = newSegments))
                    },
                    label = { Text(segmentConfig.unit) },
                    placeholder = { Text(segmentConfig.placeholder) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = hasInput && !isValid,
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Direction picker (N/S or E/W)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            config.directionOptions.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = values.direction == option,
                    onClick = { onValuesChange(values.copy(direction = option)) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = config.directionOptions.size
                    )
                ) {
                    Text(option)
                }
            }
        }
    }
}
