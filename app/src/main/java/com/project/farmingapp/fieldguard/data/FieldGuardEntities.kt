package com.project.farmingapp.fieldguard.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class IncidentStatus {
    ALERTED,
    UNCERTAIN,
    NO_ACTION,
    QUEUED,
    DELIVERED
}

@Entity(tableName = "fieldguard_incidents")
data class FieldGuardIncident(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val evidenceSummary: String,
    val modelConfidenceEstimate: Double,
    val severity: String,
    val status: String,
    val scenarioId: String = "",
    val decisionSummary: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "fieldguard_queued_alerts")
data class FieldGuardQueuedAlert(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val message: String,
    val level: String = "wildlife",
    val createdAt: Long = System.currentTimeMillis(),
    val delivered: Boolean = false,
    val lastError: String = "",
    val attempts: Int = 0
)
