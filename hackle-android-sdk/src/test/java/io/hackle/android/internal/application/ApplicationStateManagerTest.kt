package io.hackle.android.internal.application

import android.app.Activity
import io.hackle.android.internal.lifecycle.AppState
import io.hackle.android.internal.lifecycle.Lifecycle
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isSameInstanceAs
import java.util.concurrent.Executor

class ApplicationStateManagerTest {

    private lateinit var manager: ApplicationStateManager
    private val mockListener = mockk<ApplicationStateListener>(relaxed = true)
    private val mockExecutor = mockk<Executor>()
    private val mockInstallDeterminer = mockk<ApplicationInstallDeterminer>()
    private val mockActivity1 = TestActivity()
    private val mockActivity2 = TestActivity2()

    private class TestActivity : Activity()
    private class TestActivity2 : Activity()
    
    @Before
    fun setUp() {
        manager = ApplicationStateManager()
        manager.addListener(mockListener)

        // Setup default mock behaviors
        every { mockInstallDeterminer.determine() } returns ApplicationInstallState.NONE
    }

    @Test
    fun `instance should return singleton instance`() {
        // Test singleton behavior by accessing it multiple times
        val instance1 = ApplicationStateManager.instance
        val instance2 = ApplicationStateManager.instance
        val instance3 = ApplicationStateManager.instance

        expectThat(instance1).isSameInstanceAs(instance2)
        expectThat(instance2).isSameInstanceAs(instance3)
    }

    @Test
    fun `setExecutor should set executor for async execution`() {
        // given
        val executorSlot = slot<Runnable>()
        every { mockExecutor.execute(capture(executorSlot)) } answers {
            executorSlot.captured.run()
        }
        manager.setExecutor(mockExecutor)

        // when
        manager.onLifecycle(Lifecycle.STARTED, mockActivity1, 123L)

        // then
        verify { mockExecutor.execute(any()) }
        verify { mockListener.onState(AppState.FOREGROUND, 123L) }
    }

    @Test
    fun `setApplicationInstallDeterminer should set determiner`() {
        manager.setApplicationInstallDeterminer(mockInstallDeterminer)

        // This will be tested through onApplicationOpened
        every { mockInstallDeterminer.determine() } returns ApplicationInstallState.INSTALL

        manager.onApplicationOpened(123L)

        verify { mockInstallDeterminer.determine() }
    }

    @Test
    fun `onLifecycle STARTED should trigger foreground state when no active activities`() {
        // when
        manager.onLifecycle(Lifecycle.STARTED, mockActivity1, 123L)

        // then
        verify { mockListener.onState(AppState.FOREGROUND, 123L) }
    }

    @Test
    fun `onLifecycle STARTED should not trigger foreground state when activities already active`() {
        // given - first activity starts
        manager.onLifecycle(Lifecycle.STARTED, mockActivity1, 123L)
        verify { mockListener.onState(AppState.FOREGROUND, 123L) }

        // when - second activity starts
        manager.onLifecycle(Lifecycle.STARTED, mockActivity2, 456L)

        // then - foreground should only be triggered once
        verify(exactly = 1) { mockListener.onState(AppState.FOREGROUND, any()) }
    }

    @Test
    fun `onLifecycle STOPPED should not trigger background state when other activities are still active`() {
        // given - both activities are started
        manager.onLifecycle(Lifecycle.STARTED, mockActivity1, 123L)
        manager.onLifecycle(Lifecycle.STARTED, mockActivity2, 456L)

        // when - first activity stops
        manager.onLifecycle(Lifecycle.STOPPED, mockActivity1, 789L)

        // then - background should not be triggered
        verify(exactly = 0) { mockListener.onState(AppState.BACKGROUND, any()) }
    }

    @Test
    fun `onLifecycle STOPPED should trigger background state when all activities are stopped`() {
        // given - both activities are started
        manager.onLifecycle(Lifecycle.STARTED, mockActivity1, 123L)
        manager.onLifecycle(Lifecycle.STARTED, mockActivity2, 456L)

        // when - both activities stop
        manager.onLifecycle(Lifecycle.STOPPED, mockActivity1, 789L)
        manager.onLifecycle(Lifecycle.STOPPED, mockActivity2, 1011L)

        // then - background should be triggered after last activity stops
        verify { mockListener.onState(AppState.BACKGROUND, 1011L) }
    }

    @Test
    fun `onLifecycle should ignore CREATED RESUMED PAUSED DESTROYED events`() {
        // when
        manager.onLifecycle(Lifecycle.CREATED, mockActivity1, 123L)
        manager.onLifecycle(Lifecycle.RESUMED, mockActivity1, 456L)
        manager.onLifecycle(Lifecycle.PAUSED, mockActivity1, 789L)
        manager.onLifecycle(Lifecycle.DESTROYED, mockActivity1, 1011L)

        // then
        verify(exactly = 0) { mockListener.onState(any(), any()) }
    }

