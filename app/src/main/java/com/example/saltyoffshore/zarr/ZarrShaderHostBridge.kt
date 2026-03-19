package com.example.saltyoffshore.zarr

/**
 * JNI bridge for ZarrShaderHost C++ implementation.
 *
 * Provides native OpenGL ES 3.0 rendering for Zarr data visualization.
 * Must be called from the GL thread for render operations.
 *
 * Matches iOS `ZarrShaderHost` API exactly.
 */
class ZarrShaderHostBridge {

    companion object {
        init {
            System.loadLibrary("zarr-shader")
        }
    }

    // Native pointer handle
    private var handle: Long = 0

    /**
     * Create native ZarrShaderHost instance.
     * Call this before any other operations.
     */
    fun create() {
        if (handle == 0L) {
            handle = nativeCreate()
        }
    }

    /**
     * Destroy native ZarrShaderHost instance.
     * Call this when done with the layer.
     */
    fun destroy() {
        if (handle != 0L) {
            nativeDestroy(handle)
            handle = 0
        }
    }

    /**
     * Initialize OpenGL ES pipeline.
     * Must be called from GL thread after context is available.
     */
    fun initialize() {
        if (handle != 0L) {
            nativeInitialize(handle)
        }
    }

    /**
     * Render the current frame.
     * Must be called from GL thread.
     *
     * @param projectionMatrix 4x4 column-major projection matrix from Mapbox
     * @param zoom Current map zoom level
     */
    fun render(projectionMatrix: DoubleArray, zoom: Double) {
        if (handle != 0L) {
            nativeRender(handle, projectionMatrix, zoom)
        }
    }

    /**
     * Deinitialize OpenGL ES pipeline.
     * Must be called from GL thread before context is destroyed.
     */
    fun deinitialize() {
        if (handle != 0L) {
            nativeDeinitialize(handle)
        }
    }

    /**
     * Upload a frame to GPU memory.
     *
     * @param entryId Unique identifier for this frame (typically timeEntry ID)
     * @param floats Float array of data values
     * @param width Texture width
     * @param height Texture height
     * @param swEasting Southwest corner easting (EPSG:3857)
     * @param swNorthing Southwest corner northing (EPSG:3857)
     * @param neEasting Northeast corner easting (EPSG:3857)
     * @param neNorthing Northeast corner northing (EPSG:3857)
     * @param dataMin Data range minimum (for colormap)
     * @param dataMax Data range maximum (for colormap)
     */
    fun uploadFrame(
        entryId: String,
        floats: FloatArray,
        width: Int, height: Int,
        swEasting: Double, swNorthing: Double,
        neEasting: Double, neNorthing: Double,
        dataMin: Float, dataMax: Float
    ) {
        if (handle != 0L) {
            nativeUploadFrame(
                handle, entryId, floats,
                width, height,
                swEasting, swNorthing, neEasting, neNorthing,
                dataMin, dataMax
            )
        }
    }

    /**
     * Switch to a cached frame for display.
     *
     * @param entryId The frame to show (must have been uploaded first)
     * @return true if frame was found and displayed
     */
    fun showFrame(entryId: String): Boolean {
        return if (handle != 0L) {
            nativeShowFrame(handle, entryId)
        } else false
    }

    /**
     * Set colormap LUT texture.
     *
     * @param rgbaBytes 256×1 RGBA pixel data (1024 bytes)
     */
    fun setColormap(rgbaBytes: ByteArray) {
        if (handle != 0L) {
            nativeSetColormap(handle, rgbaBytes)
        }
    }

    /**
     * Set shader uniforms.
     *
     * @param opacity Layer opacity [0-1]
     * @param filterMin Filter range minimum
     * @param filterMax Filter range maximum
     * @param filterMode 0=squash, 1=hideShow
     * @param scaleMode 0=linear, 1=log10, 2=divergent, 3=sqrt
     * @param blendFactor Crossfade factor [0-1], 0=previous, 1=current
     */
    fun setUniforms(
        opacity: Float,
        filterMin: Float, filterMax: Float,
        filterMode: Int, scaleMode: Int,
        blendFactor: Float
    ) {
        if (handle != 0L) {
            nativeSetUniforms(handle, opacity, filterMin, filterMax, filterMode, scaleMode, blendFactor)
        }
    }

    /**
     * Clear all cached frames.
     * Call when switching datasets.
     */
    fun clearFrames() {
        if (handle != 0L) {
            nativeClearFrames(handle)
        }
    }

    /**
     * Check if a frame is loaded in the cache.
     *
     * @param entryId Frame identifier
     * @return true if frame is cached
     */
    fun isLoaded(entryId: String): Boolean {
        return if (handle != 0L) {
            nativeIsLoaded(handle, entryId)
        } else false
    }

    /**
     * Set layer visibility.
     *
     * @param visible true to show layer
     */
    fun setVisible(visible: Boolean) {
        if (handle != 0L) {
            nativeSetVisible(handle, visible)
        }
    }

    // Native method declarations
    private external fun nativeCreate(): Long
    private external fun nativeDestroy(handle: Long)
    private external fun nativeInitialize(handle: Long)
    private external fun nativeRender(handle: Long, projectionMatrix: DoubleArray, zoom: Double)
    private external fun nativeDeinitialize(handle: Long)
    private external fun nativeUploadFrame(
        handle: Long, entryId: String, floats: FloatArray,
        width: Int, height: Int,
        swEasting: Double, swNorthing: Double,
        neEasting: Double, neNorthing: Double,
        dataMin: Float, dataMax: Float
    )
    private external fun nativeShowFrame(handle: Long, entryId: String): Boolean
    private external fun nativeSetColormap(handle: Long, rgbaBytes: ByteArray)
    private external fun nativeSetUniforms(
        handle: Long,
        opacity: Float,
        filterMin: Float, filterMax: Float,
        filterMode: Int, scaleMode: Int,
        blendFactor: Float
    )
    private external fun nativeClearFrames(handle: Long)
    private external fun nativeIsLoaded(handle: Long, entryId: String): Boolean
    private external fun nativeSetVisible(handle: Long, visible: Boolean)
}
