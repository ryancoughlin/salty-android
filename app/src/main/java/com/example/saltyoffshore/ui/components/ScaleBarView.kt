package com.example.saltyoffshore.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saltyoffshore.config.CrosshairConstants
import kotlin.math.pow

/**
 * Scale bar showing distance at current zoom level.
 * Only visible at zoom >= 8.
 * Matches iOS ScaleBarView.
 */
@Composable
fun ScaleBarView(
    zoom: Double,
    latitude: Double,
    modifier: Modifier = Modifier
) {
    val visible = zoom >= CrosshairConstants.SCALE_BAR_MIN_ZOOM

    // Calculate scale bar width and label
    val (widthDp, label) = remember(zoom, latitude) {
        calculateScaleBar(zoom, latitude)
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            // Scale bar line
            Canvas(
                modifier = Modifier
                    .width(widthDp.dp)
                    .height(20.dp)
            ) {
                val strokeWidth = 2.dp.toPx()
                val endCapHeight = 6.dp.toPx()

                // Main horizontal line
                drawLine(
                    color = Color.White,
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )

                // Left end cap
                drawLine(
                    color = Color.White,
                    start = Offset(strokeWidth / 2, size.height / 2 - endCapHeight / 2),
                    end = Offset(strokeWidth / 2, size.height / 2 + endCapHeight / 2),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )

                // Right end cap
                drawLine(
                    color = Color.White,
                    start = Offset(size.width - strokeWidth / 2, size.height / 2 - endCapHeight / 2),
                    end = Offset(size.width - strokeWidth / 2, size.height / 2 + endCapHeight / 2),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )

                // Shadow
                val shadowOffset = 1.dp.toPx()
                drawLine(
                    color = Color.Black.copy(alpha = 0.5f),
                    start = Offset(shadowOffset, size.height / 2 + shadowOffset),
                    end = Offset(size.width + shadowOffset, size.height / 2 + shadowOffset),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }

            // Label below
            Text(
                text = label,
                style = TextStyle(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                ),
                modifier = Modifier.padding(top = 24.dp)
            )
        }
    }
}

/**
 * Calculate scale bar width in dp and distance label.
 */
private fun calculateScaleBar(zoom: Double, latitude: Double): Pair<Float, String> {
    // Meters per pixel at zoom level
    // Formula: 156543.03392 * cos(lat) / 2^zoom
    val metersPerPixel = 156543.03392 * kotlin.math.cos(Math.toRadians(latitude)) / 2.0.pow(zoom)

    // Target scale bar width in pixels (80-120 range)
    val targetPixels = 100.0

    // Meters represented by target width
    val metersAtTarget = metersPerPixel * targetPixels

    // Find nice round number
    val (niceDistance, label) = when {
        metersAtTarget >= 10000 -> {
            val km = (metersAtTarget / 1000).roundToNice()
            km * 1000 to "${km.toInt()} km"
        }
        metersAtTarget >= 1000 -> {
            val km = (metersAtTarget / 1000).roundToNice()
            km * 1000 to "${km.toInt()} km"
        }
        metersAtTarget >= 100 -> {
            val m = metersAtTarget.roundToNice()
            m to "${m.toInt()} m"
        }
        else -> {
            val nm = metersAtTarget / 1852.0
            val niceNm = nm.roundToNice()
            niceNm * 1852 to "${"%.1f".format(niceNm)} nm"
        }
    }

    // Calculate pixel width for nice distance
    val widthPixels = niceDistance / metersPerPixel

    return widthPixels.toFloat() to label
}

/**
 * Round to nice number (1, 2, 5, 10, 20, 50, etc.)
 */
private fun Double.roundToNice(): Double {
    val magnitude = 10.0.pow(kotlin.math.floor(kotlin.math.log10(this)))
    val normalized = this / magnitude

    val nice = when {
        normalized < 1.5 -> 1.0
        normalized < 3.5 -> 2.0
        normalized < 7.5 -> 5.0
        else -> 10.0
    }

    return nice * magnitude
}
