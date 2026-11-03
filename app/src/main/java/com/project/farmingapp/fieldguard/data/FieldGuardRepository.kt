package com.project.farmingapp.fieldguard.data

import android.content.Context
import com.project.farmingapp.fieldguard.iot.DemoNodes
import com.project.farmingapp.fieldguard.iot.IotDemoPattern
import com.project.farmingapp.fieldguard.iot.NodeSimulator

class FieldGuardRepository(context: Context) {
    private val db = FieldGuardDatabase.getInstance(context)
    private val dao = db.dao()
    private val iotDao = db.iotDao()
    private val simulator = NodeSimulator()

    @Volatile
    private var activePattern: IotDemoPattern = IotDemoPattern.NORMAL

    fun observeIncidents() = dao.observeIncidents()
    fun observeQueued() = dao.observeQueuedAlerts()
    fun observeLatestIot() = iotDao.observeLatestPerNode()

    fun currentPattern(): IotDemoPattern = activePattern

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

    /**
     * Ensures 7 days of 15-minute simulated readings exist for all demo nodes.
     */
    suspend fun ensureIotHistory(
        pattern: IotDemoPattern = activePattern,
        forceRegenerate: Boolean = false
    ) {
        activePattern = pattern
        val count = iotDao.count()
        if (!forceRegenerate && count > 0) {
            pruneIotRetention()
            return
        }
        iotDao.clearAll()
        val offlineId = DemoNodes.ALL.first().nodeId
        val batch = simulator.generateHistory(
            pattern = pattern,
            offlineNodeId = offlineId
        )
        // Insert in chunks to avoid giant transactions
        batch.chunked(500).forEach { iotDao.insertAll(it) }
        pruneIotRetention()
    }

    suspend fun applyDemoPattern(pattern: IotDemoPattern) {
        ensureIotHistory(pattern = pattern, forceRegenerate = true)
    }

    suspend fun latestIotPerNode(): List<IotReading> = iotDao.latestPerNode()

    suspend fun readingsForNode(nodeId: String, hours: Int): List<IotReading> {
        val from = System.currentTimeMillis() - hours * NodeSimulator.HOUR_MS
        return iotDao.readingsForNodeSince(nodeId, from)
    }

    private suspend fun pruneIotRetention() {
        val cutoff = System.currentTimeMillis() - NodeSimulator.RETENTION_DAYS * NodeSimulator.DAY_MS
        iotDao.deleteOlderThan(cutoff)
    }
}
