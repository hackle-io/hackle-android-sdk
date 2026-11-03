package io.hackle.android.ui.core

import android.net.Uri
import io.hackle.android.HackleDelegate
import io.hackle.android.HackleUrlChannel
import io.hackle.android.HackleUrlContext
import io.hackle.sdk.core.internal.log.Logger

internal class HackleUrlDispatcher(delegate: HackleDelegate?) {

    @Volatile
    private var delegate: HackleDelegate? = delegate

    val hasDelegate: Boolean get() = delegate != null

    fun setDelegate(delegate: HackleDelegate?) {
        this.delegate = delegate
    }

    /**
     * Dispatches the url to the registered [HackleDelegate].
     *
     * @return true if the url was handled by the delegate, false if the caller should open it itself
     */
    fun dispatch(url: Uri, channel: HackleUrlChannel): Boolean {
        val delegate = this.delegate ?: return false
        log.debug { "Dispatch url to delegate: $url [channel=$channel]" }
        try {
            delegate.handleUrl(HackleUrlContext(url, channel))
        } catch (e: Throwable) {
            log.error { "Unexpected exception while handling url by delegate: $url\n$e" }
        }
        return true
    }

    companion object {
        private val log = Logger<HackleUrlDispatcher>()
    }
}
