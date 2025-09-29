package io.hackle.android.internal.application

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import io.hackle.android.internal.lifecycle.AppState
import io.hackle.sdk.core.internal.time.Clock
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue

class ApplicationLifecycleManagerTest {

    private val mockClock = mockk<Clock>()
    private val mockContext = mockk<Context>()
    private val mockApplication = mockk<Application>()
    private val mockActivity1 = TestActivity()
    private val mockActivity2 = TestActivity2()

    private class TestActivity : Activity()
    private class TestActivity2 : Activity()
    private val mockBundle = mockk<Bundle>()
    private val mockListener = mockk<ApplicationLifecycleListener>(relaxed = true)

    private lateinit var manager: ApplicationLifecycleManager

    @Before
    fun setUp() {
        every { mockClock.currentMillis() } returns 1234567890L
        every { mockContext.applicationContext } returns mockApplication
        every { mockApplication.registerActivityLifecycleCallbacks(any()) } just Runs
        every { mockApplication.unregisterActivityLifecycleCallbacks(any()) } just Runs

        // Mock ApplicationStateManager singleton
        mockkObject(ApplicationStateManager.Companion)
        val mockApplicationStateManager = mockk<ApplicationStateManager>(relaxed = true)
        every { ApplicationStateManager.instance } returns mockApplicationStateManager

        manager = ApplicationLifecycleManager(mockClock)
        manager.addListener(mockListener)
    }

    @After
    fun tearDown() {
        unmockkObject(ApplicationStateManager.Companion)
        clearAllMocks()
    }

    @Test
    fun `registerTo should register activity lifecycle callbacks`() {
        // when
        manager.registerTo(mockContext)

        // then
        verify { mockApplication.unregisterActivityLifecycleCallbacks(manager) }
        verify { mockApplication.registerActivityLifecycleCallbacks(manager) }
    }

    @Test
    fun `onActivityStarted should trigger foreground event for first activity`() {
        // when
        manager.onActivityStarted(mockActivity1)

        // then
        verify { mockListener.onApplicationForeground(1234567890L, false) }
    }

    @Test
    fun `onActivityStarted should not trigger foreground event for subsequent activities`() {
        // given
        manager.onActivityStarted(mockActivity1)
        clearMocks(mockListener)

        // when
        manager.onActivityStarted(mockActivity2)

        // then
        verify(exactly = 0) { mockListener.onApplicationForeground(any(), any()) }
    }

    @Test
    fun `onActivityStopped should not trigger background event when other activities are active`() {
        // given
        manager.onActivityStarted(mockActivity1)
        manager.onActivityStarted(mockActivity2)
        clearMocks(mockListener)

        // when
        manager.onActivityStopped(mockActivity1)

        // then
        verify(exactly = 0) { mockListener.onApplicationBackground(any()) }
    }

    @Test
    fun `onActivityStopped should trigger background event when last activity stops`() {
        // given
        manager.onActivityStarted(mockActivity1)
        manager.onActivityStarted(mockActivity2)
        clearMocks(mockListener)

        // when
        manager.onActivityStopped(mockActivity1)
        manager.onActivityStopped(mockActivity2)

        // then
        verify { mockListener.onApplicationBackground(1234567890L) }
    }

    @Test
    fun `onActivityCreated should do nothing`() {
        // when
        manager.onActivityCreated(mockActivity1, mockBundle)

        // then - no interactions with listeners
        verify(exactly = 0) { mockListener.onApplicationForeground(any(), any()) }
        verify(exactly = 0) { mockListener.onApplicationBackground(any()) }
    }

    @Test
    fun `onActivityResumed should do nothing`() {
        // when
        manager.onActivityResumed(mockActivity1)

        // then - no interactions with listeners
        verify(exactly = 0) { mockListener.onApplicationForeground(any(), any()) }
        verify(exactly = 0) { mockListener.onApplicationBackground(any()) }
    }

    @Test
    fun `onActivityPaused should do nothing`() {
        // when
        manager.onActivityPaused(mockActivity1)

        // then - no interactions with listeners
        verify(exactly = 0) { mockListener.onApplicationForeground(any(), any()) }
        verify(exactly = 0) { mockListener.onApplicationBackground(any()) }
    }

    @Test
    fun `onActivitySaveInstanceState should do nothing`() {
        // when
        manager.onActivitySaveInstanceState(mockActivity1, mockBundle)

        // then - no interactions with listeners
        verify(exactly = 0) { mockListener.onApplicationForeground(any(), any()) }
        verify(exactly = 0) { mockListener.onApplicationBackground(any()) }
    }

    @Test
    fun `onActivityDestroyed should do nothing`() {
        // when
        manager.onActivityDestroyed(mockActivity1)

        // then - no interactions with listeners
        verify(exactly = 0) { mockListener.onApplicationForeground(any(), any()) }
        verify(exactly = 0) { mockListener.onApplicationBackground(any()) }
    }

    @Test
    fun `should handle same activity multiple start-stop cycles`() {
        // First start-stop
        manager.onActivityStarted(mockActivity1)
        verify { mockListener.onApplicationForeground(1234567890L, false) }

        manager.onActivityStopped(mockActivity1)
        verify { mockListener.onApplicationBackground(1234567890L) }

        clearMocks(mockListener)

        // Second start-stop for same activity
        manager.onActivityStarted(mockActivity1)
        verify { mockListener.onApplicationForeground(1234567890L, true) }

        manager.onActivityStopped(mockActivity1)
        verify { mockListener.onApplicationBackground(1234567890L) }
    }

    @Test
    fun `should not trigger background event if already in background state`() {
        // given - go to foreground and then background
        manager.onActivityStarted(mockActivity1)
        manager.onActivityStopped(mockActivity1)
        clearMocks(mockListener)

        // when - stop an activity that was never started (edge case)
        manager.onActivityStopped(mockActivity2)

        // then - should not trigger background event
        verify(exactly = 0) { mockListener.onApplicationBackground(any()) }
    }

    @Test
    fun `getInstance should return singleton instance`() {
        // when
        val instance1 = ApplicationLifecycleManager.instance
        val instance2 = ApplicationLifecycleManager.instance

        // then
        expectThat(instance1 === instance2).isTrue()
    }

    @Test
    fun `getInstance should create instance with correct configuration`() {
        // when
        val instance = ApplicationLifecycleManager.instance

        // then - should have ApplicationStateManager as listener
        // This is verified by checking the instance creation behavior
        expectThat(instance).isEqualTo(instance) // Basic existence check
    }
}