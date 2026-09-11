package io.hackle.android.internal.screen

import android.app.Activity
import io.hackle.android.internal.activity.lifecycle.ActivityProvider
import io.hackle.android.internal.activity.lifecycle.ActivityState
import io.hackle.android.internal.activity.lifecycle.ActivityLifecycle
import io.hackle.android.internal.user.local.LocalUserManager
import io.hackle.sdk.common.Screen
import io.hackle.sdk.common.User
import io.mockk.every
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

    private lateinit var userManager: LocalUserManager
    private lateinit var listener: ScreenListener
    private lateinit var sut: ScreenManager

    @Before
    fun before() {
        userManager = mockk()
        every { userManager.currentUser } returns User.of("test")
        listener = mockk(relaxed = true)
        sut = screenManager(manualScreenViewDedupEnabled = true)
        sut.addListener(listener)
    }

    private fun screenManager(manualScreenViewDedupEnabled: Boolean): ScreenManager {
        return ScreenManager(
            userManager,
            object : ActivityProvider {
                override val currentActivity: Activity?
                    get() = activity
                override val currentState: ActivityState
                    get() = ActivityState.ACTIVE
            },
            manualScreenViewDedupEnabled
        )
    }

    @Test
    fun `setCurrentScreen - first screen`() {
        val screen = Screen("name", "class")
        sut.setCurrentScreen(screen, 42)

        expectThat(sut.currentScreen).isEqualTo(screen)
        verify(exactly = 1) {
            listener.onScreenStarted(null, screen, any(), 42)
        }
        verify(exactly = 0) {
            listener.onScreenEnded(any(), any(), any())
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
            listener.onScreenStarted(null, currentScreen, any(), 42)
        }
        verify(exactly = 0) {
            listener.onScreenEnded(any(), any(), any())
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
            listener.onScreenStarted(any(), any(), any(), any())
        }
        verify(exactly = 1) {
            listener.onScreenEnded(any(), any(), any())
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
        sut.onLifecycle(ActivityLifecycle.RESUMED, TestActivity(), 42)
        expectThat(sut.currentScreen).isEqualTo(Screen("TestActivity", "TestActivity"))
    }

    @Test
    fun `onLifecycle - do nothing`() {
        sut.onLifecycle(ActivityLifecycle.PAUSED, TestActivity(), 42)
        sut.onLifecycle(ActivityLifecycle.CREATED, TestActivity(), 42)
        sut.onLifecycle(ActivityLifecycle.STOPPED, TestActivity(), 42)
        sut.onLifecycle(ActivityLifecycle.STARTED, TestActivity(), 42)
        sut.onLifecycle(ActivityLifecycle.DESTROYED, TestActivity(), 42)
        expectThat(sut.currentScreen).isNull()
    }

    @Test
    fun `setCurrentScreen - manualScreenViewDedupEnabled false - current screen == new screen`() {
        // given
        val sut = screenManager(manualScreenViewDedupEnabled = false)
        val listener = mockk<ScreenListener>(relaxed = true)
        sut.addListener(listener)
        val currentScreen = Screen("name", "class")
        val newScreen = Screen("name", "class")
        sut.setCurrentScreen(currentScreen, 42)

        // when
        sut.setCurrentScreen(newScreen, 43)

        // then
        expectThat(sut.currentScreen).isSameInstanceAs(newScreen)
        verify(exactly = 1) {
            listener.onScreenStarted(null, currentScreen, any(), 42)
        }
        verify(exactly = 1) {
            listener.onScreenEnded(currentScreen, any(), 43)
        }
        verify(exactly = 1) {
            listener.onScreenStarted(currentScreen, newScreen, any(), 43)
        }
    }

    @Test
    fun `setCurrentScreen - manualScreenViewDedupEnabled false - current screen != new screen`() {
        // given
        val sut = screenManager(manualScreenViewDedupEnabled = false)
        val listener = mockk<ScreenListener>(relaxed = true)
        sut.addListener(listener)
        val currentScreen = Screen("name", "class")
        val newScreen = Screen("new_name", "class")
        sut.setCurrentScreen(currentScreen, 42)

        // when
        sut.setCurrentScreen(newScreen, 43)

        // then
        expectThat(sut.currentScreen).isSameInstanceAs(newScreen)
        verify(exactly = 2) {
            listener.onScreenStarted(any(), any(), any(), any())
        }
        verify(exactly = 1) {
            listener.onScreenEnded(any(), any(), any())
        }
    }

    @Test
    fun `onLifecycle RESUMED - manualScreenViewDedupEnabled false - always dedup`() {
        // given
        val sut = screenManager(manualScreenViewDedupEnabled = false)
        val listener = mockk<ScreenListener>(relaxed = true)
        sut.addListener(listener)

        // when
        sut.onLifecycle(ActivityLifecycle.RESUMED, TestActivity(), 42)
        sut.onLifecycle(ActivityLifecycle.RESUMED, TestActivity(), 43)

        // then
        expectThat(sut.currentScreen).isEqualTo(Screen("TestActivity", "TestActivity"))
        verify(exactly = 1) {
            listener.onScreenStarted(any(), any(), any(), any())
        }
        verify(exactly = 0) {
            listener.onScreenEnded(any(), any(), any())
        }
    }

    private class TestActivity : Activity()
}
