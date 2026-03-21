package com.example.saltyoffshore.ui.components

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ControlButton(
    icon: ImageVector,
    label: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .height(36.dp)
            .then(if (label == null) Modifier.width(36.dp) else Modifier)
            .clip(RoundedCornerShape(6.dp))
            .drawBehind { drawRect(Color.Black.copy(alpha = 0.25f)) }
            .clickable {
                Log.d("ControlButton", "TAPPED: ${label ?: "icon-only"}")
                onClick()
            }
            .padding(horizontal = if (label != null) 12.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label ?: "Control",
            modifier = Modifier.size(if (label == null) 16.dp else 14.dp),
            tint = Color.White
        )
        if (label != null) {
            Text(
                text = label,
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            )
        }
    }
}
