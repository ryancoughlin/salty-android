package com.example.saltyoffshore.data.weather

import java.time.LocalDate

/**
 * Groups forecast data by day for day-selector UI.
 * Matches iOS DayOverview.
 */
data class DayOverview(
    val date: LocalDate,
    val windForecasts: List<WeatherConditions>,
    val waveForecasts: List<WaveConditions>
)
