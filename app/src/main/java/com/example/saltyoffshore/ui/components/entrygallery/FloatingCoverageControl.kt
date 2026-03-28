package com.example.saltyoffshore.ui.components.entrygallery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun FloatingCoverageControl(
    minClearPercentage: Float,
    onMinClearPercentageChanged: (Float) -> Unit,
    filteredCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    val isActive = minClearPercentage > 0f
    val displayLabel = if (isActive) "≥ ${minClearPercentage.roundToInt()}%" else "All"
    val countLabel = if (isActive) "$filteredCount of $totalCount" else ""

    val haptic = LocalHapticFeedback.current
    var isDragging by remember { mutableStateOf(false) }
    var lastMilestone by remember { mutableStateOf(0f) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        // Count feedback pill
        AnimatedVisibility(
            visible = countLabel.isNotEmpty(),
            enter = scaleIn(),
            exit = scaleOut()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .background(Color.Black.copy(alpha = 0.8f), CircleShape)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.width(12.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = countLabel,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Main control bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .scale(if (isDragging) 1.02f else 1f)
                .shadow(8.dp, CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Status label capsule
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceContainerLowest,
                        CircleShape
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.WbSunny,
                    contentDescription = null,
                    tint = if (isActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = displayLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.width(12.dp))

            // Coverage slider
            Slider(
                value = minClearPercentage,
                onValueChange = { newValue ->
                    if (!isDragging) {
                        isDragging = true
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    onMinClearPercentageChanged(newValue)
                    // Haptic at 25% milestones
                    val newMilestone = (newValue / 25f).roundToInt() * 25f
                    if (newMilestone != lastMilestone) {
                        lastMilestone = newMilestone
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                },
                onValueChangeFinished = { isDragging = false },
                valueRange = 0f..100f,
                steps = 19,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.width(160.dp)
            )
        }
    }
}
