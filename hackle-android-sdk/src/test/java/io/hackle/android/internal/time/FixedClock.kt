package io.hackle.android.internal.time

import io.hackle.sdk.core.internal.time.Clock

class FixedClock(private var time: Long) : Clock {
    override fun currentMillis(): Long {
        return time
    }

    override fun tick(): Long {
        return time
    }

    fun fastForward(seconds: Int) {
        time = time + seconds * 1000L
    }
}
