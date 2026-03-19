package com.example.saltyoffshore.zarr

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// MARK: - Zarr Data Contract
//
// Backend encoding rules:
// - All floats: <f4 (float32). Time: <i8 (int64).
// - Regional (4D): chunks = [1, 1, H, W] → single spatial chunk per frame.
// - Wind (3D global): chunks = [1, latTile, lonTile] → viewport subsetting.

/**
 * Zarr v2 format constants. Determines metadata file name and chunk key encoding.
 * Matches iOS `ZarrFormat` enum.
 */
enum class ZarrFormat {
    V2;

    /** File name for per-variable array metadata. */
    val metadataFileName: String get() = ".zarray"

    /** File name for root consolidated metadata. */
    val rootMetadataFileName: String get() = ".zmetadata"

    /**
     * Build a chunk storage key from dimension indices.
     * v2 keys: `0.1.2.3` (dot-separated, no prefix)
     */
    fun chunkKey(indices: List<Int>): String = indices.joinToString(".")
}

/**
 * Compressor types supported by Zarr arrays.
 * Currently only zlib is used in production.
 */
enum class ZarrCompressor {
    ZLIB;

    companion object {
        fun fromId(id: String): ZarrCompressor? = when (id.lowercase()) {
            "zlib" -> ZLIB
            else -> null
        }
    }
}

/**
 * Fill value representation for Zarr arrays.
 * Matches iOS `ZarrArrayMetadata.FillValue`.
 */
sealed class ZarrFillValue {
    data object Null : ZarrFillValue()
    data object NaN : ZarrFillValue()
    data class Number(val value: Double) : ZarrFillValue()
}

/**
 * Unified Zarr array metadata — parsed from v2 `.zarray`.
 * Downstream code never inspects format; only `fetchMetadata` and `fetchChunk` use it.
 * Matches iOS `ZarrArrayMetadata` exactly.
 */
data class ZarrArrayMetadata(
    val format: ZarrFormat,
    val shape: List<Int>,
    val chunks: List<Int>,
    val compressor: ZarrCompressor?,
    val fillValue: ZarrFillValue?,
    val order: String,  // "C" row-major
    val isStructured: Boolean  // true for structured dtypes (e.g. metadata records)
) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        /**
         * Parse from Zarr v2 `.zarray` JSON data.
         */
        fun fromV2(data: ByteArray): ZarrArrayMetadata {
            val root = json.decodeFromString<JsonObject>(data.decodeToString())
            return fromV2(root)
        }

        /**
         * Parse from already-decoded JsonObject.
         */
        fun fromV2(root: JsonObject): ZarrArrayMetadata {
            val shape = root["shape"]?.jsonArray?.map { it.jsonPrimitive.int }
                ?: throw ZarrError.InvalidShape("Missing shape in .zarray")
            val chunks = root["chunks"]?.jsonArray?.map { it.jsonPrimitive.int }
                ?: throw ZarrError.InvalidShape("Missing chunks in .zarray")
            val order = root["order"]?.jsonPrimitive?.content ?: "C"

            // Parse compressor
            val compressor = root["compressor"]?.let { compressorJson ->
                if (compressorJson is JsonNull) null
                else {
                    val id = compressorJson.jsonObject["id"]?.jsonPrimitive?.content
                    id?.let { ZarrCompressor.fromId(it) }
                }
            }

            // Parse fill_value (can be null, "NaN", or a number)
            val fillValue = parseFillValue(root["fill_value"])

            // Parse dtype - check if structured (array of arrays)
            val isStructured = root["dtype"]?.let { dtype ->
                dtype is JsonArray
            } ?: false

            return ZarrArrayMetadata(
                format = ZarrFormat.V2,
                shape = shape,
                chunks = chunks,
                compressor = compressor,
                fillValue = fillValue,
                order = order,
                isStructured = isStructured
            )
        }

        private fun parseFillValue(element: JsonElement?): ZarrFillValue? {
            if (element == null || element is JsonNull) return ZarrFillValue.Null
            if (element is JsonPrimitive) {
                if (element.isString && element.content == "NaN") return ZarrFillValue.NaN
                element.doubleOrNull?.let { return ZarrFillValue.Number(it) }
            }
            return ZarrFillValue.Null
        }
    }
}

/**
 * Well-known coordinate array names present in every Zarr store.
 * Single source of truth — eliminates magic strings in ZarrReader.
 * Matches iOS `ZarrCoordinate` enum.
 */
enum class ZarrCoordinate(val value: String) {
    X("x"),
    Y("y"),
    TIME("time"),
    DEPTH("depth")
}

/**
 * Parsed from root `.zmetadata` — lists every variable and its full array metadata.
 * One HTTP request replaces N per-variable metadata fetches.
 * Matches iOS `ZarrStoreManifest` exactly.
 */
data class ZarrStoreManifest(
    /** Variable name → array metadata (shape, chunks, codecs) */
    val arrays: Map<String, ZarrArrayMetadata>
) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        /**
         * Parse from v2 `.zmetadata` consolidated metadata.
         * Keys are like `"sea_surface_temperature/.zarray"` — filter for `/.zarray` suffix,
         * strip suffix to get variable name.
         */
        fun fromRootMetadata(data: ByteArray): ZarrStoreManifest {
            val root = json.decodeFromString<JsonObject>(data.decodeToString())
            val metadata = root["metadata"]?.jsonObject
                ?: throw ZarrError.DataNotFound(".zmetadata missing metadata key")

            val arrays = mutableMapOf<String, ZarrArrayMetadata>()
            val suffix = "/.zarray"

            for ((key, value) in metadata) {
                if (!key.endsWith(suffix)) continue
                if (value !is JsonObject) continue
                val varName = key.dropLast(suffix.length)
                val arrayMeta = ZarrArrayMetadata.fromV2(value)
                arrays[varName] = arrayMeta
            }

            return ZarrStoreManifest(arrays)
        }
    }
}
