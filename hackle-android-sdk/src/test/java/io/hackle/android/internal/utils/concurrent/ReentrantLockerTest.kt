package io.hackle.android.internal.utils.concurrent

import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

internal class ReentrantLockerTest {

    @Test
    fun `returns the result of the action`() {
        val locker = ReentrantLocker()
        expectThat(locker.withLock { 42 }).isEqualTo(42)
    }

    @Test
    fun `allows reentrant acquisition on the same thread`() {
        val locker = ReentrantLocker()
        val result = locker.withLock {
            locker.withLock {
                "ok"
            }
        }
        expectThat(result).isEqualTo("ok")
    }
}
