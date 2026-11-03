package com.project.farmingapp.fieldguard.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface FieldGuardDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncident(incident: FieldGuardIncident): Long

    @Query("SELECT * FROM fieldguard_incidents ORDER BY createdAt DESC")
    fun observeIncidents(): LiveData<List<FieldGuardIncident>>

    @Query("SELECT * FROM fieldguard_incidents ORDER BY createdAt DESC")
    suspend fun listIncidents(): List<FieldGuardIncident>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueuedAlert(alert: FieldGuardQueuedAlert): Long

    @Update
    suspend fun updateQueuedAlert(alert: FieldGuardQueuedAlert)

    @Query("SELECT * FROM fieldguard_queued_alerts WHERE delivered = 0 ORDER BY createdAt ASC")
    suspend fun pendingQueuedAlerts(): List<FieldGuardQueuedAlert>

    @Query("SELECT * FROM fieldguard_queued_alerts ORDER BY createdAt DESC")
    fun observeQueuedAlerts(): LiveData<List<FieldGuardQueuedAlert>>
}
