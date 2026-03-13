package io.hackle.android.internal.optout

import io.hackle.sdk.core.internal.log.Logger

internal class OptOutManager(
    optOutTracking: Boolean
) {

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
    }

    companion object {
        private val log = Logger<OptOutManager>()
    }
}
