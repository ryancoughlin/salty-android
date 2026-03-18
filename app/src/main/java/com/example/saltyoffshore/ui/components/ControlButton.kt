package com.example.saltyoffshore.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Matches iOS ControlButton.swift exactly:
 * - Black background at 25% opacity
 * - White text/icons
 * - 4dp corner radius
 * - 28dp height, 28dp width if icon-only
 */
@Composable
fun ControlButton(
    icon: ImageVector,
    label: String? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Black.copy(alpha = 0.25f),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier
            .height(28.dp)
            .then(if (label == null) Modifier.width(28.dp) else Modifier)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (label != null) 10.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(if (label == null) 12.dp else 11.dp),
                tint = Color.White
            )
            if (label != null) {
                Text(
                    text = label,
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                )
            }
        }
    }
}
