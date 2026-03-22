package com.example.saltyoffshore.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.data.DatasetRenderConfig
import com.example.saltyoffshore.data.DatasetType
import com.example.saltyoffshore.data.DatasetUnit
import com.example.saltyoffshore.data.FilterMode
import com.example.saltyoffshore.data.TemperatureUnits
import com.example.saltyoffshore.data.renderingConfig
import com.example.saltyoffshore.ui.theme.SaltyLayout
import com.example.saltyoffshore.ui.theme.SaltyType
import com.example.saltyoffshore.ui.theme.Spacing

/**
 * Fixed-position filter panel — NOT a ModalBottomSheet.
 *
 * ModalBottomSheet's anchoredDraggable steals horizontal drag events from
 * FilterGradientBar handles, making them unusable. iOS uses a fixed sheet with
 * interactiveDismissDisabled(true) — no swipe dismiss at all. This matches that
 * by rendering a scrim + bottom-anchored Surface with no drag gesture system.
 */
@Composable
fun DatasetFilterSheet(
    config: DatasetRenderConfig,
    dataRange: ClosedFloatingPointRange<Double>,
    datasetType: DatasetType,
    apiUnit: DatasetUnit,
    temperatureUnits: TemperatureUnits = TemperatureUnits.FAHRENHEIT,
    decimalPlaces: Int = 1,
    onConfigChanged: (DatasetRenderConfig) -> Unit,
    onDragRangeChanged: ((Float, Float) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isFilterModified = config.customRange != null
    val effectiveColorscale = config.colorscale ?: datasetType.renderingConfig.colorscale
    val defaultColorscale = datasetType.renderingConfig.colorscale

    // Scrim — tapping dismisses (matches ModalBottomSheet behavior)
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.32f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onDismiss() }
        )

        // Bottom-anchored panel
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .heightIn(min = 240.dp)
            ) {
                // ── Header (56dp, matches iOS) ──────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = Spacing.extraLarge),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Range & Color",
                        style = SaltyType.heading,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                    ) {
                        if (isFilterModified) {
                            TextButton(onClick = {
                                onConfigChanged(config.clearFilter())
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }) {
                                Text(
                                    text = "Reset",
                                    style = SaltyType.body.copy(color = MaterialTheme.colorScheme.primary)
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                // ── Top controls ────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.large, vertical = Spacing.medium),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.weight(1f))

                    FilterModePicker(
                        selected = config.filterMode,
                        onSelected = { mode ->
                            onConfigChanged(config.copy(filterMode = mode))
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    )

                    ColorscalePickerButton(
                        selection = config.colorscale,
                        defaultColorscale = defaultColorscale,
                        onChanged = { newColorscale ->
                            onConfigChanged(config.copy(colorscale = newColorscale))
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    )
                }

                // ── FilterGradientBar ───────────────────────────────────
                FilterGradientBar(
                    selectedRange = config.customRange,
                    valueRange = dataRange,
                    colorscale = effectiveColorscale,
                    datasetType = datasetType,
                    apiUnit = apiUnit,
                    temperatureUnits = temperatureUnits,
                    onRangeChanged = { newRange ->
                        onConfigChanged(config.copy(customRange = newRange))
                    },
                    onDragRangeChanged = onDragRangeChanged,
                    decimalPlaces = decimalPlaces,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.large)
                )

                Spacer(modifier = Modifier.height(Spacing.extraLarge))
            }
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
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterMode.entries.forEach { mode ->
            val isActive = mode == selected
            TextButton(
                onClick = { onSelected(mode) }
            ) {
                Text(
                    text = mode.displayName,
                    style = SaltyType.bodySmall.copy(
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}
