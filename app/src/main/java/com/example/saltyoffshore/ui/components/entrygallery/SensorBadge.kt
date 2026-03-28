package com.example.saltyoffshore.ui.components.entrygallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SensorBadge(sensor: String?, temporalCoverage: String?) {
    val hasSensor = !sensor.isNullOrEmpty()
    val hasCoverage = !temporalCoverage.isNullOrEmpty()
    if (!hasSensor && !hasCoverage) return

    Row(
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(50)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hasSensor) {
            Text(
                text = sensor!!,
                color = Color.White,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }

        if (hasSensor && hasCoverage) {
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(12.dp)
                    .background(Color.White.copy(alpha = 0.2f))
            )
        }

        if (hasCoverage) {
            Text(
                text = temporalCoverage!!,
                color = Color.White,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
    }
}
