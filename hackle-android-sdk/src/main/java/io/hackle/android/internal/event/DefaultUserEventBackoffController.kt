package io.hackle.android.internal.event

import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.time.Clock
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min
import kotlin.math.pow

interface UserEventBackoffController {
    fun checkResponse(isSuccess: Boolean)
    fun isAllowNextFlush(): Boolean
}

internal class DefaultUserEventBackoffController(
    private val userEventRetryIntervalMillis: Int,
    private val clock: Clock
) : UserEventBackoffController {
    private var nextFlushAllowDate: Long? = 0
    private var failureCount: AtomicInteger = AtomicInteger(0)

    override fun checkResponse(isSuccess: Boolean) {
        val count = if (isSuccess) {
            failureCount.set(0)
            0
        } else {
            failureCount.addAndGet(1)
        }

        calculateNextFlushDate(count)
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

    private fun calculateNextFlushDate(failureCount: Int) {
        nextFlushAllowDate = if (failureCount == 0) {
            null
        } else {
            val exponential = 2.0.pow(failureCount.toDouble() - 1).toInt()
            val intervalMillis = min(exponential * userEventRetryIntervalMillis, Constants.USER_EVENT_RETRY_MAX_INTERVAL)
            clock.currentMillis() + intervalMillis
        }
    }

    companion object {
        private val log = Logger<DefaultUserEventBackoffController>()
    }
}
