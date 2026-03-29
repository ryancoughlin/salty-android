package com.example.saltyoffshore.ui.controls.overlay

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saltyoffshore.data.DatasetType
import com.example.saltyoffshore.ui.theme.SaltyColors
import com.example.saltyoffshore.ui.theme.Spacing

/**
 * Bottom sheet for adding/removing overlay datasets.
 * Shows all dataset types except the current primary.
 * Matches iOS OverlayPickerSheet + AddOverlayPicker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverlayPickerSheet(
    availableTypes: List<DatasetType>,
    activeOverlays: Set<DatasetType>,
    onToggle: (DatasetType) -> Unit,
    sheetState: SheetState,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SaltyColors.base,
        contentColor = SaltyColors.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.large)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Overlay Datasets",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = SaltyColors.textPrimary
            )

            Spacer(Modifier.height(Spacing.medium))

            for (type in availableTypes) {
                val isActive = type in activeOverlays
                OverlayPickerRow(
                    type = type,
                    isActive = isActive,
                    onToggle = { onToggle(type) }
                )
            }
        }
    }
}

@Composable
private fun OverlayPickerRow(
    type: DatasetType,
    isActive: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = type.displayName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = SaltyColors.textPrimary
            )
            Text(
                text = type.shortName,
                fontSize = 13.sp,
                color = SaltyColors.textSecondary
            )
        }

        Icon(
            imageVector = if (isActive) Icons.Default.Check else Icons.Default.Add,
            contentDescription = if (isActive) "Active" else "Add",
            tint = if (isActive) SaltyColors.accent else SaltyColors.textSecondary,
            modifier = Modifier.size(22.dp)
        )
    }
}
