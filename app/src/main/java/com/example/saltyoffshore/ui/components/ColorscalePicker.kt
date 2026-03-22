package com.example.saltyoffshore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.data.Colorscale
import com.example.saltyoffshore.ui.theme.SaltyColors
import com.example.saltyoffshore.ui.theme.SaltyType
import com.example.saltyoffshore.ui.theme.Spacing

@Composable
fun ColorscalePicker(
    currentSelection: Colorscale?,
    defaultColorscale: Colorscale,
    onSelected: (Colorscale?) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(Spacing.large)
    ) {
        // -- COLORED --
        item(span = { GridItemSpan(3) }) {
            Text(
                text = "COLORED",
                style = SaltyType.caption,
                color = SaltyColors.textSecondary,
                modifier = Modifier.padding(top = Spacing.large, bottom = Spacing.small)
            )
        }
        items(Colorscale.colorfulScales, key = { it.id }) { colorscale ->
            val isDefault = colorscale.id == defaultColorscale.id
            val isSelected = if (isDefault) currentSelection == null else currentSelection?.id == colorscale.id
            ColorscaleSwatch(
                colorscale = colorscale,
                isSelected = isSelected,
                isDefault = isDefault,
                onClick = {
                    onSelected(if (isDefault) null else colorscale)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            )
        }

        // -- SINGLE COLOR --
        item(span = { GridItemSpan(3) }) {
            Text(
                text = "SINGLE COLOR",
                style = SaltyType.caption,
                color = SaltyColors.textSecondary,
                modifier = Modifier.padding(top = Spacing.large, bottom = Spacing.small)
            )
        }
        items(Colorscale.singleColorScales, key = { it.id }) { colorscale ->
            val isDefault = colorscale.id == defaultColorscale.id
            val isSelected = if (isDefault) currentSelection == null else currentSelection?.id == colorscale.id
            ColorscaleSwatch(
                colorscale = colorscale,
                isSelected = isSelected,
                isDefault = isDefault,
                onClick = {
                    onSelected(if (isDefault) null else colorscale)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            )
        }

        // -- NEUTRAL --
        item(span = { GridItemSpan(3) }) {
            Text(
                text = "NEUTRAL",
                style = SaltyType.caption,
                color = SaltyColors.textSecondary,
                modifier = Modifier.padding(top = Spacing.large, bottom = Spacing.small)
            )
        }
        items(Colorscale.neutralScales, key = { it.id }) { colorscale ->
            val isDefault = colorscale.id == defaultColorscale.id
            val isSelected = if (isDefault) currentSelection == null else currentSelection?.id == colorscale.id
            ColorscaleSwatch(
                colorscale = colorscale,
                isSelected = isSelected,
                isDefault = isDefault,
                onClick = {
                    onSelected(if (isDefault) null else colorscale)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            )
        }
    }
}

@Composable
private fun ColorscaleSwatch(
    colorscale: Colorscale,
    isSelected: Boolean,
    isDefault: Boolean,
    onClick: () -> Unit
) {
    val gradientColors = colorscale.hexColors.map { hex ->
        Color(android.graphics.Color.parseColor(hex))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (isSelected) Modifier.border(3.dp, Color.White, RoundedCornerShape(8.dp))
                else Modifier
            )
            .background(Brush.horizontalGradient(gradientColors))
            .clickable { onClick() }
    ) {
        // Name + Default badge at bottom-start
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 6.dp, y = (-6).dp)
                .background(
                    Color.Black.copy(alpha = 0.7f),
                    RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 6.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = colorscale.name,
                style = SaltyType.mono(10),
                color = Color.White
            )
            if (isDefault) {
                Text(
                    text = "Default",
                    style = SaltyType.mono(8),
                    color = Color.White
                )
            }
        }

        // Checkmark at top-end when selected
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-6).dp, y = 6.dp)
                    .size(16.dp)
            )
        }
    }
}
