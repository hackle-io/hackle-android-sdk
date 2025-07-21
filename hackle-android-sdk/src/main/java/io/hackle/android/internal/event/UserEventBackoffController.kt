package io.hackle.android.internal.event

import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.time.Clock
import kotlin.math.min
import kotlin.math.pow


internal class UserEventBackoffController(
    private val userEventRetryIntervalMillis: Int,
    private val clock: Clock
) {
    private var nextFlushAllowDateMillis: Long? = 0
    private var failureCount: Int = 0

    fun checkResponse(isSuccess: Boolean) {
        synchronized(LOCK) {
            failureCount = if (isSuccess) {
                0
            } else {
                failureCount + 1
            }

            calculateNextFlushDate()
        }
    }

    fun isAllowNextFlush(): Boolean {
        synchronized(LOCK) {
            if (nextFlushAllowDateMillis == null) {
                return true
            }

            val now = clock.currentMillis()
            val nextFlushTime = nextFlushAllowDateMillis!!

            if (now < nextFlushTime) {
                log.debug { "Skipping flush. Next flush date: $nextFlushTime, current time: $now" }
                return false
            }

            return true
        }
    }

    private fun calculateNextFlushDate() {
        nextFlushAllowDateMillis = if (failureCount == 0) {
            null
        } else {
            val exponential = 2.0.pow(failureCount.toDouble() - 1).toInt()
            val intervalMillis =
                min(exponential * userEventRetryIntervalMillis, Constants.USER_EVENT_RETRY_MAX_INTERVAL_MILLIS)
            clock.currentMillis() + intervalMillis
        }
    }

    companion object {
        private val log = Logger<UserEventBackoffController>()
        private val LOCK = Any()
    }
}
