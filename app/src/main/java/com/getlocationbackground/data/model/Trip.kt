package com.stylish.spacex.data.model

data class Trip(
    val end_time: String,
    val locations: ArrayList<Location>,
    val start_time: String,
    val trip_id: Int
)

data class Location(
    val accuracy: Float,
    val latitude: Double,
    val longitide: Double,
    val timestamp: String
)
