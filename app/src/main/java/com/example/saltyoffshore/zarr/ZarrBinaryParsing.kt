package com.example.saltyoffshore.zarr

import java.nio.ByteBuffer
import java.nio.ByteOrder

// MARK: - Binary Parsing (pure functions)
//
// Zarr dtype contract:
// - Data variables: <f4 (float32). Coordinates/metadata: <f4 (float32). Time: <i8 (int64).
// These are top-level functions so they can be called from any concurrency context.

/**
 * Parse little-endian float32 data via ByteBuffer.
 * Android is little-endian ARM — decompressed bytes are already IEEE 754 floats.
 * Used for data variables (SST, chlorophyll, currents, etc.) and coordinate arrays.
 *
 * Matches iOS `parseFloats(from:)` exactly.
 */
fun parseFloats(data: ByteArray): FloatArray {
    val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
    val count = data.size / Float.SIZE_BYTES
    return FloatArray(count) { buffer.float }
}

/**
 * Parse little-endian int64 data via ByteBuffer.
 * Used for time arrays.
 *
 * Matches iOS `parseInt64(from:)` exactly.
 */
fun parseInt64(data: ByteArray): LongArray {
    val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
    val count = data.size / Long.SIZE_BYTES
    return LongArray(count) { buffer.long }
}

/**
 * Parse little-endian float64 data via ByteBuffer.
 * Used for coordinate arrays when stored as double precision.
 */
fun parseDoubles(data: ByteArray): DoubleArray {
    val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
    val count = data.size / Double.SIZE_BYTES
    return DoubleArray(count) { buffer.double }
}

/**
 * Flip rows vertically (row 0 ↔ row height-1) for a row-major float array.
 * Used by ZarrReader (velocity grids) and heatmap conversion.
 *
 * Matches iOS `flipRows(_:width:height:)` exactly.
 */
fun flipRows(data: FloatArray, width: Int, height: Int): FloatArray {
    val flipped = FloatArray(data.size)
    for (row in 0 until height) {
        val srcStart = row * width
        val dstStart = (height - 1 - row) * width
        for (col in 0 until width) {
            flipped[dstStart + col] = data[srcStart + col]
        }
    }
    return flipped
}

/**
 * Check if a float value represents valid data (not NaN or fill value).
 * Most ocean data uses NaN as fill value; some use large numbers like 1e35.
 */
fun isValidValue(v: Float): Boolean {
    return !v.isNaN() && v < 1e30f
}
