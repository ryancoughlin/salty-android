package com.example.saltyoffshore.zarr

import android.util.Log
import com.mapbox.maps.ProjectedMeters
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import java.util.Date
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

private const val TAG = "ZarrReader"

/**
 * Reader for Zarr v2 arrays (zlib compressed).
 * Network-first reader with offline cache fallback for downloaded regions.
 *
 * Chunk format: `.zarr/variable/{time}.{depth}.{y}.{x}`
 *
 * Matches iOS `ZarrReader` actor exactly.
 */
class ZarrReader(
    private val chunkLoader: ZarrChunkLoader = ZarrChunkLoader()
) {
    // MARK: - Prepared Dataset (fetch-once, reuse for all frames)

    /**
     * Pre-fetched metadata + coordinates for a Zarr variable.
     * Format: 4D [time, vertical, y, x] in Web Mercator (EPSG:3857).
     * Fetched once per dataset load, shared across all frame loads.
     * Used for both ocean data (depth dimension) and forecast data (level dimension).
     *
     * Matches iOS `ZarrReader.PreparedDataset` exactly.
     */
    data class PreparedDataset(
        val baseUrl: String,
        val variableName: String,
        val metadata: ZarrArrayMetadata,
        /** Texture edge bounds (half-pixel expanded for full coverage rendering). */
        val textureEdgeBounds: GridBounds,
        /** Data cell bounds (raw Zarr coordinate extent, not expanded). */
        val dataCellBounds: GridBounds,
        val width: Int,
        val height: Int,
        /**
         * True when the raw y-coordinate array descends (y[0] > y[1]), meaning
         * chunk row 0 is the northernmost data.
         */
        val yGoesNorthToSouth: Boolean,
        /** Cached velocity component metadata for ocean currents (uo/vo). */
        val velocityMetadata: VelocityMetadata?,
        /** Cached velocity component metadata for wind (u10/v10). */
        val windVelocityMetadata: VelocityMetadata?
    ) {
        /** Southwest corner as ProjectedMeters (convenience accessor) */
        val sw: ProjectedMeters get() = textureEdgeBounds.sw

        /** Northeast corner as ProjectedMeters (convenience accessor) */
        val ne: ProjectedMeters get() = textureEdgeBounds.ne

        /** True when shape is 4D (has depth dimension) */
        val hasDepth: Boolean get() = metadata.shape.size == 4

        /** URL for the variable's array directory */
        val arrayUrl: String get() = "$baseUrl/$variableName"

        data class VelocityMetadata(
            val uMetadata: ZarrArrayMetadata,
            val vMetadata: ZarrArrayMetadata
        )
    }

    // MARK: - Prepare Dataset

    /**
     * Prepare dataset using pre-parsed manifest (no metadata HTTP).
     * Format: 4D [time, vertical, y, x] in Web Mercator.
     *
     * Ocean coordinate variables: x, y (Mercator EPSG:3857).
     */
    suspend fun prepareDataset(
        storeUrl: String,
        variableName: String,
        manifest: ZarrStoreManifest
    ): PreparedDataset {
        // MIGRATION: Subsurface temp uses "temperature" instead of "sea_surface_temperature".
        val (resolvedName, metadata) = when {
            manifest.arrays.containsKey(variableName) ->
                variableName to manifest.arrays[variableName]!!
            variableName == "sea_surface_temperature" && manifest.arrays.containsKey("temperature") ->
                "temperature" to manifest.arrays["temperature"]!!
            else ->
                throw ZarrError.DataNotFound("Variable '$variableName' not in store manifest")
        }

        val dims = metadata.shape.size
        if (dims != 3 && dims != 4) {
            throw ZarrError.InvalidShape("Expected 3D or 4D array, got ${dims}D: ${metadata.shape}")
        }

        // Spatial dimensions are always the last two
        val height = metadata.shape[dims - 2]
        val width = metadata.shape[dims - 1]

        // Fetch coordinates using manifest metadata
        val coords = fetchMercatorCoordinates(storeUrl, manifest)

        if (coords.x.isEmpty() || coords.y.isEmpty()) {
            throw ZarrError.InvalidShape("Empty coordinate arrays")
        }

        val x0 = coords.x.first()
        val x1 = coords.x.last()
        val y0 = coords.y.first()
        val y1 = coords.y.last()

        // Texture edge bounds (half-pixel expanded for heatmap rendering)
        val textureEdgeBounds = GridBounds.pixelEdgeBounds(
            x0 = x0, x1 = x1, y0 = y0, y1 = y1,
            width = width, height = height
        )

        // Detect y-direction
        val yNorthToSouth = coords.y.size >= 2 && coords.y[0] > coords.y[1]

        // Cache velocity metadata if uo/vo exist in manifest
        val velocityMetadata = if (manifest.arrays.containsKey("uo") && manifest.arrays.containsKey("vo")) {
            PreparedDataset.VelocityMetadata(
                uMetadata = manifest.arrays["uo"]!!,
                vMetadata = manifest.arrays["vo"]!!
            )
        } else null

        // Cache wind velocity metadata if u10/v10 exist
        val windVelocityMetadata = if (manifest.arrays.containsKey("u10") && manifest.arrays.containsKey("v10")) {
            PreparedDataset.VelocityMetadata(
                uMetadata = manifest.arrays["u10"]!!,
                vMetadata = manifest.arrays["v10"]!!
            )
        } else null

        // Cell-center bounds for particle sampling
        val dataCellBounds = GridBounds(
            swEasting = x0,
            swNorthing = min(y0, y1),
            neEasting = x1,
            neNorthing = max(y0, y1)
        )

        return PreparedDataset(
            baseUrl = storeUrl,
            variableName = resolvedName,
            metadata = metadata,
            textureEdgeBounds = textureEdgeBounds,
            dataCellBounds = dataCellBounds,
            width = width,
            height = height,
            yGoesNorthToSouth = yNorthToSouth,
            velocityMetadata = velocityMetadata,
            windVelocityMetadata = windVelocityMetadata
        )
    }

    // MARK: - Public API

    /**
     * Fast-path: load a frame using pre-fetched metadata + coordinates.
     * Handles both 3D [time, y, x] and 4D [time, depth, y, x].
     */
    suspend fun loadSlice(
        prepared: PreparedDataset,
        timeIndex: Int,
        depthIndex: Int = 0
    ): ZarrSliceData {
        val metadata = prepared.metadata
        val timeCount = metadata.shape[0]

        if (timeIndex >= timeCount) {
            throw ZarrError.InvalidShape("Time index $timeIndex out of range (0..<$timeCount)")
        }

        val effectiveDepthIndex: Int? = if (prepared.hasDepth) {
            val depthCount = metadata.shape[1]
            if (depthIndex >= depthCount) {
                throw ZarrError.InvalidShape("Depth index $depthIndex out of range (0..<$depthCount)")
            }
            depthIndex
        } else {
            null  // 3D — no depth dimension
        }

        val floats = fetch2DSlice(
            timeIndex = timeIndex,
            depthIndex = effectiveDepthIndex,
            metadata = metadata,
            baseUrl = prepared.arrayUrl
        )

        return ZarrSliceData(
            floats = floats,
            width = prepared.width,
            height = prepared.height,
            bounds = prepared.textureEdgeBounds
        )
    }

    /**
     * Fetch the time array from Zarr (Unix timestamps → Dates).
     * @param manifest When provided, uses manifest metadata to skip the `time/.zarray` network call.
     * @param bustCache When true, bypasses CDN edge cache and ignores manifest metadata.
     */
    suspend fun fetchTimeArray(
        zarrBaseUrl: String,
        manifest: ZarrStoreManifest? = null,
        bustCache: Boolean = false
    ): List<Date> {
        val timeUrl = "$zarrBaseUrl/${ZarrCoordinate.TIME.value}"

        val metadata = if (manifest?.arrays?.containsKey(ZarrCoordinate.TIME.value) == true && !bustCache) {
            manifest.arrays[ZarrCoordinate.TIME.value]!!
        } else {
            chunkLoader.fetchMetadata(timeUrl, bustCache)
        }

        if (metadata.shape.size != 1) {
            throw ZarrError.InvalidShape("Expected 1D time array, got ${metadata.shape}")
        }

        // Time arrays may span multiple chunks
        val totalCount = metadata.shape[0]
        val chunkSize = metadata.chunks[0]
        val chunkCount = ceil(totalCount.toDouble() / chunkSize).toInt()

        // Fetch all chunks in parallel
        val chunkResults = coroutineScope {
            (0 until chunkCount).map { chunkIndex ->
                async {
                    val chunkData = chunkLoader.fetchChunk(
                        indices = listOf(chunkIndex),
                        metadata = metadata,
                        baseUrl = timeUrl,
                        bustCache = bustCache
                    )
                    chunkIndex to parseInt64(chunkData)
                }
            }.awaitAll()
        }

        // Sort by chunk index and flatten
        val allTimestamps = chunkResults
            .sortedBy { it.first }
            .flatMap { it.second.toList() }

        // Trim to actual shape and convert to dates
        // Guard for datetime64[ns]: values > 1e15 are nanoseconds
        return allTimestamps.take(totalCount).map { raw ->
            val seconds = if (abs(raw) > 1_000_000_000_000_000L) {
                raw / 1_000_000_000.0
            } else {
                raw.toDouble()
            }
            Date((seconds * 1000).toLong())
        }
    }

    // MARK: - Manifest

    /**
     * Fetch the consolidated `.zmetadata` from a Zarr store.
     * @param bustCache When true, bypasses CDN edge cache.
     */
    suspend fun fetchManifest(storeUrl: String, bustCache: Boolean = false): ZarrStoreManifest {
        return chunkLoader.fetchManifest(storeUrl, bustCache)
    }

    // MARK: - Coordinates

    /**
     * Fetch x and y coordinate arrays from Zarr (Mercator meters, <f4).
     * Uses manifest metadata — skips x/.zarray and y/.zarray network calls.
     * Fires x/0 and y/0 in parallel (single RTT).
     */
    private suspend fun fetchMercatorCoordinates(
        zarrUrl: String,
        manifest: ZarrStoreManifest
    ): MercatorCoordinates {
        val xMeta = manifest.arrays[ZarrCoordinate.X.value]
            ?: throw ZarrError.DataNotFound("x coordinates not in manifest")
        val yMeta = manifest.arrays[ZarrCoordinate.Y.value]
            ?: throw ZarrError.DataNotFound("y coordinates not in manifest")

        val xUrl = "$zarrUrl/${ZarrCoordinate.X.value}"
        val yUrl = "$zarrUrl/${ZarrCoordinate.Y.value}"

        val (xRaw, yRaw) = coroutineScope {
            val xDeferred = async {
                chunkLoader.fetchChunk(listOf(0), xMeta, xUrl)
            }
            val yDeferred = async {
                chunkLoader.fetchChunk(listOf(0), yMeta, yUrl)
            }
            xDeferred.await() to yDeferred.await()
        }

        return MercatorCoordinates(
            x = parseFloats(xRaw).map { it.toDouble() },
            y = parseFloats(yRaw).map { it.toDouble() }
        )
    }

    // MARK: - Chunk Fetching

    /**
     * Fetch all spatial chunks for a single time slice.
     * 4D: chunk key = [time, depth, yChunk, xChunk]
     * 3D: chunk key = [time, yChunk, xChunk] (depthIndex is null)
     */
    private suspend fun fetch2DSlice(
        timeIndex: Int,
        depthIndex: Int?,
        metadata: ZarrArrayMetadata,
        baseUrl: String
    ): FloatArray {
        val dims = metadata.shape.size
        val yChunkSize = metadata.chunks[dims - 2]
        val xChunkSize = metadata.chunks[dims - 1]
        val ySize = metadata.shape[dims - 2]
        val xSize = metadata.shape[dims - 1]

        val yChunkCount = ceil(ySize.toDouble() / yChunkSize).toInt()
        val xChunkCount = ceil(xSize.toDouble() / xChunkSize).toInt()

        fun chunkIndices(yChunk: Int, xChunk: Int): List<Int> {
            return if (depthIndex != null) {
                listOf(timeIndex, depthIndex, yChunk, xChunk)
            } else {
                listOf(timeIndex, yChunk, xChunk)
            }
        }

        // Single chunk = no coroutine overhead needed
        if (yChunkCount == 1 && xChunkCount == 1) {
            val chunkData = chunkLoader.fetchChunk(
                indices = chunkIndices(0, 0),
                metadata = metadata,
                baseUrl = baseUrl
            )
            val floats = parseFloats(chunkData)
            return if (floats.size == ySize * xSize) {
                floats
            } else {
                trimChunk(floats, 0, 0, yChunkSize, xChunkSize, ySize, xSize)
            }
        }

        // Multiple chunks: fire all in parallel
        val result = FloatArray(ySize * xSize) { Float.NaN }

        coroutineScope {
            val tasks = mutableListOf<kotlinx.coroutines.Deferred<Triple<Int, Int, FloatArray>>>()
            for (yChunk in 0 until yChunkCount) {
                for (xChunk in 0 until xChunkCount) {
                    tasks.add(async {
                        // Check cancellation early — prevents wasted HTTP requests when user scrubs rapidly
                        ensureActive()
                        val chunkData = chunkLoader.fetchChunk(
                            indices = chunkIndices(yChunk, xChunk),
                            metadata = metadata,
                            baseUrl = baseUrl
                        )
                        Triple(yChunk, xChunk, parseFloats(chunkData))
                    })
                }
            }

            tasks.awaitAll().forEach { (yChunk, xChunk, floats) ->
                for (localY in 0 until yChunkSize) {
                    val globalY = yChunk * yChunkSize + localY
                    if (globalY >= ySize) continue
                    for (localX in 0 until xChunkSize) {
                        val globalX = xChunk * xChunkSize + localX
                        if (globalX >= xSize) continue
                        val localIndex = localY * xChunkSize + localX
                        val globalIndex = globalY * xSize + globalX
                        if (localIndex < floats.size) {
                            result[globalIndex] = floats[localIndex]
                        }
                    }
                }
            }
        }

        return result
    }

    /**
     * Trim a padded chunk into the final grid dimensions
     */
    private fun trimChunk(
        floats: FloatArray,
        chunkY: Int, chunkX: Int,
        yChunkSize: Int, xChunkSize: Int,
        ySize: Int, xSize: Int
    ): FloatArray {
        val result = FloatArray(ySize * xSize) { Float.NaN }
        for (localY in 0 until yChunkSize) {
            val globalY = chunkY * yChunkSize + localY
            if (globalY >= ySize) continue
            for (localX in 0 until xChunkSize) {
                val globalX = chunkX * xChunkSize + localX
                if (globalX >= xSize) continue
                val localIndex = localY * xChunkSize + localX
                val globalIndex = globalY * xSize + globalX
                if (localIndex < floats.size) {
                    result[globalIndex] = floats[localIndex]
                }
            }
        }
        return result
    }
}

/**
 * Container for Mercator coordinate arrays.
 */
private data class MercatorCoordinates(
    val x: List<Double>,
    val y: List<Double>
)
