package com.example.saltyoffshore.ui.map.layers

import android.util.Log
import com.example.saltyoffshore.data.ContourLayerState
import com.example.saltyoffshore.data.Dataset
import com.example.saltyoffshore.data.DatasetRenderingSnapshot
import com.example.saltyoffshore.data.DatasetType
import com.example.saltyoffshore.data.RegionMetadata
import com.example.saltyoffshore.data.TimeEntry
import com.example.saltyoffshore.data.VisualLayerSource
import com.example.saltyoffshore.zarr.ZarrVisualLayer
import com.mapbox.maps.LayerPosition
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.vectorSource

private const val TAG = "DatasetLayers"

/**
 * Orchestrates all dataset visualization layers.
 * Zarr GPU rendering only — no fallbacks.
 */
class DatasetLayers(
    private val mapboxMap: MapboxMap
) {
    private var currentRegionId: String? = null
    private var zarrLayerId: String? = null
    private var currentZarrRenderer: ZarrVisualLayer? = null
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
        snapshot: DatasetRenderingSnapshot,
        visualSource: VisualLayerSource = VisualLayerSource.None
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

        // 1. Visual Layer (Zarr GPU shader)
        renderVisualLayer(regionId, snapshot, visualSource)

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
        renderContourLayer(regionId, datasetType, entry, snapshot)

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

    // MARK: - Visual Layer (Zarr only)

    private fun renderVisualLayer(
        regionId: String,
        snapshot: DatasetRenderingSnapshot,
        visualSource: VisualLayerSource
    ) {
        if (!snapshot.visualEnabled) {
            removeZarrLayer()
            return
        }

        when (visualSource) {
            is VisualLayerSource.Zarr -> {
                renderZarrLayer(regionId, visualSource.renderer, snapshot)
            }
            is VisualLayerSource.None -> {
                // No visual layer yet (Zarr not loaded)
                removeZarrLayer()
            }
        }
    }

    private fun renderZarrLayer(
        regionId: String,
        renderer: ZarrVisualLayer,
        snapshot: DatasetRenderingSnapshot
    ) {
        val layerId = "zarr-visual-$regionId"

        // Update visibility based on snapshot
        renderer.setVisible(snapshot.visualEnabled)

        // If layer already added with same renderer, just update
        if (zarrLayerId == layerId && currentZarrRenderer === renderer) {
            return
        }

        // Remove old Zarr layer if different
        removeZarrLayer()

        // Add new Zarr layer to map
        mapboxMap.style?.let { style ->
            if (!style.styleLayerExists(layerId)) {
                style.addStyleCustomLayer(
                    layerId = layerId,
                    layerHost = renderer,
                    layerPosition = LayerPosition(null, null, null)
                )
                Log.d(TAG, "Added Zarr CustomLayer: $layerId")
            }
        }

        zarrLayerId = layerId
        currentZarrRenderer = renderer
    }

    private fun removeZarrLayer() {
        zarrLayerId?.let { layerId ->
            mapboxMap.style?.let { style ->
                if (style.styleLayerExists(layerId)) {
                    style.removeStyleLayer(layerId)
                    Log.d(TAG, "Removed Zarr layer: $layerId")
                }
            }
        }
        zarrLayerId = null
        currentZarrRenderer = null
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
        return "$baseUrl/{z}/{x}/{y}"
    }

    // MARK: - Cleanup

    fun removeAllLayers() {
        Log.d(TAG, "Removing all layers")

        removeZarrLayer()

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
