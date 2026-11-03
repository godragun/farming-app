package com.project.farmingapp.fieldguard.data

import android.content.Context

class FieldGuardRepository(context: Context) {
    private val dao = FieldGuardDatabase.getInstance(context).dao()

    fun observeIncidents() = dao.observeIncidents()
    fun observeQueued() = dao.observeQueuedAlerts()

    suspend fun recordIncident(incident: FieldGuardIncident): Long = dao.insertIncident(incident)

    suspend fun queueAlert(message: String, level: String = "wildlife"): Long {
        return dao.insertQueuedAlert(
            FieldGuardQueuedAlert(message = message, level = level)
        )
    }

    suspend fun pendingAlerts() = dao.pendingQueuedAlerts()

    suspend fun markDelivered(alert: FieldGuardQueuedAlert) {
        dao.updateQueuedAlert(alert.copy(delivered = true, lastError = ""))
    }

    suspend fun markAttemptFailed(alert: FieldGuardQueuedAlert, error: String) {
        dao.updateQueuedAlert(
            alert.copy(attempts = alert.attempts + 1, lastError = error.take(300))
        )
    }
}
