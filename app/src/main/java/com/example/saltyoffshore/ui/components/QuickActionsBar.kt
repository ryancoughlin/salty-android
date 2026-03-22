package com.example.saltyoffshore.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.ui.theme.Spacing

/**
 * Horizontal scrollable bar for quick-access chips: variables, depth, presets.
 * Sits between the right-side toolbar and bottom controls.
 *
 * Currently a placeholder — will be populated in Phase 3 (presets) and Phase 4 (variables/colorscale).
 *
 * iOS ref: Map/Controls/QuickActionsBar.swift
 */
@Composable
fun QuickActionsBar(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = Spacing.medium)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Phase 3: Preset chips
        // Phase 4: Variable chips, Depth chip
    }
}
