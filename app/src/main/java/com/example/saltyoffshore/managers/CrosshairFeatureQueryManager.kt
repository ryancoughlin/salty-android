package com.example.saltyoffshore.managers

import android.util.Log
import com.example.saltyoffshore.config.CrosshairConstants
import com.example.saltyoffshore.data.CurrentValue
import com.example.saltyoffshore.data.DatasetConfiguration
import com.example.saltyoffshore.data.DatasetType
import com.mapbox.geojson.Feature
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.RenderedQueryGeometry
import com.mapbox.maps.RenderedQueryOptions
import com.mapbox.maps.ScreenBox
import com.mapbox.maps.ScreenCoordinate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val TAG = "CrosshairQueryManager"

/**
 * Manages crosshair feature queries with throttling.
 * Two-step query: water check → data value extraction.
 */
class CrosshairFeatureQueryManager(
    private val mapboxMap: MapboxMap,
    private val scope: CoroutineScope
) {
    private var queryJob: Job? = null
    private var lastQueryTime = 0L

    /**
     * Query for value at screen point with throttling.
     * @param screenPoint Screen coordinate to query
     * @param zoom Current map zoom level
     * @param datasetType Active dataset type for value extraction
     * @param onResult Callback with query result
     */
    fun queryAtPoint(
        screenPoint: ScreenCoordinate,
        zoom: Double,
        datasetType: DatasetType?,
        onResult: (CurrentValue) -> Unit
    ) {
        // Cancel any pending query
        queryJob?.cancel()

        // Throttle queries
        val now = System.currentTimeMillis()
        val timeSinceLastQuery = now - lastQueryTime
        val throttleDelay = if (timeSinceLastQuery < CrosshairConstants.QUERY_THROTTLE_MS) {
            CrosshairConstants.QUERY_THROTTLE_MS - timeSinceLastQuery
        } else {
            0L
        }

        queryJob = scope.launch(Dispatchers.Main) {
            if (throttleDelay > 0) {
                onResult(CurrentValue.Loading)
                delay(throttleDelay)
            }

            lastQueryTime = System.currentTimeMillis()

            // Step 1: Check if over water
            checkWater(screenPoint) { isOverWater ->
                if (!isOverWater) {
                    onResult(CurrentValue.Land)
                    return@checkWater
                }

                // Step 2: Query data layer
                if (datasetType == null) {
                    onResult(CurrentValue.None)
                    return@checkWater
                }

                queryDataValue(screenPoint, zoom, datasetType, onResult)
            }
        }
    }

    /**
     * Check if point is over water using Mapbox water layer.
     */
    private fun checkWater(screenPoint: ScreenCoordinate, callback: (Boolean) -> Unit) {
        // Query the Mapbox standard water layer
        val waterLayerIds = listOf("water", "water-depth")
        val queryOptions = RenderedQueryOptions(waterLayerIds, null)

        mapboxMap.queryRenderedFeatures(
            RenderedQueryGeometry(screenPoint),
            queryOptions
        ) { expected ->
            val features = expected.value
            val isOverWater = features?.isNotEmpty() == true
            Log.d(TAG, "Water check: $isOverWater (${features?.size ?: 0} features)")
            callback(isOverWater)
        }
    }

    /**
     * Query the data layer for value at point.
     */
    private fun queryDataValue(
        screenPoint: ScreenCoordinate,
        zoom: Double,
        datasetType: DatasetType,
        onResult: (CurrentValue) -> Unit
    ) {
        // Build query box based on zoom
        val boxSize = CrosshairConstants.queryBoxSize(zoom)
        val halfBox = boxSize / 2.0

        val screenBox = ScreenBox(
            ScreenCoordinate(screenPoint.x - halfBox, screenPoint.y - halfBox),
            ScreenCoordinate(screenPoint.x + halfBox, screenPoint.y + halfBox)
        )

        val queryOptions = RenderedQueryOptions(
            listOf("data-layer"), // DataQueryLayer.LAYER_ID
            null
        )

        mapboxMap.queryRenderedFeatures(
            RenderedQueryGeometry(screenBox),
            queryOptions
        ) { expected ->
            val features = expected.value
            if (features.isNullOrEmpty()) {
                Log.d(TAG, "No data features found")
                onResult(CurrentValue.NoData)
                return@queryRenderedFeatures
            }

            // Get first feature and extract value
            val feature = features.first().queriedFeature.feature
            val value = extractValue(feature, datasetType)

            if (value != null) {
                val config = DatasetConfiguration.forDatasetType(datasetType)
                val formatted = formatValue(value, config.decimalPlaces)
                Log.d(TAG, "Found value: $formatted ${config.unit.symbol}")
                onResult(CurrentValue.Value(value, formatted, config.unit))
            } else {
                Log.d(TAG, "Could not extract value from feature")
                onResult(CurrentValue.NoData)
            }
        }
    }

    /**
     * Extract numeric value from feature properties.
     */
    private fun extractValue(feature: Feature, datasetType: DatasetType): Double? {
        val config = DatasetConfiguration.forDatasetType(datasetType)
        val properties = feature.properties() ?: return null

        // Try primary value key
        val value = properties.get(config.valueKey)?.asDouble
        if (value != null) return value

        // Fallback keys for different dataset sources
        val fallbackKeys = listOf(
            datasetType.contourFieldName,
            datasetType.rangeKey,
            "value",
            "v"
        )

        for (key in fallbackKeys) {
            val fallbackValue = properties.get(key)?.asDouble
            if (fallbackValue != null) return fallbackValue
        }

        return null
    }

    /**
     * Format value with specified decimal places.
     */
    private fun formatValue(value: Double, decimalPlaces: Int): String {
        return when (decimalPlaces) {
            0 -> value.roundToInt().toString()
            1 -> String.format("%.1f", value)
            2 -> String.format("%.2f", value)
            3 -> String.format("%.3f", value)
            else -> String.format("%.${decimalPlaces}f", value)
        }
    }

    /**
     * Cancel any pending queries.
     */
    fun cancel() {
        queryJob?.cancel()
        queryJob = null
    }
}
