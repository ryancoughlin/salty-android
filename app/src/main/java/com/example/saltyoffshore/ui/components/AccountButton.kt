package com.example.saltyoffshore.ui.components

import androidx.compose.animation.core.animateFloatAsState
import com.example.saltyoffshore.ui.theme.SaltyMotion
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * Circular gradient account button matching iOS AccountButton.swift.
 *
 * - Linear gradient: blue (0x1A99E6) to teal (0x33CCCC), topLeading -> bottomTrailing
 * - White person icon (26dp)
 * - White border at 0.3 opacity
 * - Drop shadow
 * - Press-scale animation: 0.92x on press, spring return
 */
private val TopBarElementHeight = 44.dp

@Composable
fun AccountButton(
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    val accountGradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.tertiary
        ),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = SaltyMotion.springBouncy(),
        label = "accountButtonScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(TopBarElementHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = 8.dp,
                shape = CircleShape,
                ambientColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                spotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
            )
            .clip(CircleShape)
            .background(accountGradient)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f),
                shape = CircleShape
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                        onClick()
                    }
                )
            }
    ) {
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = "Account",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(26.dp)
        )
    }
}
