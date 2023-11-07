package io.hackle.android.internal.lifecycle

import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.util.concurrent.Executor

class HackleActivityLifecycleCallbacksTest {

    @Test
    fun `onActivityResumed - FOREGROUND`() {
        val executor = ExecutorStub()
        val appStateManager = AppStateManager()
        val callbacks = HackleActivityLifecycleCallbacks(executor, appStateManager)

        val listener = mockk<AppStateChangeListener>()
        callbacks.addListener(listener)

        callbacks.onEvent(LifecycleManager.Event.ON_RESUME, mockk())

        expectThat(executor.executeCount) isEqualTo 1
        expectThat(appStateManager.currentState) isEqualTo AppState.FOREGROUND
        verify(exactly = 1) {
            listener.onChanged(AppState.FOREGROUND, any())
        }
    }

    @Test
    fun `onActivityPaused - BACKGROUND`() {
        val executor = ExecutorStub()
        val appStateManager = AppStateManager().also {
            it.onChanged(AppState.FOREGROUND, 42)
        }
        val callbacks = HackleActivityLifecycleCallbacks(executor, appStateManager)

        val listener = mockk<AppStateChangeListener>()
        callbacks.addListener(listener)

        callbacks.onEvent(LifecycleManager.Event.ON_PAUSE, mockk())

        expectThat(executor.executeCount) isEqualTo 1
        expectThat(appStateManager.currentState) isEqualTo AppState.BACKGROUND
        verify(exactly = 1) {
            listener.onChanged(AppState.BACKGROUND, any())
        }
    }

    @Test
    fun `do not call any app state`() {
        val executor = ExecutorStub()
        val appStateManager = AppStateManager()
        val callbacks = HackleActivityLifecycleCallbacks(executor, appStateManager)

        val listener = mockk<AppStateChangeListener>()
        callbacks.addListener(listener)

        callbacks.onEvent(LifecycleManager.Event.ON_CREATE, mockk())
        callbacks.onEvent(LifecycleManager.Event.ON_START, mockk())
        callbacks.onEvent(LifecycleManager.Event.ON_STOP, mockk())
        callbacks.onEvent(LifecycleManager.Event.ON_DESTROY, mockk())

        expectThat(executor.executeCount) isEqualTo 0
        expectThat(appStateManager.currentState) isEqualTo AppState.BACKGROUND
        verify {
            listener wasNot Called
        }
    }

    class ExecutorStub : Executor {
        var executeCount = 0
        override fun execute(command: Runnable) {
            command.run()
            executeCount++
        }
    }
}