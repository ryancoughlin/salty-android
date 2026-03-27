package com.example.saltyoffshore.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Orange announcement badge button - shows in TopBar when announcement is active.
 * Matches iOS AnnouncementButton.swift - orange circle with megaphone icon.
 *
 * iOS ref: Views/Common/AnnouncementButton.swift
 */
@Composable
fun AnnouncementButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledIconButton(
        onClick = onClick,
        modifier = modifier.size(36.dp),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
    ) {
        Icon(
            imageVector = Icons.Default.Campaign,
            contentDescription = "Announcement",
            modifier = Modifier.size(18.dp)
        )
    }
}
