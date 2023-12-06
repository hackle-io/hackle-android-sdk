package io.hackle.android.ui.notification

import android.app.ActivityManager
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import io.hackle.android.ui.notification.Constants.KEY_HACKLE
import io.hackle.android.ui.notification.Constants.KEY_MESSAGE_ID
import io.hackle.android.ui.notification.Constants.KEY_SHOW_FOREGROUND
import io.hackle.sdk.core.internal.log.Logger

internal object NotificationHandler {

    private const val TAG = "hackle_notification"
    private val log = Logger<NotificationHandler>()

    fun isHackleIntent(intent: Intent): Boolean {
        val extras = intent.extras ?: return false
        return extras.getString(KEY_HACKLE) == "true"
    }

    private fun isAppInForeground(context: Context): Boolean {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (keyguardManager.isKeyguardLocked) {
            return false
        }

        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val appProcesses = activityManager.runningAppProcesses ?: return false
        val packageName = context.packageName
        for (appProcess in appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                appProcess.processName == packageName) {
                return true
            }
        }

        return false
    }

    private fun shouldDisplayNotificationInForeground(intent: Intent): Boolean {
        val extras = intent.extras ?: return false
        return extras.getString(KEY_SHOW_FOREGROUND) == "true"
    }

    fun handleNotificationIntent(context: Context, intent: Intent): Boolean {
        if (!isHackleIntent(intent)) {
            log.debug { "Non hackle notification received." }
            return false
        }

        val notificationExtras = intent.extras ?: return false
        log.debug { "Hackle notification received : $notificationExtras" }

        if (!shouldDisplayNotificationInForeground(intent) && isAppInForeground(context)) {
            log.debug { "Bypass hackle notification handling because app in foregrounded." }
            return false
        }

        try {
            val messageId = checkNotNull(notificationExtras.getString(KEY_MESSAGE_ID)) {
                "$KEY_MESSAGE_ID must not be null"
            }
            val notification = NotificationFactory.createNotification(context, notificationExtras)
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(TAG, messageId.hashCode(), notification)
        } catch (e: Exception) {
            log.debug { e.message ?: "Handle notification intent error." }
            return false
        }

        return true
    }
}