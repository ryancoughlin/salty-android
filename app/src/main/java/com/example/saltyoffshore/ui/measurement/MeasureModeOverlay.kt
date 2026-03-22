package com.example.saltyoffshore.ui.measurement

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.data.DistanceUnits
import com.example.saltyoffshore.data.measurement.formatDistance
import com.example.saltyoffshore.ui.theme.SaltyColors
import com.example.saltyoffshore.ui.theme.SaltyLayout
import com.example.saltyoffshore.ui.theme.SaltyType
import com.example.saltyoffshore.ui.theme.Spacing

/**
 * Measurement mode floating toolbar: status pill + action buttons.
 *
 * Surface hierarchy (from Color.kt):
 *   Outer card  → surfaceContainerHigh (Raised) — same level as SaltyDatasetControl
 *   Status pill → primary (accent) or surfaceVariant (sunken) for empty state
 *   Done button → primary (accent fill) — highest emphasis, clear call to action
 *   Undo/Clear  → surface (raised) with outline — secondary actions, visually receded
 *
 * iOS ref: Features/Measurement/Views/MeasureModeOverlay.swift
 */
@Composable
fun MeasureModeOverlay(
    totalDistanceMeters: Double,
    hasMeasurements: Boolean,
    canUndo: Boolean,
    distanceUnits: DistanceUnits,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.large),
        shape = RoundedCornerShape(SaltyLayout.cardCornerRadius),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp,
        shadowElevation = 6.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(Spacing.medium)
        ) {
            // Status pill — accent when measuring, sunken when empty
            StatusPill(
                totalDistanceMeters = totalDistanceMeters,
                hasMeasurements = hasMeasurements,
                distanceUnits = distanceUnits
            )

            Spacer(Modifier.height(Spacing.medium))

            // Action row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Undo — outlined secondary action
                IconButton(
                    onClick = onUndo,
                    enabled = canUndo,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = SaltyColors.textPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surface,
                        disabledContentColor = SaltyColors.textSecondary
                    ),
                    modifier = Modifier
                        .size(40.dp)
                        .alpha(if (canUndo) 1f else 0.35f)
                ) {
                    Icon(Icons.Default.Undo, contentDescription = "Undo", modifier = Modifier.size(20.dp))
                }

                // Clear — outlined secondary action
                IconButton(
                    onClick = onClear,
                    enabled = hasMeasurements,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = SaltyColors.textPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surface,
                        disabledContentColor = SaltyColors.textSecondary
                    ),
                    modifier = Modifier
                        .size(40.dp)
                        .alpha(if (hasMeasurements) 1f else 0.35f)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(20.dp))
                }

                Spacer(Modifier.weight(1f))

                // Done — primary filled button, highest emphasis
                Button(
                    onClick = onDone,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(SaltyLayout.controlCornerRadius)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Done", style = SaltyType.bodySmall)
                }
            }
        }
    }
}

// ── Status Pill ─────────────────────────────────────────────────────────────

@Composable
private fun StatusPill(
    totalDistanceMeters: Double,
    hasMeasurements: Boolean,
    distanceUnits: DistanceUnits
) {
    val hasDistance = hasMeasurements && totalDistanceMeters > 0

    Surface(
        shape = RoundedCornerShape(SaltyLayout.controlCornerRadius),
        color = if (hasDistance)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.surfaceVariant,
        border = if (!hasDistance)
            BorderStroke(1.dp, SaltyColors.borderSubtle)
        else
            null
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.large, vertical = Spacing.medium)
        ) {
            Icon(
                imageVector = if (hasDistance) Icons.Default.Straighten else Icons.Default.TouchApp,
                contentDescription = null,
                tint = if (hasDistance)
                    MaterialTheme.colorScheme.onPrimary
                else
                    SaltyColors.textSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (hasDistance)
                    formatDistance(totalDistanceMeters, distanceUnits)
                else
                    "Tap map to add points",
                style = if (hasDistance) SaltyType.mono(16) else SaltyType.bodySmall,
                color = if (hasDistance)
                    MaterialTheme.colorScheme.onPrimary
                else
                    SaltyColors.textSecondary
            )
        }
    }
}
