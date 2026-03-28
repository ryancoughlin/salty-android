package com.example.saltyoffshore.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.saltyoffshore.data.station.StationCurrents
import com.example.saltyoffshore.data.station.StationCurrentsService
import com.example.saltyoffshore.data.station.StationObservation
import com.example.saltyoffshore.data.station.StationObservationService
import com.example.saltyoffshore.data.weather.ForecastDataFilter
import com.example.saltyoffshore.data.weather.WeatherConditions
import com.example.saltyoffshore.data.weather.WeatherData
import com.example.saltyoffshore.data.weather.WeatherService
import com.example.saltyoffshore.data.weather.WeatherSummaryResponse
import com.example.saltyoffshore.data.weather.WaveConditions
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private const val TAG = "StationDetailViewModel"

/**
 * Per-station data cache following iOS StationDataStore pattern.
 * Manages observation, currents, weather, and summary data per station.
 */
class StationDetailViewModel : ViewModel() {

    // Per-station cache
    private val observations = mutableStateMapOf<String, StationObservation>()
    private val currentsCache = mutableStateMapOf<String, StationCurrents>()
    private val weatherCache = mutableStateMapOf<String, WeatherData>()
    private val summaries = mutableStateMapOf<String, WeatherSummaryResponse>()

    // Per-component loading state
    private val observationLoading = mutableSetOf<String>()
    private val currentsLoading = mutableSetOf<String>()
    private val weatherLoading = mutableSetOf<String>()
    private val summaryLoading = mutableSetOf<String>()

    var isLoading by mutableStateOf(false)
        private set

    // MARK: - Accessors

    fun observation(stationId: String): StationObservation? = observations[stationId]
    fun currents(stationId: String): StationCurrents? = currentsCache[stationId]
    fun weather(stationId: String): WeatherData? = weatherCache[stationId]
    fun summary(stationId: String): WeatherSummaryResponse? = summaries[stationId]

    fun summaryText(stationId: String): String =
        summaries[stationId]?.summary ?: "Loading station summary..."

    fun isObservationLoading(stationId: String): Boolean =
        observationLoading.contains(stationId)

    fun isWeatherLoading(stationId: String): Boolean =
        weatherLoading.contains(stationId)

    // MARK: - Derived State

    fun truncatedForecasts(stationId: String): List<WeatherConditions> {
        val weatherData = weatherCache[stationId] ?: return emptyList()
        return ForecastDataFilter.fiveDayWindForecasts(weatherData.forecast)
    }

    fun windConditionsData(stationId: String): List<Pair<LocalDate, List<WeatherConditions>>> {
        val weatherData = weatherCache[stationId] ?: return emptyList()
        val filtered = ForecastDataFilter.fiveDayWindForecasts(weatherData.forecast)
        return filtered
            .groupBy { it.time.atZone(ZoneId.systemDefault()).toLocalDate() }
            .toSortedMap()
            .map { (day, forecasts) -> day to forecasts.sortedBy { it.time } }
    }

    fun waveConditionsData(stationId: String): List<Pair<LocalDate, List<WaveConditions>>> {
        val weatherData = weatherCache[stationId] ?: return emptyList()
        val filtered = ForecastDataFilter.fiveDayWaveForecasts(weatherData.waveForecast)
        return filtered
            .groupBy { it.time.atZone(ZoneId.systemDefault()).toLocalDate() }
            .toSortedMap()
            .map { (day, forecasts) -> day to forecasts.sortedBy { it.time } }
    }

    // MARK: - Loading

    /** Load all data for a station. Safe to call multiple times — skips if already loading. */
    fun loadData(stationId: String) {
        viewModelScope.launch {
            // Phase 1: observation + currents in parallel
            coroutineScope {
                launch { loadObservation(stationId) }
                launch { loadCurrents(stationId) }
            }
            // Phase 2: weather + summary (both need observation's coordinate)
            coroutineScope {
                launch { loadWeather(stationId) }
                launch { loadSummary(stationId) }
            }
        }
    }

    fun refresh(stationId: String) {
        observations.remove(stationId)
        currentsCache.remove(stationId)
        weatherCache.remove(stationId)
        summaries.remove(stationId)
        loadData(stationId)
    }

    // MARK: - Private Loaders

    private suspend fun loadObservation(stationId: String) {
        if (observationLoading.contains(stationId)) return
        observationLoading.add(stationId)
        updateLoadingState()
        try {
            observations[stationId] = StationObservationService.fetchObservation(stationId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load observation for $stationId", e)
        } finally {
            observationLoading.remove(stationId)
            updateLoadingState()
        }
    }

    private suspend fun loadCurrents(stationId: String) {
        if (currentsLoading.contains(stationId)) return
        currentsLoading.add(stationId)
        try {
            val result = StationCurrentsService.fetchCurrents(stationId)
            if (result != null) currentsCache[stationId] = result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load currents for $stationId", e)
        } finally {
            currentsLoading.remove(stationId)
        }
    }

    private suspend fun loadWeather(stationId: String) {
        if (weatherLoading.contains(stationId)) return
        val obs = observations[stationId]
        if (obs == null) {
            Log.w(TAG, "loadWeather: No observation for $stationId — skipping weather fetch")
            return
        }
        weatherLoading.add(stationId)
        updateLoadingState()
        try {
            val lat = obs.location.latitude
            val lon = obs.location.longitude
            Log.d(TAG, "loadWeather: Fetching weather for $stationId at ($lat, $lon)")
            val data = WeatherService.fetchWeatherData(lat, lon)
            Log.d(TAG, "loadWeather: Got ${data.forecast.size} wind forecasts, ${data.waveForecast.size} wave forecasts")
            val overviews = data.dayOverviews
            Log.d(TAG, "loadWeather: dayOverviews count=${overviews.size}, days=${overviews.map { it.date }}")
            overviews.forEach { o ->
                Log.d(TAG, "  Day ${o.date}: wind=${o.windForecasts.size}, wave=${o.waveForecasts.size}")
            }
            weatherCache[stationId] = data
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load weather for $stationId", e)
        } finally {
            weatherLoading.remove(stationId)
            updateLoadingState()
        }
    }

    private suspend fun loadSummary(stationId: String) {
        if (summaryLoading.contains(stationId)) return
        val obs = observations[stationId] ?: return
        summaryLoading.add(stationId)
        try {
            val lat = obs.location.latitude
            val lon = obs.location.longitude
            summaries[stationId] = WeatherService.fetchWeatherSummary(lat, lon)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load summary for $stationId", e)
        } finally {
            summaryLoading.remove(stationId)
        }
    }

    private fun updateLoadingState() {
        isLoading = observationLoading.isNotEmpty() || weatherLoading.isNotEmpty()
    }
}
