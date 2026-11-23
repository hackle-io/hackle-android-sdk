package io.hackle.android.ui.notification

import android.net.Uri
import android.os.Bundle

internal class PendingNotificationLink(
    val uri: Uri,
    val extras: Bundle,
)

/**
 * Holds the push landing url tapped before the HackleApp instance is registered.
 * Only the most recent link is kept. Dispatching is done by HackleApp on initialization.
 */
internal object PendingNotificationLinkStorage {

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
}
