package io.hackle.android.ui.notification

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import io.hackle.android.HackleApp
import io.hackle.android.HackleUrlChannel
import io.hackle.android.ui.core.LinkOpener
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
                LinkOpener.startLauncherActivity(this, extras)
            }
            NotificationClickAction.DEEP_LINK -> {
                if (uri == null) {
                    log.debug { "Landing url is empty." }
                    LinkOpener.startLauncherActivity(this, extras)
                } else {
                    dispatchLink(uri, extras)
                }
            }
        }
    }

    /**
     * The trampoline runs in the app process, so Application.onCreate has already completed.
     * The instance is missing only when the app initializes HackleApp later (e.g. in an Activity).
     */
    private fun dispatchLink(uri: Uri, extras: Bundle) {
        val app = HackleApp.currentInstance()
        if (app == null) {
            log.debug { "HackleApp is not initialized. Pending landing url until initialized: $uri" }
            PendingNotificationLinkStorage.store(PendingNotificationLink(uri, extras))
            LinkOpener.startLauncherActivity(this, extras)
            return
        }
        app.urlDispatcher.open(this, uri, HackleUrlChannel.PUSH, extras)
    }

    companion object {
        private val log = Logger<NotificationTrampolineActivity>()
    }
}
