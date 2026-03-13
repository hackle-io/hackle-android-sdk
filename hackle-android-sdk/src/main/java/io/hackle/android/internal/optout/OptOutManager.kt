package io.hackle.android.internal.optout

import io.hackle.android.internal.core.listener.ApplicationListenerRegistry
import io.hackle.sdk.core.internal.log.Logger

internal class OptOutManager(
    optOutTracking: Boolean
) : ApplicationListenerRegistry<OptOutListener>() {

    private val lock = Any()

    @Volatile
    var isOptOutTracking: Boolean = optOutTracking
        private set

    fun setOptOutTracking(optOut: Boolean) {
        val previous: Boolean
        synchronized(lock) {
            if (isOptOutTracking == optOut) return
            previous = isOptOutTracking
            isOptOutTracking = optOut
        }
        log.info { "OptOutTracking changed to $optOut" }
        for (listener in listeners) {
            try {
                listener.onOptOutChanged(previous, optOut)
            } catch (e: Exception) {
                log.error { "Unexpected exception while notifying OptOutListener: $e" }
            }
        }
    }

    companion object {
        private val log = Logger<OptOutManager>()
    }
}
