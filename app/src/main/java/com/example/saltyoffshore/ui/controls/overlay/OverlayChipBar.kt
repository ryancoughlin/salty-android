package com.example.saltyoffshore.ui.controls.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saltyoffshore.data.DatasetType
import com.example.saltyoffshore.ui.theme.SaltyColors
import com.example.saltyoffshore.ui.theme.Spacing

/**
 * Horizontal scrollable row of active overlay chips plus an add button.
 * Matches iOS OverlayChipBar.
 */
@Composable
fun OverlayChipBar(
    overlayOrder: List<DatasetType>,
    onAddOverlay: () -> Unit,
    onRemoveOverlay: (DatasetType) -> Unit,
    onOverlayTap: (DatasetType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Spacing.large),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Active overlay chips
        for (type in overlayOrder) {
            OverlayChip(
                type = type,
                onTap = { onOverlayTap(type) },
                onRemove = { onRemoveOverlay(type) }
            )
        }

        // Add button
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(SaltyColors.raised)
                .clickable { onAddOverlay() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add overlay",
                tint = SaltyColors.textSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun OverlayChip(
    type: DatasetType,
    onTap: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SaltyColors.raised)
            .clickable { onTap() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = type.shortName,
            color = SaltyColors.textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )

        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Remove ${type.shortName}",
            tint = SaltyColors.textSecondary,
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .clickable { onRemove() }
        )
    }
}
