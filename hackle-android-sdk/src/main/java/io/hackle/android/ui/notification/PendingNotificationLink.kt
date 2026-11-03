package io.hackle.android.ui.notification

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import io.hackle.android.HackleUrlChannel
import io.hackle.android.ui.core.HackleUrlDispatcher
import io.hackle.sdk.core.internal.log.Logger

internal class PendingNotificationLink(
    val uri: Uri,
    val extras: Bundle,
)

/**
 * Holds the push landing url tapped before HackleApp is initialized.
 * Only the most recent link is kept.
 */
internal object PendingNotificationLinkStorage {

    private val log = Logger<PendingNotificationLinkStorage>()

    private var pendingLink: PendingNotificationLink? = null

    @Synchronized
    fun store(link: PendingNotificationLink) {
        pendingLink = link
    }

    @Synchronized
    fun drain(): PendingNotificationLink? {
        val link = pendingLink
        pendingLink = null
        return link
    }

    /**
     * Opens the pending link, if any, through the [HackleUrlDispatcher].
     * Must be called on the main thread.
     */
    fun dispatch(context: Context, urlDispatcher: HackleUrlDispatcher) {
        val link = drain() ?: return
        log.debug { "Dispatch pending landing url: ${link.uri}" }
        if (urlDispatcher.dispatch(link.uri, HackleUrlChannel.PUSH)) {
            return
        }
        val intent = Intent(Intent.ACTION_VIEW, link.uri)
        intent.putExtras(link.extras)
        intent.addFlags(NotificationTrampolineActivity.NOTIFICATION_INTENT_FLAGS)
        try {
            context.startActivity(intent)
            log.debug { "Redirected to: ${link.uri}" }
        } catch (e: ActivityNotFoundException) {
            log.debug { "Failed to land anywhere: ${link.uri}" }
        }
    }
}
