package com.example.saltyoffshore.ui.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saltyoffshore.R
import com.example.saltyoffshore.ui.theme.SplineSans
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Colors matching iOS LaunchView
private val BaseBackground = Color(0xFF262626)
private val AccentColor = Color(0xFF3B909C)

/**
 * Animated launch screen matching iOS LaunchView.swift.
 *
 * Sequence:
 * 1. Logo mark springs in (scale + rotation)
 * 2. "Salty" text slides in from left
 * 3. "Offshore" text slides in from right
 * 4. Hold, then fade out
 * 5. Call onFinished
 */
@Composable
fun LaunchView(onFinished: () -> Unit) {
    val logoScale = remember { Animatable(0.5f) }
    val logoAlpha = remember { Animatable(0f) }
    val logoRotation = remember { Animatable(-30f) }
    val saltyOffsetX = remember { Animatable(-50f) }
    val saltyAlpha = remember { Animatable(0f) }
    val offshoreOffsetX = remember { Animatable(50f) }
    val offshoreAlpha = remember { Animatable(0f) }
    val containerAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // 1. Logo springs in (3 properties animate in parallel)
        coroutineScope {
            val springSpec = spring<Float>(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)
            launch { logoScale.animateTo(1f, springSpec) }
            launch { logoAlpha.animateTo(1f, springSpec) }
            launch { logoRotation.animateTo(0f, springSpec) }
        }

        delay(300)

        // 2. "Salty" slides in
        coroutineScope {
            val springSpec = spring<Float>(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
            launch { saltyOffsetX.animateTo(0f, springSpec) }
            launch { saltyAlpha.animateTo(1f, springSpec) }
        }

        delay(150)

        // 3. "Offshore" slides in
        coroutineScope {
            val springSpec = spring<Float>(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
            launch { offshoreOffsetX.animateTo(0f, springSpec) }
            launch { offshoreAlpha.animateTo(1f, springSpec) }
        }

        // 4. Hold, then fade out
        delay(1000)
        containerAlpha.animateTo(0f, tween(400))
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BaseBackground)
            .alpha(containerAlpha.value),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.salty_mark),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .scale(logoScale.value)
                    .rotate(logoRotation.value)
                    .alpha(logoAlpha.value)
            )

            Column(verticalArrangement = Arrangement.spacedBy((-2).dp)) {
                Text(
                    text = "Salty",
                    fontFamily = SplineSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = Color.White,
                    modifier = Modifier
                        .graphicsLayer { translationX = saltyOffsetX.value }
                        .alpha(saltyAlpha.value)
                )
                Text(
                    text = "Offshore",
                    fontFamily = SplineSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = AccentColor,
                    modifier = Modifier
                        .graphicsLayer { translationX = offshoreOffsetX.value }
                        .alpha(offshoreAlpha.value)
                )
            }
        }
    }
}
