package com.project.farmingapp.fieldguard.agent

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.project.farmingapp.R

object FieldGuardNotifier {
    const val CHANNEL_WILDLIFE = "fieldguard_wildlife"
    const val CHANNEL_DEFAULT = "fieldguard_default"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val wildlife = NotificationChannel(
            CHANNEL_WILDLIFE,
            context.getString(R.string.fieldguard_channel_wildlife),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.fieldguard_channel_wildlife_desc)
            enableVibration(true)
        }
        val normal = NotificationChannel(
            CHANNEL_DEFAULT,
            context.getString(R.string.fieldguard_channel_default),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        manager.createNotificationChannel(wildlife)
        manager.createNotificationChannel(normal)
    }

    /**
     * @return "delivered" or "failed: …"
     */
    fun send(context: Context, level: String, message: String, forceFail: Boolean = false): String {
        if (forceFail) return "failed: network unavailable (demo)"
        return try {
            ensureChannels(context)
            val channel = if (level.equals("wildlife", ignoreCase = true)) CHANNEL_WILDLIFE else CHANNEL_DEFAULT
            val priority = if (level.equals("wildlife", ignoreCase = true)) {
                NotificationCompat.PRIORITY_HIGH
            } else {
                NotificationCompat.PRIORITY_DEFAULT
            }
            val notification = NotificationCompat.Builder(context, channel)
                .setSmallIcon(R.drawable.ic_baseline_photo_camera_24)
                .setContentTitle(context.getString(R.string.fieldguard_alert_title))
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(priority)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context)
                .notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
            "delivered"
        } catch (e: SecurityException) {
            "failed: notification permission denied"
        } catch (e: Exception) {
            "failed: ${e.message ?: e.javaClass.simpleName}"
        }
    }
}
