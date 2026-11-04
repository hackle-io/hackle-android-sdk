package io.hackle.android.ui.notification

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import io.hackle.android.HackleApp
import io.hackle.android.HackleUrlChannel
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
                } else if (handleByHackle(uri, extras)) {
                    log.debug { "Landing url is handled by Hackle: $uri" }
                } else {
                    NotificationLinkOpener.openLink(this, uri, extras)
                }
            }
        }
    }

    private fun handleByHackle(uri: Uri, extras: Bundle): Boolean {
        val app = HackleApp.currentInstance()
        if (app == null) {
            log.debug { "HackleApp is not initialized. Pending landing url until initialized: $uri" }
            PendingNotificationLinkStorage.store(PendingNotificationLink(uri, extras))
            startLauncherActivity(extras, flags = FOREGROUND_INTENT_FLAGS)
            return true
        }
        val urlDispatcher = app.urlDispatcher
        val delegate = urlDispatcher.currentDelegate() ?: return false
        startLauncherActivity(extras, flags = FOREGROUND_INTENT_FLAGS)
        urlDispatcher.dispatch(delegate, uri, HackleUrlChannel.PUSH)
        return true
    }

    private fun startLauncherActivity(bundle: Bundle, flags: Int = NOTIFICATION_INTENT_FLAGS) {
        NotificationLinkOpener.startLauncherActivity(this, bundle, flags = flags)
    }

    companion object {

        private val log = Logger<NotificationTrampolineActivity>()

        internal val NOTIFICATION_INTENT_FLAGS =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP

        /**
         * Brings the existing task to the foreground as it is, like tapping the app icon.
         * Used when the link is handled after launching the app, so the back stack is preserved.
         */
        internal val FOREGROUND_INTENT_FLAGS =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
    }
}