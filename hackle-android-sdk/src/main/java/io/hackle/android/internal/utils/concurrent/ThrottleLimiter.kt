package io.hackle.android.internal.utils.concurrent

import io.hackle.sdk.core.internal.time.Clock

internal class ThrottleLimiter(
    private val intervalMillis: Long,
    private val limit: Int,
    private val clock: Clock,
) {

    private val lock = Any()
    private var currentScope: ThrottleScope? = null

    fun tryAcquire(): Boolean {
        return synchronized(lock) {
            val now = clock.currentMillis()
            val scope = refreshScopeIfNeeded(now)
            scope.tryAcquire()
        }
    }

    private fun refreshScopeIfNeeded(now: Long): ThrottleScope {
        val currentScope = this.currentScope
        if (currentScope == null || currentScope.isExpired(now)) {
            val scope = ThrottleScope(now + intervalMillis, limit)
            this.currentScope = scope
            return scope
        }
        return currentScope
    }
}

private class ThrottleScope(
    private val expirationTimestamp: Long,
    private var token: Int
) {

    fun isExpired(now: Long): Boolean {
        return expirationTimestamp < now
    }

    fun tryAcquire(): Boolean {
        return if (this.token > 0) {
            token--
            true
        } else {
            false
        }
    }
}
