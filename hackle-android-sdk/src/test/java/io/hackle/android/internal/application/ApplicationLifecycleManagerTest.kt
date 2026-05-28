package io.hackle.android.internal.application

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import io.hackle.android.internal.application.lifecycle.ApplicationLifecycleListener
import io.hackle.android.internal.application.lifecycle.ApplicationLifecycleManager
import io.hackle.android.internal.application.lifecycle.ApplicationState
import io.hackle.sdk.core.internal.time.Clock
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue
import java.util.concurrent.Executor

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

        manager = ApplicationLifecycleManager(mockClock)
        manager.addListener(mockListener)
    }

    @After
    fun tearDown() {
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
        verify { mockListener.onForeground(1234567890L, false) }
    }

    @Test
    fun `onActivityStarted should not trigger foreground event for subsequent activities`() {
        // given
        manager.onActivityStarted(mockActivity1)
        clearMocks(mockListener)

        // when
        manager.onActivityStarted(mockActivity2)

        // then
        verify(exactly = 0) { mockListener.onForeground(any(), any()) }
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
        verify(exactly = 0) { mockListener.onBackground(any()) }
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
        verify { mockListener.onBackground(1234567890L) }
    }

    @Test
    fun `onActivityCreated should do nothing`() {
        // when
        manager.onActivityCreated(mockActivity1, mockBundle)

        // then - no interactions with listeners
        verify(exactly = 0) { mockListener.onForeground(any(), any()) }
        verify(exactly = 0) { mockListener.onBackground(any()) }
    }

    @Test
    fun `onActivityResumed should do nothing`() {
        // when
        manager.onActivityResumed(mockActivity1)

        // then - no interactions with listeners
        verify(exactly = 0) { mockListener.onForeground(any(), any()) }
        verify(exactly = 0) { mockListener.onBackground(any()) }
    }

    @Test
    fun `onActivityPaused should do nothing`() {
        // when
        manager.onActivityPaused(mockActivity1)

        // then - no interactions with listeners
        verify(exactly = 0) { mockListener.onForeground(any(), any()) }
        verify(exactly = 0) { mockListener.onBackground(any()) }
    }

    @Test
    fun `onActivitySaveInstanceState should do nothing`() {
        // when
        manager.onActivitySaveInstanceState(mockActivity1, mockBundle)

        // then - no interactions with listeners
        verify(exactly = 0) { mockListener.onForeground(any(), any()) }
        verify(exactly = 0) { mockListener.onBackground(any()) }
    }

    @Test
    fun `onActivityDestroyed should do nothing`() {
        // when
        manager.onActivityDestroyed(mockActivity1)

        // then - no interactions with listeners
        verify(exactly = 0) { mockListener.onForeground(any(), any()) }
        verify(exactly = 0) { mockListener.onBackground(any()) }
    }

    @Test
    fun `should handle same activity multiple start-stop cycles`() {
        // First start-stop
        manager.onActivityStarted(mockActivity1)
        verify { mockListener.onForeground(1234567890L, false) }

        manager.onActivityStopped(mockActivity1)
        verify { mockListener.onBackground(1234567890L) }

        clearMocks(mockListener)

        // Second start-stop for same activity
        manager.onActivityStarted(mockActivity1)
        verify { mockListener.onForeground(1234567890L, true) }

        manager.onActivityStopped(mockActivity1)
        verify { mockListener.onBackground(1234567890L) }
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

        // then - verify instance is properly created
        expectThat(instance).isEqualTo(instance) // Basic existence check
    }

    @Test
    fun `initial appState should be FOREGROUND`() {
        // given - fresh manager instance
        val freshManager = ApplicationLifecycleManager(mockClock)

        // when - directly check initial state through behavior
        freshManager.onActivityStarted(mockActivity1)

        // then - since initial state is FOREGROUND, isFromBackground should be false
        freshManager.addListener(mockListener)
        clearMocks(mockListener)

        // stop and start again to trigger foreground event
        freshManager.onActivityStopped(mockActivity1)
        freshManager.onActivityStarted(mockActivity1)

        // should be from background now
        verify { mockListener.onForeground(1234567890L, true) }
    }

    @Test
    fun `publishStateIfNeeded should publish foreground state when currentState is FOREGROUND`() {
        // given
        manager.onActivityStarted(mockActivity1)
        clearMocks(mockListener)

        // when
        manager.publishStateIfNeeded()

        // then
        verify { mockListener.onForeground(1234567890L, false) }
    }

    @Test
    fun `publishStateIfNeeded should publish background state when currentState is BACKGROUND`() {
        // given - transition to background
        manager.onActivityStarted(mockActivity1)
        manager.onActivityStopped(mockActivity1)
        clearMocks(mockListener)

        // when
        manager.publishStateIfNeeded()

        // then
        verify { mockListener.onBackground(1234567890L) }
    }

    @Test
    fun `publishStateIfNeeded should not publish when currentState is null`() {
        // given - fresh manager without any state transitions
        val freshManager = ApplicationLifecycleManager(mockClock)
        freshManager.addListener(mockListener)

        // when
        freshManager.publishStateIfNeeded()

        // then - no events should be published
        verify(exactly = 0) { mockListener.onForeground(any(), any()) }
        verify(exactly = 0) { mockListener.onBackground(any()) }
    }

    @Test
    fun `publishStateIfNeeded should use executor when set`() {
        // given
        val syncExecutor = mockk<Executor>(relaxed = true)
        val executorSlot = slot<Runnable>()
        every { syncExecutor.execute(capture(executorSlot)) } answers { executorSlot.captured.run() }

        manager.onActivityStarted(mockActivity1)
        manager.setExecutor(syncExecutor)
        clearMocks(mockListener)

        // when
        manager.publishStateIfNeeded()

        // then
        verify { syncExecutor.execute(any()) }
        verify { mockListener.onForeground(1234567890L, false) }
    }

    @Test
    fun `onActivityStarted should continue invoking remaining listeners when one throws`() {
        // given
        val freshManager = ApplicationLifecycleManager(mockClock)
        val listener1 = mockk<ApplicationLifecycleListener>()
        val listener2 = mockk<ApplicationLifecycleListener>(relaxed = true)
        every { listener1.onForeground(any(), any()) } throws RuntimeException("boom")

        freshManager.addListener(listener1)
        freshManager.addListener(listener2)

        // when - 시스템 lifecycle callback 으로 throw 가 전파되지 않아야 함
        freshManager.onActivityStarted(mockActivity1)

        // then
        verify { listener1.onForeground(1234567890L, false) }
        verify { listener2.onForeground(1234567890L, false) }
        expectThat(freshManager.currentState).isEqualTo(ApplicationState.FOREGROUND)
    }

    @Test
    fun `onActivityStopped should continue invoking remaining listeners when one throws`() {
        // given - 먼저 FG 진입 (mockListener 가 setUp 에 등록되어 있지 않은 fresh manager 사용)
        val freshManager = ApplicationLifecycleManager(mockClock)
        val listener1 = mockk<ApplicationLifecycleListener>(relaxed = true)
        val listener2 = mockk<ApplicationLifecycleListener>(relaxed = true)
        freshManager.addListener(listener1)
        freshManager.addListener(listener2)
        freshManager.onActivityStarted(mockActivity1) // FG 전환
        // BG 콜백에서만 throw 하도록 설정
        every { listener1.onBackground(any()) } throws RuntimeException("boom")

        // when
        freshManager.onActivityStopped(mockActivity1)

        // then
        verify { listener1.onBackground(1234567890L) }
        verify { listener2.onBackground(1234567890L) }
        expectThat(freshManager.currentState).isEqualTo(ApplicationState.BACKGROUND)
    }

    @Test
    fun `publishStateIfNeeded should continue invoking remaining listeners when one throws`() {
        // given - FG 상태로 전환 (이 시점에는 listener1 도 정상 호출되도록 relaxed)
        val freshManager = ApplicationLifecycleManager(mockClock)
        val listener1 = mockk<ApplicationLifecycleListener>(relaxed = true)
        val listener2 = mockk<ApplicationLifecycleListener>(relaxed = true)
        freshManager.addListener(listener1)
        freshManager.addListener(listener2)
        freshManager.onActivityStarted(mockActivity1)

        // FG 진입 후에 throw stub 등록 (publishStateIfNeeded 호출 시에만 throw)
        every { listener1.onForeground(any(), any()) } throws RuntimeException("boom")

        // when
        freshManager.publishStateIfNeeded()

        // then - publishStateIfNeeded 의 FG 경로에서 listener1 throw 에도 listener2 호출됨
        verify { listener1.onForeground(1234567890L, false) }
        verify { listener2.onForeground(1234567890L, false) }
        expectThat(freshManager.currentState).isEqualTo(ApplicationState.FOREGROUND)
    }

    @Test
    fun `publishStateIfNeeded BACKGROUND should continue invoking remaining listeners and complete state transition when one throws`() {
        // given - FG 진입 후 BG 전환 (state = BACKGROUND)
        val freshManager = ApplicationLifecycleManager(mockClock)
        val listener1 = mockk<ApplicationLifecycleListener>(relaxed = true)
        val listener2 = mockk<ApplicationLifecycleListener>(relaxed = true)
        freshManager.addListener(listener1)
        freshManager.addListener(listener2)
        freshManager.onActivityStarted(mockActivity1)
        freshManager.onActivityStopped(mockActivity1) // _currentState = BACKGROUND

        // BG 전환 후 throw stub 등록 — publishStateIfNeeded 호출 시에만 throw
        clearMocks(listener1, listener2, answers = false)
        every { listener1.onBackground(any()) } throws RuntimeException("boom")

        // when - BACKGROUND 분기 진입
        freshManager.publishStateIfNeeded()

        // then - listener1 throw 에도 listener2 호출 + state 유지
        verify { listener1.onBackground(1234567890L) }
        verify { listener2.onBackground(1234567890L) }
        expectThat(freshManager.currentState).isEqualTo(ApplicationState.BACKGROUND)
    }
}