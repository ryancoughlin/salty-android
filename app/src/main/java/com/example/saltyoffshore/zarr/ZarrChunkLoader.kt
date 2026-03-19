package com.example.saltyoffshore.zarr

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.zip.Inflater
import kotlin.math.max

private const val TAG = "ZarrChunkLoader"

// MARK: - Network Configuration

/**
 * Tuning knobs for Zarr network I/O.
 * Matches iOS `ZarrNetworkConfig` exactly.
 */
object ZarrNetworkConfig {
    /** Max simultaneous HTTP connections to S3 per host. */
    const val CONNECTIONS_PER_HOST = 12

    /** How long to wait for a single HTTP request before giving up (ms). */
    const val REQUEST_TIMEOUT_MS = 120_000L

    /** Largest chunk dimension (512×512 float32 = 1MB uncompressed). */
    const val MAX_CHUNK_BYTES = 512 * 512 * Float.SIZE_BYTES
}

/**
 * Stateless service for fetching and decompressing Zarr chunks.
 *
 * Responsibilities:
 * - Network I/O with offline cache fallback
 * - Chunk decompression (zlib)
 * - Metadata and manifest parsing
 *
 * This is a stateless object because it has no mutable state to protect.
 * HttpClient is thread-safe, and decompression is a pure function.
 *
 * Used by ZarrReader for data loading.
 * Matches iOS `ZarrChunkLoader` exactly.
 */
class ZarrChunkLoader(
    private val httpClient: HttpClient = HttpClient(OkHttp) {
        engine {
            config {
                connectTimeout(ZarrNetworkConfig.REQUEST_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS)
                readTimeout(ZarrNetworkConfig.REQUEST_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS)
            }
        }
    }
) {
    // MARK: - Public API

    /**
     * Fetch the consolidated `.zmetadata` from a Zarr store.
     * @param bustCache When true, appends a timestamp query param to bypass CDN edge cache.
     */
    suspend fun fetchManifest(storeUrl: String, bustCache: Boolean = false): ZarrStoreManifest {
        val url = buildUrl(storeUrl, ".zmetadata", bustCache)
        val data = fetchData(url)
        val manifest = ZarrStoreManifest.fromRootMetadata(data)
        if (manifest.arrays.isEmpty()) {
            throw ZarrError.DataNotFound(".zmetadata contains no arrays")
        }
        return manifest
    }

    /**
     * Fetch `.zarray` metadata for a variable.
     * @param bustCache When true, bypasses CDN edge cache.
     */
    suspend fun fetchMetadata(baseUrl: String, bustCache: Boolean = false): ZarrArrayMetadata {
        val url = buildUrl(baseUrl, ".zarray", bustCache)
        val data = fetchData(url)
        return ZarrArrayMetadata.fromV2(data)
    }

    /**
     * Fetch and decompress a single chunk.
     * @param bustCache When true, bypasses CDN edge cache.
     */
    suspend fun fetchChunk(
        indices: List<Int>,
        metadata: ZarrArrayMetadata,
        baseUrl: String,
        bustCache: Boolean = false
    ): ByteArray {
        val chunkKey = metadata.format.chunkKey(indices)
        val url = buildUrl(baseUrl, chunkKey, bustCache)
        val compressed = fetchData(url)
        return decompress(compressed, metadata.compressor)
    }

    // MARK: - Network

    private fun buildUrl(base: String, path: String, bustCache: Boolean): String {
        val cleanBase = base.trimEnd('/')
        val url = "$cleanBase/$path"
        return if (bustCache) {
            val timestamp = System.currentTimeMillis() / 1000
            if (url.contains("?")) "$url&_cb=$timestamp" else "$url?_cb=$timestamp"
        } else {
            url
        }
    }

    private suspend fun fetchData(url: String): ByteArray {
        val startTime = System.currentTimeMillis()
        val label = url.substringAfterLast("/", "chunk")

        return withContext(Dispatchers.IO) {
            try {
                val response = httpClient.get(url)
                if (!response.status.isSuccess()) {
                    throw ZarrError.NetworkError("HTTP ${response.status.value} for $url")
                }
                val data = response.bodyAsBytes()
                val elapsed = System.currentTimeMillis() - startTime
                Log.d(TAG, "[Timing] network $label: ${elapsed}ms ${data.size}B")
                data
            } catch (e: ZarrError) {
                throw e
            } catch (e: Exception) {
                val elapsed = System.currentTimeMillis() - startTime
                Log.w(TAG, "[Timing] failed $label: ${elapsed}ms error=${e.message}")
                throw ZarrError.NetworkError(e.message ?: "Unknown network error")
            }
        }
    }

    // MARK: - Decompression (pure functions)

    /**
     * Pure function: decompress chunk data.
     * Zlib library is thread-safe; no mutable state accessed.
     */
    private fun decompress(data: ByteArray, compressor: ZarrCompressor?): ByteArray {
        return when (compressor) {
            ZarrCompressor.ZLIB -> decompressZlib(data)
            null -> data
        }
    }

    /**
     * Decompress raw DEFLATE data.
     */
    private fun decompressRawDeflate(deflateData: ByteArray): ByteArray {
        // 512×512 float32 = 1,048,576 bytes uncompressed (largest chunk in use).
        // Ocean-edge chunks compress to very small sizes (mostly NaN), so
        // ratio-based sizing can underestimate. Use a floor that covers the
        // largest known chunk, plus a ratio for unexpected larger payloads.
        val bufferSize = max(deflateData.size * 4, ZarrNetworkConfig.MAX_CHUNK_BYTES)

        val inflater = Inflater()
        inflater.setInput(deflateData)

        val output = ByteArrayOutputStream()
        val buffer = ByteArray(bufferSize)

        try {
            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                if (count == 0 && inflater.needsInput()) {
                    break
                }
                output.write(buffer, 0, count)
            }
        } finally {
            inflater.end()
        }

        val result = output.toByteArray()
        if (result.isEmpty()) {
            throw ZarrError.DecompressionFailed("deflate decompression produced 0 bytes")
        }
        return result
    }

    /**
     * Decompress zlib data (2-byte header + deflate + 4-byte Adler-32).
     * Matches iOS `decompressZlib(_:)` exactly.
     */
    private fun decompressZlib(data: ByteArray): ByteArray {
        if (data.size <= 6) {
            throw ZarrError.DecompressionFailed("zlib data too short")
        }
        // Strip 2-byte header and 4-byte Adler-32 checksum
        val deflateData = data.sliceArray(2 until data.size - 4)
        return decompressRawDeflate(deflateData)
    }
}
