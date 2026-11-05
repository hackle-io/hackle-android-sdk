package io.hackle.android.ui.core

import android.content.Context
import android.net.Uri
import android.os.Bundle
import io.hackle.android.HackleDelegate
import io.hackle.android.HackleUrlChannel
import io.hackle.android.HackleUrlContext
import io.hackle.sdk.core.internal.log.Logger

/**
 * Hands links over to the registered [HackleDelegate].
 */
internal class DelegateUrlHandler(private val delegate: HackleDelegate) : UrlHandler {

    override fun open(context: Context, url: Uri, channel: HackleUrlChannel, extras: Bundle?) {
        try {
            delegate.handleUrl(HackleUrlContext(url, channel))
        } catch (e: Throwable) {
            log.error(e) { "Unexpected exception while handling url by delegate: $url" }
        }
    }

    companion object {
        private val log = Logger<DelegateUrlHandler>()
    }
}
