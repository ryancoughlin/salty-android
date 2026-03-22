package com.example.saltyoffshore.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.ui.theme.ColorScales

enum class SSTLoadingSize(val frameSize: Dp, val strokeWidth: Dp) {
    ExtraSmall(16.dp, 2.dp),
    Small(20.dp, 2.4.dp),
    Medium(24.dp, 3.dp),
    Large(54.dp, 4.dp)
}

private val SSTColors = ColorScales.sst.map { hex ->
    Color(android.graphics.Color.parseColor(hex))
}
private val SSTSweepBrush = Brush.sweepGradient(SSTColors)

@Composable
fun SSTLoadingIndicator(
    size: SSTLoadingSize = SSTLoadingSize.Medium,
    modifier: Modifier = Modifier
) {

    val infiniteTransition = rememberInfiniteTransition(label = "sst-loading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Canvas(
        modifier = modifier.size(size.frameSize)
    ) {
        rotate(rotation) {
            drawArc(
                brush = SSTSweepBrush,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = size.strokeWidth.toPx())
            )
        }
    }
}
