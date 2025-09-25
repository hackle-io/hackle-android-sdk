package io.hackle.android.internal.lifecycle

import io.hackle.android.internal.time.FixedClock
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isSameInstanceAs
import java.util.concurrent.Executor

class AppStateManagerTest {
    @Test
    fun `singleton instance`() {
        val instance = ActivityStateManager.instance
        expectThat(instance) isSameInstanceAs ActivityStateManager.instance
    }

    @Test
    fun `onLifecycle - RESUME`() {
        // given
        val sut = ActivityStateManager(FixedClock(42))
        val listener = mockk<ActivityStateListener>()
        sut.addListener(listener)

        // when
        sut.onLifecycle(Lifecycle.RESUMED, mockk(), 100)

        // then
        expectThat(sut.currentState).isEqualTo(AppState.FOREGROUND)
        verify(exactly = 1) {
            listener.onState(AppState.FOREGROUND, 100)
        }
    }

    @Test
    fun `onLifecycle - PAUSED`() {
        // given
        val sut = ActivityStateManager(FixedClock(42))
        val listener = mockk<ActivityStateListener>()
        sut.addListener(listener)

        // when
        sut.onLifecycle(Lifecycle.PAUSED, mockk(), 100)

        // then
        expectThat(sut.currentState).isEqualTo(AppState.BACKGROUND)
        verify(exactly = 1) {
            listener.onState(AppState.BACKGROUND, 100)
        }
    }

    @Test
    fun `onLifecycle - do nothing`() {
        // given
        val sut = ActivityStateManager(FixedClock(42))
        val listener = mockk<ActivityStateListener>()
        sut.addListener(listener)

        // when
        sut.onLifecycle(Lifecycle.CREATED, mockk(), 100)
        sut.onLifecycle(Lifecycle.STARTED, mockk(), 100)
        sut.onLifecycle(Lifecycle.STOPPED, mockk(), 100)
        sut.onLifecycle(Lifecycle.DESTROYED, mockk(), 100)

        // then
        expectThat(sut.currentState).isEqualTo(AppState.BACKGROUND)
        verify {
            listener wasNot Called
        }
    }

    @Test
    fun `publishStateIfNeeded - when currentState is null then do nothing`() {
        // given
        val sut = ActivityStateManager(FixedClock(42))
        val listener = mockk<ActivityStateListener>()
        sut.addListener(listener)

        // when
        sut.publishStateIfNeeded()

        // then
        verify {
            listener wasNot Called
        }
    }

    @Test
    fun `publishStateIfNeeded - when currentState is not null then publish state`() {
        // given
        val sut = ActivityStateManager(FixedClock(42))
        sut.onLifecycle(Lifecycle.RESUMED, mockk(), 100)

        // when
        val listener = mockk<ActivityStateListener>()
        sut.addListener(listener)
        sut.publishStateIfNeeded()

        // then
        verify(exactly = 1) {
            listener.onState(AppState.FOREGROUND, 42)
        }
    }

    @Test
    fun `executor`() {
        val sut = ActivityStateManager(FixedClock(42))
        val executor = mockk<Executor>()
        every { executor.execute(any()) } answers { firstArg<Runnable>().run() }

        sut.setExecutor(executor)

        val listener = mockk<ActivityStateListener>()
        sut.addListener(listener)

        sut.onLifecycle(Lifecycle.RESUMED, mockk(), 100)
        verify(exactly = 1) {
            listener.onState(AppState.FOREGROUND, 100)
        }
        verify(exactly = 1) {
            executor.execute(any())
        }
    }
}
