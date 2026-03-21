# Phase 10: Stations & Weather

> **iOS is the source of truth.**

## Requirement
Display NDBC buoy stations on map with real-time observations, 5-day wind/wave forecasts, and support long-press-to-fetch weather at any coordinate.

## UX Description
- **Station markers**: NDBC stations loaded from bundled JSON. Appear as map annotations. Tap opens station detail sheet.
- **Station detail**: Shows current observations (wind speed/direction/gust, wave height/period/direction, air/water temp, pressure), AI-generated summary, 5-day wind forecast chart, 5-day wave forecast chart, and legacy forecast table toggle.
- **Weather fetch**: Long-press anywhere on map shows confirmation pill at that coordinate. Confirming fetches 7-day weather + wave forecast from API. Ripple animation during load. Opens weather details sheet.
- **Weather details sheet**: Shows current conditions, wind forecast, wave forecast, precipitation, temperature, UV, atmospheric data grouped by day.

## Acceptance Criteria
1. NDBC station annotations render on map from bundled station data
2. Tapping a station opens detail sheet with real-time observations
3. Wind and wave data displays in user's preferred units
4. 5-day wind forecast chart shows 4 bars per day with direction arrows
5. 5-day wave forecast chart shows height and period
6. Long-press on map shows weather confirmation pill at coordinate
7. Confirming fetches weather data with loading animation
8. Weather details sheet shows 7-day forecast organized by day

## iOS Reference Files
- `Features/StationDetails/Types/Station.swift` -- Station model
- `Features/StationDetails/Types/StationObservation.swift` -- Observation data with wind/wave/met
- `Features/StationDetails/Types/StationForecast.swift` -- Forecast values, Temperature, Distance, Speed, Direction
- `Features/StationDetails/StationDetailsViewModel.swift` -- Parallel data loading
- `Features/StationDetails/StationsViewModel.swift` -- Station list management
- `Features/StationDetails/Charts/WaveConditionsChart.swift` -- Wave chart
- `Features/StationDetails/Charts/WindConditionsChart.swift` -- Wind chart
- `Features/Weather/Types/WeatherData.swift` -- WeatherData, WeatherConditions, WindData
- `Features/Weather/Types/WaveData.swift` -- WaveData, WaveConditions
- `Features/Weather/ViewModels/WeatherFetchViewModel.swift` -- Long-press weather flow

## Key Types to Port

```
Station: id (String), name (String), location (Location), type (String),
         hasRealTimeData (Boolean), owner (String)

StationObservation: stationId, name, location, observations
  - observations: time, wind (speed/direction/gust), wave (height/period/direction),
                  met (pressure/airTemp/waterTemp/dewpoint/visibility), dataAge

WeatherData: location, currentConditions, forecast ([WeatherConditions]),
             waveForecast ([WaveConditions])

WeatherConditions: time, wind (WindData), weather (WeatherInfo),
                   precipitation, temperature, uv, atmospheric

WindData: speed (mph), direction (degrees), gust (mph)

WaveData: height (feet), period (seconds), direction (degrees)

WaveConditions: time, wave (WaveData)

StationDetailsViewModel: stationId, observation, weatherData, summary
  - loads observation, weather, summary in parallel
  - windConditionsData/waveConditionsData grouped by day

WeatherFetchViewModel: loadingState, weatherData,
                       confirmationCoordinate, rippleCoordinate
  - showWeatherConfirmation, confirmWeatherFetch, fetchWeatherData
```

## Tasks
1. Port station types (`Station`, `StationObservation` with nested wind/wave/met)
2. Port weather types (`WeatherData`, `WeatherConditions`, `WindData`, `WaveData`, `WaveConditions`)
3. Bundle NDBC stations JSON and load at startup
4. Build station map annotations layer
5. Implement `StationDetailsViewModel` with parallel observation/weather/summary loading
6. Build station detail sheet -- observations, wind chart, wave chart
7. Implement `WeatherFetchViewModel` -- confirmation pill, ripple animation, fetch flow
8. Build weather details sheet with day-grouped forecast
9. Build wind conditions chart (bar chart with direction arrows)
10. Build wave conditions chart (height + period)
