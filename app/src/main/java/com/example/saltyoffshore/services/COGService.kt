package com.example.saltyoffshore.services

import com.example.saltyoffshore.data.DatasetRenderingSnapshot
import com.example.saltyoffshore.data.DatasetType
import com.example.saltyoffshore.data.FilterMode
import java.net.URLEncoder
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min

/**
 * Service for generating TiTiler COG tile URLs.
 * Handles dataset-specific expressions, colormaps, and filter params.
 */
object COGService {
    private const val TITILER_BASE_URL = "https://tiler.saltyoffshore.com"

    /**
     * Generate COG tile URL for Mapbox RasterSource.
     *
     * @param cogUrl Raw COG URL from API (TimeEntry.layers.cog)
     * @param datasetType Type of dataset for expression selection
     * @param snapshot Current rendering state
     * @param dataRange Data range from API (TimeEntry.ranges[dataset.rangeKey])
     * @return Tile URL template with {z}/{x}/{y} placeholders
     */
    fun generateTileURL(
        cogUrl: String,
        datasetType: DatasetType,
        snapshot: DatasetRenderingSnapshot,
        dataRange: ClosedFloatingPointRange<Double>
    ): String? {
        val encodedCog = URLEncoder.encode(cogUrl, "UTF-8")
        val params = mutableListOf("url=$encodedCog")

        // Build dataset-specific expression parameters
        params.addAll(buildExpressionParams(datasetType, snapshot, dataRange))

        // Add resampling method
        params.add("resampling=${snapshot.resamplingMethod}")

        val queryString = params.joinToString("&")
        return "$TITILER_BASE_URL/cog/tiles/WebMercatorQuad/{z}/{x}/{y}.png?$queryString"
    }

    private fun buildExpressionParams(
        datasetType: DatasetType,
        snapshot: DatasetRenderingSnapshot,
        dataRange: ClosedFloatingPointRange<Double>
    ): List<String> {
        // Determine rescale range
        val rescaleRange = if (snapshot.isFilterActive) {
            snapshot.filterMin..snapshot.filterMax
        } else {
            dataRange
        }

        val params = mutableListOf<String>()

        // Dataset-specific expression and rescale
        when (datasetType) {
            DatasetType.SST,
            DatasetType.MLD,
            DatasetType.DISSOLVED_OXYGEN,
            DatasetType.SALINITY -> {
                // Linear: expression=b1, rescale=min,max
                params.add("expression=b1")
                params.add("rescale=${fmt(rescaleRange.start)},${fmt(rescaleRange.endInclusive)}")
            }

            DatasetType.CHLOROPHYLL -> {
                // Special algorithm handles log10 + 19-stop color scale
                params.add("algorithm=chlorophyll_log10_rgb")
                // Colormap handled by algorithm, skip colormap_name
            }

            DatasetType.CURRENTS -> {
                // Log10 transformation for better visual distribution
                val logMin = log10(max(rescaleRange.start, 0.01))
                val logMax = log10(max(rescaleRange.endInclusive, 0.02))
                params.add("expression=${encodeExpr("log(b1)")}")
                params.add("rescale=${fmt(logMin)},${fmt(logMax)}")
                params.add("return_mask=true")
            }

            DatasetType.EDDYS -> {
                // SSH: linear with symmetric range around zero
                params.add("expression=b1")
                params.add("rescale=${fmt(rescaleRange.start)},${fmt(rescaleRange.endInclusive)}")
                params.add("return_mask=true")
            }

            DatasetType.FSLE -> {
                params.add("bidx=1")
                params.add("expression=b1")
                params.add("rescale=${fmt(rescaleRange.start)},${fmt(rescaleRange.endInclusive)}")
                params.add("return_mask=true")
                params.add("nodata=-9999")
            }

            DatasetType.WATER_CLARITY,
            DatasetType.WATER_TYPE -> {
                params.add("expression=b1")
                params.add("rescale=${fmt(rescaleRange.start)},${fmt(rescaleRange.endInclusive)}")
            }
        }

        // Add colormap (skip for chlorophyll - algorithm handles it)
        if (datasetType != DatasetType.CHLOROPHYLL) {
            val colorscaleId = snapshot.selectedColorscaleId ?: datasetType.defaultColorscaleId
            params.add("colormap_name=$colorscaleId")
        }

        // Add filter masking if hideShow mode
        if (snapshot.isFilterActive && snapshot.filterMode == FilterMode.HIDE_SHOW) {
            when (datasetType) {
                DatasetType.CHLOROPHYLL -> {
                    val clampedMin = max(0.01, min(snapshot.filterMin, 8.0))
                    val clampedMax = max(0.01, min(snapshot.filterMax, 8.0))
                    val algorithmParams = """{"min_value":${fmt(clampedMin)},"max_value":${fmt(clampedMax)}}"""
                    params.add("algorithm_params=${URLEncoder.encode(algorithmParams, "UTF-8")}")
                }
                else -> {
                    params.add("algorithm=ocean_mask")
                    val algorithmParams = """{"min_temp":${fmt(snapshot.filterMin)},"max_temp":${fmt(snapshot.filterMax)}}"""
                    params.add("algorithm_params=${URLEncoder.encode(algorithmParams, "UTF-8")}")
                }
            }
        }

        params.add("return_mask=true")
        return params
    }

    private fun fmt(d: Double): String = "%.6f".format(d)

    private fun encodeExpr(s: String): String = URLEncoder.encode(s, "UTF-8")
        .replace("+", "%2B")
}
