package com.example.saltyoffshore.ui.components

import androidx.compose.animation.AnimatedVisibility
import com.example.saltyoffshore.ui.theme.SaltyMotion
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.data.AppNotification
import com.example.saltyoffshore.ui.components.notification.UnifiedNotificationCenter

/**
 * TopBar - Three-slot layout matching iOS TopBar.swift.
 *
 * Layout: left (weight 1, left-aligned) | center (fixed) | right (weight 1, right-aligned)
 *
 * Animate in on appear: fade + slide from -20dp, spring(response=0.4, damping=0.85).
 * Hide when shouldHideTopUI is true (dataset control expanded or special mode active).
 */
@Composable
fun TopBar(
    isVisible: Boolean,
    notifications: List<AppNotification>,
    onAccountTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val offsetPx = with(density) { -20.dp.roundToPx() }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(
            animationSpec = SaltyMotion.springSnappy()
        ) + slideInVertically(
            animationSpec = SaltyMotion.springSnappy(),
            initialOffsetY = { offsetPx }
        ),
        exit = fadeOut(
            animationSpec = SaltyMotion.springSnappy()
        ) + slideOutVertically(
            animationSpec = SaltyMotion.springSnappy(),
            targetOffsetY = { offsetPx }
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT slot: Crew chips / recording pill (future phases)
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                // Empty — crew chips phase
            }

            // CENTER slot: Loading / error capsules
            Box(contentAlignment = Alignment.Center) {
                UnifiedNotificationCenter(notifications = notifications)
            }

            // RIGHT slot: Account button
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterEnd
            ) {
                AccountButton(onClick = onAccountTap)
            }
        }
    }
}
