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

    // Track current PMTiles URLs and dataset type to detect when source needs rebuilding
    private var currentContourPmtilesUrl: String? = null
    private var currentContourDatasetType: DatasetType? = null
    private var currentDataQueryPmtilesUrl: String? = null
    private var currentCurrentsPmtilesUrl: String? = null
    private var currentBreaksPmtilesUrl: String? = null
    private var currentNumbersPmtilesUrl: String? = null

    // MARK: - Overlay Layer State

    /** Active overlay Zarr layer IDs in activation order (for z-ordering). */
    private val overlayZarrLayerIds = mutableListOf<String>()

    /** Per-overlay layer instances keyed by DatasetType.rawValue. */
    private val overlayContourLayers = mutableMapOf<String, ContourLayer>()
    private val overlayCurrentsLayers = mutableMapOf<String, CurrentsLayer>()
    private val overlayBreaksLayers = mutableMapOf<String, BreaksVectorLayer>()
    private val overlayNumbersLayers = mutableMapOf<String, NumbersLayer>()

    // Track overlay PMTiles URLs per overlay key
    private val overlayContourUrls = mutableMapOf<String, String>()
    private val overlayCurrentsUrls = mutableMapOf<String, String>()
    private val overlayBreaksUrls = mutableMapOf<String, String>()
    private val overlayNumbersUrls = mutableMapOf<String, String>()

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

    // MARK: - Overlay Rendering

    /**
     * Render overlay datasets as additional Mapbox layers stacked above the primary.
     * Each overlay is added above the previous one, maintaining activation order.
     *
     * @param overlayOrder Ordered list of overlay types (activation order = z-order)
     * @param overlaySnapshots Per-overlay rendering snapshots
     * @param overlayVisualSources Per-overlay visual layer sources keyed by rawValue
     * @param overlayDatasets Per-overlay datasets with populated entries
     * @param overlayEntries Per-overlay resolved entries
     */
    fun renderOverlays(
        overlayOrder: List<DatasetType>,
        overlaySnapshots: Map<DatasetType, DatasetRenderingSnapshot>,
        overlayVisualSources: Map<String, VisualLayerSource>,
        overlayDatasets: Map<DatasetType, Dataset>,
        overlayEntries: Map<DatasetType, TimeEntry>
    ) {
        // Remove layers for overlays that are no longer active
        val activeKeys = overlayOrder.map { it.rawValue }.toSet()
        removeStaleOverlayLayers(activeKeys)

        // Track previous layer ID for z-ordering (stack above primary or previous overlay).
        // Only advance previousLayerId when the layer actually exists on the map,
        // so the next overlay positions itself above a real layer — not a phantom ID.
        var previousLayerId = zarrLayerId

        for (type in overlayOrder) {
            val key = type.rawValue
            val snapshot = overlaySnapshots[type] ?: continue
            val source = overlayVisualSources[key]
            val dataset = overlayDatasets[type]
            val entry = overlayEntries[type]

            // 1. Overlay Visual Layer (Zarr GPU)
            renderOverlayVisualLayer(key, snapshot, source, previousLayerId)

            // Only advance the chain if this overlay's layer is actually on the map.
            // If it hasn't been added yet (source still loading), keep previousLayerId
            // pointing at the last layer that DOES exist so the next overlay stacks correctly.
            val overlayZarrId = "zarr-overlay-$key"
            mapboxMap.style?.let { style ->
                if (style.styleLayerExists(overlayZarrId)) {
                    previousLayerId = overlayZarrId
                }
            }

            // 2. Overlay supporting layers (contours, arrows, breaks, numbers)
            if (entry != null && dataset != null) {
                val datasetType = DatasetType.fromRawValue(dataset.type)

                // Contours
                renderOverlayContourLayer(key, datasetType, entry, snapshot)

                // Arrows (currents only)
                if (datasetType == DatasetType.CURRENTS) {
                    renderOverlayCurrentsLayer(key, entry, snapshot)
                } else {
                    overlayCurrentsLayers.remove(key)?.removeFromMap()
                    overlayCurrentsUrls.remove(key)
                }

                // Breaks
                if (datasetType?.supportsFronts == true) {
                    renderOverlayBreaksLayer(key, entry, snapshot)
                } else {
                    overlayBreaksLayers.remove(key)?.removeFromMap()
                    overlayBreaksUrls.remove(key)
                }

                // Numbers
                renderOverlayNumbersLayer(key, datasetType, entry, snapshot)
            }
        }
    }

    /**
     * Render an overlay's Zarr visual layer, positioned above previousLayerId.
     *
     * Uses Mapbox moveStyleLayer() to reposition already-added layers when the
     * z-order chain changes (e.g. overlay 1 loads after overlay 2).
     *
     * Docs: https://docs.mapbox.com/android/maps/guides/styles/work-with-layers/
     */
    private fun renderOverlayVisualLayer(
        key: String,
        snapshot: DatasetRenderingSnapshot,
        source: VisualLayerSource?,
        previousLayerId: String?
    ) {
        val layerId = "zarr-overlay-$key"

        if (!snapshot.visualEnabled || source == null) {
            removeOverlayZarrLayer(layerId)
            return
        }

        when (source) {
            is VisualLayerSource.Zarr -> {
                val renderer = source.renderer
                renderer.setVisible(snapshot.visualEnabled)

                mapboxMap.style?.let { style ->
                    val alreadyExists = style.styleLayerExists(layerId)

                    if (alreadyExists) {
                        // Layer exists — ensure it's in the correct position.
                        // moveStyleLayer repositions without re-creating the GL resources.
                        val position = if (previousLayerId != null) {
                            LayerPosition(previousLayerId, null, null)
                        } else {
                            LayerPosition(null, null, null)
                        }
                        style.moveStyleLayer(layerId, position)
                    } else {
                        // Layer doesn't exist yet — only add it if the anchor layer
                        // we want to stack above actually exists on the map.
                        // If the anchor is missing (still loading), skip this pass;
                        // the next render cycle will pick it up once the anchor is ready.
                        if (previousLayerId != null && !style.styleLayerExists(previousLayerId)) {
                            Log.d(TAG, "Deferring overlay $layerId — anchor $previousLayerId not on map yet")
                            return
                        }

                        val position = if (previousLayerId != null) {
                            LayerPosition(previousLayerId, null, null)
                        } else {
                            LayerPosition(null, null, null)
                        }

                        style.addStyleCustomLayer(
                            layerId = layerId,
                            layerHost = renderer,
                            layerPosition = position
                        )
                        overlayZarrLayerIds.add(layerId)
                        Log.d(TAG, "Added overlay Zarr layer: $layerId above $previousLayerId")
                    }
                }
            }
            is VisualLayerSource.None -> {
                removeOverlayZarrLayer(layerId)
            }
        }
    }

    private fun removeOverlayZarrLayer(layerId: String) {
        mapboxMap.style?.let { style ->
            if (style.styleLayerExists(layerId)) {
                style.removeStyleLayer(layerId)
                Log.d(TAG, "Removed overlay Zarr layer: $layerId")
            }
        }
        overlayZarrLayerIds.remove(layerId)
    }

    /**
     * Remove overlay layers for types no longer active.
     */
    private fun removeStaleOverlayLayers(activeKeys: Set<String>) {
        // Remove stale Zarr layers
        val staleZarrIds = overlayZarrLayerIds.filter { id ->
            val key = id.removePrefix("zarr-overlay-")
            key !in activeKeys
        }
        for (id in staleZarrIds) {
            removeOverlayZarrLayer(id)
        }

        // Remove stale supporting layers
        for (key in overlayContourLayers.keys.toList()) {
            if (key !in activeKeys) {
                overlayContourLayers.remove(key)?.removeFromMap()
                overlayContourUrls.remove(key)
            }
        }
        for (key in overlayCurrentsLayers.keys.toList()) {
            if (key !in activeKeys) {
                overlayCurrentsLayers.remove(key)?.removeFromMap()
                overlayCurrentsUrls.remove(key)
            }
        }
        for (key in overlayBreaksLayers.keys.toList()) {
            if (key !in activeKeys) {
                overlayBreaksLayers.remove(key)?.removeFromMap()
                overlayBreaksUrls.remove(key)
            }
        }
        for (key in overlayNumbersLayers.keys.toList()) {
            if (key !in activeKeys) {
                overlayNumbersLayers.remove(key)?.removeFromMap()
                overlayNumbersUrls.remove(key)
            }
        }
    }

    // MARK: - Overlay Supporting Layers

    private fun renderOverlayContourLayer(
        key: String,
        datasetType: DatasetType?,
        entry: TimeEntry,
        snapshot: DatasetRenderingSnapshot
    ) {
        val pmtiles = entry.layers.pmtiles
        if (pmtiles == null || !pmtiles.layers.contains("contours") || !snapshot.contourEnabled) {
            overlayContourLayers.remove(key)?.removeFromMap()
            overlayContourUrls.remove(key)
            return
        }

        val type = datasetType ?: DatasetType.SST
        val tileUrl = buildPMTilesTileURL(pmtiles.url)
        val sourceId = "contour-overlay-source-$key"
        val layerId = "contour-overlay-$key"

        val prevUrl = overlayContourUrls[key]
        if (prevUrl != null && prevUrl != tileUrl) {
            overlayContourLayers.remove(key)?.removeFromMap()
        }
        overlayContourUrls[key] = tileUrl

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
            color = android.graphics.Color.BLACK,
            opacity = snapshot.contourOpacity,
            valueRange = snapshot.contourFilterRange,
            datasetType = type,
            dynamicColoring = false,
            sourceLayer = "contours",
            sourceId = sourceId,
            layerId = layerId
        )

        if (overlayContourLayers[key] == null) {
            val layer = ContourLayer(mapboxMap, state)
            layer.addToMap()
            overlayContourLayers[key] = layer
        } else {
            overlayContourLayers[key]?.updateOpacity(snapshot.contourOpacity)
        }
    }

    private fun renderOverlayCurrentsLayer(
        key: String,
        entry: TimeEntry,
        snapshot: DatasetRenderingSnapshot
    ) {
        val pmtiles = entry.layers.pmtiles
        if (pmtiles == null || !pmtiles.layers.contains("data") || !snapshot.arrowsEnabled) {
            overlayCurrentsLayers.remove(key)?.removeFromMap()
            overlayCurrentsUrls.remove(key)
            return
        }

        val tileUrl = buildPMTilesTileURL(pmtiles.url)
        val prevUrl = overlayCurrentsUrls[key]
        if (prevUrl != null && prevUrl != tileUrl) {
            overlayCurrentsLayers.remove(key)?.removeFromMap()
        }
        overlayCurrentsUrls[key] = tileUrl

        if (overlayCurrentsLayers[key] == null) {
            val layer = CurrentsLayer(
                mapboxMap = mapboxMap,
                pmtilesURL = tileUrl,
                sourceLayer = "data",
                opacity = snapshot.arrowsOpacity,
                regionId = "overlay-$key",
                speedRange = snapshot.renderRange
            )
            layer.addToMap()
            overlayCurrentsLayers[key] = layer
        } else {
            overlayCurrentsLayers[key]?.updateOpacity(snapshot.arrowsOpacity)
        }
    }

    private fun renderOverlayBreaksLayer(
        key: String,
        entry: TimeEntry,
        snapshot: DatasetRenderingSnapshot
    ) {
        val pmtiles = entry.layers.pmtiles
        if (pmtiles == null || !pmtiles.layers.contains("breaks") || !snapshot.breaksEnabled) {
            overlayBreaksLayers.remove(key)?.removeFromMap()
            overlayBreaksUrls.remove(key)
            return
        }

        val tileUrl = buildPMTilesTileURL(pmtiles.url)
        val sourceId = "breaks-overlay-source-$key"
        val layerId = "breaks-overlay-$key"

        val prevUrl = overlayBreaksUrls[key]
        if (prevUrl != null && prevUrl != tileUrl) {
            overlayBreaksLayers.remove(key)?.removeFromMap()
        }
        overlayBreaksUrls[key] = tileUrl

        if (overlayBreaksLayers[key] == null) {
            val layer = BreaksVectorLayer(
                mapboxMap = mapboxMap,
                sourceId = sourceId,
                layerId = layerId,
                pmtilesURL = tileUrl,
                sourceLayer = "breaks",
                opacity = snapshot.breaksOpacity,
                selectedBreakId = snapshot.selectedBreakId
            )
            layer.addToMap()
            overlayBreaksLayers[key] = layer
        } else {
            overlayBreaksLayers[key]?.updateOpacity(snapshot.breaksOpacity)
        }
    }

    private fun renderOverlayNumbersLayer(
        key: String,
        datasetType: DatasetType?,
        entry: TimeEntry,
        snapshot: DatasetRenderingSnapshot
    ) {
        val pmtiles = entry.layers.pmtiles
        if (pmtiles == null || !pmtiles.layers.contains("data") || !snapshot.numbersEnabled || datasetType == null) {
            overlayNumbersLayers.remove(key)?.removeFromMap()
            overlayNumbersUrls.remove(key)
            return
        }

        val tileUrl = buildPMTilesTileURL(pmtiles.url)
        val sourceId = "numbers-overlay-source-$key"
        val layerId = "numbers-overlay-$key"

        val prevUrl = overlayNumbersUrls[key]
        if (prevUrl != null && prevUrl != tileUrl) {
            overlayNumbersLayers.remove(key)?.removeFromMap()
        }
        overlayNumbersUrls[key] = tileUrl

        if (overlayNumbersLayers[key] == null) {
            val layer = NumbersLayer(
                mapboxMap = mapboxMap,
                sourceId = sourceId,
                layerId = layerId,
                datasetType = datasetType,
                pmtilesURL = tileUrl,
                sourceLayer = "data",
                opacity = snapshot.numbersOpacity
            )
            layer.addToMap()
            overlayNumbersLayers[key] = layer
        } else {
            overlayNumbersLayers[key]?.updateOpacity(snapshot.numbersOpacity)
        }
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
            currentDataQueryPmtilesUrl = null
            return
        }

        val tileUrl = buildPMTilesTileURL(pmtiles.url)

        // Rebuild if URL changed
        if (currentDataQueryPmtilesUrl != null && currentDataQueryPmtilesUrl != tileUrl) {
            dataQueryLayer?.removeFromMap()
            dataQueryLayer = null
        }
        currentDataQueryPmtilesUrl = tileUrl

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
            currentCurrentsPmtilesUrl = null
            return
        }

        val tileUrl = buildPMTilesTileURL(pmtiles.url)

        // Rebuild if URL changed
        if (currentCurrentsPmtilesUrl != null && currentCurrentsPmtilesUrl != tileUrl) {
            currentsLayer?.removeFromMap()
            currentsLayer = null
        }
        currentCurrentsPmtilesUrl = tileUrl

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
            removeContourLayer()
            return
        }

        val type = datasetType ?: DatasetType.SST
        val tileUrl = buildPMTilesTileURL(pmtiles.url)
        val sourceId = "contour-source-$regionId"
        val layerId = "contour-layer-$regionId"

        // Rebuild if URL or dataset type changed (different types have different layer structures)
        val needsRebuild = (currentContourPmtilesUrl != null && currentContourPmtilesUrl != tileUrl)
            || (currentContourDatasetType != null && currentContourDatasetType != type)
        if (needsRebuild) {
            Log.d(TAG, "Contour source changed (url or type), rebuilding layers")
            removeContourLayer()
        }
        currentContourPmtilesUrl = tileUrl
        currentContourDatasetType = type

        // Add or update source
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
            color = android.graphics.Color.BLACK,
            opacity = snapshot.contourOpacity,
            valueRange = snapshot.contourFilterRange,
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

    private fun removeContourLayer() {
        contourLayer?.removeFromMap()
        contourLayer = null
        // Also remove the source so it gets recreated with new URL
        currentContourPmtilesUrl?.let {
            currentRegionId?.let { regionId ->
                val sourceId = "contour-source-$regionId"
                mapboxMap.style?.let { style ->
                    if (style.styleSourceExists(sourceId)) {
                        style.removeStyleSource(sourceId)
                    }
                }
            }
        }
        currentContourPmtilesUrl = null
        currentContourDatasetType = null
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
            currentBreaksPmtilesUrl = null
            return
        }

        val tileUrl = buildPMTilesTileURL(pmtiles.url)
        val sourceId = BreaksVectorLayer.sourceId(regionId)
        val layerId = BreaksVectorLayer.layerId(regionId)

        // Rebuild if URL changed
        if (currentBreaksPmtilesUrl != null && currentBreaksPmtilesUrl != tileUrl) {
            breaksLayer?.removeFromMap()
            breaksLayer = null
        }
        currentBreaksPmtilesUrl = tileUrl

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
            currentNumbersPmtilesUrl = null
            return
        }

        val tileUrl = buildPMTilesTileURL(pmtiles.url)
        val sourceId = NumbersLayer.sourceId(regionId)
        val layerId = NumbersLayer.layerId(regionId)

        // Rebuild if URL changed
        if (currentNumbersPmtilesUrl != null && currentNumbersPmtilesUrl != tileUrl) {
            numbersLayer?.removeFromMap()
            numbersLayer = null
        }
        currentNumbersPmtilesUrl = tileUrl

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

        removeContourLayer()

        breaksLayer?.removeFromMap()
        breaksLayer = null

        numbersLayer?.removeFromMap()
        numbersLayer = null

        // Remove all overlay layers
        removeAllOverlayLayers()

        currentRegionId = null
        currentDataQueryPmtilesUrl = null
        currentCurrentsPmtilesUrl = null
        currentBreaksPmtilesUrl = null
        currentNumbersPmtilesUrl = null
    }

    /**
     * Remove all overlay layers from the map.
     */
    fun removeAllOverlayLayers() {
        for (id in overlayZarrLayerIds.toList()) {
            removeOverlayZarrLayer(id)
        }
        overlayContourLayers.values.forEach { it.removeFromMap() }
        overlayContourLayers.clear()
        overlayContourUrls.clear()
        overlayCurrentsLayers.values.forEach { it.removeFromMap() }
        overlayCurrentsLayers.clear()
        overlayCurrentsUrls.clear()
        overlayBreaksLayers.values.forEach { it.removeFromMap() }
        overlayBreaksLayers.clear()
        overlayBreaksUrls.clear()
        overlayNumbersLayers.values.forEach { it.removeFromMap() }
        overlayNumbersLayers.clear()
        overlayNumbersUrls.clear()
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
