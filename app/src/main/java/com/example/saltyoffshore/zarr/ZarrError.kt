package com.example.saltyoffshore.zarr

/**
 * Zarr-specific errors for data loading operations.
 * Matches iOS `ZarrError` enum exactly.
 */
sealed class ZarrError(override val message: String) : Exception(message) {
    class InvalidShape(msg: String) : ZarrError("Invalid shape: $msg")
    class DataNotFound(msg: String) : ZarrError("Data not found: $msg")
    class UnsupportedCompressor(msg: String) : ZarrError("Unsupported compressor: $msg")
    class DecompressionFailed(msg: String) : ZarrError("Decompression failed: $msg")
    class NetworkError(msg: String) : ZarrError("Network error: $msg")
}
