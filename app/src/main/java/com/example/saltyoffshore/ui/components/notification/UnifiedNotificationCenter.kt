package com.example.saltyoffshore.ui.components.notification

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.saltyoffshore.data.AppNotification
import com.example.saltyoffshore.ui.theme.SaltyMotion

/**
 * Stacks notification capsules vertically in the center slot.
 * Matches iOS UnifiedNotificationCenter.swift.
 *
 * Each notification animates in from the top with fade.
 */
@Composable
fun UnifiedNotificationCenter(
    notifications: List<AppNotification>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Loading capsule
        val hasLoading = notifications.any { it is AppNotification.Loading }
        AnimatedVisibility(
            visible = hasLoading,
            enter = slideInVertically(
                animationSpec = SaltyMotion.springSnappy(),
                initialOffsetY = { -it }
            ) + fadeIn(animationSpec = SaltyMotion.springSnappy()),
            exit = slideOutVertically(
                animationSpec = SaltyMotion.springSnappy(),
                targetOffsetY = { -it }
            ) + fadeOut(animationSpec = SaltyMotion.springSnappy())
        ) {
            LoadingCapsule()
        }

        // Error capsule — remember last message so exit animation has content
        val error = notifications.filterIsInstance<AppNotification.Error>().firstOrNull()
        var lastErrorMessage by remember { mutableStateOf("") }
        if (error != null) lastErrorMessage = error.message

        AnimatedVisibility(
            visible = error != null,
            enter = slideInVertically(
                animationSpec = SaltyMotion.springSnappy(),
                initialOffsetY = { -it }
            ) + fadeIn(animationSpec = SaltyMotion.springSnappy()),
            exit = slideOutVertically(
                animationSpec = SaltyMotion.springSnappy(),
                targetOffsetY = { -it }
            ) + fadeOut(animationSpec = SaltyMotion.springSnappy())
        ) {
            ErrorCapsule(message = lastErrorMessage)
        }
    }
}
