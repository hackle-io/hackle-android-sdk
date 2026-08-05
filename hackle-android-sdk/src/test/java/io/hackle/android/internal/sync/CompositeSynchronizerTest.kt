package io.hackle.android.internal.sync

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.util.concurrent.CompletableFuture


class CompositeSynchronizerTest {

    private lateinit var workspaceSynchronizer: Synchronizer
    private lateinit var cohortSynchronizer: Synchronizer
    private lateinit var sut: CompositeSynchronizer

    @Before
    fun before() {
        workspaceSynchronizer = mockk()
        cohortSynchronizer = mockk()
        sut = CompositeSynchronizer()
        sut.add(workspaceSynchronizer)
        sut.add(cohortSynchronizer)
    }

    @Test
    fun `sync - 모든 synchronizer를 sync한다`() {
        // given
        every { workspaceSynchronizer.sync() } returns CompletableFuture.completedFuture(null)
        every { cohortSynchronizer.sync() } returns CompletableFuture.completedFuture(null)

        // when
        sut.sync().get()

        // then
        verify(exactly = 1) { workspaceSynchronizer.sync() }
        verify(exactly = 1) { cohortSynchronizer.sync() }
    }

    @Test
    fun `sync - 모든 synchronizer가 완료되어야 완료된다`() {
        // given
        val workspaceFuture = CompletableFuture<Void>()
        val cohortFuture = CompletableFuture<Void>()
        every { workspaceSynchronizer.sync() } returns workspaceFuture
        every { cohortSynchronizer.sync() } returns cohortFuture

        // when
        val actual = sut.sync()

        // then
        expectThat(actual.isDone) isEqualTo false

        workspaceFuture.complete(null)
        expectThat(actual.isDone) isEqualTo false

        cohortFuture.complete(null)
        actual.get()
    }

    @Test
    fun `sync - synchronizer가 동기적으로 예외를 던져도 전체는 성공으로 완료된다`() {
        // given
        every { workspaceSynchronizer.sync() } returns CompletableFuture.completedFuture(null)
        every { cohortSynchronizer.sync() } throws IllegalArgumentException("fail")

        // when
        sut.sync().get()

        // then
        verify(exactly = 1) { workspaceSynchronizer.sync() }
    }

    @Test
    fun `sync - 일부 synchronizer가 실패해도 전체는 성공으로 완료된다`() {
        // given
        every { workspaceSynchronizer.sync() } returns CompletableFuture.completedFuture(null)
        val failedFuture = CompletableFuture<Void>()
        failedFuture.completeExceptionally(IllegalArgumentException("fail"))
        every { cohortSynchronizer.sync() } returns failedFuture

        // when
        sut.sync().get()

        // then
        verify(exactly = 1) { workspaceSynchronizer.sync() }
        verify(exactly = 1) { cohortSynchronizer.sync() }
    }
}
