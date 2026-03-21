package com.example.saltyoffshore.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.ceil

/**
 * Canvas-based checkerboard pattern.
 * Matches iOS CheckerboardPattern exactly.
 *
 * Colors: White at 50% opacity + Gray at 10% opacity, alternating by (row + column) parity.
 */
enum class CheckerboardSize(val cellDp: Float) {
    SMALL(3f),  // For GradientScaleBar (6px height)
    LARGE(8f)   // For FilterGradientBar (30px height)
}

@Composable
fun CheckerboardPattern(
    size: CheckerboardSize = CheckerboardSize.LARGE,
    modifier: Modifier = Modifier
) {
    val primaryColor = Color.White.copy(alpha = 0.5f)
    val secondaryColor = Color.Gray.copy(alpha = 0.1f)
    val cellSizeDp = size.cellDp

    Canvas(modifier = modifier) {
        val cellSizePx = cellSizeDp * density
        val rowCount = ceil(this.size.height / cellSizePx).toInt()
        val columnCount = ceil(this.size.width / cellSizePx).toInt()

        for (row in 0 until rowCount) {
            for (column in 0 until columnCount) {
                val color = if ((row + column) % 2 == 0) primaryColor else secondaryColor
                drawRect(
                    color = color,
                    topLeft = Offset(column * cellSizePx, row * cellSizePx),
                    size = Size(cellSizePx, cellSizePx)
                )
            }
        }
    }
}