    @Test
    fun `onApplicationOpened should trigger install event when determiner returns INSTALL`() {
        // given
        manager.setApplicationInstallDeterminer(mockInstallDeterminer)
        every { mockInstallDeterminer.determine() } returns ApplicationInstallState.INSTALL

        // when
        manager.onApplicationOpened(123L)

        // then
        verify { mockListener.onInstall(123L) }
        verify { mockListener.onOpen(123L) }
    }

    @Test
    fun `onApplicationOpened should trigger update event when determiner returns UPDATE`() {
        // given
        manager.setApplicationInstallDeterminer(mockInstallDeterminer)
        every { mockInstallDeterminer.determine() } returns ApplicationInstallState.UPDATE

        // when
        manager.onApplicationOpened(123L)

        // then
        verify { mockListener.onUpdate(123L) }
        verify { mockListener.onOpen(123L) }
    }

    @Test
    fun `onApplicationOpened should only trigger open event when determiner returns NONE`() {
        // given
        manager.setApplicationInstallDeterminer(mockInstallDeterminer)
        every { mockInstallDeterminer.determine() } returns ApplicationInstallState.NONE

        // when
        manager.onApplicationOpened(123L)

        // then
        verify(exactly = 0) { mockListener.onInstall(any()) }
        verify(exactly = 0) { mockListener.onUpdate(any()) }
        verify { mockListener.onOpen(123L) }
    }

    @Test
    fun `onApplicationOpened should only trigger open event when determiner is not set`() {
        // given - no determiner set

        // when
        manager.onApplicationOpened(123L)

        // then
        verify(exactly = 0) { mockListener.onInstall(any()) }
        verify(exactly = 0) { mockListener.onUpdate(any()) }
        verify { mockListener.onOpen(123L) }
    }

    @Test
    fun `onApplicationOpened should handle NONE determiner result gracefully`() {
        // given
        manager.setApplicationInstallDeterminer(mockInstallDeterminer)
        // mockInstallDeterminer is already configured to return NONE in setUp()

        // when
        manager.onApplicationOpened(123L)

        // then
        verify(exactly = 0) { mockListener.onInstall(any()) }
        verify(exactly = 0) { mockListener.onUpdate(any()) }
        verify { mockListener.onOpen(123L) }
    }

    @Test
    fun `execute should run block directly when no executor is set`() {
        // given - no executor set
        var executed = false

        // when
        manager.onLifecycle(Lifecycle.STARTED, mockActivity1, 123L)

        // then - should execute directly and trigger listener
        verify { mockListener.onState(AppState.FOREGROUND, 123L) }
    }

    @Test
    fun `execute should run block via executor when executor is set`() {
        // given
        val executorSlot = slot<Runnable>()
        every { mockExecutor.execute(capture(executorSlot)) } answers {
            // Simulate async execution
            executorSlot.captured.run()
        }
        manager.setExecutor(mockExecutor)

        // when
        manager.onApplicationOpened(123L)

        // then
        verify { mockExecutor.execute(any()) }
        verify { mockListener.onOpen(123L) }
    }

    @Test
    fun `multiple listeners should all receive events`() {
        // given
        val mockListener2 = mockk<ApplicationStateListener>(relaxed = true)
        manager.addListener(mockListener2)

        // when
        manager.onLifecycle(Lifecycle.STARTED, mockActivity1, 123L)

        // then
        verify { mockListener.onState(AppState.FOREGROUND, 123L) }
        verify { mockListener2.onState(AppState.FOREGROUND, 123L) }
    }

    @Test
    fun `manager without listener should not trigger events`() {
        // given
        val newManager = ApplicationStateManager()

        // when
        newManager.onLifecycle(Lifecycle.STARTED, mockActivity1, 123L)

        // then
        verify(exactly = 0) { mockListener.onState(any(), any()) }
    }

    @Test
    fun `activity lifecycle state management should handle same activity multiple times`() {
        // given
        val activityHash = mockActivity1.hashCode()

        // when - same activity starts multiple times (should not happen in real Android, but test edge case)
        manager.onLifecycle(Lifecycle.STARTED, mockActivity1, 123L)
        manager.onLifecycle(Lifecycle.STARTED, mockActivity1, 456L)

        // then - should only trigger foreground once
        verify(exactly = 1) { mockListener.onState(AppState.FOREGROUND, any()) }

        // when - same activity stops multiple times
        manager.onLifecycle(Lifecycle.STOPPED, mockActivity1, 789L)
        manager.onLifecycle(Lifecycle.STOPPED, mockActivity1, 1011L)

        // then - should trigger background after first stop since it's the only activity
        verify { mockListener.onState(AppState.BACKGROUND, 789L) }
    }
}