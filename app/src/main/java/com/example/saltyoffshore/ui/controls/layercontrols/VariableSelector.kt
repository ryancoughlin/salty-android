package com.example.saltyoffshore.ui.controls.layercontrols

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.data.DatasetVariable

/**
 * Variable selector for datasets with multiple variables (e.g., Temperature vs Gradient).
 * Uses SegmentedButtonRow matching iOS variable selection chips.
 *
 * iOS ref: Views/DatasetControls/Components/QuickActionsBar variable chips
 */
@Composable
fun VariableSelector(
    variables: List<DatasetVariable>,
    selectedVariableId: String?,
    onVariableSelected: (DatasetVariable) -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleVariables = variables.filter { it.isVisible }
    if (visibleVariables.size <= 1) return

    val effectiveSelectedId = selectedVariableId ?: visibleVariables.firstOrNull()?.id

    Column(modifier = modifier) {
        Text(
            text = "Variable",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        SingleChoiceSegmentedButtonRow {
            visibleVariables.forEachIndexed { index, variable ->
                SegmentedButton(
                    selected = variable.id == effectiveSelectedId,
                    onClick = { onVariableSelected(variable) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = visibleVariables.size
                    )
                ) {
                    Text(variable.displayName)
                }
            }
        }
    }
}
