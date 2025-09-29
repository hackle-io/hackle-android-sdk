package io.hackle.android.internal.application

import android.app.Activity
import io.hackle.android.internal.lifecycle.AppState
import io.hackle.android.internal.lifecycle.Lifecycle
import io.hackle.sdk.core.internal.time.Clock
import io.mockk.*
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isSameInstanceAs
import strikt.assertions.isTrue
import java.util.concurrent.Executor

class ApplicationStateManagerTest {

    private val mockClock = mockk<Clock>()
    private var manager: ApplicationStateManager = ApplicationStateManager(mockClock)
    private val mockListener = mockk<ApplicationStateListener>(relaxed = true)
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
    fun `onApplicationForeground should notify all listeners`() {
        // when
        manager.onApplicationForeground(1234567890L, true)

        // then
        verify { mockExecutor.execute(any()) }
        verify { mockListener.onForeground(1234567890L, true) }
    }

    @Test
    fun `onApplicationForeground should handle isAppLaunch false`() {
        // when
        manager.onApplicationForeground(1234567890L, false)

        // then
        verify { mockExecutor.execute(any()) }
        verify { mockListener.onForeground(1234567890L, false) }
    }

    @Test
    fun `onApplicationBackground should notify all listeners`() {
        // when
        manager.onApplicationBackground(1234567890L)

        // then
        verify { mockExecutor.execute(any()) }
        verify { mockListener.onBackground(1234567890L) }
    }

    @Test
    fun `onApplicationForeground should work without executor`() {
        // given - manager without executor
        val managerWithoutExecutor = ApplicationStateManager(mockClock)
        managerWithoutExecutor.addListener(mockListener)

        // when
        managerWithoutExecutor.onApplicationForeground(1234567890L, true)

        // then
        verify { mockListener.onForeground(1234567890L, true) }
    }

    @Test
    fun `onApplicationBackground should work without executor`() {
        // given - manager without executor
        val managerWithoutExecutor = ApplicationStateManager(mockClock)
        managerWithoutExecutor.addListener(mockListener)

        // when
        managerWithoutExecutor.onApplicationBackground(1234567890L)

        // then
        verify { mockListener.onBackground(1234567890L) }
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
        val managerWithoutDeterminer = ApplicationStateManager(mockClock)
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
        val managerWithoutExecutor = ApplicationStateManager(mockClock)
        managerWithoutExecutor.addListener(mockListener)
        managerWithoutExecutor.setApplicationInstallDeterminer(mockInstallDeterminer)
        every { mockInstallDeterminer.determine() } returns ApplicationInstallState.INSTALL

        // when
        managerWithoutExecutor.checkApplicationInstall()

        // then
        verify { mockListener.onInstall(1234567890L) }
    }

    @Test
    fun `should handle multiple listeners for foreground events`() {
        // given
        val mockListener2 = mockk<ApplicationStateListener>(relaxed = true)
        manager.addListener(mockListener2)

        // when
        manager.onApplicationForeground(1234567890L, true)

        // then
        verify { mockListener.onForeground(1234567890L, true) }
        verify { mockListener2.onForeground(1234567890L, true) }
    }

    @Test
    fun `should handle multiple listeners for background events`() {
        // given
        val mockListener2 = mockk<ApplicationStateListener>(relaxed = true)
        manager.addListener(mockListener2)

        // when
        manager.onApplicationBackground(1234567890L)

        // then
        verify { mockListener.onBackground(1234567890L) }
        verify { mockListener2.onBackground(1234567890L) }
    }

    @Test
    fun `should handle multiple listeners for install events`() {
        // given
        val mockListener2 = mockk<ApplicationStateListener>(relaxed = true)
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
        val mockListener2 = mockk<ApplicationStateListener>(relaxed = true)
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

        // when
        manager.setExecutor(newExecutor)
        manager.onApplicationForeground(1234567890L, true)

        // then
        verify { newExecutor.execute(any()) }
        verify { mockListener.onForeground(1234567890L, true) }
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
        val instance1 = ApplicationStateManager.instance
        val instance2 = ApplicationStateManager.instance

        // then
        expectThat(instance1 === instance2).isTrue()
    }
}