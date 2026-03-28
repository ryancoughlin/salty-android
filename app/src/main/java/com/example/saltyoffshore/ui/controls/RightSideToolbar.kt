package com.example.saltyoffshore.ui.controls

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.R
import com.example.saltyoffshore.ui.theme.SaltyMotion
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
    onMeasureClick: () -> Unit = {},
    onToolsClick: () -> Unit = {},
    shouldHide: Boolean = false,
    modifier: Modifier = Modifier
) {
    var hasAppeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(250)
        hasAppeared = true
    }

    val hideAlpha by animateFloatAsState(
        targetValue = if (shouldHide) 0f else 1f,
        animationSpec = SaltyMotion.springDefault(),
        label = "toolbar_hide"
    )
    val hideScale by animateFloatAsState(
        targetValue = if (shouldHide) 0.9f else 1f,
        animationSpec = SaltyMotion.springDefault(),
        label = "toolbar_scale"
    )

    Column(
        modifier = modifier
            .alpha(hideAlpha)
            .scale(hideScale),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.End
    ) {
        StaggeredMapButton(hasAppeared, 100) {
            MapControlButton(
                icon = Icons.Default.Tune,
                contentDescription = "Filter",
                onClick = onFilterClick
            )
        }

        StaggeredMapButton(hasAppeared, 200) {
            MapControlButton(
                iconResId = R.drawable.ic_layers,
                contentDescription = "Layers",
                onClick = onLayersClick
            )
        }

        StaggeredMapButton(hasAppeared, 300) {
            MapControlButton(
                icon = Icons.Default.Straighten,
                contentDescription = "Measure",
                onClick = onMeasureClick
            )
        }

        StaggeredMapButton(hasAppeared, 400) {
            MapControlButton(
                icon = Icons.Default.MoreVert,
                contentDescription = "Tools",
                onClick = onToolsClick
            )
        }
    }
}

// ── Reusable map control button ─────────────────────────────────────────────
// Matches iOS RoundedMapButton: 48×48, 12pt radius, 20pt icon.
// Normal → FilledTonalIconButton (surfaceContainerHighest).
// Active → FilledIconButton (primary).
//
// iOS ref: Map/Controls/RoundedMapButton.swift

private val MapControlButtonSize = 48.dp
private val MapControlIconSize = 20.dp

@Composable
fun MapControlButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false
) {
    MapControlButtonContainer(
        onClick = onClick,
        isActive = isActive,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(MapControlIconSize)
        )
    }
}

@Composable
fun MapControlButton(
    iconResId: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false
) {
    MapControlButtonContainer(
        onClick = onClick,
        isActive = isActive,
        modifier = modifier
    ) {
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = contentDescription,
            modifier = Modifier.size(MapControlIconSize)
        )
    }
}

@Composable
private fun MapControlButtonContainer(
    onClick: () -> Unit,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shape = MaterialTheme.shapes.extraLarge
    val shadowMod = modifier
        .shadow(
            elevation = 4.dp,
            shape = shape,
            ambientColor = MaterialTheme.colorScheme.scrim,
            spotColor = MaterialTheme.colorScheme.scrim
        )
        .size(MapControlButtonSize)

    if (isActive) {
        FilledIconButton(
            onClick = onClick,
            modifier = shadowMod,
            shape = shape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            content = content
        )
    } else {
        FilledTonalIconButton(
            onClick = onClick,
            modifier = shadowMod,
            shape = shape,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            content = content
        )
    }
}

// ── Staggered entrance animation ────────────────────────────────────────────

@Composable
private fun StaggeredMapButton(
    hasAppeared: Boolean,
    delayMs: Long,
    content: @Composable () -> Unit
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
        animationSpec = SaltyMotion.springDefault(),
        label = "stagger_alpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        animationSpec = SaltyMotion.springDefault(),
        label = "stagger_scale"
    )

    Column(modifier = Modifier.alpha(alpha).scale(scale)) {
        content()
    }
}
