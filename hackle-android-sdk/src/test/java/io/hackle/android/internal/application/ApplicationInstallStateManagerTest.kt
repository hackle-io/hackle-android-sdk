package io.hackle.android.internal.application

import io.hackle.sdk.core.internal.time.Clock
import io.mockk.*
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isTrue
import java.util.concurrent.Executor

class ApplicationInstallStateManagerTest {

    private val mockClock = mockk<Clock>()
    private val mockListener = mockk<ApplicationInstallStateListener>(relaxed = true)
    private val mockExecutor = mockk<Executor>()
    private val mockInstallDeterminer = mockk<ApplicationInstallDeterminer>()
    private val executorSlot = slot<Runnable>()

    private lateinit var manager: ApplicationInstallStateManager

    @Before
    fun setUp() {
        every { mockClock.currentMillis() } returns 1234567890L
        every { mockExecutor.execute(capture(executorSlot)) } answers { executorSlot.captured.run() }
        every { mockInstallDeterminer.determine() } returns ApplicationInstallState.NONE

        manager = ApplicationInstallStateManager(mockExecutor, mockClock)
        manager.addListener(mockListener)
        manager.setApplicationInstallDeterminer(mockInstallDeterminer)
    }

    @Test
    fun `checkApplicationInstall should do nothing when determiner returns NONE`() {
        // given
        every { mockInstallDeterminer.determine() } returns ApplicationInstallState.NONE

        // when
        manager.checkApplicationInstall()

        // then
        verify(exactly = 0) { mockListener.onInstall(any()) }
        verify(exactly = 0) { mockListener.onUpdate(any()) }
    }

    @Test
    fun `checkApplicationInstall should trigger onInstall when determiner returns INSTALL`() {
        // given
        every { mockInstallDeterminer.determine() } returns ApplicationInstallState.INSTALL

        // when
        manager.checkApplicationInstall()

        // then
        verify { mockExecutor.execute(any()) }
        verify { mockListener.onInstall(1234567890L) }
        verify(exactly = 0) { mockListener.onUpdate(any()) }
    }

    @Test
    fun `checkApplicationInstall should trigger onUpdate when determiner returns UPDATE`() {
        // given
        every { mockInstallDeterminer.determine() } returns ApplicationInstallState.UPDATE

        // when
        manager.checkApplicationInstall()

        // then
        verify { mockExecutor.execute(any()) }
        verify { mockListener.onUpdate(1234567890L) }
        verify(exactly = 0) { mockListener.onInstall(any()) }
    }

    @Test
    fun `checkApplicationInstall should handle null determiner gracefully`() {
        // given - manager without install determiner
        val managerWithoutDeterminer = ApplicationInstallStateManager(mockExecutor, mockClock)
        managerWithoutDeterminer.addListener(mockListener)

        // when
        managerWithoutDeterminer.checkApplicationInstall()

        // then - should not crash and no events should be triggered
        verify(exactly = 0) { mockListener.onInstall(any()) }
        verify(exactly = 0) { mockListener.onUpdate(any()) }
    }

    @Test
    fun `checkApplicationInstall should execute with provided executor`() {
        // given
        every { mockInstallDeterminer.determine() } returns ApplicationInstallState.INSTALL

        // when
        manager.checkApplicationInstall()

        // then
        verify { mockExecutor.execute(any()) }
        verify { mockListener.onInstall(1234567890L) }
    }

    @Test
    fun `should handle multiple listeners for install events`() {
        // given
        val mockListener2 = mockk<ApplicationInstallStateListener>(relaxed = true)
        manager.addListener(mockListener2)
        every { mockInstallDeterminer.determine() } returns ApplicationInstallState.INSTALL

        // when
        manager.checkApplicationInstall()

        // then
        verify { mockListener.onInstall(1234567890L) }
        verify { mockListener2.onInstall(1234567890L) }
    }

    @Test
    fun `should handle multiple listeners for update events`() {
        // given
        val mockListener2 = mockk<ApplicationInstallStateListener>(relaxed = true)
        manager.addListener(mockListener2)
        every { mockInstallDeterminer.determine() } returns ApplicationInstallState.UPDATE

        // when
        manager.checkApplicationInstall()

        // then
        verify { mockListener.onUpdate(1234567890L) }
        verify { mockListener2.onUpdate(1234567890L) }
    }

    @Test
    fun `executor should be used during checkApplicationInstall`() {
        // given
        every { mockInstallDeterminer.determine() } returns ApplicationInstallState.INSTALL

        // when
        manager.checkApplicationInstall()

        // then
        verify { mockExecutor.execute(any()) }
        verify { mockListener.onInstall(1234567890L) }
    }

    @Test
    fun `setApplicationInstallDeterminer should update determiner`() {
        // given
        val newDeterminer = mockk<ApplicationInstallDeterminer>()
        every { newDeterminer.determine() } returns ApplicationInstallState.INSTALL

        // when
        manager.setApplicationInstallDeterminer(newDeterminer)
        manager.checkApplicationInstall()

        // then
        verify { newDeterminer.determine() }
        verify { mockListener.onInstall(1234567890L) }
    }

    @Test
    fun `executor should handle async execution correctly`() {
        // given
        val asyncExecutor = mockk<Executor>()
        val executedRunnables = mutableListOf<Runnable>()
        every { asyncExecutor.execute(capture(executedRunnables)) } just Runs
        every { mockInstallDeterminer.determine() } returns ApplicationInstallState.INSTALL

        val asyncManager = ApplicationInstallStateManager(asyncExecutor, mockClock)
        asyncManager.addListener(mockListener)
        asyncManager.setApplicationInstallDeterminer(mockInstallDeterminer)

        // when
        asyncManager.checkApplicationInstall()

        // then - executor was called but runnable not executed yet
        verify { asyncExecutor.execute(any()) }
        verify(exactly = 0) { mockListener.onInstall(any()) }

        // when - manually execute the captured runnable
        executedRunnables.first().run()

        // then - listener should be called now
        verify { mockListener.onInstall(1234567890L) }
    }

    @Test
    fun `multiple managers should be independent`() {
        // given
        val executor2 = mockk<Executor>()
        val listener2 = mockk<ApplicationInstallStateListener>(relaxed = true)
        val executorSlot2 = slot<Runnable>()
        every { executor2.execute(capture(executorSlot2)) } answers { executorSlot2.captured.run() }

        val manager2 = ApplicationInstallStateManager(executor2, mockClock)
        manager2.addListener(listener2)
        manager2.setApplicationInstallDeterminer(mockInstallDeterminer)

        every { mockInstallDeterminer.determine() } returns ApplicationInstallState.INSTALL

        // when - call checkApplicationInstall on both managers
        manager.checkApplicationInstall()
        manager2.checkApplicationInstall()

        // then - each manager should trigger its own listener
        verify { mockListener.onInstall(1234567890L) }
        verify { listener2.onInstall(1234567890L) }
        verify { mockExecutor.execute(any()) }
        verify { executor2.execute(any()) }
    }
}