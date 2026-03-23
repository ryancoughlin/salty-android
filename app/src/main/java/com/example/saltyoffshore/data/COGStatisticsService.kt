package com.example.saltyoffshore.data

import com.example.saltyoffshore.config.AppConstants
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.net.URLEncoder

// MARK: - COG Statistics Response

/**
 * Raw statistics response from tiler service.
 * Dynamically handles ANY band configuration - single or multi-band COG files.
 *
 * Examples:
 * - SST: { "b1": {...} }
 * - Sea Surface Height: { "sla": {...}, "adt": {...} }
 * - Currents: { "u": {...}, "v": {...} }
 *
 * iOS: COGStatisticsResponse
 */
@Serializable(with = COGStatisticsResponseSerializer::class)
data class COGStatisticsResponse(
    private val bands: Map<String, COGBandStatistics>
) {
    fun statistics(forBand: String): COGBandStatistics? = bands[forBand]

    val availableBands: List<String> get() = bands.keys.sorted()

    fun primaryBandStatistics(preferredBand: String? = null): COGBandStatistics? {
        preferredBand?.let { bands[it] }?.let { return it }
        bands["b1"]?.let { return it }
        bands["sla"]?.let { return it }
        return bands.values.firstOrNull()
    }

    val b1: COGBandStatistics? get() = bands["b1"]
    val sla: COGBandStatistics? get() = bands["sla"]
    val isMultiBand: Boolean get() = bands.size > 1
}

/**
 * Custom serializer for COGStatisticsResponse.
 * The JSON is a flat object like {"b1": {...stats...}, "sla": {...stats...}}.
 */
object COGStatisticsResponseSerializer : KSerializer<COGStatisticsResponse> {
    private val mapSerializer = MapSerializer(String.serializer(), COGBandStatistics.serializer())

    override val descriptor: SerialDescriptor = mapSerializer.descriptor

    override fun serialize(encoder: Encoder, value: COGStatisticsResponse) {
        // Re-encode by reading availableBands + statistics
        val map = value.availableBands.associateWith { value.statistics(it)!! }
        encoder.encodeSerializableValue(mapSerializer, map)
    }

    override fun deserialize(decoder: Decoder): COGStatisticsResponse {
        val map = decoder.decodeSerializableValue(mapSerializer)
        return COGStatisticsResponse(bands = map)
    }
}

// MARK: - Statistics Service

/**
 * Service for fetching COG statistics from TiTiler.
 * iOS: actor COGStatisticsService
 */
object COGStatisticsService {

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    suspend fun fetchStatistics(cogUrl: String): COGStatisticsResponse {
        val encodedUrl = URLEncoder.encode(cogUrl, "UTF-8")
        val url = "${AppConstants.titilerBaseURL}/cog/statistics?url=$encodedUrl&max_size=1024"
        val response: COGStatisticsResponse = client.get(url).body()

        val primaryStats = response.primaryBandStatistics()
        if (primaryStats?.validPixels == 0.0) {
            throw StatisticsError.NoDataAvailable
        }
        return response
    }
}

// MARK: - Error Types

sealed class StatisticsError(message: String) : Exception(message) {
    data object InvalidURL : StatisticsError("Invalid COG URL")
    data object InvalidResponse : StatisticsError("Invalid response from statistics service")
    data class HttpError(val code: Int) : StatisticsError("HTTP error: $code")
    data class DecodingFailed(val error: Throwable) : StatisticsError("Failed to decode statistics: ${error.message}")
    data object NoDataAvailable : StatisticsError("No satellite data available for this time period")
}
