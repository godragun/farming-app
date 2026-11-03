package com.project.farmingapp.fieldguard.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.project.farmingapp.fieldguard.agent.FieldGuardNotifier
import com.project.farmingapp.fieldguard.agent.SafetyRules
import com.project.farmingapp.fieldguard.data.FieldGuardRepository
import java.util.concurrent.TimeUnit

class QueuedAlertWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val repo = FieldGuardRepository(applicationContext)
        val pending = repo.pendingAlerts()
        if (pending.isEmpty()) return Result.success()
        var anyFailure = false
        pending.forEach { alert ->
            val message = if (SafetyRules.isWildlifeAlertType(alert.level) ||
                alert.message.contains("elephant", ignoreCase = true)
            ) {
                SafetyRules.elephantAlertMessage(alert.message)
            } else {
                alert.message
            }
            val result = FieldGuardNotifier.send(applicationContext, alert.level, message, forceFail = false)
            if (result == "delivered") {
                repo.markDelivered(alert)
            } else {
                repo.markAttemptFailed(alert, result)
                anyFailure = true
            }
        }
        return if (anyFailure) Result.retry() else Result.success()
    }

    companion object {
        private const val UNIQUE = "fieldguard_queued_alerts"

        fun enqueue(context: Context, delaySeconds: Long = 5) {
            val request = OneTimeWorkRequestBuilder<QueuedAlertWorker>()
                .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniqueWork(UNIQUE, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
