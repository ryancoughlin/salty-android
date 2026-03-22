package com.example.saltyoffshore.data.measurement

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mapbox.geojson.Point

class MeasurementState {

    var isActive by mutableStateOf(false)
        private set

    var activeMeasurement by mutableStateOf<MapMeasurement?>(null)
        private set

    var completedMeasurements by mutableStateOf<List<MapMeasurement>>(emptyList())
        private set

    val allMeasurements: List<MapMeasurement>
        get() = buildList {
            addAll(completedMeasurements)
            activeMeasurement?.let { add(it) }
        }

    val hasMeasurements: Boolean
        get() = activeMeasurement != null || completedMeasurements.isNotEmpty()

    val canUndo: Boolean
        get() = activeMeasurement?.points?.isNotEmpty() == true

    val totalDistanceMeters: Double
        get() = allMeasurements.sumOf { it.totalDistanceMeters }

    fun enter() {
        isActive = true
        if (activeMeasurement == null) {
            activeMeasurement = MapMeasurement()
        }
    }

    fun exit() {
        finishMeasurement()
        isActive = false
    }

    fun addPoint(point: Point) {
        val current = activeMeasurement ?: MapMeasurement()
        val newPoint = MeasurementPoint(coordinate = point)
        activeMeasurement = current.copy(points = current.points + newPoint)
    }

    fun undoLastPoint() {
        val current = activeMeasurement ?: return
        if (current.points.isEmpty()) return
        activeMeasurement = current.copy(points = current.points.dropLast(1))
    }

    fun finishMeasurement() {
        val current = activeMeasurement ?: return
        if (current.hasSegments) {
            completedMeasurements = completedMeasurements + current
        }
        activeMeasurement = null
    }

    fun clearAll() {
        activeMeasurement = null
        completedMeasurements = emptyList()
    }

    fun startNewMeasurement() {
        finishMeasurement()
        activeMeasurement = MapMeasurement()
    }
}
