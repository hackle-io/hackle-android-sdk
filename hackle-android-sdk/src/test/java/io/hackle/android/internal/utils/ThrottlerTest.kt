package io.hackle.android.internal.utils

import io.hackle.sdk.core.internal.threads.NamedThreadFactory
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.startsWith
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class ThrottlerTest {

    private lateinit var threadNamePrefix: String
    private lateinit var executor: Executor

    @Before
    fun setup() {
        threadNamePrefix = UUID.randomUUID()
            .toString()
            .slice(0 until 8)
        executor = ThreadPoolExecutor(
            2, Int.MAX_VALUE,
            60, TimeUnit.SECONDS,
            SynchronousQueue(),
            NamedThreadFactory("$threadNamePrefix-", true)
        )
    }

    @Test(timeout = 1_000L)
    fun `action callback must run on executor thread`() {
        val countDownLatch = CountDownLatch(1)
        val throttler = Throttler(
            intervalInSeconds = 1,
            executor = executor,
            limitInScope = 1
        )
        throttler.execute({
            expectThat(Thread.currentThread().name)
                .startsWith(threadNamePrefix)
            countDownLatch.countDown()
        })
        countDownLatch.await()
    }

    @Test(timeout = 1_000L)
    fun `throttled callback must run on executor thread`() {
        val countDownLatch = CountDownLatch(1)
        val throttler = Throttler(
            intervalInSeconds = 1,
            executor = executor,
            limitInScope = 1
        )
        throttler.execute(action = {})
        throttler.execute(
            action = {},
            throttled = {
                expectThat(Thread.currentThread().name)
                    .startsWith(threadNamePrefix)
                countDownLatch.countDown()
            }
        )
        countDownLatch.await()
    }

    @Test
    fun `action callback must call even throttled callback is provided`() {
        val throttler = Throttler(
            intervalInSeconds = 1,
            executor = executor,
            limitInScope = 1
        )
        val executedCount = AtomicInteger(0)
        throttler.execute({ executedCount.incrementAndGet() })
        throttler.execute({ executedCount.incrementAndGet() })
        Thread.sleep(1_000L)
        throttler.execute({ executedCount.incrementAndGet() })
        Thread.sleep(1_000L)
        expectThat(executedCount.get()) isEqualTo 2
    }

    @Test
    fun `not throttle after interval seconds later`() {
        val throttler = Throttler(
            intervalInSeconds = 1,
            executor = executor,
            limitInScope = 1
        )
        val executedCount = AtomicInteger(0)
        val throttledCount = AtomicInteger(0)
        throttler.execute(
            action = { executedCount.incrementAndGet() },
            throttled = { throttledCount.incrementAndGet() }
        )
        throttler.execute(
            action = { executedCount.incrementAndGet() },
            throttled = { throttledCount.incrementAndGet() }
        )
        Thread.sleep(1_000L)
        throttler.execute(
            action = { executedCount.incrementAndGet() },
            throttled = { throttledCount.incrementAndGet() }
        )

        Thread.sleep(1_000L)
        expectThat(executedCount.get()) isEqualTo 2
        expectThat(throttledCount.get()) isEqualTo 1
    }

    @Test
    fun `throttle more than 2 limits`() {
        val throttler = Throttler(
            intervalInSeconds = 1,
            executor = executor,
            limitInScope = 2
        )
        val executedCount = AtomicInteger(0)
        val throttledCount = AtomicInteger(0)
        throttler.execute(
            action = { executedCount.incrementAndGet() },
            throttled = { throttledCount.incrementAndGet() }
        )
        throttler.execute(
            action = { executedCount.incrementAndGet() },
            throttled = { throttledCount.incrementAndGet() }
        )
        throttler.execute(
            action = { executedCount.incrementAndGet() },
            throttled = { throttledCount.incrementAndGet() }
        )
        Thread.sleep(1_000L)
        throttler.execute(
            action = { executedCount.incrementAndGet() },
            throttled = { throttledCount.incrementAndGet() }
        )

        Thread.sleep(1_000L)
        expectThat(executedCount.get()) isEqualTo 3
        expectThat(throttledCount.get()) isEqualTo 1
    }
}