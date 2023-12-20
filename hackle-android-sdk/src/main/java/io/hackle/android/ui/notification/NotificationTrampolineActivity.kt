package io.hackle.android.ui.notification

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import io.hackle.sdk.core.internal.log.Logger

internal class NotificationTrampolineActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        log.debug { "Notification trampoline activity open." }

        val notificationExtras = intent?.extras
        if (notificationExtras == null) {
            log.debug { "Notification trampoline activity received nothing." }
            finish()
            return
        }

        val notificationData = NotificationData.from(intent)
        if (notificationData == null) {
            log.debug { "Notification data parse error." }
            finish()
            return
        }

        NotificationHandler.getInstance(this).handleNotificationData(notificationData)
        trampoline(intent.data, notificationExtras, notificationData)
        finish()
    }

    private fun trampoline(uri: Uri?, extras: Bundle, data: NotificationData) {
        log.debug { "Notification click action: ${data.clickAction}" }

        when (data.clickAction) {
            NotificationClickAction.APP_OPEN -> {
                startLauncherActivity(extras)
            }
            NotificationClickAction.DEEP_LINK -> {
                if (uri == null) {
                    log.debug { "Landing url is empty." }
                    startLauncherActivity(extras)
                } else {
                    val trampolineIntent = Intent(Intent.ACTION_VIEW, uri)
                    trampolineIntent.putExtras(extras)
                    trampolineIntent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
                    try {
                        startActivity(trampolineIntent)
                        log.debug { "Redirected to: $uri" }
                    } catch (e: ActivityNotFoundException) {
                        log.debug { "Failed to land anywhere." }
                        startLauncherActivity(extras)
                    }
                }
            }
        }
    }

    private fun startLauncherActivity(bundle: Bundle) {
        val launcherIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launcherIntent == null) {
            log.debug { "Cannot find launcher activity." }
            return
        }
        launcherIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        launcherIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        launcherIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        launcherIntent.putExtras(bundle)
        startActivity(launcherIntent)

        log.debug { "Started launcher activity." }
    }

    companion object {

        private val log = Logger<NotificationTrampolineActivity>()
    }
}