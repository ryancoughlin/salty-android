package com.example.saltyoffshore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.data.Colorscale

/**
 * Renders a horizontal gradient bar for a colorscale.
 * Use in colorscale picker, legend bars, etc.
 */
@Composable
fun ColorscaleGradient(
    colorscale: Colorscale,
    modifier: Modifier = Modifier,
    height: Int = 24,
    cornerRadius: Int = 4
) {
    val colors = colorscale.hexColors.map { hex ->
        Color(android.graphics.Color.parseColor(hex))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(
                brush = Brush.horizontalGradient(colors = colors)
            )
    )
}

/**
 * Renders a vertical gradient bar for a colorscale.
 */
@Composable
fun ColorscaleGradientVertical(
    colorscale: Colorscale,
    modifier: Modifier = Modifier,
    width: Int = 24,
    cornerRadius: Int = 4
) {
    val colors = colorscale.hexColors.map { hex ->
        Color(android.graphics.Color.parseColor(hex))
    }

    Box(
        modifier = modifier
            .width(width.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(
                brush = Brush.verticalGradient(colors = colors)
            )
    )
}

/**
 * Renders a horizontal gradient from a list of hex colors directly.
 * Useful when not using Colorscale objects.
 */
@Composable
fun HexGradient(
    hexColors: List<String>,
    modifier: Modifier = Modifier,
    height: Int = 24,
    cornerRadius: Int = 4
) {
    val colors = hexColors.map { hex ->
        Color(android.graphics.Color.parseColor(hex))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(
                brush = Brush.horizontalGradient(colors = colors)
            )
    )
}
