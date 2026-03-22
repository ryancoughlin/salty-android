package com.example.saltyoffshore.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.saltyoffshore.config.CrosshairConstants
import com.example.saltyoffshore.data.CurrentValue
import com.example.saltyoffshore.data.TemperatureUnits

/**
 * Complete crosshair overlay assembly.
 * Positions crosshair above screen center, scale bar at bottom.
 * Matches iOS CrosshairOverlay.
 */
@Composable
fun CrosshairOverlay(
    primaryValue: CurrentValue,
    temperatureUnits: TemperatureUnits,
    zoom: Double,
    latitude: Double,
    isDataLayerActive: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isDataLayerActive) return

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Crosshair at center with Y offset
        CrosshairView(
            primaryValue = primaryValue,
            temperatureUnits = temperatureUnits,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = CrosshairConstants.yOffset)
        )

        // Scale bar at bottom center
        ScaleBarView(
            zoom = zoom,
            latitude = latitude,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = CrosshairConstants.yOffset * -3) // Position above dataset control
        )
    }
}
