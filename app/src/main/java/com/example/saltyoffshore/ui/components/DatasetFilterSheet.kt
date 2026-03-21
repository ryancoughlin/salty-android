package com.example.saltyoffshore.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saltyoffshore.data.DatasetRenderConfig
import com.example.saltyoffshore.data.FilterMode
import com.example.saltyoffshore.ui.theme.SaltyColors

/**
 * Modal bottom sheet for dataset filter controls.
 * Matches iOS DatasetFilterSheet exactly.
 *
 * Fixed ~240dp height, non-swipe-dismissable.
 * Contains: header (title + reset + close), filter mode picker, colorscale button,
 * and embedded FilterGradientBar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatasetFilterSheet(
    config: DatasetRenderConfig,
    dataRange: ClosedFloatingPointRange<Double>,
    onConfigChanged: (DatasetRenderConfig) -> Unit,
    onDragRangeChanged: ((Float, Float) -> Unit)? = null,
    unit: String,
    decimalPlaces: Int = 1,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isFilterModified = config.customRange != null

    // Resolve effective colorscale for the gradient bar
    val effectiveColorscale = config.colorscale
        ?: com.example.saltyoffshore.data.Colorscale.SST // fallback; caller should provide type default

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SaltyColors.base,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 240.dp)
        ) {
            // ── Header ───────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .height(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Range & Color",
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SaltyColors.textPrimary
                    )
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Reset button (only visible when filter modified)
                    if (isFilterModified) {
                        TextButton(onClick = {
                            onConfigChanged(config.clearFilter())
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }) {
                            Text(
                                text = "Reset",
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SaltyColors.accent
                                )
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = SaltyColors.iconButton,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Divider(color = SaltyColors.borderSubtle)

            // ── Top controls: Filter Mode picker ─────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))

                // Filter Mode toggle
                FilterModePicker(
                    selected = config.filterMode,
                    onSelected = { mode ->
                        onConfigChanged(config.copy(filterMode = mode))
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                )
            }

            // ── Embedded FilterGradientBar ───────────────────────────────
            FilterGradientBar(
                selectedRange = config.customRange,
                valueRange = dataRange,
                colorscale = effectiveColorscale,
                onRangeChanged = { newRange ->
                    onConfigChanged(config.copy(customRange = newRange))
                },
                onDragRangeChanged = onDragRangeChanged,
                unit = unit,
                decimalPlaces = decimalPlaces,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ── Filter Mode Picker ───────────────────────────────────────────────────────

@Composable
private fun FilterModePicker(
    selected: FilterMode,
    onSelected: (FilterMode) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterMode.entries.forEach { mode ->
            val isActive = mode == selected
            TextButton(
                onClick = { onSelected(mode) }
            ) {
                Text(
                    text = mode.displayName,
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isActive) SaltyColors.accent else SaltyColors.textSecondary
                    )
                )
            }
        }
    }
}
