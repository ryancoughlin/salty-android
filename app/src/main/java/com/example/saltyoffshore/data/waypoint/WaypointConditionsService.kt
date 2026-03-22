package com.example.saltyoffshore.data.waypoint

import com.example.saltyoffshore.config.AppConstants
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.Json

/**
 * Service for fetching environmental conditions at waypoint locations.
 *
 * iOS ref: WaypointConditionsService (actor)
 * Endpoints:
 *   GET /conditions?lat={lat}&lon={lon}
 *   GET /conditions/history?lat={lat}&lon={lon}&days=7
 */
object WaypointConditionsService {

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    /** Fetch current conditions at a coordinate. */
    suspend fun fetchConditions(lat: Double, lon: Double, regionId: String? = null): ConditionsResponse {
        return client.get("${AppConstants.apiBaseURL}/conditions") {
            parameter("lat", lat)
            parameter("lon", lon)
            if (regionId != null) parameter("region_id", regionId)
        }.body()
    }

    /** Fetch 7-day historical conditions at a coordinate. */
    suspend fun fetchHistory(lat: Double, lon: Double, regionId: String? = null): ConditionsHistoryResponse {
        return client.get("${AppConstants.apiBaseURL}/conditions/history") {
            parameter("lat", lat)
            parameter("lon", lon)
            parameter("days", 7)
            if (regionId != null) parameter("region_id", regionId)
        }.body()
    }
}

// ── API Response Models ─────────────────────────────────────────────────────

/**
 * Complete conditions response from /conditions endpoint.
 * iOS ref: ConditionsResponse
 */
@Serializable
data class ConditionsResponse(
    @SerialName("region_id") val regionId: String,
    val location: LocationData,
    @SerialName("generated_at") val generatedAt: String,
    val results: List<ConditionResult>,
    val solar: SolarInfo,
    val moon: MoonInfo
) {
    /** Combined sunrise/sunset string like "6:21 AM / 6:31 PM" */
    val sunriseSunsetString: String?
        get() {
            val rise = solar.sunrise ?: return null
            val set = solar.sunset ?: return null
            return "${formatTimeFromISO(rise)} / ${formatTimeFromISO(set)}"
        }
}

@Serializable
data class LocationData(
    val lat: Double,
    val lon: Double
)

@Serializable
data class SolarInfo(
    val sunrise: String? = null,
    val sunset: String? = null,
    @SerialName("solar_noon") val solarNoon: String? = null
)

@Serializable
data class MoonInfo(
    @SerialName("phase_name") val phaseName: String,
    val illumination: Int,
    @SerialName("phase_value") val phaseValue: Double
)

/**
 * Individual condition result.
 * iOS ref: ConditionResult
 *
 * The `value` field is polymorphic: it can be a simple number or a
 * JSON object (for currents: {speed, direction}).  We keep it as
 * JsonElement and expose helpers.
 */
@Serializable
data class ConditionResult(
    val type: String,
    val condition: String,
    val value: JsonElement? = null,
    @SerialName("observed_at") val observedAt: String? = null
) {
    val hasData: Boolean get() = condition != "No data"

    /** User-friendly display name. iOS ref: ConditionResult.displayName */
    val displayName: String
        get() = when (type) {
            "sst" -> "Sea Temperature"
            "chlorophyll" -> "Water Color"
            "salinity" -> "Salinity"
            "mld" -> "Mixed Layer Depth"
            "ssh", "eddys" -> "Sea Surface Height"
            "currents" -> "Currents"
            "dissolved_oxygen" -> "Dissolved Oxygen"
            "phytoplankton" -> "Phytoplankton"
            "fsle" -> "FSLE"
            else -> type.replaceFirstChar { it.uppercase() }
        }

    /** Short abbreviation. iOS ref: ConditionResult.shortName */
    val shortName: String
        get() = when (type) {
            "sst" -> "SST"
            "chlorophyll" -> "CHL"
            "salinity" -> "SAL"
            "mld" -> "MLD"
            "ssh", "eddys" -> "SSH"
            "currents" -> "CUR"
            "dissolved_oxygen" -> "DO"
            "phytoplankton" -> "PHY"
            "fsle" -> "FSLE"
            else -> type.uppercase().take(3)
        }

    /** Relative time string for observation. iOS ref: ConditionResult.relativeTimeString */
    val relativeTimeString: String
        get() {
            val observed = observedAt ?: return "\u2014"
            val millis = parseISO8601Millis(observed) ?: return "\u2014"
            val diff = System.currentTimeMillis() - millis
            val hours = (diff / 3_600_000).toInt()
            val days = (diff / 86_400_000).toInt()
            return when {
                hours < 1 -> "Just now"
                hours < 24 -> "$hours hour${if (hours == 1) "" else "s"} ago"
                else -> "$days day${if (days == 1) "" else "s"} ago"
            }
        }
}

// ── History Response Models ─────────────────────────────────────────────────

/**
 * 7-day historical conditions response.
 * iOS ref: ConditionsHistoryResponse
 */
@Serializable
data class ConditionsHistoryResponse(
    @SerialName("region_id") val regionId: String,
    val location: LocationData,
    @SerialName("date_range") val dateRange: DateRange,
    @SerialName("generated_at") val generatedAt: String,
    val results: List<ConditionHistoryResult>
)

@Serializable
data class DateRange(
    val start: String,
    val end: String
)

@Serializable
data class ConditionHistoryResult(
    val type: String,
    @SerialName("data_points") val dataPoints: List<HistoryDataPoint>,
    @SerialName("update_frequency") val updateFrequency: String,
    val summary: String? = null
)

@Serializable
data class HistoryDataPoint(
    val date: String,
    val value: JsonElement? = null,
    val condition: String? = null,
    @SerialName("observed_at") val observedAt: String? = null
) {
    /** Numeric value for charting; handles both simple numbers and currents objects. */
    val numericValue: Double?
        get() = when {
            value == null -> null
            value is JsonPrimitive && value.jsonPrimitive.isString.not() -> {
                value.jsonPrimitive.double
            }
            value is JsonObject -> {
                value.jsonObject["speed"]?.jsonPrimitive?.double
            }
            else -> null
        }
}

// ── Helpers ─────────────────────────────────────────────────────────────────

/** Minimal ISO-8601 time parser returning epoch millis. */
private fun parseISO8601Millis(iso: String): Long? {
    return try {
        java.time.Instant.parse(iso).toEpochMilli()
    } catch (_: Exception) {
        null
    }
}

/** Format an ISO-8601 string to a short local time like "6:21 AM". */
private fun formatTimeFromISO(iso: String): String {
    return try {
        val instant = java.time.Instant.parse(iso)
        val local = java.time.LocalTime.ofInstant(instant, java.time.ZoneId.systemDefault())
        val formatter = java.time.format.DateTimeFormatter.ofPattern("h:mm a")
        local.format(formatter)
    } catch (_: Exception) {
        iso
    }
}
