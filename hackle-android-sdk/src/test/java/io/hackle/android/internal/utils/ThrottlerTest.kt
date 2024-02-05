package io.hackle.android.internal.utils

import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.util.concurrent.Executor

class ThrottlerTest {

    private lateinit var executor: Executor

    @Before
    fun setup() {
        executor = mockk<Executor>()
        every { executor.execute(any()) } answers { firstArg<Runnable>().run() }
    }

    @Test
    fun `should call action callback even throttled callback is null`() {
        val throttler = Throttler(
            intervalInSeconds = 1,
            executor = executor,
            limitInScope = 1
        )
        val throttleHistories: MutableList<Boolean> = ArrayList()
        throttler.execute(
            action = { throttleHistories.add(true) }
        )
        throttler.execute(
            action = { throttleHistories.add(true) }
        )
        Thread.sleep(1_000L)
        throttler.execute(
            action = { throttleHistories.add(true) }
        )

        expectThat(throttleHistories) isEqualTo mutableListOf(true, true)
    }

    @Test
    fun `not throttle after interval seconds later`() {
        val throttler = Throttler(
            intervalInSeconds = 1,
            executor = executor,
            limitInScope = 1
        )
        val throttleHistories: MutableList<Boolean> = ArrayList()
        throttler.execute(
            action = { throttleHistories.add(true) },
            throttled = { throttleHistories.add(false) }
        )
        throttler.execute(
            action = { throttleHistories.add(true) },
            throttled = { throttleHistories.add(false) }
        )
        Thread.sleep(1_000L)
        throttler.execute(
            action = { throttleHistories.add(true) },
            throttled = { throttleHistories.add(false) }
        )

        expectThat(throttleHistories) isEqualTo mutableListOf(true, false, true)
    }

    @Test
    fun `throttle more than 2 limits`() {
        val throttler = Throttler(
            intervalInSeconds = 1,
            executor = executor,
            limitInScope = 2
        )
        val throttleHistories: MutableList<Boolean> = ArrayList()
        throttler.execute(
            action = { throttleHistories.add(true) },
            throttled = { throttleHistories.add(false) }
        )
        throttler.execute(
            action = { throttleHistories.add(true) },
            throttled = { throttleHistories.add(false) }
        )
        throttler.execute(
            action = { throttleHistories.add(true) },
            throttled = { throttleHistories.add(false) }
        )
        Thread.sleep(1_000L)
        throttler.execute(
            action = { throttleHistories.add(true) },
            throttled = { throttleHistories.add(false) }
        )

        expectThat(throttleHistories) isEqualTo mutableListOf(true, true, false, true)
    }
}