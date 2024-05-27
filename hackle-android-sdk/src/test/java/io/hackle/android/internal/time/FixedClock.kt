package io.hackle.android.internal.time

import io.hackle.sdk.core.internal.time.Clock

class FixedClock(private val time: Long) : Clock {
    override fun currentMillis(): Long {
        return time
    }

    override fun tick(): Long {
        return time
    }
}
