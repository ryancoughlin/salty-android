package com.example.saltyoffshore.ui.satellite

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.data.satellite.DayNight

enum class BadgeSize(val frame: Dp, val iconSize: Dp, val cornerRadius: Dp) {
    COMPACT(20.dp, 10.dp, 4.dp),
    REGULAR(28.dp, 14.dp, 6.dp)
}

@Composable
fun DayNightBadge(dayNight: DayNight, size: BadgeSize = BadgeSize.REGULAR) {
    val icon: ImageVector
    val backgroundColor: Color
    val foregroundColor: Color

    when (dayNight) {
        DayNight.DAY -> {
            icon = Icons.Filled.WbSunny
            backgroundColor = Color(0xFFFFEB3B)
            foregroundColor = Color.Black
        }
        DayNight.NIGHT -> {
            icon = Icons.Filled.DarkMode
            backgroundColor = Color(0xFF3F51B5)
            foregroundColor = Color.White
        }
        DayNight.BOTH -> {
            icon = Icons.Filled.Contrast
            backgroundColor = Color(0xFF9C27B0)
            foregroundColor = Color.White
        }
    }

    Box(
        modifier = Modifier
            .size(size.frame)
            .clip(RoundedCornerShape(size.cornerRadius))
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = dayNight.name,
            modifier = Modifier.size(size.iconSize),
            tint = foregroundColor
        )
    }
}
