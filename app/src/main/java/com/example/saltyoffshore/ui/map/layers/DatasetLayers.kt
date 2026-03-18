package com.example.saltyoffshore.ui.map.layers

import android.util.Log
import com.example.saltyoffshore.data.ContourLayerState
import com.example.saltyoffshore.data.Dataset
import com.example.saltyoffshore.data.DatasetRenderingSnapshot
import com.example.saltyoffshore.data.DatasetType
import com.example.saltyoffshore.data.RegionMetadata
import com.example.saltyoffshore.data.TimeEntry
import com.example.saltyoffshore.services.COGService
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.vectorSource

private const val TAG = "DatasetLayers"

/**
 * Orchestrates all dataset visualization layers.
 * Entry point for rendering COG rasters, PMTiles vectors, contours, currents.
 */
class DatasetLayers(
    private val mapboxMap: MapboxMap
) {
    private var currentRegionId: String? = null
    private var cogLayer: COGVisualLayer? = null
    private var dataQueryLayer: DataQueryLayer? = null
    private var currentsLayer: CurrentsLayer? = null
    private var contourLayer: ContourLayer? = null
    private var breaksLayer: BreaksVectorLayer? = null
    private var numbersLayer: NumbersLayer? = null

    /**
     * Render all dataset layers for current selection.
     */
    fun render(
        dataset: Dataset?,
        entry: TimeEntry?,
        region: RegionMetadata?,
        snapshot: DatasetRenderingSnapshot
    ) {
        if (dataset == null || entry == null || region == null) {
            Log.d(TAG, "Missing data - clearing layers")
            removeAllLayers()
            return
        }

        val regionId = region.id
        val datasetType = DatasetType.fromRawValue(dataset.type)

        // If region changed, clean up old layers
        if (currentRegionId != null && currentRegionId != regionId) {
            Log.d(TAG, "Region changed from $currentRegionId to $regionId - removing old layers")
            removeAllLayers()
        }
        currentRegionId = regionId

        Log.d(TAG, "Rendering layers for ${dataset.name} in ${region.name}")
        Log.d(TAG, "  COG: ${entry.layers.cog}")
        Log.d(TAG, "  PMTiles: ${entry.layers.pmtiles?.url}")
        Log.d(TAG, "  Visual enabled: ${snapshot.visualEnabled}")

        // 1. COG Visual Layer (raster heat map)
        renderCOGLayer(regionId, dataset, datasetType, entry, snapshot)

        // 2. Data Query Layer (invisible, for crosshair queries)
        renderDataQueryLayer(regionId, entry)

        // 3. Currents Layer (arrows - only for currents dataset)
        if (datasetType == DatasetType.CURRENTS) {
            renderCurrentsLayer(regionId, entry, snapshot)
        } else {
            currentsLayer?.removeFromMap()
            currentsLayer = null
        }

        // 4. Contour Layer
        renderContourLayer(regionId, dataset, datasetType, entry, snapshot)

        // 5. Breaks Layer (thermal fronts - only for datasets that support it)
        if (datasetType?.supportsFronts == true) {
            renderBreaksLayer(regionId, entry, snapshot)
        } else {
            breaksLayer?.removeFromMap()
            breaksLayer = null
        }

        // 6. Numbers Layer
        renderNumbersLayer(regionId, datasetType, entry, snapshot)
    }

    // MARK: - COG Visual Layer

    private fun renderCOGLayer(
        regionId: String,
        dataset: Dataset,
        datasetType: DatasetType?,
        entry: TimeEntry,
        snapshot: DatasetRenderingSnapshot
    ) {
        val cogUrl = entry.layers.cog
        if (cogUrl == null || !snapshot.visualEnabled) {
            cogLayer?.removeFromMap()
            cogLayer = null
            return
        }

        val type = datasetType ?: DatasetType.SST
        val rangeKey = type.rangeKey
        val rangeData = entry.ranges?.get(rangeKey)
        val dataRange = if (rangeData?.min != null && rangeData.max != null) {
            rangeData.min..rangeData.max
        } else {
            snapshot.dataMin..snapshot.dataMax
        }

        val tileUrl = COGService.generateTileURL(
            cogUrl = cogUrl,
            datasetType = type,
            snapshot = snapshot,
            dataRange = dataRange
        )

        if (tileUrl == null) {
            Log.w(TAG, "Failed to generate COG tile URL")
            return
        }

        Log.d(TAG, "COG tile URL: ${tileUrl.take(100)}...")

        if (cogLayer == null) {
            cogLayer = COGVisualLayer(
                mapboxMap = mapboxMap,
                sourceId = COGVisualLayer.sourceId(regionId),
                layerId = COGVisualLayer.layerId(regionId),
                cogURL = tileUrl,
                opacity = snapshot.visualOpacity
            )
            cogLayer?.addToMap()
        } else {
            cogLayer?.updateTileURL(tileUrl)
            cogLayer?.updateOpacity(snapshot.visualOpacity)
        }
    }

    // MARK: - Data Query Layer

    private fun renderDataQueryLayer(regionId: String, entry: TimeEntry) {
        val pmtiles = entry.layers.pmtiles
        if (pmtiles == null || !pmtiles.layers.contains("data")) {
            dataQueryLayer?.removeFromMap()
            dataQueryLayer = null
            return
        }

        val tileUrl = buildPMTilesTileURL(pmtiles.url)

        if (dataQueryLayer == null) {
            dataQueryLayer = DataQueryLayer(
                mapboxMap = mapboxMap,
                regionId = regionId,
                pmtilesURL = tileUrl,
                sourceLayer = "data"
            )
            dataQueryLayer?.addToMap()
        } else {
            dataQueryLayer?.updatePMTilesURL(tileUrl)
        }
    }

    // MARK: - Currents Layer

    private fun renderCurrentsLayer(
        regionId: String,
        entry: TimeEntry,
        snapshot: DatasetRenderingSnapshot
    ) {
        val pmtiles = entry.layers.pmtiles
        if (pmtiles == null || !pmtiles.layers.contains("data") || !snapshot.arrowsEnabled) {
            currentsLayer?.removeFromMap()
            currentsLayer = null
            return
        }

        val tileUrl = buildPMTilesTileURL(pmtiles.url)

        if (currentsLayer == null) {
            currentsLayer = CurrentsLayer(
                mapboxMap = mapboxMap,
                pmtilesURL = tileUrl,
                sourceLayer = "data",
                opacity = snapshot.arrowsOpacity,
                regionId = regionId,
                speedRange = snapshot.renderRange
            )
            currentsLayer?.addToMap()
        } else {
            currentsLayer?.updateOpacity(snapshot.arrowsOpacity)
        }
    }

    // MARK: - Contour Layer

    private fun renderContourLayer(
        regionId: String,
        dataset: Dataset,
        datasetType: DatasetType?,
        entry: TimeEntry,
        snapshot: DatasetRenderingSnapshot
    ) {
        val pmtiles = entry.layers.pmtiles
        if (pmtiles == null || !pmtiles.layers.contains("contours") || !snapshot.contourEnabled) {
            contourLayer?.removeFromMap()
            contourLayer = null
            return
        }

        val type = datasetType ?: DatasetType.SST
        val tileUrl = buildPMTilesTileURL(pmtiles.url)
        val sourceId = "contour-source-$regionId"
        val layerId = "contour-layer-$regionId"

        // Add source if needed
        mapboxMap.style?.let { style ->
            if (!style.styleSourceExists(sourceId)) {
                style.addSource(
                    vectorSource(sourceId) {
                        tiles(listOf(tileUrl))
                        maxzoom(8)
                    }
                )
            }
        }

        val state = ContourLayerState(
            color = type.contourColor,
            opacity = snapshot.contourOpacity,
            valueRange = snapshot.contourRange,
            datasetType = type,
            dynamicColoring = false,
            sourceLayer = "contours",
            sourceId = sourceId,
            layerId = layerId
        )

        if (contourLayer == null) {
            contourLayer = ContourLayer(mapboxMap, state)
            contourLayer?.addToMap()
        } else {
            contourLayer?.updateOpacity(snapshot.contourOpacity)
        }
    }

    // MARK: - Breaks Layer

    private fun renderBreaksLayer(
        regionId: String,
        entry: TimeEntry,
        snapshot: DatasetRenderingSnapshot
    ) {
        val pmtiles = entry.layers.pmtiles
        if (pmtiles == null || !pmtiles.layers.contains("breaks") || !snapshot.breaksEnabled) {
            breaksLayer?.removeFromMap()
            breaksLayer = null
            return
        }

        val tileUrl = buildPMTilesTileURL(pmtiles.url)
        val sourceId = BreaksVectorLayer.sourceId(regionId)
        val layerId = BreaksVectorLayer.layerId(regionId)

        if (breaksLayer == null) {
            breaksLayer = BreaksVectorLayer(
                mapboxMap = mapboxMap,
                sourceId = sourceId,
                layerId = layerId,
                pmtilesURL = tileUrl,
                sourceLayer = "breaks",
                opacity = snapshot.breaksOpacity,
                selectedBreakId = snapshot.selectedBreakId
            )
            breaksLayer?.addToMap()
        } else {
            breaksLayer?.updateOpacity(snapshot.breaksOpacity)
        }
    }

    // MARK: - Numbers Layer

    private fun renderNumbersLayer(
        regionId: String,
        datasetType: DatasetType?,
        entry: TimeEntry,
        snapshot: DatasetRenderingSnapshot
    ) {
        val pmtiles = entry.layers.pmtiles
        if (pmtiles == null || !pmtiles.layers.contains("data") || !snapshot.numbersEnabled || datasetType == null) {
            numbersLayer?.removeFromMap()
            numbersLayer = null
            return
        }

        val tileUrl = buildPMTilesTileURL(pmtiles.url)
        val sourceId = NumbersLayer.sourceId(regionId)
        val layerId = NumbersLayer.layerId(regionId)

        if (numbersLayer == null) {
            numbersLayer = NumbersLayer(
                mapboxMap = mapboxMap,
                sourceId = sourceId,
                layerId = layerId,
                datasetType = datasetType,
                pmtilesURL = tileUrl,
                sourceLayer = "data",
                opacity = snapshot.numbersOpacity
            )
            numbersLayer?.addToMap()
        } else {
            numbersLayer?.updateOpacity(snapshot.numbersOpacity)
        }
    }

    // MARK: - Helpers

    private fun buildPMTilesTileURL(baseUrl: String): String {
        // Martin tile server format: {url}/{z}/{x}/{y}
        // No extension needed - Martin returns application/x-protobuf
        return "$baseUrl/{z}/{x}/{y}"
    }

    // MARK: - Cleanup

    fun removeAllLayers() {
        Log.d(TAG, "Removing all layers")
        cogLayer?.removeFromMap()
        cogLayer = null

        dataQueryLayer?.removeFromMap()
        dataQueryLayer = null

        currentsLayer?.removeFromMap()
        currentsLayer = null

        contourLayer?.removeFromMap()
        contourLayer = null

        breaksLayer?.removeFromMap()
        breaksLayer = null

        numbersLayer?.removeFromMap()
        numbersLayer = null

        currentRegionId = null
    }

    /**
     * Update just the visual layer opacity (for slider changes)
     */
    fun updateVisualOpacity(opacity: Double) {
        cogLayer?.updateOpacity(opacity)
    }

    /**
     * Update contour layer opacity
     */
    fun updateContourOpacity(opacity: Double) {
        contourLayer?.updateOpacity(opacity)
    }

    /**
     * Update currents/arrows layer opacity
     */
    fun updateArrowsOpacity(opacity: Double) {
        currentsLayer?.updateOpacity(opacity)
    }

    /**
     * Update breaks layer opacity
     */
    fun updateBreaksOpacity(opacity: Double) {
        breaksLayer?.updateOpacity(opacity)
    }

    /**
     * Update numbers layer opacity
     */
    fun updateNumbersOpacity(opacity: Double) {
        numbersLayer?.updateOpacity(opacity)
    }
}
