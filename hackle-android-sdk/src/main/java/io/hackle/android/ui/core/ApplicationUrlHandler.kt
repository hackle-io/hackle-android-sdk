package io.hackle.android.ui.core

import android.content.Context
import android.net.Uri
import android.os.Bundle
import io.hackle.android.HackleUrlChannel

/**
 * Opens links by the SDK itself. Used when no [io.hackle.android.HackleDelegate] is registered.
 */
internal class ApplicationUrlHandler : UrlHandler {

    override fun open(context: Context, url: Uri, channel: HackleUrlChannel, extras: Bundle?) {
        when (channel) {
            HackleUrlChannel.PUSH -> LinkOpener.openLink(context, url, extras)
            HackleUrlChannel.IN_APP_MESSAGE -> LinkOpener.openInAppMessageLink(context, url)
        }
    }
}
