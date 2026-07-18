package com.project.farmingapp.model.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "field_surveys")
data class FieldSurvey(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    /**
     * Stable, globally-unique ID used for remote sync (Firestore document id).
     * This prevents cross-device overwrites caused by local auto-increment ids.
     */
    val remoteId: String = "",
    val farmerName: String = "",
    val locationText: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val cropType: String = "",
    val soilCondition: String = "",
    val irrigationType: String = "",
    val pestNotes: String = "",
    val estimatedYieldKg: String = "",
    val imagePath: String = "",
    val audioPath: String = "",
    val imageUrl: String = "",
    val audioUrl: String = "",
    val savedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
