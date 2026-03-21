package com.example.saltyoffshore.ui.controls.layercontrols

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.ui.theme.SaltyColors
import com.example.saltyoffshore.ui.theme.SaltyLayout

/**
 * Reusable section component for layer controls.
 * Shows header with toggle, expands to show content when enabled.
 * Matches iOS LayerSection exactly.
 *
 * Styling mirrors iOS `.layerCard(selected:)`:
 * - OFF: Clear bg, subtle border
 * - ON: Raised bg, subtle border
 */
@Composable
fun LayerSection(
    title: String,
    enabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    val internalPadding = 12.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (enabled) SaltyColors.raised else Color.Transparent)
            .border(
                width = 1.dp,
                color = SaltyColors.borderSubtle,
                shape = shape
            )
    ) {
        // Header row with title and toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEnabledChanged(!enabled) }
                .padding(internalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = SaltyColors.textPrimary
            )

            Spacer(modifier = Modifier.weight(1f))

            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChanged,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = SaltyColors.borderSubtle
                )
            )
        }

        // Content when enabled
        AnimatedVisibility(
            visible = enabled,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = internalPadding),
                    color = SaltyColors.borderSubtle
                )
                Box(modifier = Modifier.padding(internalPadding)) {
                    content()
                }
            }
        }
    }
}
