package io.hackle.android.internal.lifecycle

import android.app.Activity
import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class LifecycleManagerTest {

    private lateinit var lifecycleManager: LifecycleManager

    @Before
    fun setup() {
        lifecycleManager = LifecycleManager()
    }

    @Test
    fun `receive app state after dispatch start`() {

        val listener = mockk<AppStateChangeListener>()
        lifecycleManager.addListener(listener)

        lifecycleManager.onActivityCreated(mockk(), mockk())
        lifecycleManager.onActivityStarted(mockk())
        lifecycleManager.onActivityResumed(mockk())

        verify { listener wasNot Called }

        val timeInMillis = 12345L
        lifecycleManager.dispatchStart(timeInMillis = timeInMillis)

        verify(exactly = 1) {
            listener.onChanged(AppState.FOREGROUND, timeInMillis)
        }
    }

    @Test
    fun `receive foreground app state`() {
        val listener = mockk<AppStateChangeListener>()
        lifecycleManager.dispatchStart()
        lifecycleManager.addListener(listener)

        lifecycleManager.onActivityCreated(mockk(), mockk())
        lifecycleManager.onActivityStarted(mockk())
        lifecycleManager.onActivityResumed(mockk())

        verify(exactly = 1) {
            listener.onChanged(AppState.FOREGROUND, any())
        }
    }

    @Test
    fun `receive foreground and background app state`() {
        val listener = mockk<AppStateChangeListener>()
        lifecycleManager.dispatchStart()
        lifecycleManager.addListener(listener)

        lifecycleManager.onActivityCreated(mockk(), mockk())
        lifecycleManager.onActivityStarted(mockk())
        lifecycleManager.onActivityResumed(mockk())
        lifecycleManager.onActivityPaused(mockk())
        lifecycleManager.onActivityStopped(mockk())
        lifecycleManager.onActivityDestroyed(mockk())

        verifySequence {
            listener.onChanged(AppState.FOREGROUND, any())
            listener.onChanged(AppState.BACKGROUND, any())
        }
    }

    @Test
    fun `change current app state to foreground when on activity resumed`() {
        assertThat(lifecycleManager.currentState, `is`(AppState.BACKGROUND))
        lifecycleManager.onActivityResumed(mockk())
        assertThat(lifecycleManager.currentState, `is`(AppState.FOREGROUND))
    }

    @Test
    fun `change current app state to background when on activity paused`() {
        assertThat(lifecycleManager.currentState, `is`(AppState.BACKGROUND))
        lifecycleManager.onActivityPaused(mockk())
        assertThat(lifecycleManager.currentState, `is`(AppState.BACKGROUND))
    }

    @Test
    fun `change current app state sequentially`() {
        assertThat(lifecycleManager.currentState, `is`(AppState.BACKGROUND))

        lifecycleManager.onActivityCreated(mockk(), mockk())
        lifecycleManager.onActivityStarted(mockk())
        lifecycleManager.onActivityResumed(mockk())

        assertThat(lifecycleManager.currentState, `is`(AppState.FOREGROUND))

        lifecycleManager.onActivityPaused(mockk())
        lifecycleManager.onActivityStopped(mockk())
        lifecycleManager.onActivityDestroyed(mockk())

        assertThat(lifecycleManager.currentState, `is`(AppState.BACKGROUND))
    }

    @Test
    fun `current activity should not null after activity created`() {
        assertNull(lifecycleManager.currentActivity)

        val activity = mockk<Activity>()
        lifecycleManager.onActivityCreated(activity, mockk())

        assertThat(lifecycleManager.currentActivity, `is`(activity))
    }

    @Test
    fun `current activity should not null after activity started`() {
        assertNull(lifecycleManager.currentActivity)

        val activity = mockk<Activity>()
        lifecycleManager.onActivityCreated(activity, mockk())
        lifecycleManager.onActivityStarted(activity)

        assertThat(lifecycleManager.currentActivity, `is`(activity))
    }

    @Test
    fun `current activity should not null after activity resumed`() {
        assertNull(lifecycleManager.currentActivity)

        val activity = mockk<Activity>()
        lifecycleManager.onActivityCreated(activity, mockk())
        lifecycleManager.onActivityStarted(activity)
        lifecycleManager.onActivityResumed(activity)

        assertThat(lifecycleManager.currentActivity, `is`(activity))
    }

    @Test
    fun `change current activity after activity transition`() {
        assertNull(lifecycleManager.currentActivity)

        val firstActivity = mockk<Activity>()
        lifecycleManager.onActivityCreated(firstActivity, mockk())
        lifecycleManager.onActivityStarted(firstActivity)
        lifecycleManager.onActivityResumed(firstActivity)

        assertThat(lifecycleManager.currentActivity, `is`(firstActivity))

        val secondActivity = mockk<Activity>()
        lifecycleManager.onActivityPaused(firstActivity)
        lifecycleManager.onActivityCreated(secondActivity, mockk())
        lifecycleManager.onActivityStarted(secondActivity)
        lifecycleManager.onActivityResumed(secondActivity)
        lifecycleManager.onActivityStopped(firstActivity)

        assertThat(lifecycleManager.currentActivity, `is`(secondActivity))
    }
}