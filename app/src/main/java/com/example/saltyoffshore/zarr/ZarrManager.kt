package com.example.saltyoffshore.zarr

import android.util.Log
import com.example.saltyoffshore.data.Colorscale
import com.example.saltyoffshore.data.ScaleMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlin.math.abs

private const val TAG = "ZarrManager"

/**
 * Zarr playback configuration.
 * Matches iOS `ZarrPlaybackConfig`.
 */
object ZarrPlaybackConfig {
    /** How many frames to load simultaneously during background loading. */
    const val PARALLEL_LOAD_LIMIT = 6
}

/**
 * Zarr data manager — loads all frames into GPU memory for instant scrubbing.
 *
 * Loading strategy:
 * 1. On dataset select: Fetch metadata, load initial frame via HTTP → show immediately
 * 2. Background: Load ALL remaining frames into GPU (no window, no direction bias)
 * 3. On timeline scrub: GPU-cached frames instant, uncached load on-demand via HTTP
 *
 * Matches iOS `ZarrManager` exactly.
 */
class ZarrManager(
    private val zarrReader: ZarrReader = ZarrReader(),
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    companion object {
        const val PRIMARY_KEY = "primary"
    }

    // MARK: - Loading State

    sealed class LoadingState {
        data object Idle : LoadingState()
        data object Loading : LoadingState()
        data object Ready : LoadingState()
        data class Failed(val message: String) : LoadingState()

        val isReady: Boolean get() = this is Ready
    }

    // MARK: - State

    /** Shader host for GPU rendering */
    private var _shaderHost: ZarrVisualLayer? = null
    val shaderHost: ZarrVisualLayer?
        get() = _shaderHost

    /** Loading state observable */
    private val _loadingState = MutableStateFlow<LoadingState>(LoadingState.Idle)
    val loadingState: StateFlow<LoadingState> = _loadingState.asStateFlow()

    /** Frame error state */
    private val _currentFrameError = MutableStateFlow<String?>(null)
    val currentFrameError: StateFlow<String?> = _currentFrameError.asStateFlow()

    /** Frame version counter (increments on each frame display) */
    var frameVersion: Int = 0
        private set

    /** Repaint callback — set by map, used for animation */
    var repaint: (() -> Unit)? = null

    // MARK: - Internal State

    private var context: ZarrDatasetContext? = null
    private var currentDepth: Int = 0
    private var currentFrameKey: String? = null

    // Task management
    private var loadTask: Job? = null
    private var backgroundLoadTask: Job? = null
    private var onDemandTask: Job? = null
    private var showFrameGeneration: Int = 0

    // MARK: - Public API

    /**
     * Set the shader host for GPU rendering.
     * Must be called before loading data.
     */
    fun setShaderHost(host: ZarrVisualLayer) {
        _shaderHost = host
    }

    /**
     * Current depth for this manager (defaults to context's default if not set).
     */
    fun currentDepth(): Int {
        return context?.let { ctx ->
            if (ctx.availableDepths.contains(currentDepth)) currentDepth
            else ctx.defaultDepth
        } ?: 0
    }

    /**
     * Aggregate data range for the current dataset/depth (same for all frames).
     */
    fun aggregateDataRange(): ClosedFloatingPointRange<Float>? {
        val ctx = context ?: return null
        return ctx.frames(currentDepth()).firstOrNull()?.dataRange
    }

    // MARK: - Loading

    /**
     * Load a dataset for visualization.
     *
     * @param zarrUrl URL to the Zarr store
     * @param variableName Zarr variable to load (e.g., "sea_surface_temperature")
     * @param entries List of time entries with their IDs and timestamps
     * @param depths Available depth levels
     * @param dataRange Color scale domain for all frames
     * @param initialEntryId Optional entry to show first (defaults to first entry)
     * @param colorscale Colorscale to use for rendering
     * @param scaleMode Scale mode for normalization
     */
    fun load(
        zarrUrl: String,
        variableName: String,
        entries: List<TimeEntry>,
        depths: List<Int> = listOf(0),
        dataRange: ClosedFloatingPointRange<Float>,
        initialEntryId: String? = null,
        colorscale: Colorscale,
        scaleMode: ScaleMode = ScaleMode.LINEAR
    ) {
        val host = _shaderHost ?: run {
            Log.e(TAG, "No shader host set")
            _loadingState.value = LoadingState.Failed("No shader host")
            return
        }

        // Cancel existing tasks
        cancelTasks()

        // Clear frames if URL changed
        context?.let { ctx ->
            if (ctx.zarrUrl != zarrUrl) {
                host.clearFrames()
            }
        }

        context = null
        _loadingState.value = LoadingState.Loading

        // Prepare colormap bytes on main thread (cheap), defer GL calls until context ready
        val colormapBytes = ColormapTextureFactory.createTextureBytes(colorscale)
        val scaleModeOrdinal = scaleMode.ordinal
        host.pendingColormap = colormapBytes
        host.pendingScaleMode = scaleModeOrdinal

        loadTask = coroutineScope.launch {
            try {
                val loadStart = System.currentTimeMillis()
                Log.d(TAG, "[Timing] Load start. variable=$variableName url=$zarrUrl")

                // Phase 1: Fetch manifest
                val phase1Start = System.currentTimeMillis()
                val manifest = zarrReader.fetchManifest(zarrUrl)
                Log.d(TAG, "[Timing] Phase 1 manifest: ${System.currentTimeMillis() - phase1Start}ms")

                // Phase 2: Prepare metadata + coordinates
                val phase2Start = System.currentTimeMillis()
                val preparedDataset = zarrReader.prepareDataset(zarrUrl, variableName, manifest)
                val zarrTimes = zarrReader.fetchTimeArray(zarrUrl, manifest)
                Log.d(TAG, "[Timing] Phase 2 metadata+coords: ${System.currentTimeMillis() - phase2Start}ms timeCount=${zarrTimes.size}")

                // Map Zarr timestamps to indices
                val timeToIndex = zarrTimes.mapIndexed { index, date ->
                    date.time / 1000 to index
                }.toMap()

                // Build frame metadata grouped by depth
                val depthFrames = buildDepthFrames(entries, depths, timeToIndex, dataRange)

                val ctx = ZarrDatasetContext(
                    zarrUrl = zarrUrl,
                    preparedDataset = preparedDataset,
                    depthFrames = depthFrames,
                    availableDepths = depths
                )

                context = ctx
                currentDepth = ctx.defaultDepth

                val allFrames = ctx.frames(currentDepth)
                if (allFrames.isEmpty()) {
                    _loadingState.value = LoadingState.Failed("No valid entries")
                    return@launch
                }

                // Phase 3: Load initial frame
                val entryIdToIndex = ctx.entryIdToIndex(currentDepth)
                val initialIndex = if (initialEntryId != null && entryIdToIndex.containsKey(initialEntryId)) {
                    entryIdToIndex[initialEntryId]!!
                } else {
                    0
                }

                val initialFrame = allFrames[initialIndex]
                val zarrIdx = initialFrame.zarrIndex
                if (zarrIdx == null) {
                    Log.e(TAG, "[Zarr] No zarrIndex for initial entry")
                    _loadingState.value = LoadingState.Ready
                    _currentFrameError.value = "Unable to load frame"
                    return@launch
                }

                val depthIndex = ctx.depthIndex(currentDepth)
                val phase3Start = System.currentTimeMillis()
                val slice = zarrReader.loadSlice(preparedDataset, zarrIdx, depthIndex)
                Log.d(TAG, "[Timing] Phase 3 initial frame: ${System.currentTimeMillis() - phase3Start}ms")

                ensureActive()

                // Upload and show initial frame (data copy is thread-safe)
                host.uploadFrame(
                    entryId = initialFrame.entryId,
                    slice = slice,
                    dataMin = initialFrame.dataRange.start,
                    dataMax = initialFrame.dataRange.endInclusive
                )
                host.showFrame(initialFrame.entryId)
                currentFrameKey = initialFrame.entryId
                frameVersion++

                Log.d(TAG, "[Timing] Total to first frame: ${System.currentTimeMillis() - loadStart}ms")

                // Notify UI on main thread (lightweight — just state flip + repaint trigger)
                withContext(Dispatchers.Main) {
                    _loadingState.value = LoadingState.Ready
                    repaint?.invoke()
                }

                // Background loading deferred — frames load on-demand when user scrubs timeline
                // TODO: Add progressive background loading with chunked batches (not all 476 at once)
                Log.d(TAG, "Initial frame displayed. Remaining ${allFrames.size - 1} frames load on-demand.")

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Load failed", e)
                withContext(Dispatchers.Main) {
                    _loadingState.value = LoadingState.Failed(e.message ?: "Unknown error")
                }
            }
        }
    }

    // MARK: - Frame Control

    /**
     * Show frame for timeline scrubbing — loads on-demand if not cached.
     */
    fun showFrame(entryId: String) {
        _currentFrameError.value = null

        val host = _shaderHost ?: return

        // Cache hit — instant
        if (host.isLoaded(entryId)) {
            host.showFrame(entryId)
            currentFrameKey = entryId
            frameVersion++
            repaint?.invoke()
            return
        }

        // Cache miss — load on demand
        showFrameGeneration++
        val generation = showFrameGeneration

        onDemandTask?.cancel()
        onDemandTask = coroutineScope.launch {
            try {
                val ctx = context ?: return@launch
                val depth = currentDepth()
                val frameIndex = ctx.entryIdToIndex(depth)[entryId] ?: return@launch
                val frame = ctx.frames(depth)[frameIndex]

                val zarrIdx = frame.zarrIndex ?: run {
                    _currentFrameError.value = "Frame not available"
                    return@launch
                }

                val depthIndex = ctx.depthIndex(depth)
                val slice = zarrReader.loadSlice(ctx.preparedDataset, zarrIdx, depthIndex)

                // Check generation counter
                if (showFrameGeneration != generation) return@launch

                host.uploadFrame(
                    entryId = frame.entryId,
                    slice = slice,
                    dataMin = frame.dataRange.start,
                    dataMax = frame.dataRange.endInclusive
                )
                host.showFrame(entryId)
                currentFrameKey = entryId
                frameVersion++
                repaint?.invoke()

            } catch (e: CancellationException) {
                // User scrubbed past — expected
            } catch (e: Exception) {
                if (showFrameGeneration == generation) {
                    Log.e(TAG, "Frame load failed", e)
                    _currentFrameError.value = "Unable to load frame"
                }
            }
        }
    }

    // MARK: - Depth Selection

    /**
     * Update depth selection — clears all frames and restarts background loading.
     */
    fun updateDepth(depthMeters: Int): Boolean {
        val ctx = context ?: return false
        if (!ctx.availableDepths.contains(depthMeters)) return false
        if (depthMeters == currentDepth) return false

        currentDepth = depthMeters

        backgroundLoadTask?.cancel()
        onDemandTask?.cancel()
        _shaderHost?.clearFrames()

        startBackgroundLoadAll(0)
        return true
    }

    // MARK: - Config Sync

    /**
     * Update shader uniforms.
     */
    fun setUniforms(
        opacity: Float = 1.0f,
        filterMin: Float = 0f,
        filterMax: Float = 0f,
        filterMode: Int = 0,
        scaleMode: Int = 0,
        blendFactor: Float = 1.0f
    ) {
        _shaderHost?.setUniforms(opacity, filterMin, filterMax, filterMode, scaleMode, blendFactor)
    }

    /**
     * Update colorscale.
     */
    fun setColorscale(colorscale: Colorscale, distribution: ColormapTextureFactory.StopDistribution = ColormapTextureFactory.StopDistribution.Uniform) {
        val bytes = ColormapTextureFactory.createTextureBytes(colorscale, distribution)
        _shaderHost?.setColormap(bytes)
    }

    // MARK: - Cleanup

    /**
     * Clear for dataset switch.
     */
    fun clearForDatasetSwitch() {
        cancelTasks()
        _shaderHost?.clearFrames()
        context = null
        _loadingState.value = LoadingState.Idle
        _currentFrameError.value = null
        currentFrameKey = null
    }

    /**
     * Full teardown — region change.
     */
    fun removeAll() {
        cancelTasks()
        _shaderHost?.clearFrames()
        context = null
        _loadingState.value = LoadingState.Idle
        _currentFrameError.value = null
        currentFrameKey = null
    }

    // MARK: - Private

    private fun cancelTasks() {
        loadTask?.cancel()
        loadTask = null
        backgroundLoadTask?.cancel()
        backgroundLoadTask = null
        onDemandTask?.cancel()
        onDemandTask = null
    }

    private fun buildDepthFrames(
        entries: List<TimeEntry>,
        depths: List<Int>,
        timeToIndex: Map<Long, Int>,
        dataRange: ClosedFloatingPointRange<Float>
    ): Map<Int, ZarrDatasetContext.DepthFrames> {
        val result = mutableMapOf<Int, ZarrDatasetContext.DepthFrames>()

        for (depth in depths) {
            val depthEntries = entries.filter { it.depth == depth }
            val frames = mutableListOf<ZarrFrameMetadata>()
            val index = mutableMapOf<String, Int>()

            for (entry in depthEntries) {
                val timestamp = entry.timestamp
                val zarrIndex = timeToIndex[timestamp]
                index[entry.id] = frames.size
                frames.add(ZarrFrameMetadata(
                    entryId = entry.id,
                    zarrIndex = zarrIndex,
                    dataRange = dataRange,
                    timestamp = timestamp
                ))
            }

            result[depth] = ZarrDatasetContext.DepthFrames(frames, index)
        }

        return result
    }

    private fun startBackgroundLoadAll(initialIndex: Int) {
        val ctx = context ?: return
        val host = _shaderHost ?: return

        backgroundLoadTask?.cancel()

        val depth = currentDepth()
        val frames = ctx.frames(depth)
            .mapIndexed { index, frame -> index to frame }
            .filter { (_, frame) -> frame.hasZarrData && !host.isLoaded(frame.entryId) }
            .sortedBy { (index, _) -> abs(index - initialIndex) }
            .map { it.second }

        if (frames.isEmpty()) return

        val semaphore = Semaphore(ZarrPlaybackConfig.PARALLEL_LOAD_LIMIT)
        val preparedDataset = ctx.preparedDataset
        val depthIndex = ctx.depthIndex(depth)

        backgroundLoadTask = coroutineScope.launch(Dispatchers.IO) {
            frames.map { frame ->
                async {
                    semaphore.withPermit {
                        ensureActive()
                        if (host.isLoaded(frame.entryId)) return@async

                        val zarrIdx = frame.zarrIndex ?: return@async
                        try {
                            val slice = zarrReader.loadSlice(preparedDataset, zarrIdx, depthIndex)
                            ensureActive()
                            // Upload immediately as each frame completes (no batching)
                            host.uploadFrame(
                                entryId = frame.entryId,
                                floats = slice.floats,
                                width = slice.width,
                                height = slice.height,
                                bounds = slice.bounds,
                                dataMin = frame.dataRange.start,
                                dataMax = frame.dataRange.endInclusive
                            )
                        } catch (_: Exception) {
                            // Skip failed frames silently
                        }
                    }
                }
            }.awaitAll()
        }
    }

}

/**
 * Time entry for Zarr loading.
 * Simplified representation of API entry data.
 */
data class TimeEntry(
    val id: String,
    val timestamp: Long,  // Unix seconds
    val depth: Int = 0
)
