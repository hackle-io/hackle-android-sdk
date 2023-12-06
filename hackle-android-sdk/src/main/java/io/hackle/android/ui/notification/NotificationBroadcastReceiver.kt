package io.hackle.android.ui.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.hackle.android.internal.task.TaskExecutors.runOnBackground
import io.hackle.sdk.core.internal.log.Logger

internal class NotificationBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent != null) {
            runOnBackground {
                try {
                    NotificationHandler.handleNotificationIntent(context, intent)
                } catch (e: Exception) {
                    log.error(e) { "Unexpected error while handle notification intent." }
                }
            }
        }
    }

    companion object {

        private val log = Logger<NotificationBroadcastReceiver>()
    }
}