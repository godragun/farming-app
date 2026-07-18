package com.project.farmingapp.model.data

/**
 * Matches OpenWeatherMap "main" object (forecast API). Numeric fields must use numeric types
 * so Gson can deserialize JSON numbers; using [String] here previously broke parsing and
 * prevented the dashboard weather card from updating.
 */
data class WeatherMain(
    val temp: Float,
    val humidity: Int,
    val temp_min: Float,
    val temp_max: Float
)
