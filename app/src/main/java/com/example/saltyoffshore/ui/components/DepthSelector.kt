package com.example.saltyoffshore.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saltyoffshore.data.DepthFilterState

/**
 * Design constants matching iOS DepthSelectorConstants
 */
private object DepthSelectorConstants {
    // Layout & Sizing
    val controlWidth = 48.dp
    val depthStopHeight = 40.dp
    val cornerRadius = 12.dp
    val padding = 6.dp
    val depthSpacing = 2.dp
    val innerCornerRadius = 8.dp

    // Typography
    val depthFontSize = 11.sp
    val unitFontSize = 9.sp

    // Animation
    const val animationStiffness = Spring.StiffnessMediumLow
    const val animationDamping = Spring.DampingRatioMediumBouncy
}

/**
 * Vertical depth selector - supports both tap and drag gestures.
 * 0m at top, deepest at bottom.
 */
@Composable
fun DepthSelector(
    depthFilter: DepthFilterState,
    onDepthSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Reversed so 0m is at top, deepest at bottom
    val reversedDepths = remember(depthFilter.availableDepths) {
        depthFilter.availableDepths.reversed()
    }

    val itemHeightPx = with(androidx.compose.ui.platform.LocalDensity.current) {
        (DepthSelectorConstants.depthStopHeight + DepthSelectorConstants.depthSpacing).toPx()
    }
    val paddingPx = with(androidx.compose.ui.platform.LocalDensity.current) {
        DepthSelectorConstants.padding.toPx()
    }

    Surface(
        modifier = modifier
            .width(DepthSelectorConstants.controlWidth)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(DepthSelectorConstants.cornerRadius),
                spotColor = Color.Black.copy(alpha = 0.1f)
            )
            .clip(RoundedCornerShape(DepthSelectorConstants.cornerRadius))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(DepthSelectorConstants.cornerRadius)
            )
            .pointerInput(reversedDepths, depthFilter.selectedDepth) {
                detectVerticalDragGestures { change, _ ->
                    change.consume()
                    val contentY = change.position.y - paddingPx
                    val rawIndex = (contentY / itemHeightPx).toInt()
                    val clampedIndex = rawIndex.coerceIn(0, reversedDepths.size - 1)
                    val targetDepth = reversedDepths[clampedIndex]

                    if (targetDepth != depthFilter.selectedDepth) {
                        onDepthSelected(targetDepth)
                    }
                }
            },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
        shape = RoundedCornerShape(DepthSelectorConstants.cornerRadius)
    ) {
        Column(
            modifier = Modifier.padding(DepthSelectorConstants.padding),
            verticalArrangement = Arrangement.spacedBy(DepthSelectorConstants.depthSpacing)
        ) {
            reversedDepths.forEach { depth ->
                DepthStop(
                    depth = depth,
                    isSelected = depthFilter.selectedDepth == depth,
                    onClick = { onDepthSelected(depth) }
                )
            }
        }
    }
}

@Composable
private fun DepthStop(
    depth: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.surface
        } else {
            Color.Transparent
        },
        animationSpec = spring(
            stiffness = DepthSelectorConstants.animationStiffness,
            dampingRatio = DepthSelectorConstants.animationDamping
        ),
        label = "depthStopBackground"
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        },
        animationSpec = spring(
            stiffness = DepthSelectorConstants.animationStiffness,
            dampingRatio = DepthSelectorConstants.animationDamping
        ),
        label = "depthStopTextColor"
    )

    val unitColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        },
        animationSpec = spring(
            stiffness = DepthSelectorConstants.animationStiffness,
            dampingRatio = DepthSelectorConstants.animationDamping
        ),
        label = "depthStopUnitColor"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(DepthSelectorConstants.depthStopHeight)
            .clip(RoundedCornerShape(DepthSelectorConstants.innerCornerRadius))
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$depth",
                fontSize = DepthSelectorConstants.depthFontSize,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                fontFamily = FontFamily.Monospace,
                color = textColor,
                maxLines = 1
            )
            Text(
                text = "m",
                fontSize = DepthSelectorConstants.unitFontSize,
                fontFamily = FontFamily.Monospace,
                color = unitColor
            )
        }
    }
}
