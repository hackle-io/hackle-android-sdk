package io.hackle.android.ui.core

import android.content.Context
import android.net.Uri
import android.os.Bundle
import io.hackle.android.HackleDelegate
import io.hackle.android.HackleUrlChannel
import io.hackle.sdk.core.internal.log.Logger

/**
 * Dispatches links from push notifications and in-app messages to the current [UrlHandler].
 *
 * Owns the default handler and swaps the current handler
 * according to the registered [HackleDelegate].
 */
internal class HackleUrlDispatcher(private val defaultHandler: UrlHandler) {

    @Volatile
    private var urlHandler: UrlHandler = defaultHandler

    /**
     * Routes links to the [delegate], or back to the default handler when null.
     */
    fun setDelegate(delegate: HackleDelegate?) {
        urlHandler = delegate?.let(::DelegateUrlHandler) ?: defaultHandler
    }

    fun open(context: Context, url: Uri, channel: HackleUrlChannel, extras: Bundle? = null) {
        val handler = urlHandler
        log.debug { "Dispatch url: $url via ${handler.javaClass.simpleName} [channel=$channel]" }
        handler.open(context, url, channel, extras)
    }

    companion object {
        private val log = Logger<HackleUrlDispatcher>()
    }
}
