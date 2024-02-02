package io.hackle.android.internal.utils

import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max

class Throttler(
    private val intervalInSeconds: Int,
    private val executor: Executor,
    private val limitInScope: Int = 1
) {

    private val throttleLock = Unit
    private val executedCountInScope = AtomicInteger(0)

    private var firstExecutedTimestampInScope = AtomicLong(0)

    fun execute(action: () -> Unit, throttled: (() -> Unit)? = null) {
        val isThrottled = throttle(
            executeTimestamp = System.currentTimeMillis()
        )
        executor.execute {
            if (isThrottled && throttled != null) {
                throttled()
            } else {
                action()
            }
        }
    }

    private fun throttle(executeTimestamp: Long): Boolean = synchronized(throttleLock) {
        expireCurrentScopeIfNeeded(executeTimestamp)
        val isThrottled = calculateQuotesInCurrentScope() <= 0
        if (!isThrottled) {
            executedCountInScope.incrementAndGet()
        }
        return@synchronized isThrottled
    }

    private fun expireCurrentScopeIfNeeded(executeTimestamp: Long) {
        if (isCurrentScopeExpired(executeTimestamp)) {
            executedCountInScope.set(0)
            firstExecutedTimestampInScope.set(executeTimestamp)
        }
    }

    private fun isCurrentScopeExpired(timestamp: Long): Boolean {
        return calculateEndTimestampInCurrentScope() - timestamp <= 0
    }

    private fun calculateEndTimestampInCurrentScope(): Long {
        return firstExecutedTimestampInScope.get() + (intervalInSeconds * 1_000L)
    }

    private fun calculateQuotesInCurrentScope(): Int {
        return max(limitInScope - executedCountInScope.get(), 0)
    }

    private fun isThrottledInCurrentScope(): Boolean {
        return calculateQuotesInCurrentScope() <= 0
    }
}