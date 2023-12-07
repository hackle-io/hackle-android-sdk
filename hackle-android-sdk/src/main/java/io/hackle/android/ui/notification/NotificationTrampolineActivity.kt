package io.hackle.android.ui.notification

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import io.hackle.android.internal.database.Database
import io.hackle.android.internal.database.DatabaseHelper
import io.hackle.android.internal.database.repository.NotificationRepository
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

        saveNotificationData(notificationData)
        trampoline(intent.data, notificationExtras, notificationData)
        finish()
    }

    private fun saveNotificationData(data: NotificationData) {
        try {
            val sharedDatabase = DatabaseHelper.getSharedDatabase(this)
            val repository = NotificationRepository(sharedDatabase)
            repository.save(data)
            log.debug { "Saved notification data : ${data.messageId}" }
        } catch (e: Exception) {
            log.debug { "Notification data save error" }
        }
    }

    private fun trampoline(uri: Uri?, extras: Bundle, data: NotificationData) {
        log.debug { "Notification click action : ${data.clickAction}" }

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
                    } catch (e: ActivityNotFoundException) {
                        log.debug { "Failed to land anywhere." }
                        startLauncherActivity(extras)
                    }

                    log.debug { "Redirected to : $uri" }
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