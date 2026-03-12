package io.hackle.android.internal.optout

import io.hackle.android.internal.event.DefaultEventProcessor
import io.hackle.sdk.core.internal.log.Logger

internal class OptOutManager(
    private val eventProcessor: DefaultEventProcessor,
    private val optOutState: OptOutState,
) {

    private val lock = Any()

    val isOptOutTracking: Boolean get() = optOutState.isOptOutTracking

    fun setOptOutTracking(optOut: Boolean) {
        synchronized(lock) {
            if (optOut == optOutState.isOptOutTracking) {
                return
            }

            if (optOut) {
                // optIn -> optOut 전환: 기존 로컬 이벤트를 best-effort flush한 후 차단
                eventProcessor.flush()
            }

            optOutState.isOptOutTracking = optOut
            log.info { "OptOutTracking changed to $optOut" }
        }
    }

    companion object {
        private val log = Logger<OptOutManager>()
    }
}
