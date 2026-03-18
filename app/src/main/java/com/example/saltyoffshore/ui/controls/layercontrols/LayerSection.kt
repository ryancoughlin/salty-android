package com.example.saltyoffshore.ui.controls.layercontrols

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Reusable section component for layer controls.
 * Shows header with toggle, expands to show content when enabled.
 * Matches iOS LayerSection exactly.
 *
 * Styling:
 * - OFF: Outline button style (clear bg, solid border, black text)
 * - ON: Filled style (white bg, subtle border, black text)
 */
@Composable
fun LayerSection(
    title: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val backgroundColor by animateFloatAsState(
        targetValue = if (enabled) 1f else 0f,
        label = "bg"
    )

    val shape = RoundedCornerShape(12.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (enabled) Color.White else Color.Transparent
            )
            .border(
                width = 1.dp,
                color = if (enabled) Color.LightGray.copy(alpha = 0.5f) else Color.Gray.copy(alpha = 0.3f),
                shape = shape
            )
    ) {
        // Header row with title and toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle(!enabled) }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                color = Color.Black
            )

            Spacer(modifier = Modifier.weight(1f))

            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f)
                )
            )
        }

        // Content when enabled
        AnimatedVisibility(
            visible = enabled,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                Divider(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    color = Color.LightGray.copy(alpha = 0.5f)
                )
                Box(modifier = Modifier.padding(12.dp)) {
                    content()
                }
            }
        }
    }
}
