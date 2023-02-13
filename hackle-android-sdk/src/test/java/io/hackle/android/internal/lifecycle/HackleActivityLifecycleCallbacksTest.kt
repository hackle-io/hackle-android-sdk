package io.hackle.android.internal.lifecycle

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
        val callbacks = HackleActivityLifecycleCallbacks(executor)

        val listener = mockk<AppStateChangeListener>()
        callbacks.addListener(listener)


        callbacks.onActivityResumed(mockk())

        expectThat(executor.executeCount) isEqualTo 1
        verify(exactly = 1) {
            listener.onChanged(AppState.FOREGROUND, any())
        }
    }

    @Test
    fun `onActivityPaused - BACKGROUND`() {
        val executor = ExecutorStub()
        val callbacks = HackleActivityLifecycleCallbacks(executor)

        val listener = mockk<AppStateChangeListener>()
        callbacks.addListener(listener)


        callbacks.onActivityPaused(mockk())

        expectThat(executor.executeCount) isEqualTo 1
        verify(exactly = 1) {
            listener.onChanged(AppState.BACKGROUND, any())
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