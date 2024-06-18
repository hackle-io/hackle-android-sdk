package io.hackle.android.external

import io.hackle.android.HackleConfig
import java.util.concurrent.atomic.AtomicBoolean

internal object OptOutManager {

    private val OFFLINE = AtomicBoolean(false)
    private val OPT_OUT_TRACKING = AtomicBoolean(false)

    val isOffline: Boolean get() = OFFLINE.get()
    val isOptOutTracking: Boolean get() = OPT_OUT_TRACKING.get()

    fun setOptOutTracking(optOutTracking: Boolean) {
        OPT_OUT_TRACKING.set(optOutTracking)
    }

    fun configure(config: HackleConfig) {
        OFFLINE.set(config.offline)
        setOptOutTracking(config.outOutTracking)
    }
}
