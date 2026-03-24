package com.example.saltyoffshore.data.weather

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Filters forecast data to 5-day windows.
 * Matches iOS ForecastDataFilter.
 */
object ForecastDataFilter {

    fun fiveDayWindForecasts(forecasts: List<WeatherConditions>): List<WeatherConditions> {
        val range = fiveDayRange()
        return forecasts.filter { it.time >= range.first && it.time <= range.second }
    }

    fun fiveDayWaveForecasts(forecasts: List<WaveConditions>): List<WaveConditions> {
        val range = fiveDayRange()
        return forecasts.filter { it.time >= range.first && it.time <= range.second }
    }

    fun fiveDayArray(): List<LocalDate> {
        val today = LocalDate.now()
        return (0L..4L).map { today.plusDays(it) }
    }

    private fun fiveDayRange(): Pair<Instant, Instant> {
        val now = Instant.now()
        val end = now.plus(5, ChronoUnit.DAYS)
        return now to end
    }
}
