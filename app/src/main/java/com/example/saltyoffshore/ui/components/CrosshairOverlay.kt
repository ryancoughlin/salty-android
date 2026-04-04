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
/**
 * Lambda provider pattern: zoom and latitude change every frame during pan/zoom.
 * By accepting lambdas instead of raw values, the "read" happens inside THIS composable's
 * scope — so only CrosshairOverlay recomposes on camera moves, not the parent MapScreen.
 *
 * iOS equivalent: Only CrosshairOverlay observes CrosshairFeatureQueryManager.value.
 * MapboxMapView_V2 never rebuilds for camera changes.
 */
@Composable
fun CrosshairOverlay(
    primaryValue: CurrentValue,
    temperatureUnits: TemperatureUnits,
    zoomProvider: () -> Double,
    latitudeProvider: () -> Double,
    isDataLayerActive: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isDataLayerActive) return

    // Read the lambdas HERE — this scopes the recomposition to CrosshairOverlay only
    val zoom = zoomProvider()
    val latitude = latitudeProvider()

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
