package com.example.saltyoffshore.ui.savedmaps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.ui.theme.SaltyColors
import com.example.saltyoffshore.ui.theme.SaltyType
import com.example.saltyoffshore.ui.theme.Spacing

@Composable
fun SavedMapsEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.Map,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = SaltyColors.textSecondary,
        )
        Spacer(Modifier.height(Spacing.large))
        Text(
            "No saved maps",
            style = SaltyType.heading,
            color = SaltyColors.textPrimary,
        )
        Spacer(Modifier.height(Spacing.small))
        Text(
            "Build your map, then save it from the tools menu to come back to it later.",
            style = SaltyType.bodySmall,
            color = SaltyColors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = Spacing.extraLarge),
        )
    }
}
