package com.example.saltyoffshore.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.data.DatasetPreset
import com.example.saltyoffshore.data.DatasetType

/**
 * Horizontal row of preset filter chips.
 * Shows static + dynamic presets for the current dataset type.
 * Break presets disabled when no crosshair value.
 *
 * Emits chips directly into parent Row — no wrapping layout.
 *
 * iOS ref: Features/Presets/Views/PresetQuickActions.swift
 */
@Composable
fun PresetQuickActions(
    datasetType: DatasetType,
    allPresets: List<DatasetPreset>,
    selectedPreset: DatasetPreset?,
    currentValue: Double?,
    valueRange: ClosedFloatingPointRange<Double>,
    isLoadingPresets: Boolean,
    onPresetSelected: (DatasetPreset) -> Unit
) {
    if (allPresets.isEmpty() && !isLoadingPresets) return

    allPresets.forEach { preset ->
        val isSelected = selectedPreset?.id == preset.id
        val isBreakPreset = preset.id.startsWith("micro_break")
        val isEnabled = !isBreakPreset || currentValue != null

        val rangeText = if (isSelected) {
            preset.calculateRange(currentValue, valueRange)?.let { range ->
                formatRangeText(datasetType, range)
            }
        } else null

        // Dynamic presets already have range in label — don't duplicate
        val effectiveRangeText = if (preset.id.startsWith("dynamic_")) null else rangeText

        QuickActionChip(
            label = preset.label,
            selected = isSelected,
            onClick = { onPresetSelected(preset) },
            enabled = isEnabled,
            trailingText = effectiveRangeText
        )
    }

    if (isLoadingPresets) {
        SuggestionChip(
            onClick = {},
            label = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.5.dp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Loading...",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            },
            shape = RoundedCornerShape(8.dp),
            colors = SuggestionChipDefaults.suggestionChipColors(
                containerColor = Color.White.copy(alpha = 0.20f)
            ),
            border = null
        )
    }
}

/** Format range text by dataset type */
private fun formatRangeText(
    datasetType: DatasetType,
    range: ClosedFloatingPointRange<Double>
): String {
    return when (datasetType) {
        DatasetType.SST -> String.format("%.1f-%.1f\u00B0", range.start, range.endInclusive)
        DatasetType.MLD -> String.format("%.0f-%.0fm", range.start, range.endInclusive)
        DatasetType.CHLOROPHYLL -> String.format("%.2f-%.2f", range.start, range.endInclusive)
        DatasetType.CURRENTS -> String.format("%.1f-%.1f kts", range.start, range.endInclusive)
        DatasetType.SALINITY -> String.format("%.1f-%.1f PSU", range.start, range.endInclusive)
        else -> String.format("%.1f-%.1f", range.start, range.endInclusive)
    }
}
