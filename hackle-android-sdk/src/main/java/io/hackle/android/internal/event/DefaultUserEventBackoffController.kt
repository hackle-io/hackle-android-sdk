package io.hackle.android.internal.event

import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.time.Clock
import kotlin.math.min
import kotlin.math.pow

interface UserEventBackoffController {
    fun checkResponse(isSuccess: Boolean)
    fun isAllowNextFlush(): Boolean
}

internal class DefaultUserEventBackoffController(private val clock: Clock) : UserEventBackoffController {
    private var nextFlushAllowDate: Long? = 0
    private var failureCount: Int = 0

    override fun checkResponse(isSuccess: Boolean) {
        failureCount = if (isSuccess) 0 else failureCount + 1
        calculateNextFlushDate()
    }

    override fun isAllowNextFlush(): Boolean {
        return nextFlushAllowDate?.run {
            val now = clock.currentMillis()
            if (now < this) {
                log.debug { "Skipping flush. Next flush date: $this, current time: $now" }
                false
            } else {
                true
            }
        } ?: true
    }

    private fun calculateNextFlushDate() {
        nextFlushAllowDate = if (failureCount == 0) {
            null
        } else {
            val interval = 2.0.pow(failureCount.toDouble() - 1).toInt()
            val intervalMillis = min(interval * Constants.USER_EVENT_RETRY_INTERVAL, Constants.USER_EVENT_RETRY_MAX_INTERVAL)
            clock.currentMillis() + intervalMillis
        }
    }

    companion object {
        private val log = Logger<DefaultUserEventBackoffController>()
    }
}
