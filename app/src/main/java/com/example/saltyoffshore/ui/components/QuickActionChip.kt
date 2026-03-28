package com.example.saltyoffshore.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saltyoffshore.ui.theme.Spacing

/**
 * Two visual states only:
 *
 * **Active** — Solid white background, black text.
 *   "This is what's applied right now."
 *
 * **Inactive** — Faded white background (20% opacity), white text (70% opacity).
 *   "Available but not applied."
 *
 * Variables: exactly one always active (radio).
 * Presets: zero or one active (toggle).
 * Disabled: same as inactive but further dimmed (break presets without crosshair).
 *
 * iOS ref: PresetChipButton — .ultraThinMaterial (inactive) vs Color.white (active)
 */
@Composable
fun QuickActionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trailingText: String? = null
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    style = chipTextStyle
                )
                if (selected && trailingText != null) {
                    Spacer(Modifier.width(Spacing.small))
                    Text(
                        text = trailingText,
                        style = chipTrailingTextStyle,
                        color = Color.Black.copy(alpha = 0.7f)
                    )
                }
            }
        },
        shape = RoundedCornerShape(8.dp),
        colors = FilterChipDefaults.filterChipColors(
            // Active: solid white, black text
            selectedContainerColor = Color.White,
            selectedLabelColor = Color.Black,
            // Inactive: faded white, muted white text
            containerColor = Color.White.copy(alpha = 0.20f),
            labelColor = Color.White.copy(alpha = 0.70f),
            // Disabled: further dimmed inactive
            disabledContainerColor = Color.White.copy(alpha = 0.10f),
            disabledLabelColor = Color.White.copy(alpha = 0.35f)
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = enabled,
            selected = selected,
            borderColor = Color.Transparent,
            selectedBorderColor = Color.Transparent,
            disabledBorderColor = Color.Transparent
        ),
        modifier = modifier
    )
}

private val chipTextStyle
    @Composable get() = androidx.compose.material3.MaterialTheme.typography.labelMedium.copy(
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium
    )

private val chipTrailingTextStyle
    @Composable get() = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
        fontSize = 11.sp
    )
