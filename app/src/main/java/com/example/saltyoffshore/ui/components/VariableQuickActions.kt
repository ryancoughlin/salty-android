package com.example.saltyoffshore.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.example.saltyoffshore.data.DatasetVariable

/**
 * Variable selector chips for datasets with multiple variables (e.g., Temperature vs Gradient).
 * Only renders when there are 2+ visible variables.
 *
 * Emits chips directly into parent Row — no wrapping layout.
 *
 * iOS ref: Features/Presets/Views/VariableQuickActions.swift
 */
@Composable
fun VariableQuickActions(
    variables: List<DatasetVariable>,
    selectedVariable: DatasetVariable,
    onVariableSelected: (DatasetVariable) -> Unit
) {
    val visibleVariables = remember(variables) { variables.filter { it.isVisible } }
    if (visibleVariables.size <= 1) return

    visibleVariables.forEach { variable ->
        QuickActionChip(
            label = variable.displayName,
            selected = variable.id == selectedVariable.id,
            onClick = { onVariableSelected(variable) }
        )
    }
}
