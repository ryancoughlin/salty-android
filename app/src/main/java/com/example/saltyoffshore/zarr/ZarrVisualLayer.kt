package com.example.saltyoffshore.zarr

import android.util.Log
import com.mapbox.maps.CustomLayerHost
import com.mapbox.maps.CustomLayerRenderParameters

private const val TAG = "ZarrVisualLayer"

/**
 * Mapbox CustomLayerHost implementation for Zarr data visualization.
 *
 * Bridges Mapbox's render loop to the native OpenGL ES 3.0 shader host.
 * All rendering is delegated to C++ for maximum performance.
 *
 * Matches iOS `ZarrShaderHost` as a `CustomLayerHost` exactly.
 */
class ZarrVisualLayer : CustomLayerHost {

    private val bridge = ZarrShaderHostBridge()

    // Track initialization state
    private var isInitialized = false

    init {
        bridge.create()
        Log.d(TAG, "ZarrVisualLayer created")
    }

    // MARK: - CustomLayerHost Protocol

    override fun initialize() {
        bridge.initialize()
        isInitialized = true
        Log.d(TAG, "ZarrVisualLayer initialized")
    }

    override fun render(parameters: CustomLayerRenderParameters) {
        if (!isInitialized) return

        // Convert Mapbox projection matrix to double array
        val projMatrix = DoubleArray(16)
        for (i in 0 until 16) {
            projMatrix[i] = parameters.projectionMatrix[i].toDouble()
        }

        bridge.render(projMatrix, parameters.zoom)
    }

    override fun contextLost() {
        Log.w(TAG, "GL context lost")
        isInitialized = false
    }

    override fun deinitialize() {
        bridge.deinitialize()
        isInitialized = false
        Log.d(TAG, "ZarrVisualLayer deinitialized")
    }

    // MARK: - Public API

    /**
     * Upload a frame to GPU memory.
     * Thread-safe: can be called from any thread.
     */
    fun uploadFrame(
        entryId: String,
        floats: FloatArray,
        width: Int, height: Int,
        bounds: GridBounds,
        dataMin: Float, dataMax: Float
    ) {
        bridge.uploadFrame(
            entryId = entryId,
            floats = floats,
            width = width,
            height = height,
            swEasting = bounds.swEasting,
            swNorthing = bounds.swNorthing,
            neEasting = bounds.neEasting,
            neNorthing = bounds.neNorthing,
            dataMin = dataMin,
            dataMax = dataMax
        )
    }

    /**
     * Upload a frame from ZarrSliceData.
     */
    fun uploadFrame(
        entryId: String,
        slice: ZarrSliceData,
        dataMin: Float,
        dataMax: Float
    ) {
        uploadFrame(
            entryId = entryId,
            floats = slice.floats,
            width = slice.width,
            height = slice.height,
            bounds = slice.bounds,
            dataMin = dataMin,
            dataMax = dataMax
        )
    }

    /**
     * Switch to a cached frame for display.
     * @return true if frame was found and displayed
     */
    fun showFrame(entryId: String): Boolean {
        return bridge.showFrame(entryId)
    }

    /**
     * Set colormap from RGBA byte array.
     * @param rgbaBytes 256×1 RGBA pixel data (1024 bytes)
     */
    fun setColormap(rgbaBytes: ByteArray) {
        bridge.setColormap(rgbaBytes)
    }

    /**
     * Set shader uniforms for rendering.
     */
    fun setUniforms(
        opacity: Float = 1.0f,
        filterMin: Float = 0f,
        filterMax: Float = 0f,
        filterMode: Int = 0,
        scaleMode: Int = 0,
        blendFactor: Float = 1.0f
    ) {
        bridge.setUniforms(opacity, filterMin, filterMax, filterMode, scaleMode, blendFactor)
    }

    /**
     * Clear all cached frames.
     * Call when switching datasets.
     */
    fun clearFrames() {
        bridge.clearFrames()
    }

    /**
     * Check if a frame is loaded in the cache.
     */
    fun isLoaded(entryId: String): Boolean {
        return bridge.isLoaded(entryId)
    }

    /**
     * Set layer visibility.
     */
    fun setVisible(visible: Boolean) {
        bridge.setVisible(visible)
    }

    /**
     * Destroy native resources.
     * Call when layer is no longer needed.
     */
    fun destroy() {
        bridge.destroy()
        Log.d(TAG, "ZarrVisualLayer destroyed")
    }
}
