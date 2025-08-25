package io.hackle.android.ui.notification

import android.app.ActivityManager
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import io.hackle.android.internal.task.TaskExecutors.runOnBackground
import io.hackle.android.ui.notification.Constants.DEFAULT_NOTIFICATION_CHANNEL_ID
import io.hackle.sdk.core.internal.log.Logger

internal class NotificationBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent != null) {
            runOnBackground {
                try {
                    displayNotification(context, intent)
                } catch (e: Exception) {
                    log.error(e) { "Unexpected error while handle notification intent." }
                }
            }
        }
    }

    private fun displayNotification(context: Context, intent: Intent): Boolean {
        if (!NotificationHandler.isHackleIntent(intent)) {
            log.debug { "Non hackle notification received." }
            return false
        }

        val notificationExtras = intent.extras
        if (notificationExtras == null) {
            log.debug { "No data received." }
            return false
        }

        val notificationData = NotificationData.from(intent)
        if (notificationData == null) {
            log.debug { "Notification data parse error." }
            return false
        }

        log.debug { "Parsed notification data: $notificationData" }

        if (!notificationData.showForeground) {
            if (isAppInForeground(context)) {
                log.debug { "Bypass notification handling because app in foregrounded." }
                return false
            }
        }

        try {
            val notification = NotificationFactory.createNotification(context, notificationExtras, notificationData)
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(TAG, notificationData.notificationId, notification)
        } catch (e: Exception) {
            log.debug { e.message ?: "Handle notification intent error." }
            return false
        }

        return true
    }

    private fun isAppInForeground(context: Context): Boolean {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (keyguardManager.isKeyguardLocked) {
            return false
        }

        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val appProcesses = activityManager.runningAppProcesses ?: return false
        for (appProcess in appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                appProcess.processName == context.packageName) {
                return true
            }
        }

        return false
    }

    companion object {

        private const val TAG = "hackle_notification"
        private val log = Logger<NotificationBroadcastReceiver>()
    }
}