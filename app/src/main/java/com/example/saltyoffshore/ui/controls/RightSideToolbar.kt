package com.example.saltyoffshore.ui.controls

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.R
import com.example.saltyoffshore.ui.theme.SaltyLayout
import kotlinx.coroutines.delay

/**
 * Right-side toolbar buttons matching iOS RightSideToolbar.
 * Buttons appear with staggered spring animations and hide when bottom control expands.
 *
 * iOS ref: Map/Controls/RightSideToolbar.swift
 */
@Composable
fun RightSideToolbar(
    onFilterClick: () -> Unit,
    onLayersClick: () -> Unit,
    shouldHide: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Entrance animation state
    var hasAppeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(250)
        hasAppeared = true
    }

    // Hide animation
    val hideAlpha by animateFloatAsState(
        targetValue = if (shouldHide) 0f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "toolbar_hide"
    )
    val hideScale by animateFloatAsState(
        targetValue = if (shouldHide) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "toolbar_scale"
    )

    Column(
        modifier = modifier
            .alpha(hideAlpha)
            .scale(hideScale),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.End
    ) {
        // Filter button (delay 100ms)
        StaggeredMapButton(
            icon = Icons.Default.Tune,
            onClick = onFilterClick,
            contentDescription = "Filter",
            hasAppeared = hasAppeared,
            delayMs = 100
        )

        // Layers button (delay 200ms)
        StaggeredMapButton(
            iconResId = R.drawable.ic_layers,
            onClick = onLayersClick,
            contentDescription = "Layers",
            hasAppeared = hasAppeared,
            delayMs = 200
        )
    }
}

/**
 * A RoundedMapButton with staggered entrance animation.
 */
@Composable
private fun StaggeredMapButton(
    icon: ImageVector? = null,
    iconResId: Int? = null,
    onClick: () -> Unit,
    contentDescription: String,
    hasAppeared: Boolean,
    delayMs: Long
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(hasAppeared) {
        if (hasAppeared) {
            delay(delayMs)
            visible = true
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "btn_alpha_$contentDescription"
    )
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "btn_scale_$contentDescription"
    )

    if (icon != null) {
        RoundedMapButton(
            icon = icon,
            onClick = onClick,
            contentDescription = contentDescription,
            modifier = Modifier.alpha(alpha).scale(scale)
        )
    } else if (iconResId != null) {
        RoundedMapButton(
            iconResId = iconResId,
            onClick = onClick,
            contentDescription = contentDescription,
            modifier = Modifier.alpha(alpha).scale(scale)
        )
    }
}

/**
 * Rounded rectangle map control button.
 * Matches iOS RoundedMapButton: 48x48, 12pt corner radius, ultraThinMaterial bg, elevation shadow.
 *
 * iOS ref: Map/Controls/RoundedMapButton.swift
 */
@Composable
fun RoundedMapButton(
    iconResId: Int,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color? = null,
    iconTint: Color? = null
) {
    val bgColor = backgroundColor ?: MaterialTheme.colorScheme.surfaceContainerHighest
    val tint = iconTint ?: MaterialTheme.colorScheme.onSurface

    Surface(
        onClick = onClick,
        modifier = modifier.size(48.dp),
        shape = RoundedCornerShape(SaltyLayout.cardCornerRadius),
        color = bgColor,
        shadowElevation = 4.dp,
        tonalElevation = 2.dp
    ) {
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Overload accepting an ImageVector instead of a drawable resource.
 */
@Composable
fun RoundedMapButton(
    icon: ImageVector,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color? = null,
    iconTint: Color? = null
) {
    val bgColor = backgroundColor ?: MaterialTheme.colorScheme.surfaceContainerHighest
    val tint = iconTint ?: MaterialTheme.colorScheme.onSurface

    Surface(
        onClick = onClick,
        modifier = modifier.size(48.dp),
        shape = RoundedCornerShape(SaltyLayout.cardCornerRadius),
        color = bgColor,
        shadowElevation = 4.dp,
        tonalElevation = 2.dp
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}
