package io.hackle.android.internal.utils.concurrent

import io.hackle.sdk.core.internal.time.Clock
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors

class ThrottleLimiterTest {

    @Test
    fun `throttle 1`() {
        val sut = ThrottleLimiter(1000, 1, Clock.SYSTEM)
        expectThat(sut.tryAcquire()).isTrue()
        repeat(10) {
            expectThat(sut.tryAcquire()).isFalse()
        }
    }

    @Test
    fun `throttle N`() {
        val sut = ThrottleLimiter(1000, 10, Clock.SYSTEM)

        val results = List(100) {
            sut.tryAcquire()
        }

        expectThat(results.count { it }) isEqualTo 10
        expectThat(results.count { !it }) isEqualTo 90
    }

    @Test
    fun `refresh after interval`() {
        val clock = mockk<Clock>()
        every { clock.currentMillis() } returnsMany listOf(0, 100, 101)
        val sut = ThrottleLimiter(100, 1, clock)

        expectThat(sut.tryAcquire()).isTrue()
        expectThat(sut.tryAcquire()).isFalse()
        expectThat(sut.tryAcquire()).isTrue()
    }

    @Test
    fun `concurrent`() {
        val sut = ThrottleLimiter(1000, 1, Clock.SYSTEM)

        val executor = Executors.newFixedThreadPool(32)
        val barrier = CyclicBarrier(32)

        val futures = List(32) {
            CompletableFuture.supplyAsync({
                barrier.await()
                sut.tryAcquire()
            }, executor)
        }

        val results = futures.map { it.get() }
        expectThat(results.count { it }) isEqualTo 1
    }
}
