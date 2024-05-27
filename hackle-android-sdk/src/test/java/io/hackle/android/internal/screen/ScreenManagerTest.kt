package io.hackle.android.internal.screen

import android.app.Activity
import io.hackle.android.internal.lifecycle.ActivityProvider
import io.hackle.android.internal.lifecycle.Lifecycle
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import strikt.assertions.isSameInstanceAs

class ScreenManagerTest {

    private var activity: Activity? = null

    private lateinit var listener: ScreenListener
    private lateinit var sut: ScreenManager

    @Before
    fun before() {
        listener = mockk(relaxed = true)
        sut = ScreenManager(object : ActivityProvider {
            override val currentActivity: Activity?
                get() = activity
        })
        sut.addListener(listener)
    }

    @Test
    fun `setCurrentScreen - first screen`() {
        val screen = Screen("name", "class")
        sut.setCurrentScreen(screen, 42)

        expectThat(sut.currentScreen).isEqualTo(screen)
        verify(exactly = 1) {
            listener.onScreenStarted(null, screen, 42)
        }
        verify(exactly = 0) {
            listener.onScreenEnded(any(), any())
        }
    }


    @Test
    fun `setCurrentScreen - current screen == new screen`() {
        // given
        val currentScreen = Screen("name", "class")
        val newScreen = Screen("name", "class")
        sut.setCurrentScreen(currentScreen, 42)

        // when
        sut.setCurrentScreen(newScreen, 42)

        // then
        expectThat(sut.currentScreen).isSameInstanceAs(newScreen)
        verify(exactly = 1) {
            listener.onScreenStarted(null, currentScreen, 42)
        }
        verify(exactly = 0) {
            listener.onScreenEnded(any(), any())
        }
    }

    @Test
    fun `setCurrentScreen - current screen != new screen`() {
        // given
        val currentScreen = Screen("name", "class")
        val newScreen = Screen("new_name", "class")
        sut.setCurrentScreen(currentScreen, 42)

        // when
        sut.setCurrentScreen(newScreen, 42)

        // then
        expectThat(sut.currentScreen).isSameInstanceAs(newScreen)
        verify(exactly = 2) {
            listener.onScreenStarted(any(), any(), any())
        }
        verify(exactly = 1) {
            listener.onScreenEnded(any(), any())
        }
    }

    @Test
    fun `resolveScreenClass - screenClass != null`() {
        expectThat(sut.resolveScreenClass("TestActivity")).isEqualTo("TestActivity")
    }

    @Test
    fun `resolveScreenClass - screenClass == null && currentActivity == null`() {
        expectThat(sut.resolveScreenClass(null)).isEqualTo("Unknown")
    }

    @Test
    fun `resolveScreenClass - screenClass == null && currentActivity != null`() {
        activity = TestActivity()
        expectThat(sut.resolveScreenClass(null)).isEqualTo("TestActivity")
    }

    @Test
    fun `onLifecycle - RESUME`() {
        sut.onLifecycle(Lifecycle.RESUMED, TestActivity(), 42)
        expectThat(sut.currentScreen).isEqualTo(Screen("TestActivity", "TestActivity"))
    }

    @Test
    fun `onLifecycle - do nothing`() {
        sut.onLifecycle(Lifecycle.PAUSED, TestActivity(), 42)
        sut.onLifecycle(Lifecycle.CREATED, TestActivity(), 42)
        sut.onLifecycle(Lifecycle.STOPPED, TestActivity(), 42)
        sut.onLifecycle(Lifecycle.STARTED, TestActivity(), 42)
        sut.onLifecycle(Lifecycle.DESTROYED, TestActivity(), 42)
        expectThat(sut.currentScreen).isNull()
    }

    private class TestActivity : Activity()
}
