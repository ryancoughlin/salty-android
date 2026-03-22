package com.example.saltyoffshore.data.waypoint

import android.util.Log
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.time.Instant
import java.util.UUID
import kotlin.math.abs

/**
 * Pure logic service for GPX import -- parses and deduplicates, never touches the store.
 * Callers apply the result to the ViewModel/Store.
 *
 * Ported from iOS GPXImportService.swift + GPXParser.swift.
 */
object GPXImportService {

    private const val TAG = "GPXImportService"

    // ~3m duplicate threshold (matches iOS 0.00003 degrees)
    private const val DUPLICATE_COORD_THRESHOLD = 0.00003

    data class ImportResult(
        val waypointsToAdd: List<Waypoint>,
        val waypointsToRemove: List<Waypoint>
    )

    /**
     * Parse a GPX input stream and deduplicate against existing waypoints.
     * Returns which waypoints to add and which to remove (for replace mode).
     */
    suspend fun parseAndDeduplicate(
        inputStream: InputStream,
        options: GPXImportOptions = GPXImportOptions(),
        existingWaypoints: List<Waypoint>
    ): ImportResult = withContext(Dispatchers.Default) {
        Log.d(TAG, "Starting import from GPX data")

        val parsed = parseGPX(inputStream, options)
        Log.d(TAG, "Parsed ${parsed.size} waypoints")

        when (options.duplicateHandling) {
            GPXImportOptions.DuplicateHandlingStrategy.SkipDuplicates -> {
                val toAdd = filterOutDuplicates(parsed, existingWaypoints)
                Log.d(TAG, "After dedup: ${toAdd.size} to add")
                ImportResult(waypointsToAdd = toAdd, waypointsToRemove = emptyList())
            }
            GPXImportOptions.DuplicateHandlingStrategy.ReplaceDuplicates -> {
                val toRemove = existingWaypoints.filter { existing ->
                    parsed.any { isDuplicate(existing, it) }
                }
                ImportResult(waypointsToAdd = parsed, waypointsToRemove = toRemove)
            }
            GPXImportOptions.DuplicateHandlingStrategy.AlwaysAdd -> {
                ImportResult(waypointsToAdd = parsed, waypointsToRemove = emptyList())
            }
        }
    }

    // -- Parsing --

    private fun parseGPX(
        inputStream: InputStream,
        options: GPXImportOptions
    ): List<Waypoint> {
        val parser = Xml.newPullParser()
        parser.setInput(inputStream, "UTF-8")
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)

        val waypoints = mutableListOf<Waypoint>()

        // Current waypoint being built
        var lat: Double? = null
        var lon: Double? = null
        var name: String? = null
        var desc: String? = null
        var sym: String? = null
        var time: String? = null
        var type: String? = null
        var inWpt = false
        var inExtensions = false
        var inWaypointExtension = false
        var currentText = StringBuilder()

        // Extension fields
        var altName: String? = null
        var notes: String? = null
        var category: String? = null

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentText.clear()
                    val tag = parser.name

