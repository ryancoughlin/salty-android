package com.example.saltyoffshore.ui.station

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp

@Composable
fun StationSummaryView(
    summaryText: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = summaryText,
        style = MaterialTheme.typography.headlineSmall,
        lineHeight = 28.sp,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.fillMaxWidth()
    )
}
