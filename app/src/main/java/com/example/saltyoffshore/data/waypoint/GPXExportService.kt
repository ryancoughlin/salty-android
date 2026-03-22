package com.example.saltyoffshore.data.waypoint

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant

/**
 * Service for exporting waypoints to GPX format.
 * Ported from iOS GPXExportService.swift.
 */
object GPXExportService {

    /**
     * Generate GPX XML string from waypoints.
     * Used for sharing via Intent.ACTION_SEND.
     */
    fun exportToGPX(waypoints: List<Waypoint>): String {
        val currentDate = Instant.now().toString()

        return buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine("""<gpx version="1.1" creator="Salty Offshore" xmlns="http://www.topografix.com/GPX/1/1">""")
            appendLine("  <metadata>")
            appendLine("    <name>Salty Offshore Waypoints</name>")
            appendLine("    <desc>Exported waypoints from Salty Offshore</desc>")
            appendLine("    <time>$currentDate</time>")
            appendLine("  </metadata>")

            waypoints.forEach { wp ->
                appendLine("  <wpt lat=\"${wp.latitude}\" lon=\"${wp.longitude}\">")
                appendLine("    <name>${escapeXml(wp.name ?: "")}</name>")
                appendLine("    <desc>${escapeXml(wp.notes ?: "")}</desc>")
                appendLine("    <time>${wp.createdAt}</time>")
                appendLine("    <sym>${escapeXml(wp.symbol.rawValue)}</sym>")
                appendLine("  </wpt>")
            }

            appendLine("</gpx>")
        }
    }

    /** Export a single waypoint to GPX XML. */
    fun exportToGPX(waypoint: Waypoint): String = exportToGPX(listOf(waypoint))

    /**
     * Write GPX to a temp file and return a content:// URI for sharing.
     * Requires FileProvider configured in the manifest.
     */
    suspend fun exportToFile(
        context: Context,
        waypoints: List<Waypoint>,
        fileName: String = "salty_waypoints.gpx"
    ): Uri = withContext(Dispatchers.IO) {
        val gpxContent = exportToGPX(waypoints)
        val file = File(context.cacheDir, fileName)
        file.writeText(gpxContent)
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /** Generate a safe filename for export. */
    fun exportFilename(waypoints: List<Waypoint>): String {
        val baseName = when (waypoints.size) {
            0 -> "Empty"
            1 -> waypoints[0].name ?: "Waypoint"
            else -> "Waypoints"
        }
        val safeName = baseName
            .replace(Regex("[^A-Za-z0-9_\\- ]"), "_")
            .take(30)
        return "$safeName.gpx"
    }

    private fun escapeXml(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
