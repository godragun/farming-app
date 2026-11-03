package com.project.farmingapp.fieldguard.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "iot_readings",
    indices = [
        Index(value = ["nodeId", "timestampMs"]),
        Index(value = ["timestampMs"])
    ]
)
data class IotReading(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nodeId: String,
    val timestampMs: Long,
    val temperatureC: Double,
    val humidityPct: Double,
    val pirMotion: Boolean,
    val lat: Double,
    val lng: Double,
    val batteryPct: Int,
    val linkType: String,
    val online: Boolean,
    val simulated: Boolean = true
)

/** Latest snapshot per node for the nodes list UI. */
data class IotNodeLatest(
    val nodeId: String,
    val timestampMs: Long,
    val temperatureC: Double,
    val humidityPct: Double,
    val pirMotion: Boolean,
    val batteryPct: Int,
    val linkType: String,
    val online: Boolean,
    val lat: Double,
    val lng: Double
)
