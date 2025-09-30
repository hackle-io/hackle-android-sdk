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
    private var manager: ApplicationInstallStateManager = ApplicationInstallStateManager(mockClock)
    private val mockListener = mockk<ApplicationInstallStateListener>(relaxed = true)
    private val mockExecutor = mockk<Executor>()
    private val mockInstallDeterminer = mockk<ApplicationInstallDeterminer>()
    private val executorSlot = slot<Runnable>()

    @Before
    fun setUp() {
        every { mockClock.currentMillis() } returns 1234567890L
        every { mockExecutor.execute(capture(executorSlot)) } answers { executorSlot.captured.run() }
        every { mockInstallDeterminer.determine() } returns ApplicationInstallState.NONE

        manager.addListener(mockListener)
        manager.setExecutor(mockExecutor)
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
        val managerWithoutDeterminer = ApplicationInstallStateManager(mockClock)
        managerWithoutDeterminer.addListener(mockListener)

        // when
        managerWithoutDeterminer.checkApplicationInstall()

        // then - should not crash and no events should be triggered
        verify(exactly = 0) { mockListener.onInstall(any()) }
        verify(exactly = 0) { mockListener.onUpdate(any()) }
    }

    @Test
    fun `checkApplicationInstall should work without executor`() {
        // given
        val managerWithoutExecutor = ApplicationInstallStateManager(mockClock)
        managerWithoutExecutor.addListener(mockListener)
        managerWithoutExecutor.setApplicationInstallDeterminer(mockInstallDeterminer)
        every { mockInstallDeterminer.determine() } returns ApplicationInstallState.INSTALL

        // when
        managerWithoutExecutor.checkApplicationInstall()

        // then
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
    fun `setExecutor should update executor`() {
        // given
        val newExecutor = mockk<Executor>()
        val newExecutorSlot = slot<Runnable>()
        every { newExecutor.execute(capture(newExecutorSlot)) } answers { newExecutorSlot.captured.run() }
        every { mockInstallDeterminer.determine() } returns ApplicationInstallState.INSTALL

        // when
        manager.setExecutor(newExecutor)
        manager.checkApplicationInstall()

        // then
        verify { newExecutor.execute(any()) }
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
    fun `getInstance should return singleton instance`() {
        // when
        val instance1 = ApplicationInstallStateManager.instance
        val instance2 = ApplicationInstallStateManager.instance

        // then
        expectThat(instance1 === instance2).isTrue()
    }

    @Test
    fun `executor should handle async execution correctly`() {
        // given
        val asyncExecutor = mockk<Executor>()
        val executedRunnables = mutableListOf<Runnable>()
        every { asyncExecutor.execute(capture(executedRunnables)) } just Runs
        every { mockInstallDeterminer.determine() } returns ApplicationInstallState.INSTALL

        manager.setExecutor(asyncExecutor)

        // when
        manager.checkApplicationInstall()

        // then - executor was called but runnable not executed yet
        verify { asyncExecutor.execute(any()) }
        verify(exactly = 0) { mockListener.onInstall(any()) }

        // when - manually execute the captured runnable
        executedRunnables.first().run()

        // then - listener should be called now
        verify { mockListener.onInstall(1234567890L) }
    }

    @Test
    fun `should execute synchronously when executor is null`() {
        // given - fresh manager without executor
        val syncManager = ApplicationInstallStateManager(mockClock)
        syncManager.addListener(mockListener)
        syncManager.setApplicationInstallDeterminer(mockInstallDeterminer)
        every { mockInstallDeterminer.determine() } returns ApplicationInstallState.INSTALL

        // when - call checkApplicationInstall without executor
        syncManager.checkApplicationInstall()

        // then - listener should be called immediately (synchronously)
        verify { mockListener.onInstall(1234567890L) }
    }
}