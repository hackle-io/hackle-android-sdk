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
        synchronized(lock) {
            if (isOptOutTracking == optOut) return
            isOptOutTracking = optOut
        }
        log.info { "OptOutTracking changed to $optOut" }
        for (listener in listeners) {
            try {
                listener.onOptOutChanged(optOut)
            } catch (e: Exception) {
                log.error { "Unexpected exception while notifying OptOutListener: $e" }
            }
        }
    }

    companion object {
        private val log = Logger<OptOutManager>()
    }
}
