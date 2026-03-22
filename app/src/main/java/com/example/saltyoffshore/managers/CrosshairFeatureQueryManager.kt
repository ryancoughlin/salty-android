package com.example.saltyoffshore.managers

import android.util.Log
import com.example.saltyoffshore.config.CrosshairConstants
import com.example.saltyoffshore.data.CurrentValue
import com.example.saltyoffshore.data.DatasetConfiguration
import com.example.saltyoffshore.data.DatasetType
import com.mapbox.geojson.Point
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
import kotlin.math.hypot

private const val TAG = "CrosshairQueryManager"

/**
 * Manages crosshair feature queries with throttling and coalescing.
 * Two-step query pipeline: water check -> data value extraction.
 * Owns primaryValue state directly — no callbacks.
 */
class CrosshairFeatureQueryManager(
    private val mapboxMap: MapboxMap,
    private val scope: CoroutineScope
) {
    var primaryValue: CurrentValue = CurrentValue()
        private set(value) {
            field = value
            onPrimaryValueChanged?.invoke(value)
        }

    var onPrimaryValueChanged: ((CurrentValue) -> Unit)? = null

    private var dataset: DatasetType? = null
    private var hasPMTilesData: Boolean = true
    private var throttleTask: Job? = null
    private var pendingQuery: Pair<ScreenCoordinate, Double>? = null

    fun configure(datasetType: DatasetType?, hasPMTilesData: Boolean = true) {
        dataset = datasetType
        this.hasPMTilesData = hasPMTilesData
        if (datasetType == null || !hasPMTilesData) {
            primaryValue = CurrentValue()
        }
    }

    fun queryCenterFeatures(screenPoint: ScreenCoordinate, zoom: Double) {
        if (dataset == null || !hasPMTilesData) return
        if (zoom < 3.0) return

        pendingQuery = Pair(screenPoint, zoom)

        if (throttleTask != null) return

        throttleTask = scope.launch(Dispatchers.Main) {
            delay(CrosshairConstants.QUERY_THROTTLE_MS)
            val (point, z) = pendingQuery ?: run {
                throttleTask = null
                return@launch
            }
            pendingQuery = null
            query(point, z)
            throttleTask = null
        }
    }

    fun reset() {
        primaryValue = CurrentValue()
        throttleTask?.cancel()
        throttleTask = null
        pendingQuery = null
    }

    private fun query(screenPoint: ScreenCoordinate, zoom: Double) {
        val datasetType = dataset ?: return
        val config = DatasetConfiguration.forDatasetType(datasetType)
        val boxSize = CrosshairConstants.queryBoxSize(zoom)

        // Step 1: Water check at exact center point
        mapboxMap.queryRenderedFeatures(
            RenderedQueryGeometry(screenPoint),
            RenderedQueryOptions(listOf("water"), null)
        ) { result ->
            val features = result.value
            if (features != null && features.isEmpty()) {
                // Over land — clear value
                primaryValue = CurrentValue()
                return@queryRenderedFeatures
            }
            if (features == null) {
                // Query failed — hold last value
                return@queryRenderedFeatures
            }

            // Step 2: Query data-layer in box
            val halfBox = boxSize / 2.0
            val screenBox = ScreenBox(
                ScreenCoordinate(screenPoint.x - halfBox, screenPoint.y - halfBox),
                ScreenCoordinate(screenPoint.x + halfBox, screenPoint.y + halfBox)
            )

            mapboxMap.queryRenderedFeatures(
                RenderedQueryGeometry(screenBox),
                RenderedQueryOptions(listOf("data-layer"), null)
            ) { dataResult ->
                val dataFeatures = dataResult.value
                if (dataFeatures == null) {
                    // Query failed — hold last value
                    return@queryRenderedFeatures
                }
                if (dataFeatures.isEmpty()) {
                    // No data points — clear value
                    primaryValue = CurrentValue()
                    return@queryRenderedFeatures
                }

                // Find closest by screen distance
                val maxDistance = boxSize / 2.0
                val closest = dataFeatures.mapNotNull { qrf ->
                    val geometry = qrf.queriedFeature.feature.geometry() ?: return@mapNotNull null
                    val point = geometry as? Point ?: return@mapNotNull null
                    val featureScreenPoint = mapboxMap.pixelForCoordinate(
                        Point.fromLngLat(point.longitude(), point.latitude())
                    )
                    val dx = featureScreenPoint.x - screenPoint.x
                    val dy = featureScreenPoint.y - screenPoint.y
                    val distance = hypot(dx, dy)
                    if (distance <= maxDistance) Pair(qrf, distance) else null
                }.minByOrNull { it.second }

                if (closest == null) {
                    // No points within radius — clear
                    primaryValue = CurrentValue()
                    return@queryRenderedFeatures
                }

                // Extract value
                val properties = closest.first.queriedFeature.feature.properties()
                val rawValue = properties?.get(config.valueKey)?.asDouble

                if (rawValue != null) {
                    primaryValue = CurrentValue(
                        value = rawValue,
                        apiUnit = config.unit,
                        datasetType = datasetType
                    )
                }
                // rawValue null — hold last value
            }
        }
    }
}