                    when {
                        tag == "wpt" -> {
                            inWpt = true
                            lat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
                            lon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()
                            name = null; desc = null; sym = null; time = null; type = null
                            altName = null; notes = null; category = null
                        }
                        tag == "extensions" && inWpt -> {
                            inExtensions = true
                        }
                        inExtensions && (tag == "WaypointExtension" || tag == "waypointExtension") -> {
                            inWaypointExtension = true
                        }
                    }
                }

                XmlPullParser.TEXT -> {
                    currentText.append(parser.text)
                }

                XmlPullParser.END_TAG -> {
                    val tag = parser.name
                    val text = currentText.toString().trim()

                    when {
                        tag == "wpt" && inWpt -> {
                            val wptLat = lat
                            val wptLon = lon
                            if (wptLat != null && wptLon != null &&
                                wptLat in -90.0..90.0 && wptLon in -180.0..180.0
                            ) {
                                val waypoint = createWaypoint(
                                    lat = wptLat, lon = wptLon,
                                    name = name ?: altName, desc = desc, sym = sym,
                                    time = time, notes = notes, category = category,
                                    options = options
                                )
                                waypoints.add(waypoint)
                            } else {
                                Log.w(TAG, "Skipping waypoint with invalid coords: lat=$wptLat lon=$wptLon")
                            }
                            inWpt = false
                            inExtensions = false
                            inWaypointExtension = false
                        }
                        tag == "extensions" -> {
                            inExtensions = false
                        }
                        (tag == "WaypointExtension" || tag == "waypointExtension") && inWaypointExtension -> {
                            inWaypointExtension = false
                        }
                        inWpt && !inExtensions && text.isNotEmpty() -> {
                            // Standard GPX elements
                            when (tag) {
                                "name", "n" -> name = text
                                "desc" -> desc = text
                                "sym" -> sym = text
                                "time" -> time = text
                                "type" -> type = text
                            }
                        }
                        inWpt && inWaypointExtension && text.isNotEmpty() -> {
                            // Vendor extension elements
                            when (tag) {
                                "altName", "AltName" -> altName = text
                                "Notes", "notes" -> notes = text
                                "Category", "category" -> category = text
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        return waypoints
    }

    private fun createWaypoint(
        lat: Double,
        lon: Double,
        name: String?,
        desc: String?,
        sym: String?,
        time: String?,
        notes: String?,
        category: String?,
        options: GPXImportOptions
    ): Waypoint {
        // Determine symbol based on import options
        val symbol: WaypointSymbol
        var originalSymbolName: String? = null

        when (options.symbolMapping) {
            is GPXImportOptions.SymbolMappingStrategy.Autodetect,
            is GPXImportOptions.SymbolMappingStrategy.UseCustomMapping -> {
                symbol = mapGPXSymbol(sym)
            }
            is GPXImportOptions.SymbolMappingStrategy.UseDefaultSymbol -> {
                symbol = options.symbolMapping.symbol
                originalSymbolName = sym
            }
            is GPXImportOptions.SymbolMappingStrategy.PreserveInNotes -> {
                symbol = WaypointSymbol.DOT
                originalSymbolName = sym
            }
        }

        // Parse timestamp or use now
        val createdAt = parseISO8601(time) ?: Instant.now().toString()

        // Combine notes
        val combinedNotes = combineNotes(desc, notes, originalSymbolName, category)

        return Waypoint(
            id = UUID.randomUUID().toString(),
            name = name,
            notes = combinedNotes.ifEmpty { null },
            symbol = symbol,
            latitude = lat,
            longitude = lon,
            createdAt = createdAt
        )
    }

    /**
     * Map GPX `<sym>` value to WaypointSymbol.
     * Uses case-insensitive matching like iOS.
     */
    private fun mapGPXSymbol(sym: String?): WaypointSymbol {
        if (sym.isNullOrEmpty()) return WaypointSymbol.DOT

        return when (sym.lowercase()) {
            "red circle" -> WaypointSymbol.RED_CIRCLE
            "yellow circle" -> WaypointSymbol.YELLOW_CIRCLE
            "blue circle" -> WaypointSymbol.BLUE_CIRCLE
            "green circle" -> WaypointSymbol.GREEN_CIRCLE
            "red flag" -> WaypointSymbol.RED_FLAG
            "yellow flag" -> WaypointSymbol.YELLOW_FLAG
            "blue flag" -> WaypointSymbol.BLUE_FLAG
            "green flag" -> WaypointSymbol.GREEN_FLAG
            "red square" -> WaypointSymbol.RED_SQUARE
            "yellow square" -> WaypointSymbol.YELLOW_SQUARE
            "blue square" -> WaypointSymbol.BLUE_SQUARE
            "green square" -> WaypointSymbol.GREEN_SQUARE
            "red triangle" -> WaypointSymbol.RED_TRIANGLE
            "yellow triangle" -> WaypointSymbol.YELLOW_TRIANGLE
            "blue triangle" -> WaypointSymbol.BLUE_TRIANGLE
            "green triangle" -> WaypointSymbol.GREEN_TRIANGLE
            "dot" -> WaypointSymbol.DOT
            "fishing area" -> WaypointSymbol.FISHING_AREA_1
            else -> WaypointSymbol.DOT
        }
    }

    private fun combineNotes(
        desc: String?,
        notes: String?,
        originalSymbol: String?,
        category: String?
    ): String {
        val parts = mutableListOf<String>()

        if (!desc.isNullOrEmpty()) parts.add(desc)
        if (!notes.isNullOrEmpty()) parts.add(notes)

        val needsMetadata = originalSymbol != null
        if (needsMetadata) {
            val metadata = buildList {
                originalSymbol?.let { add("Symbol: $it") }
                category?.let { add("Category: $it") }
            }.joinToString("\n")

            if (metadata.isNotEmpty()) {
                if (parts.isNotEmpty()) {
                    parts.add("--- Original Metadata ---\n$metadata")
                } else {
                    parts.add(metadata)
                }
            }
        }

        return parts.joinToString("\n\n")
    }

    private fun parseISO8601(text: String?): String? {
        if (text.isNullOrEmpty()) return null
        return try {
            Instant.parse(text).toString()
        } catch (_: Exception) {
            null
        }
    }

    // -- Deduplication --

    private fun filterOutDuplicates(
        newWaypoints: List<Waypoint>,
        existing: List<Waypoint>
    ): List<Waypoint> {
        val accepted = existing.toMutableList()
        return newWaypoints.filter { newWp ->
            val isDupe = accepted.any { isDuplicate(it, newWp) }
            if (!isDupe) accepted.add(newWp)
            !isDupe
        }
    }

    private fun isDuplicate(wp1: Waypoint, wp2: Waypoint): Boolean {
        val coordsMatch =
            abs(wp1.latitude - wp2.latitude) < DUPLICATE_COORD_THRESHOLD &&
            abs(wp1.longitude - wp2.longitude) < DUPLICATE_COORD_THRESHOLD

        val namesMatch =
            wp1.name != null && wp2.name != null &&
            wp1.name.equals(wp2.name, ignoreCase = true)

        return coordsMatch && (namesMatch || wp1.name == null || wp2.name == null)
    }
}
