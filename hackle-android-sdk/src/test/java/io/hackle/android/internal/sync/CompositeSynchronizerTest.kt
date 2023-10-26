package io.hackle.android.internal.sync

import io.hackle.android.support.assertThrows
import io.hackle.sdk.core.internal.metrics.cumulative.CumulativeMetricRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.util.concurrent.Executors
import kotlin.concurrent.thread


class CompositeSynchronizerTest {

    private lateinit var workspaceSynchronizer: Synchronizer
    private lateinit var cohortSynchronizer: Synchronizer
    private lateinit var sut: CompositeSynchronizer

    @Before
    fun before() {
        workspaceSynchronizer = mockk(relaxed = true)
        cohortSynchronizer = mockk(relaxed = true)
        sut = CompositeSynchronizer(Executors.newCachedThreadPool())
        sut.add(SynchronizerType.WORKSPACE, workspaceSynchronizer)
        sut.add(SynchronizerType.COHORT, cohortSynchronizer)
    }

    @Test
    fun `sync`() {
        sut.sync()

        verify(exactly = 1) {
            workspaceSynchronizer.sync()
        }
        verify(exactly = 1) {
            cohortSynchronizer.sync()
        }
    }

    @Test
    fun `sync only`() {
        sut.sync(SynchronizerType.WORKSPACE)
        verify(exactly = 1) {
            workspaceSynchronizer.sync()
        }
        verify(exactly = 0) {
            cohortSynchronizer.sync()
        }
    }

    @Test
    fun `async`() {
        every { workspaceSynchronizer.sync() } answers {
            Thread.sleep(100)
        }
        every { cohortSynchronizer.sync() } answers {
            Thread.sleep(100)
        }

        var count = 0
        thread {
            sut.sync()
            count++
        }

        Thread.sleep(50)
        expectThat(count).isEqualTo(0)
        Thread.sleep(100)
        expectThat(count).isEqualTo(1)
    }

    @Test
    fun `unsupported type`() {
        val sut = CompositeSynchronizer(Executors.newCachedThreadPool())

        val exception = assertThrows<IllegalArgumentException> {
            sut.sync(SynchronizerType.WORKSPACE)
        }

        expectThat(exception.message).isEqualTo("Unsupported SynchronizerType [WORKSPACE]")
    }

    @Test
    fun `safe`() {
        val counter = CumulativeMetricRegistry().counter("workspace")

        every { workspaceSynchronizer.sync() } answers {
            Thread.sleep(100)
            counter.increment()
        }

        every { cohortSynchronizer.sync() } answers {
            Thread.sleep(50)
            throw IllegalArgumentException("fail")
        }

        sut.sync()
        expectThat(counter.count()).isEqualTo(1)
    }
}