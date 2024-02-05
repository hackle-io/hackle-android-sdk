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
        val executeHistories: MutableList<Boolean> = ArrayList()
        throttler.execute({ executeHistories.add(true) })
        throttler.execute({ executeHistories.add(true) })
        Thread.sleep(1_000L)
        throttler.execute({ executeHistories.add(true) })
        Thread.sleep(1_000L)
        expectThat(executeHistories) isEqualTo mutableListOf(true, true)
    }

    @Test
    fun `not throttle after interval seconds later`() {
        val throttler = Throttler(
            intervalInSeconds = 1,
            executor = executor,
            limitInScope = 1
        )
        val executeHistories: MutableList<Boolean> = ArrayList()
        throttler.execute(
            action = { executeHistories.add(true) },
            throttled = { executeHistories.add(false) }
        )
        throttler.execute(
            action = { executeHistories.add(true) },
            throttled = { executeHistories.add(false) }
        )
        Thread.sleep(1_000L)
        throttler.execute(
            action = { executeHistories.add(true) },
            throttled = { executeHistories.add(false) }
        )

        Thread.sleep(1_000L)
        expectThat(executeHistories) isEqualTo mutableListOf(true, false, true)
    }

    @Test
    fun `throttle more than 2 limits`() {
        val throttler = Throttler(
            intervalInSeconds = 1,
            executor = executor,
            limitInScope = 2
        )
        val executeHistories: MutableList<Boolean> = ArrayList()
        throttler.execute(
            action = { executeHistories.add(true) },
            throttled = { executeHistories.add(false) }
        )
        throttler.execute(
            action = { executeHistories.add(true) },
            throttled = { executeHistories.add(false) }
        )
        throttler.execute(
            action = { executeHistories.add(true) },
            throttled = { executeHistories.add(false) }
        )
        Thread.sleep(1_000L)
        throttler.execute(
            action = { executeHistories.add(true) },
            throttled = { executeHistories.add(false) }
        )

        Thread.sleep(1_000L)
        expectThat(executeHistories) isEqualTo mutableListOf(true, true, false, true)
    }
}