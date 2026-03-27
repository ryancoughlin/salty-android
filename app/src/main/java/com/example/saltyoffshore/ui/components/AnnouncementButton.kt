package com.example.saltyoffshore.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Orange announcement badge button — shows in TopBar when announcement active.
 * Matches iOS: hardcoded orange circle, white icon, shadow.
 *
 * iOS ref: Views/Common/AnnouncementButton.swift
 */
private val AnnouncementOrange = Color(0xFFFF9500)

@Composable
fun AnnouncementButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledIconButton(
        onClick = onClick,
        modifier = modifier
            .size(36.dp)
            .shadow(
                elevation = 4.dp,
                shape = androidx.compose.foundation.shape.CircleShape,
                ambientColor = Color.Black.copy(alpha = 0.2f),
                spotColor = Color.Black.copy(alpha = 0.2f)
            ),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = AnnouncementOrange,
            contentColor = Color.White
        )
    ) {
        Icon(
            imageVector = Icons.Default.Campaign,
            contentDescription = "Announcement",
            modifier = Modifier.size(18.dp)
        )
    }
}
