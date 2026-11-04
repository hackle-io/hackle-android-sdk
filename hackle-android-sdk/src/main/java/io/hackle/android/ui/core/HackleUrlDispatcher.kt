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
     * Returns the currently registered [HackleDelegate].
     *
     * Use the returned snapshot with [dispatch] when the decision to use the delegate
     * and the dispatch must refer to the same delegate.
     */
    fun currentDelegate(): HackleDelegate? {
        return delegate
    }

    /**
     * Dispatches the url to the registered [HackleDelegate].
     *
     * @return true if the url was handled by the delegate, false if the caller should open it itself
     */
    fun dispatch(url: Uri, channel: HackleUrlChannel): Boolean {
        val delegate = this.delegate ?: return false
        dispatch(delegate, url, channel)
        return true
    }

    /**
     * Dispatches the url to the given [HackleDelegate].
     */
    fun dispatch(delegate: HackleDelegate, url: Uri, channel: HackleUrlChannel) {
        log.debug { "Dispatch url to delegate: $url [channel=$channel]" }
        try {
            delegate.handleUrl(HackleUrlContext(url, channel))
        } catch (e: Throwable) {
            log.error { "Unexpected exception while handling url by delegate: $url\n$e" }
        }
    }

    companion object {
        private val log = Logger<HackleUrlDispatcher>()
    }
}
