package com.example.saltyoffshore.ui.components

import androidx.compose.runtime.Composable

/**
 * Depth selection chip for datasets with multiple depths.
 * Shows current depth, taps to cycle through available depths.
 *
 * Emits a single chip directly into parent Row — no wrapping layout.
 *
 * iOS ref: Map/Controls/DepthQuickAction (part of QuickActionsBar)
 */
@Composable
fun DepthQuickAction(
    availableDepths: List<Int>,
    selectedDepth: Int,
    onDepthSelected: (Int) -> Unit
) {
    if (availableDepths.size <= 1) return

    val depthLabel = if (selectedDepth == 0) "Surface" else "${selectedDepth}m"

    val currentIndex = availableDepths.indexOf(selectedDepth).coerceAtLeast(0)
    val nextIndex = (currentIndex + 1) % availableDepths.size

    QuickActionChip(
        label = depthLabel,
        selected = true,
        onClick = { onDepthSelected(availableDepths[nextIndex]) }
    )
}
