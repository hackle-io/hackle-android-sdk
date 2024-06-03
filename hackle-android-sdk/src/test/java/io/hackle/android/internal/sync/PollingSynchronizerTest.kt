package io.hackle.android.internal.sync

import io.hackle.android.internal.lifecycle.AppState
import io.hackle.sdk.core.internal.scheduler.Schedulers
import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import java.util.concurrent.CyclicBarrier
import kotlin.concurrent.thread


class PollingSynchronizerTest {

    @Test
    fun `sync - delegate`() {
        // given
        val delegate = mockk<CompositeSynchronizer>(relaxed = true)
        val sut = PollingSynchronizer(delegate, Schedulers.executor("test"), 10000)

        // when
        sut.sync()

        // then
        verify(exactly = 1) {
            delegate.sync()
        }
    }

    @Test
    fun `sync one - delegate`() {
        // given
        val delegate = mockk<CompositeSynchronizer>(relaxed = true)
        val sut = PollingSynchronizer(delegate, Schedulers.executor("test"), 10000)

        // when
        sut.sync(SynchronizerType.COHORT)

        // then
        verify(exactly = 1) {
            delegate.sync(SynchronizerType.COHORT)
        }
    }

    @Test
    fun `start - no polling`() {
        // given
        val delegate = mockk<CompositeSynchronizer>(relaxed = true)
        val sut = PollingSynchronizer(delegate, Schedulers.executor("test"), -1)

        // when
        sut.start()
        Thread.sleep(1000)

        // then
        verify {
            delegate wasNot Called
        }
    }

    @Test
    fun `start - scheduling`() {
        // given
        val delegate = mockk<CompositeSynchronizer>(relaxed = true)
        val sut = PollingSynchronizer(delegate, Schedulers.executor("test"), 200)

        // when
        sut.start()
        Thread.sleep(500)

        // then
        verify(exactly = 2) {
            delegate.sync()
        }
    }

    @Test
    fun `start - once`() {
        // given
        val delegate = mockk<CompositeSynchronizer>(relaxed = true)
        val sut = PollingSynchronizer(delegate, Schedulers.executor("test"), 500)

        // when
        val cb = CyclicBarrier(10)
        repeat(10) {
            thread {
                cb.await()
                sut.start()
            }
        }
        Thread.sleep(1250)

        // then
        verify(exactly = 2) {
            delegate.sync()
        }
    }

    @Test
    fun `stop - no polling`() {
        // given
        val delegate = mockk<CompositeSynchronizer>(relaxed = true)
        val sut = PollingSynchronizer(delegate, Schedulers.executor("test"), -1)

        // when
        sut.start()
        sut.stop()
    }

    @Test
    fun `stop - cancel polling`() {
        // given
        val delegate = mockk<CompositeSynchronizer>(relaxed = true)
        val sut = PollingSynchronizer(delegate, Schedulers.executor("test"), 200)

        // when
        sut.start()
        Thread.sleep(500)
        sut.stop()
        Thread.sleep(500)

        // then
        verify(exactly = 2) {
            delegate.sync()
        }
    }

    @Test
    fun `onChanged`() {
        val delegate = mockk<CompositeSynchronizer>(relaxed = true)
        val sut = PollingSynchronizer(delegate, Schedulers.executor("test"), 200)

        sut.onState(AppState.FOREGROUND, 42)
        Thread.sleep(500)
        verify(exactly = 2) {
            delegate.sync()
        }
        sut.onState(AppState.BACKGROUND, 42)
        Thread.sleep(500)
        verify(exactly = 2) {
            delegate.sync()
        }
    }
}