package com.example.saltyoffshore.ui.map.globallayers

import android.graphics.Color
import com.example.saltyoffshore.data.ExclusionZone
import com.example.saltyoffshore.data.Tournament
import com.mapbox.bindgen.Value
import com.mapbox.geojson.Feature
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.style.expressions.dsl.generated.concat
import com.mapbox.maps.extension.style.expressions.dsl.generated.get
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.maps.extension.style.expressions.dsl.generated.numberFormat
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.fillLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.layers.properties.generated.SymbolPlacement
import com.mapbox.maps.extension.style.layers.properties.generated.TextRotationAlignment
import com.mapbox.maps.extension.style.layers.properties.generated.TextTransform
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Tournament boundary layer with dashed circle and exclusion zones.
 * Matches iOS TournamentsLayer.
 */
class TournamentsLayer(
    private val mapboxMap: MapboxMap,
    private val tournament: Tournament,
    private var opacity: Double = 1.0
) {
    private val baseSourceId = "tournament-${tournament.id}"
    private val lineSourceId = "tournament-${tournament.id}-line"
    private val labelSourceId = "tournament-${tournament.id}-label"
    private val baseLayerId = "tournament-boundary-${tournament.id}-base"
    private val dashLayerId = "tournament-boundary-${tournament.id}-dash"
    private val labelLayerId = "tournament-label-${tournament.id}"

    fun addToMap() {
        val style = mapboxMap.style ?: return
        val center = tournament.boundaryCenter ?: return
        val radius = tournament.boundaryRadius ?: return

        // Create circle geometry
        val circleCoordinates = createCircleCoordinates(center, radius)
        val circleLine = LineString.fromLngLats(circleCoordinates)
        val circlePolygon = Polygon.fromLngLats(listOf(circleCoordinates))

        // Add polygon source (for fill if needed)
        if (!style.styleSourceExists(baseSourceId)) {
            val polygonFeature = Feature.fromGeometry(circlePolygon).apply {
                addStringProperty("name", tournament.name)
                addNumberProperty("radius", tournament.boundaryRadius ?: 0.0)
                addNumberProperty("year", tournament.authorityYear.toDouble())
            }
            style.addSource(
                geoJsonSource(baseSourceId) {
                    feature(polygonFeature)
                }
            )
        }

        // Add line source for boundary
        if (!style.styleSourceExists(lineSourceId)) {
            style.addSource(
                geoJsonSource(lineSourceId) {
                    feature(Feature.fromGeometry(circleLine))
                }
            )
        }

        // Add label source
        if (!style.styleSourceExists(labelSourceId)) {
            val labelFeature = createLabelFeature(circleLine)
            style.addSource(
                geoJsonSource(labelSourceId) {
                    feature(labelFeature)
                }
            )
        }

        // Base line layer (black)
        if (!style.styleLayerExists(baseLayerId)) {
            style.addLayer(
                lineLayer(baseLayerId, lineSourceId) {
                    lineColor(Color.BLACK)
                    lineWidth(4.0)
                    lineCap(LineCap.ROUND)
                    lineJoin(LineJoin.ROUND)
                    lineOpacity(opacity)
                    slot("top")
                }
            )
        }

        // Dash line layer (white dashes)
        if (!style.styleLayerExists(dashLayerId)) {
            style.addLayer(
                lineLayer(dashLayerId, lineSourceId) {
                    lineColor(Color.WHITE)
                    lineWidth(4.0)
                    lineDasharray(listOf(1.6, 1.6))
                    lineOpacity(opacity)
                    slot("top")
                }
            )
        }

        // Arc label
        if (!style.styleLayerExists(labelLayerId)) {
            style.addLayer(
                symbolLayer(labelLayerId, labelSourceId) {
                    textField(
                        concat {
                            get { literal("name") }
                            literal(" – ")
                            numberFormat(Expression.get("distance")) {
                                minFractionDigits(0)
                                maxFractionDigits(0)
                            }
                            literal(" ")
                            get { literal("unit") }
                            literal(" Boundary")
                        }
                    )
                    textSize(14.0)
                    textFont(listOf("Roboto Condensed Bold"))
                    textColor(Color.WHITE)
                    textTransform(TextTransform.UPPERCASE)
                    textHaloColor(Color.BLACK)
                    textHaloWidth(1.5)
                    textOpacity(opacity)
                    symbolPlacement(SymbolPlacement.LINE)
                    symbolSpacing(100.0)
                    textRotationAlignment(TextRotationAlignment.MAP)
                    textIgnorePlacement(true)
                    textAllowOverlap(true)
                }
            )
        }

        // Add exclusion zones
        tournament.exclusionZones?.forEachIndexed { index, zone ->
            addExclusionZone(zone, index)
        }
    }

    private fun createCircleCoordinates(center: Point, radius: Double): List<Point> {
        val vertices = 64
        val coordinates = mutableListOf<Point>()

        for (i in 0..vertices) {
            val angle = (i.toDouble() / vertices) * 2 * PI
            val lat = center.latitude() + (radius / 111320.0) * cos(angle)
            val lon = center.longitude() + (radius / (111320.0 * cos(center.latitude() * PI / 180.0))) * sin(angle)
            coordinates.add(Point.fromLngLat(lon, lat))
        }

        return coordinates
    }

    private fun createLabelFeature(circleLine: LineString): Feature {
        val distanceValue = tournament.boundaryDistance.toInt()
        val unitDisplay = formatUnitForDisplay(tournament.boundaryUnit)

        return Feature.fromGeometry(circleLine).apply {
            addStringProperty("name", tournament.shortName)
            addNumberProperty("distance", distanceValue.toDouble())
            addStringProperty("unit", unitDisplay)
        }
    }

    private fun formatUnitForDisplay(unit: String): String {
        return when (unit.lowercase()) {
            "nm", "nmi" -> "NM"
            "mi", "miles" -> "MI"
            "km" -> "KM"
            else -> unit.uppercase()
        }
    }

    private fun addExclusionZone(zone: ExclusionZone, index: Int) {
        val style = mapboxMap.style ?: return
        val zoneName = zone.name.replace(" ", "-")
        val zoneSourceId = "exclusion-$zoneName-$index"
        val fillLayerId = "$zoneSourceId-fill"
        val lineLayerId = "$zoneSourceId-line"
        val labelSourceId = "$zoneSourceId-label"
        val labelLayerId = "$zoneSourceId-label-layer"

        // Zone polygon
        val polygon = Polygon.fromLngLats(listOf(zone.coordinates))

        // Add fill source and layer
        if (!style.styleSourceExists(zoneSourceId)) {
            style.addSource(
                geoJsonSource(zoneSourceId) {
                    feature(Feature.fromGeometry(polygon))
                }
            )
        }

        // Red fill
        if (!style.styleLayerExists(fillLayerId)) {
            style.addLayer(
                fillLayer(fillLayerId, zoneSourceId) {
                    fillColor(Color.RED)
                    fillOpacity(0.3 * opacity)
                    slot("top")
                }
            )
        }

        // Red outline
        if (!style.styleLayerExists(lineLayerId)) {
            style.addLayer(
                lineLayer(lineLayerId, zoneSourceId) {
                    lineColor(Color.RED)
                    lineWidth(3.0)
                    lineOpacity(opacity)
                    slot("top")
                }
            )
        }

        // Label source
        if (!style.styleSourceExists(labelSourceId)) {
            val labelFeature = Feature.fromGeometry(com.mapbox.geojson.Point.fromLngLat(
                zone.center.longitude(),
                zone.center.latitude()
            )).apply {
                addStringProperty("name", "${zone.name} – No Fishing")
            }
            style.addSource(
                geoJsonSource(labelSourceId) {
                    feature(labelFeature)
                }
            )
        }

        // Label layer
        if (!style.styleLayerExists(labelLayerId)) {
            style.addLayer(
                symbolLayer(labelLayerId, labelSourceId) {
                    textField(get { literal("name") })
                    textSize(12.0)
                    textFont(listOf("Roboto Condensed Bold"))
                    textColor(Color.WHITE)
                    textTransform(TextTransform.UPPERCASE)
                    textHaloColor(Color.RED)
                    textHaloWidth(2.0)
                    textOpacity(opacity)
                }
            )
        }
    }

    fun updateOpacity(newOpacity: Double) {
        opacity = newOpacity
        val style = mapboxMap.style ?: return

        style.setStyleLayerProperty(baseLayerId, "line-opacity", Value.valueOf(newOpacity))
        style.setStyleLayerProperty(dashLayerId, "line-opacity", Value.valueOf(newOpacity))
        style.setStyleLayerProperty(labelLayerId, "text-opacity", Value.valueOf(newOpacity))

        // Update exclusion zone opacities
        tournament.exclusionZones?.forEachIndexed { index, zone ->
            val zoneName = zone.name.replace(" ", "-")
            val fillLayerId = "exclusion-$zoneName-$index-fill"
            val lineLayerId = "exclusion-$zoneName-$index-line"
            val labelLayerId = "exclusion-$zoneName-$index-label-layer"

            if (style.styleLayerExists(fillLayerId)) {
                style.setStyleLayerProperty(fillLayerId, "fill-opacity", Value.valueOf(0.3 * newOpacity))
            }
            if (style.styleLayerExists(lineLayerId)) {
                style.setStyleLayerProperty(lineLayerId, "line-opacity", Value.valueOf(newOpacity))
            }
            if (style.styleLayerExists(labelLayerId)) {
                style.setStyleLayerProperty(labelLayerId, "text-opacity", Value.valueOf(newOpacity))
            }
        }
    }

    fun removeFromMap() {
        val style = mapboxMap.style ?: return

        // Remove main layers
        listOf(baseLayerId, dashLayerId, labelLayerId).forEach { layerId ->
            if (style.styleLayerExists(layerId)) {
                style.removeStyleLayer(layerId)
            }
        }

        // Remove main sources
        listOf(baseSourceId, lineSourceId, labelSourceId).forEach { sourceId ->
            if (style.styleSourceExists(sourceId)) {
                style.removeStyleSource(sourceId)
            }
        }

        // Remove exclusion zone layers and sources
        tournament.exclusionZones?.forEachIndexed { index, zone ->
            val zoneName = zone.name.replace(" ", "-")
            val zoneSourceId = "exclusion-$zoneName-$index"
            val labelSourceId = "$zoneSourceId-label"

            listOf("$zoneSourceId-fill", "$zoneSourceId-line", "$zoneSourceId-label-layer").forEach { layerId ->
                if (style.styleLayerExists(layerId)) {
                    style.removeStyleLayer(layerId)
                }
            }

            listOf(zoneSourceId, labelSourceId).forEach { sourceId ->
                if (style.styleSourceExists(sourceId)) {
                    style.removeStyleSource(sourceId)
                }
            }
        }
    }
}
